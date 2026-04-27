package com.trade.common.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 DTO
 * 用于服务间传递商品信息
 */
@Data
public class ProductDTO {

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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
