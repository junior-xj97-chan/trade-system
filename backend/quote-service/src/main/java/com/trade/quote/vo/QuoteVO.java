package com.trade.quote.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuoteVO {

    private String stockCode;

    private String stockName;

    private BigDecimal currentPrice;

    private BigDecimal openPrice;

    private BigDecimal highPrice;

    private BigDecimal lowPrice;

    private BigDecimal preClosePrice;

    private BigDecimal changePercent;

    private Long volume;

    private BigDecimal amount;

    private String market;

    private Long timestamp;
}
