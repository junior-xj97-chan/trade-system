package com.trade.quote.mq;

import lombok.Data;

@Data
public class PriceAlertMessage {
    private String msgId;
    private Long userId;
    private String stockCode;
    private String stockName;
    private Double targetPrice;
    private Double currentPrice;
    private String alertType;
    private Long timestamp;
}
