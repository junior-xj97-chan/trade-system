package com.trade.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.trade.common.BizCode;
import com.trade.common.BusinessException;
import com.trade.common.R;
import com.trade.common.config.RabbitMQConfig;
import com.trade.common.entity.Trade;
import com.trade.common.mq.OrderPaidMessage;
import com.trade.order.controller.OrderController.CreateOrderRequest;
import com.trade.order.entity.Order;
import com.trade.order.feign.AccountFeignClient;
import com.trade.order.feign.PositionFeignClient;
import com.trade.order.feign.ProductFeignClient;
import com.trade.order.feign.TradeFeignClient;
import com.trade.order.mapper.OrderMapper;
import org.apache.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final AccountFeignClient accountFeignClient;
    private final TradeFeignClient tradeFeignClient;
    private final PositionFeignClient positionFeignClient;
    private final ProductFeignClient productFeignClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    // 幂等 Key 前缀
    private static final String IDEMPOTENT_PREFIX = "order:";
    // 幂等过期时间（分钟）
    private static final long IDEMPOTENT_EXPIRE_MINUTES = 30;

    /**
     * 创建订单（简化版，实际应配合前端页面和商品服务）
     * direction: 1=买入 2=卖出
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        // ========== 通过 Feign 查询商品信息，进行规则校验 ==========
        R<ProductFeignClient.ProductDTO> productResult = productFeignClient.getById(request.getProductId());
        if (!productResult.isSuccess() || productResult.getData() == null) {
            throw new BusinessException(BizCode.PRODUCT_NOT_FOUND);
        }
        ProductFeignClient.ProductDTO product = productResult.getData();

        // 停牌股票不允许买卖
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(BizCode.PRODUCT_SUSPENDED);
        }

        // A股（SH/SZ）买卖数量必须为100的整数倍
        String market = product.getMarket();
        if (("SH".equals(market) || "SZ".equals(market)) && request.getQuantity() % 100 != 0) {
            throw new BusinessException(BizCode.INVALID_QUANTITY);
        }

        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setProductName(request.getProductName());
        order.setProductCode(request.getProductCode());  // 保存商品代码
        order.setPrice(request.getPrice());
        order.setQuantity(request.getQuantity());
        order.setAmount(request.getPrice().multiply(new BigDecimal(request.getQuantity())));
        // direction: 1=买入（默认）, 2=卖出
        order.setDirection(request.getDirection() != null ? request.getDirection() : 1);
        order.setStatus(1); // 待支付
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        orderMapper.insert(order);
        return order;
    }

    /**
     * 支付订单（使用 Seata AT 模式保证分布式事务一致性）
     * <p>
     * 完整链路：
     * 1. 冻结余额（校验账户余额是否充足）
     * 2. 扣减余额（从冻结金额中正式扣减）
     * 3. 创建交易记录
     * 4. 更新订单状态
     * <p>
     * 任意一步失败，全链路回滚
     */
    @SentinelResource(value = "order:pay", blockHandler = "payOrderBlockHandler")
    @GlobalTransactional(rollbackFor = Exception.class, name = "pay-order")
    public void payOrder(Long id) {
        // ========== 幂等性检查：防止重复支付 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "pay:" + id;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.warn("【支付订单-幂等拦截】orderId={}，请求已处理", id);
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        try {
            // 查询订单
            Order order = orderMapper.selectById(id);
            if (order == null) {
                throw new BusinessException(BizCode.ORDER_NOT_FOUND);
            }
            if (order.getStatus() != 1) {
                throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
            }

            log.info("【支付订单】orderNo={}，金额={}", order.getOrderNo(), order.getAmount());

            // ========== Seata 分布式事务分支 1：冻结余额（校验余额是否充足）==========
            log.info("【冻结余额】userId={}，amount={}", order.getUserId(), order.getAmount());
            accountFeignClient.freezeAmount(order.getUserId(), order.getAmount());

            // ========== Seata 分布式事务分支 2：扣减余额（从冻结金额中正式扣减）==========
            log.info("【扣减余额】userId={}，amount={}", order.getUserId(), order.getAmount());
            accountFeignClient.deductBalance(order.getUserId(), order.getAmount());

            // ========== Seata 分布式事务分支 3：创建交易记录 ==========
            log.info("【创建交易记录】orderId={}", id);
            TradeFeignClient.TradeRequest tradeRequest = new TradeFeignClient.TradeRequest();
            tradeRequest.setOrderId(id);
            tradeRequest.setUserId(order.getUserId());
            tradeRequest.setProductId(order.getProductId());
            tradeRequest.setPrice(order.getPrice());
            tradeRequest.setQuantity(order.getQuantity());
            tradeRequest.setDirection(1); // 买入
            R<Trade> tradeResult = tradeFeignClient.execute(tradeRequest);
            if (!tradeResult.isSuccess()) {
                throw new BusinessException(BizCode.TRADE_NOT_FOUND.getCode(), "交易记录创建失败：" + tradeResult.getMessage());
            }

            // ========== Seata 分布式事务分支 4：更新持仓（买入建仓/加仓）==========
            log.info("【更新持仓】userId={}, productId={}, quantity={}", order.getUserId(), order.getProductId(), order.getQuantity());
            R<Void> positionResult = positionFeignClient.buy(
                id,                        // orderId：用于幂等性控制
                order.getUserId(),
                order.getProductId(),
                order.getProductName(),
                order.getProductCode(),    // 传入商品代码，用于前端实时行情
                order.getQuantity(),
                order.getPrice()
            );
            if (!positionResult.isSuccess()) {
                throw new BusinessException(BizCode.POSITION_NOT_FOUND.getCode(), "持仓更新失败：" + positionResult.getMessage());
            }

            // ========== Seata 分布式事务分支 5：更新订单状态 ==========
            // 所有分支全部成功，订单直接完成
            order.setStatus(3); // 已完成
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            // ========== MQ 异步通知：发送订单支付成功消息 ==========
            log.info("【MQ发送】订单支付成功消息，orderNo={}", order.getOrderNo());
            OrderPaidMessage message = OrderPaidMessage.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .direction(order.getDirection())
                .productCode(order.getProductId().toString())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .amount(order.getAmount())
                .paidTime(LocalDateTime.now())
                .build();
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.TRADE_EXCHANGE,
                RabbitMQConfig.ORDER_PAID_KEY,
                message
            );

            log.info("【支付完成】orderNo={}", order.getOrderNo());

        } catch (Exception e) {
            // 异常时删除幂等 key，允许下次重试
            log.warn("【支付订单-异常】orderId={}，删除幂等key，允许重试，异常={}", id, e.getMessage());
            redisTemplate.delete(idempotentKey);
            throw e;
        }
    }

    /**
     * 卖出订单（使用 Seata AT 模式）
     * 卖出流程：增加用户余额 → 创建卖出交易记录 → 更新订单状态
     */
    @SentinelResource(value = "order:sell", blockHandler = "sellOrderBlockHandler")
    @GlobalTransactional(rollbackFor = Exception.class, name = "sell-order")
    public void sellOrder(Long id) {
        // ========== 幂等性检查：防止重复卖出 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "sell:" + id;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            log.warn("【卖出订单-幂等拦截】orderId={}，请求已处理", id);
            throw new BusinessException(BizCode.DUPLICATE_REQUEST);
        }

        try {
            // 查询订单
            Order order = orderMapper.selectById(id);
            if (order == null) {
                throw new BusinessException(BizCode.ORDER_NOT_FOUND);
            }
            if (order.getStatus() != 1) {
                throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
            }
            if (order.getDirection() == null || order.getDirection() != 2) {
                throw new BusinessException(BizCode.ORDER_STATUS_ERROR.getCode(), "该订单不是卖出订单，无法执行卖出操作");
            }

            log.info("【卖出订单】orderNo={}，收款金额={}", order.getOrderNo(), order.getAmount());

            // ========== Seata 分布式事务分支 1：卖出收款（增加用户余额）==========
            log.info("【卖出收款】userId={}，amount={}", order.getUserId(), order.getAmount());
            accountFeignClient.sellReceive(order.getUserId(), order.getAmount());

            // ========== Seata 分布式事务分支 2：创建卖出交易记录 ==========
            log.info("【创建卖出交易记录】orderId={}", id);
            TradeFeignClient.TradeRequest tradeRequest = new TradeFeignClient.TradeRequest();
            tradeRequest.setOrderId(id);
            tradeRequest.setUserId(order.getUserId());
            tradeRequest.setProductId(order.getProductId());
            tradeRequest.setPrice(order.getPrice());
            tradeRequest.setQuantity(order.getQuantity());
            tradeRequest.setDirection(2); // 卖出
            R<Trade> tradeResult = tradeFeignClient.execute(tradeRequest);
            if (!tradeResult.isSuccess()) {
                throw new BusinessException(BizCode.TRADE_NOT_FOUND.getCode(), "卖出交易记录创建失败：" + tradeResult.getMessage());
            }

            // ========== Seata 分布式事务分支 3：更新持仓（卖出减仓）==========
            log.info("【更新持仓】userId={}, productId={}, quantity={}", order.getUserId(), order.getProductId(), order.getQuantity());
            R<Void> positionResult = positionFeignClient.sell(
                id,                        // orderId：用于幂等性控制
                order.getUserId(),
                order.getProductId(),
                order.getQuantity(),
                order.getPrice()
            );
            if (!positionResult.isSuccess()) {
                throw new BusinessException(BizCode.POSITION_NOT_ENOUGH.getCode(), "持仓更新失败：" + positionResult.getMessage());
            }

            // ========== Seata 分布式事务分支 4：更新订单状态 ==========
            // 所有分支全部成功，订单直接完成
            order.setStatus(3); // 已完成
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);

            log.info("【卖出完成】orderNo={}", order.getOrderNo());

        } catch (Exception e) {
            // 异常时删除幂等 key，允许下次重试
            log.warn("【卖出订单-异常】orderId={}，删除幂等key，允许重试，异常={}", id, e.getMessage());
            redisTemplate.delete(idempotentKey);
            throw e;
        }
    }

    /**
     * 取消订单（支持退款）
     * 待支付订单：直接取消
     * 已支付订单：退款并取消（分布式事务）
     */
    @SentinelResource(value = "order:cancel", blockHandler = "cancelOrderBlockHandler")
    @GlobalTransactional(rollbackFor = Exception.class, name = "cancel-order")
    public void cancelOrder(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(BizCode.ORDER_NOT_FOUND);
        }

        Integer status = order.getStatus();
        if (status == 1) {
            // 待支付订单：直接取消（幂等处理：已完成/已取消的订单重复取消无害）
            if (order.getStatus() == 4) {
                log.info("【取消订单-幂等处理】orderNo={}，订单已取消", order.getOrderNo());
                return;
            }
            order.setStatus(4);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("【取消待支付订单】orderNo={}", order.getOrderNo());
        } else if (status == 2) {
            // 已支付订单：退款流程（需要幂等检查）
            // ========== 幂等性检查：防止重复退款 ==========
            String idempotentKey = IDEMPOTENT_PREFIX + "cancel:" + id;
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
            if (!success) {
                log.warn("【取消订单-幂等拦截】orderId={}，请求已处理", id);
                throw new BusinessException(BizCode.DUPLICATE_REQUEST);
            }

            log.info("【取消已支付订单】orderNo={}，退款金额={}", order.getOrderNo(), order.getAmount());
            
            // ========== Seata 分布式事务分支 1：退款到账户 ==========
            log.info("【退款到账户】userId={}，amount={}", order.getUserId(), order.getAmount());
            accountFeignClient.refund(order.getUserId(), order.getAmount());
            
            // ========== Seata 分布式事务分支 2：创建退款交易记录 ==========
            log.info("【创建退款交易记录】orderId={}", id);
            R<Trade> tradeResult = tradeFeignClient.refund(
                id, 
                order.getUserId(), 
                order.getProductId(), 
                order.getPrice(), 
                order.getQuantity()
            );
            if (!tradeResult.isSuccess()) {
                throw new BusinessException(BizCode.TRADE_NOT_FOUND.getCode(), "退款交易记录创建失败：" + tradeResult.getMessage());
            }
            
            // ========== Seata 分布式事务分支 3：更新订单状态 ==========
            order.setStatus(4);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            
            log.info("【退款完成】orderNo={}", order.getOrderNo());
        } else {
            // 其他状态（已完成、已取消）不允许取消
            throw new BusinessException(BizCode.ORDER_STATUS_ERROR);
        }
    }

    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

    public Order getByOrderNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    /**
     * 分页查询订单（可选按userId过滤）
     */
    public Page<Order> page(Long current, Long size, Long userId) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return orderMapper.selectPage(page, wrapper);
    }

    /**
     * 查询用户的所有订单
     */
    public List<Order> getByUserId(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .orderByDesc(Order::getCreateTime);
        return orderMapper.selectList(wrapper);
    }

    // ==================== Sentinel BlockHandler ====================

    /**
     * 支付订单限流/熔断处理器
     * 当 order:pay 资源触发流控规则时调用
     */
    public void payOrderBlockHandler(Long id, BlockException ex) {
        log.warn("[order:pay] 被 Sentinel 限流/熔断，orderId={}, 规则={}", id, ex.getClass().getSimpleName());
        throw new BusinessException(BizCode.RATE_LIMIT_EXCEEDED);
    }

    /**
     * 取消订单限流/熔断处理器
     * 当 order:cancel 资源触发流控规则时调用
     */
    public void cancelOrderBlockHandler(Long id, BlockException ex) {
        log.warn("[order:cancel] 被 Sentinel 限流/熔断，orderId={}, 规则={}", id, ex.getClass().getSimpleName());
        throw new BusinessException(BizCode.RATE_LIMIT_EXCEEDED);
    }

    /**
     * 卖出订单限流/熔断处理器
     * 当 order:sell 资源触发流控规则时调用
     */
    public void sellOrderBlockHandler(Long id, BlockException ex) {
        log.warn("[order:sell] 被 Sentinel 限流/熔断，orderId={}, 规则={}", id, ex.getClass().getSimpleName());
        throw new BusinessException(BizCode.RATE_LIMIT_EXCEEDED);
    }
}
