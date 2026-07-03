package com.trade.seckill.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    private Long userId;
    private Long productId;
    private String productName;
    private String productCode;
    private BigDecimal price;
    private Integer quantity;
    private Integer source;
}
