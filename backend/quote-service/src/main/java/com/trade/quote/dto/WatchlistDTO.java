package com.trade.quote.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WatchlistDTO {

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @NotBlank(message = "股票名称不能为空")
    private String stockName;

    @NotBlank(message = "市场不能为空")
    private String market;

    private String tags;

    private String note;
}
