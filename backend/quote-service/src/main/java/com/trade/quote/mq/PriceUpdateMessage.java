package com.trade.quote.mq;

import lombok.Data;

@Data
public class PriceUpdateMessage {
    private String msgId;
    private String stockCode;
    private Double price;
    private Long timestamp;
}
