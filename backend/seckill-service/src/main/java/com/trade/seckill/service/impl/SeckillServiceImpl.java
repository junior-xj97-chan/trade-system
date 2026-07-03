package com.trade.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.R;
import com.trade.seckill.config.SeckillRabbitMQConfig;
import com.trade.seckill.dto.CreateOrderRequest;
import com.trade.seckill.dto.SeckillMessage;
import com.trade.seckill.entity.SeckillActivity;
import com.trade.seckill.entity.SeckillGoods;
import com.trade.seckill.entity.SeckillOrder;
import com.trade.seckill.feign.AccountFeignClient;
import com.trade.seckill.feign.OrderFeignClient;
import com.trade.seckill.feign.ProductFeignClient;
import com.trade.seckill.mapper.SeckillActivityMapper;
import com.trade.seckill.mapper.SeckillGoodsMapper;
import com.trade.seckill.mapper.SeckillOrderMapper;
import com.trade.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final SeckillGoodsMapper goodsMapper;
    private final SeckillOrderMapper orderMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> deductStockScript;
    private final RabbitTemplate rabbitTemplate;
    private final ProductFeignClient productFeignClient;
    private final AccountFeignClient accountFeignClient;
    private final OrderFeignClient orderFeignClient;

    @Value("${seckill.salt}")
    private String salt;

    @Value("${seckill.path-expire-minutes}")
    private int pathExpireMinutes;

    @Value("${seckill.result-cache-minutes}")
    private int resultCacheMinutes;

    @Value("${seckill.pay-max-retry-count}")
    private int payMaxRetryCount;

    @Override
    public String getSeckillPath(Long activityId, Long goodsId, Long userId) {
        SeckillActivity activity = validateActivity(activityId);
        validateGoods(goodsId, activityId);
        validateProductCategory(goodsId);

        String path = generateMd5Path(userId, activityId, goodsId);
        String pathKey = "seckill:activity:" + activityId + ":path:" + userId + ":" + goodsId;
        redisTemplate.opsForValue().set(pathKey, path, Duration.ofMinutes(pathExpireMinutes));

        log.info("【秒杀路径】生成路径成功: userId={}, activityId={}, goodsId={}", userId, activityId, goodsId);
        return path;
    }

    @Override
    public String executeSeckill(Long activityId, Long goodsId, Long userId) {
        // 第2层：验证秒杀路径（由 controller 验证后传入）
        // 第3层：Redis SETNX 去重
        String setnxKey = "seckill:activity:" + activityId + ":product:" + goodsId + ":user:" + userId;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(setnxKey, "1", Duration.ofDays(1));
        if (Boolean.FALSE.equals(isNew)) {
            SeckillOrder existing = orderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>()
                    .eq(SeckillOrder::getActivityId, activityId)
                    .eq(SeckillOrder::getUserId, userId)
                    .eq(SeckillOrder::getGoodsId, goodsId)
            );
            if (existing != null) {
                switch (existing.getStatus()) {
                    case 1: throw new BusinessException(4001, "您有未支付的订单，请先支付");
                    case 2: throw new BusinessException(4002, "您已成功购买该商品");
                    case 3: throw new BusinessException(4003, "您已超时取消该订单，不可再次参与");
                }
            }
        }

        // 第4层：Redis Lua 原子扣库存
        String stockKey = "seckill:activity:" + activityId + ":stock:" + goodsId;
        Long result = redisTemplate.execute(deductStockScript, Collections.singletonList(stockKey));
        if (result == null || result == 0) {
            redisTemplate.delete(setnxKey);
            throw new BusinessException(4004, "秒杀结束，商品已被抢光");
        }

        // 发送 MQ 消息，异步下单
        SeckillMessage message = SeckillMessage.builder()
            .userId(userId)
            .activityId(activityId)
            .goodsId(goodsId)
            .productId(getProductId(goodsId))
            .timestamp(LocalDateTime.now())
            .build();

        rabbitTemplate.convertAndSend(SeckillRabbitMQConfig.SECKILL_EXCHANGE,
            SeckillRabbitMQConfig.SECKILL_ROUTING_KEY, message);

        log.info("【秒杀执行】排队成功: userId={}, activityId={}, goodsId={}", userId, activityId, goodsId);
        return "排队中，请稍后查询秒杀结果";
    }

    @Override
    public Integer getSeckillResult(Long activityId, Long goodsId, Long userId) {
        String resultKey = "seckill:activity:" + activityId + ":result:" + userId + ":" + goodsId;
        Object cached = redisTemplate.opsForValue().get(resultKey);
        if (cached != null) {
            return (Integer) cached;
        }

        SeckillOrder order = orderMapper.selectOne(
            new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getActivityId, activityId)
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getGoodsId, goodsId)
        );

        if (order != null) {
            int status = order.getStatus();
            redisTemplate.opsForValue().set(resultKey, status, Duration.ofMinutes(resultCacheMinutes));
            return status;
        }
        return -1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processSeckillOrder(SeckillMessage message) {
        Long goodsId = message.getGoodsId();

        // 第5层：DB 乐观锁减库存
        int updated = goodsMapper.deductStock(goodsId);
        if (updated == 0) {
            log.warn("【异步下单】DB库存不足: goodsId={}", goodsId);
            throw new BusinessException(4004, "库存不足");
        }

        SeckillOrder order = new SeckillOrder();
        order.setActivityId(message.getActivityId());
        order.setUserId(message.getUserId());
        order.setGoodsId(message.getGoodsId());
        order.setProductId(message.getProductId());
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setSeckillPrice(getSeckillPrice(message.getGoodsId()));
        order.setStatus(1);
        orderMapper.insert(order);
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class, name = "seckill-pay")
    public void paySeckillOrder(Long orderId, Long userId) {
        SeckillOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
        }

        // 校验支付重试次数
        String retryKey = "seckill:pay:retry:" + order.getOrderNo();
        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount != null && retryCount >= payMaxRetryCount) {
            cancelTimeoutOrder(orderId);
            throw new BusinessException(4005, "支付重试次数已达上限，订单已取消");
        }

        // 扣减余额
        R<Void> deductResult = accountFeignClient.deductBalance(order.getUserId(), order.getSeckillPrice());
        if (!deductResult.isSuccess()) {
            // 记录重试次数
            redisTemplate.opsForValue().increment(retryKey);
            throw new BusinessException(BizCode.BALANCE_NOT_ENOUGH);
        }

        // 创建正式订单
        CreateOrderRequest createRequest = new CreateOrderRequest();
        createRequest.setUserId(order.getUserId());
        createRequest.setProductId(order.getProductId());
        createRequest.setPrice(order.getSeckillPrice());
        createRequest.setQuantity(1);
        createRequest.setSource(2);

        R<Long> orderResult = orderFeignClient.createOrder(createRequest);
        if (!orderResult.isSuccess()) {
            throw new BusinessException(4006, "创建正式订单失败");
        }

        // 更新秒杀订单
        order.setStatus(2);
        order.setTradeOrderId(orderResult.getData());
        orderMapper.updateById(order);

        // 缓存秒杀结果
        String resultKey = "seckill:activity:" + order.getActivityId() + ":result:" + userId + ":" + order.getGoodsId();
        redisTemplate.opsForValue().set(resultKey, 2, Duration.ofMinutes(resultCacheMinutes));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long orderId) {
        // 仅取消状态为 1（待支付）的订单
        int updated = orderMapper.cancelTimeoutOrder(orderId);
        if (updated > 0) {
            SeckillOrder order = orderMapper.selectById(orderId);
            if (order != null) {
                // 回滚 Redis 库存
                String stockKey = "seckill:activity:" + order.getActivityId() + ":stock:" + order.getGoodsId();
                redisTemplate.opsForValue().increment(stockKey);

                // 删除用户幂等标记
                String setnxKey = "seckill:activity:" + order.getActivityId() + ":product:" + order.getGoodsId() + ":user:" + order.getUserId();
                redisTemplate.delete(setnxKey);

                log.info("【超时取消】订单已取消并回滚库存: orderId={}, goodsId={}", orderId, order.getGoodsId());
            }
        }
    }

    private SeckillActivity validateActivity(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(4007, "秒杀活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartDate())) {
            throw new BusinessException(4008, "秒杀活动尚未开始");
        }
        if (now.isAfter(activity.getEndDate())) {
            throw new BusinessException(4009, "秒杀活动已结束");
        }
        if (activity.getStatus() != 1) {
            throw new BusinessException(4010, "活动状态异常");
        }
        return activity;
    }

    private void validateGoods(Long goodsId, Long activityId) {
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(4011, "秒杀商品不存在");
        }
        if (!goods.getActivityId().equals(activityId)) {
            throw new BusinessException(4012, "商品不属于该活动");
        }
        if (goods.getStockCount() <= 0) {
            throw new BusinessException(4004, "库存不足");
        }
    }

    private void validateProductCategory(Long goodsId) {
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BusinessException(4011, "秒杀商品不存在");
        }

        R<ProductFeignClient.ProductDTO> productResult = productFeignClient.getById(goods.getProductId());
        if (!productResult.isSuccess() || productResult.getData() == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }

        ProductFeignClient.ProductDTO product = productResult.getData();
        Integer category = product.getCategory();
        if (category == null || (category != 3 && category != 4)) {
            throw new BusinessException(4013, "该商品类型不支持秒杀");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(4014, "该商品已下架，不可参与秒杀");
        }
    }

    private Long getProductId(Long goodsId) {
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        return goods != null ? goods.getProductId() : null;
    }

    private BigDecimal getSeckillPrice(Long goodsId) {
        SeckillGoods goods = goodsMapper.selectById(goodsId);
        return goods != null ? goods.getSeckillPrice() : BigDecimal.ZERO;
    }

    private String generateMd5Path(Long userId, Long activityId, Long goodsId) {
        String raw = userId + "_" + activityId + "_" + goodsId + "_" + salt;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(1000, "MD5加密失败");
        }
    }

    public int deductStockDb(Long goodsId) {
        return goodsMapper.deductStock(goodsId);
    }
}
