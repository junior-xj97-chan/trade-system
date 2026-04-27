package com.trade.common.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品同步消息
 * 用于 MQ 异步同步商品数据到 ES
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品代码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 当前价格
     */
    private BigDecimal currentPrice;

    /**
     * 商品类型（1:股票 2:基金 3:期货）
     */
    private Integer productType;

    /**
     * 交易所代码
     */
    private String exchangeCode;

    /**
     * 商品状态（1:正常 0:停牌）
     */
    private Integer status;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        CREATE,   // 新增
        UPDATE,   // 修改
        DELETE,   // 删除
        ONLINE,   // 上架
        OFFLINE   // 下架
    }
}
