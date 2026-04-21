package com.trade.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.trade.controller.TradeController.TradeRequest;
import com.trade.trade.entity.Trade;
import com.trade.trade.mapper.TradeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeMapper tradeMapper;

    /**
     * 执行交易撮合（简化版撮合引擎）
     * 实际生产中需要完整的撮合规则：价格优先、时间优先
     */
    @Transactional(rollbackFor = Exception.class)
    public Trade executeTrade(TradeRequest request) {
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
