package com.trade.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PriceAlertDTO {

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @NotBlank(message = "股票名称不能为空")
    private String stockName;

    @NotNull(message = "目标价格不能为空")
    @DecimalMin(value = "0.0001", message = "目标价格必须大于0")
    private BigDecimal targetPrice;

    @NotBlank(message = "提醒类型不能为空")
    private String alertType;

    private String conditionDesc;
}
