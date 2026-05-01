package com.trade.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付成功消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易方向：1=买入，2=卖出
     */
    private Integer direction;

    /**
     * 商品代码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 交易数量
     */
    private Integer quantity;

    /**
     * 交易单价
     */
    private BigDecimal price;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 支付时间
     */
    private LocalDateTime paidTime;
}
