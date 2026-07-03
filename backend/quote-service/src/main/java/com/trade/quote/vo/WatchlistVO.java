package com.trade.quote.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WatchlistVO {

    private Long id;

    private Long userId;

    private String stockCode;

    private String stockName;

    private String market;

    private String tags;

    private String note;

    private LocalDateTime createTime;
}
