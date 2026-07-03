package com.trade.quote.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceAlertVO {

    private Long id;

    private Long userId;

    private String stockCode;

    private String stockName;

    private BigDecimal targetPrice;

    private String alertType;

    private String conditionDesc;

    private Integer isTriggered;

    private LocalDateTime triggeredAt;

    private Integer isEnabled;

    private LocalDateTime createTime;
}
