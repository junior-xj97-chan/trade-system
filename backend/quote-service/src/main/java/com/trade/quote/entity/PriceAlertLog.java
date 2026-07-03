package com.trade.quote.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_price_alert_log")
public class PriceAlertLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long alertId;

    private Long userId;

    private String stockCode;

    private BigDecimal targetPrice;

    private BigDecimal currentPrice;

    private String alertType;

    private String triggerType;

    private LocalDateTime triggeredAt;

    private String msgId;
}
