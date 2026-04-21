package com.trade.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓实体
 * <p>
 * 记录用户的股票持仓情况
 */
@Data
@TableName("t_position")
public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;              // 用户ID
    private Long productId;           // 商品ID（股票代码）
    private String productName;      // 商品名称（冗余字段，避免关联查询）
    private Integer quantity;         // 持有数量
    private BigDecimal avgCost;      // 平均成本价
    private BigDecimal currentPrice; // 当前价格
    private Integer status;          // 状态：1正常 0已清仓

    @Version
    private Integer version;          // 乐观锁版本

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
