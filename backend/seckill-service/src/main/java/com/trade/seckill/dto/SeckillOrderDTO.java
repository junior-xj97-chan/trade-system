package com.trade.seckill.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrderDTO {
    private Long id;
    private Long activityId;
    private Long userId;
    private Long goodsId;
    private Long productId;
    private String orderNo;
    private BigDecimal seckillPrice;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createTime;
}
