package com.trade.quote.service.impl;

import com.trade.quote.service.AlertService;
import com.trade.quote.service.QuoteService;
import com.trade.quote.service.TushareClient;
import com.trade.quote.vo.QuoteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuoteServiceImpl implements QuoteService {

    private final TushareClient tushareClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final AlertService alertService;

    @Override
    public QuoteVO getRealtimeQuote(String stockCode) {
        String cacheKey = "quote:realtime:" + stockCode;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for realtime quote: {}", stockCode);
            return deserializeQuote(cached);
        }

        String emptyKey = "quote:empty:" + stockCode;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyKey))) {
            log.debug("Empty cache hit for: {}", stockCode);
            return null;
        }

        String lockKey = "quote:lock:" + stockCode;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(acquired)) {
            try {
                QuoteVO quote = tushareClient.fetchRealtimeQuote(stockCode);
                if (quote == null) {
                    stringRedisTemplate.opsForValue().set(emptyKey, "1", 5, TimeUnit.SECONDS);
                }
                return quote;
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        } else {
            try {
                Thread.sleep(100);
                String retryCached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (retryCached != null) {
                    return deserializeQuote(retryCached);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return tushareClient.fetchRealtimeQuote(stockCode);
        }
    }

    @Override
    public List<QuoteVO> getDailyKline(String stockCode, String startDate, String endDate) {
        String cacheKey = "quote:daily:" + stockCode + ":" + startDate + ":" + endDate;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserializeList(cached);
        }
        List<QuoteVO> result = tushareClient.fetchDailyKline(stockCode, startDate, endDate);
        if (!result.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, serializeList(result), 5, TimeUnit.MINUTES);
        }
        return result;
    }

    @Override
    public List<QuoteVO> searchStocks(String keyword) {
        return tushareClient.searchStocks(keyword);
    }

    @Override
    public void processPriceUpdate(String stockCode, double price, double changePercent, long timestamp) {
        String lastPushKey = "quote:last_push:" + stockCode;
        String lastPrice = stringRedisTemplate.opsForHash().get(lastPushKey, "price").toString();
        String lastTimestamp = stringRedisTemplate.opsForHash().get(lastPushKey, "timestamp").toString();

        if (lastPrice != null && Double.parseDouble(lastPrice) == price) {
            log.debug("Price unchanged for {}, skipping", stockCode);
            return;
        }

        double priceDiff = lastPrice != null ? Math.abs(price - Double.parseDouble(lastPrice)) : Double.MAX_VALUE;
        if (priceDiff < 0.01) {
            log.debug("Price diff {} below threshold for {}", priceDiff, stockCode);
            return;
        }

        if (lastTimestamp != null) {
            long timeDiff = timestamp - Long.parseLong(lastTimestamp);
            if (timeDiff < 1000) {
                log.debug("Too frequent push for {}, skipping", stockCode);
                return;
            }
        }

        stringRedisTemplate.opsForHash().put(lastPushKey, "price", String.valueOf(price));
        stringRedisTemplate.opsForHash().put(lastPushKey, "timestamp", String.valueOf(timestamp));
        stringRedisTemplate.expire(lastPushKey, 1, TimeUnit.HOURS);

        log.info("Price update triggered: stockCode={}, price={}", stockCode, price);

        alertService.checkAndTrigger(stockCode, price);
    }

    private QuoteVO deserializeQuote(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, QuoteVO.class);
        } catch (Exception e) {
            log.error("Failed to deserialize quote: {}", e.getMessage());
            return null;
        }
    }

    private List<QuoteVO> deserializeList(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, QuoteVO.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String serializeList(List<QuoteVO> list) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
