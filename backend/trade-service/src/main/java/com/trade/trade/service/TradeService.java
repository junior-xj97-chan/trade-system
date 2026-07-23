package com.trade.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.trade.controller.TradeController.TradeRequest;
import com.trade.trade.entity.Trade;
import com.trade.trade.mapper.TradeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeMapper tradeMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    // 幂等 Key 前缀
    private static final String IDEMPOTENT_PREFIX = "trade:";
    // 幂等过期时间（分钟）
    private static final long IDEMPOTENT_EXPIRE_MINUTES = 30;

    /**
     * 执行交易撮合（简化版撮合引擎）
     * 实际生产中需要完整的撮合规则：价格优先、时间优先
     */
    @Transactional(rollbackFor = Exception.class)
    public Trade executeTrade(TradeRequest request) {
        // ========== 幂等性检查：基于业务流水号防止重复创建 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "execute:" + request.getOrderId();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            // 幂等命中：查询已存在的交易记录并返回
            log.info("【执行交易-幂等命中】orderId={}", request.getOrderId());
            LambdaQueryWrapper<Trade> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Trade::getOrderId, request.getOrderId())
                   .orderByDesc(Trade::getCreateTime)
                   .last("LIMIT 1");
            return tradeMapper.selectOne(wrapper);
        }

        Trade trade = new Trade();
        trade.setTradeNo(UUID.randomUUID().toString().replace("-", ""));
        trade.setOrderId(request.getOrderId());
        trade.setUserId(request.getUserId());
        trade.setProductId(request.getProductId());
        trade.setPrice(request.getPrice());
        trade.setQuantity(request.getQuantity());
        trade.setAmount(request.getPrice().multiply(new BigDecimal(request.getQuantity())));
        trade.setDirection(request.getDirection());
        trade.setStatus(1); // 成交中
        trade.setCreateTime(LocalDateTime.now());
        trade.setUpdateTime(LocalDateTime.now());

        // 简化撮合：直接成交
        trade.setStatus(2); // 已完成

        tradeMapper.insert(trade);
        return trade;
    }

    /**
     * 创建退款交易记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Trade refundTrade(Long orderId, Long userId, Long productId, BigDecimal price, Integer quantity) {
        // ========== 幂等性检查：基于业务流水号防止重复创建 ==========
        String idempotentKey = IDEMPOTENT_PREFIX + "refund:" + orderId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            idempotentKey, "processing", IDEMPOTENT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!success) {
            // 幂等命中：查询已存在的退款记录并返回
            log.info("【退款交易-幂等命中】orderId={}", orderId);
            LambdaQueryWrapper<Trade> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Trade::getOrderId, orderId)
                   .eq(Trade::getDirection, 3)
                   .orderByDesc(Trade::getCreateTime)
                   .last("LIMIT 1");
            return tradeMapper.selectOne(wrapper);
        }

        Trade trade = new Trade();
        trade.setTradeNo(UUID.randomUUID().toString().replace("-", ""));
        trade.setOrderId(orderId);
        trade.setUserId(userId);
        trade.setProductId(productId);
        trade.setPrice(price);
        trade.setQuantity(quantity);
        trade.setAmount(price.multiply(new BigDecimal(quantity)));
        trade.setDirection(3); // 退款
        trade.setStatus(2); // 直接完成
        trade.setCreateTime(LocalDateTime.now());
        trade.setUpdateTime(LocalDateTime.now());

        tradeMapper.insert(trade);
        return trade;
    }

    public Trade getById(Long id) {
        return tradeMapper.selectById(id);
    }

    public Trade getByTradeNo(String tradeNo) {
        LambdaQueryWrapper<Trade> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Trade::getTradeNo, tradeNo);
        return tradeMapper.selectOne(wrapper);
    }
}
