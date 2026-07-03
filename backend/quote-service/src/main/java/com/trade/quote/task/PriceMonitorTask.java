package com.trade.quote.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.quote.entity.PriceAlert;
import com.trade.quote.service.AlertService;
import com.trade.quote.service.QuoteService;
import com.trade.quote.vo.QuoteVO;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceMonitorTask {

    private final AlertService alertService;
    private final QuoteService quoteService;
    private final StringRedisTemplate stringRedisTemplate;

    @XxlJob("priceAlertFallbackTask")
    public void fallbackTask() {
        log.info("Price alert fallback task started");

        LambdaQueryWrapper<PriceAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceAlert::getIsEnabled, 1)
                .eq(PriceAlert::getIsTriggered, 0);

        List<PriceAlert> alerts = alertService.list(wrapper);
        log.info("Found {} pending alerts to check", alerts.size());

        for (PriceAlert alert : alerts) {
            try {
                QuoteVO quote = quoteService.getRealtimeQuote(alert.getStockCode());
                if (quote != null && quote.getCurrentPrice() != null) {
                    alertService.checkAndTrigger(alert.getStockCode(), quote.getCurrentPrice().doubleValue());
                }
            } catch (Exception e) {
                log.error("Failed to check alert id={}: {}", alert.getId(), e.getMessage());
            }
        }

        log.info("Price alert fallback task completed");
    }
}
