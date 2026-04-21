package com.trade.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String orderNo;       // 订单号
    private Long userId;          // 用户ID
    private Long productId;       // 商品ID
    private String productName;   // 商品名称
    private BigDecimal price;     // 单价
    private Integer quantity;    // 数量
    private BigDecimal amount;    // 总金额
    private Integer status;       // 1:待支付 2:已支付 3:已完成 4:已取消
    private Integer direction;     // 1:买入 2:卖出（默认买入）
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
