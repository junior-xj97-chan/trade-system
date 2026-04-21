package com.trade.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品/股票表实体
 */
@Data
@TableName("t_product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 商品代码（股票代码）
     */
    private String productCode;

    /**
     * 商品名称（股票名称）
     */
    private String productName;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 分类：1-股票 2-基金 3-商品 4-其他
     */
    private Integer category;

    /**
     * 状态：1-正常 0-停牌/下架
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}