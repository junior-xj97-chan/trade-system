package com.trade.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易实体（放在 common 模块，方便各服务共享）
 */
@Data
@TableName("t_trade")
public class Trade implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tradeNo;      // 交易单号
    private Long orderId;        // 关联订单ID
    private Long userId;         // 用户ID
    private Long productId;      // 商品ID
    private BigDecimal price;    // 成交价格
    private Integer quantity;    // 成交数量
    private BigDecimal amount;   // 成交金额
    private Integer direction;   // 1:买入 2:卖出
    private Integer status;      // 1:成交中 2:已完成 3:失败

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
