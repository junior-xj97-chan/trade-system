package com.trade.quote.service;

import com.trade.quote.config.TushareConfig;
import com.trade.quote.vo.QuoteVO;
import com.trade.quote.feign.ProductFeignClient;
import com.trade.quote.mq.PriceAlertProducer;
import com.trade.quote.mq.PriceUpdateProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TushareClient {

    private final TushareConfig tushareConfig;
    private final StringRedisTemplate stringRedisTemplate;
    private final PriceUpdateProducer priceUpdateProducer;
    private final PriceAlertProducer priceAlertProducer;
    private final ProductFeignClient productFeignClient;
    private final AlertService alertService;

    private static final String EMPTY_CACHE_PREFIX = "quote:empty:";

    public QuoteVO fetchRealtimeQuote(String stockCode) {
        String cached = stringRedisTemplate.opsForValue().get("quote:realtime:" + stockCode);
        if (cached != null) {
            return parseQuoteFromCache(cached, stockCode);
        }

        String emptyKey = EMPTY_CACHE_PREFIX + stockCode;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(emptyKey))) {
            return null;
        }

        try {
            QuoteVO quote = callTushareRtK(stockCode);
            if (quote != null) {
                stringRedisTemplate.opsForValue().set("quote:realtime:" + stockCode,
                        serializeQuote(quote), 30, java.util.concurrent.TimeUnit.SECONDS);
                stringRedisTemplate.opsForValue().set("quote:pre_close:" + stockCode,
                        quote.getPreClosePrice().toString(), 1, java.util.concurrent.TimeUnit.DAYS);
            } else {
                stringRedisTemplate.opsForValue().set(emptyKey, "1", 5, java.util.concurrent.TimeUnit.SECONDS);
            }
            return quote;
        } catch (Exception e) {
            log.warn("Tushare rt_k failed for {}, falling back to daily: {}", stockCode, e.getMessage());
            return fetchDailyQuote(stockCode);
        }
    }

    public List<QuoteVO> fetchDailyKline(String stockCode, String startDate, String endDate) {
        List<QuoteVO> result = new ArrayList<>();
        try {
            String response = callTushareDaily(stockCode, startDate, endDate);
            if (response != null) {
                result = parseDailyKline(response);
            }
        } catch (Exception e) {
            log.error("Tushare daily failed for {}: {}", stockCode, e.getMessage());
        }
        return result;
    }

    public List<QuoteVO> searchStocks(String keyword) {
        List<QuoteVO> result = new ArrayList<>();
        try {
            String cached = stringRedisTemplate.opsForValue().get("quote:search:" + keyword);
            if (cached != null) {
                return deserializeSearchResult(cached);
            }
            String response = callTushareStockBasic(keyword);
            if (response != null) {
                result = parseSearchResult(response);
                stringRedisTemplate.opsForValue().set("quote:search:" + keyword,
                        serializeSearchResult(result), 1, java.util.concurrent.TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("Tushare search failed for {}: {}", keyword, e.getMessage());
        }
        return result;
    }

    private QuoteVO callTushareRtK(String stockCode) {
        String tsCode = stockCode.replace(".", "");
        String apiUrl = "https://api.tushare.pro" +
                "?api_name=rt_k&token=" + tushareConfig.getToken() +
                "&params={\"ts_code\":\"" + tsCode + "\"}";
        try {
            java.net.URI uri = java.net.URI.create(apiUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return parseRtKResponse(response.body(), stockCode);
        } catch (Exception e) {
            log.error("Tushare rt_k HTTP call failed: {}", e.getMessage());
            return null;
        }
    }

    private String callTushareDaily(String stockCode, String startDate, String endDate) {
        String tsCode = stockCode.replace(".", "");
        String apiUrl = "https://api.tushare.pro" +
                "?api_name=daily&token=" + tushareConfig.getToken() +
                "&params={\"ts_code\":\"" + tsCode + "\"" +
                (startDate != null ? ",\"start_date\":\"" + startDate + "\"" : "") +
                (endDate != null ? ",\"end_date\":\"" + endDate + "\"" : "") +
                "}";
        try {
            java.net.URI uri = java.net.URI.create(apiUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            log.error("Tushare daily HTTP call failed: {}", e.getMessage());
            return null;
        }
    }

    private String callTushareStockBasic(String keyword) {
        String apiUrl = "https://api.tushare.pro" +
                "?api_name=stock_basic&token=" + tushareConfig.getToken() +
                "&params={\"name\":\"" + keyword + "\"}";
        try {
            java.net.URI uri = java.net.URI.create(apiUrl);
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            log.error("Tushare stock_basic HTTP call failed: {}", e.getMessage());
            return null;
        }
    }

    private QuoteVO parseRtKResponse(String responseBody, String stockCode) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            if (data.path("items").isArray() && data.path("items").size() > 0) {
                com.fasterxml.jackson.databind.JsonNode item = data.path("items").get(0);
                com.fasterxml.jackson.databind.JsonNode fields = data.path("fields");

                QuoteVO vo = new QuoteVO();
                vo.setStockCode(stockCode);

                for (int i = 0; i < fields.size(); i++) {
                    String fieldName = fields.get(i).asText();
                    com.fasterxml.jackson.databind.JsonNode value = item.get(i);
                    switch (fieldName) {
                        case "name":
                            vo.setStockName(value.asText());
                            break;
                        case "price":
                            vo.setCurrentPrice(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                        case "open":
                            vo.setOpenPrice(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                        case "high":
                            vo.setHighPrice(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                        case "low":
                            vo.setLowPrice(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                        case "pre_close":
                            vo.setPreClosePrice(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                        case "pct_chg":
                            vo.setChangePercent(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                        case "vol":
                            vo.setVolume(value.isNumber() ? value.asLong() : 0L);
                            break;
                        case "amount":
                            vo.setAmount(value.isNumber() ? BigDecimal.valueOf(value.asDouble()) : BigDecimal.ZERO);
                            break;
                    }
                }
                vo.setTimestamp(System.currentTimeMillis());
                return vo;
            }
        } catch (Exception e) {
            log.error("Failed to parse rt_k response: {}", e.getMessage());
        }
        return null;
    }

    private List<QuoteVO> parseDailyKline(String responseBody) {
        List<QuoteVO> result = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            if (data.path("items").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : data.path("items")) {
                    QuoteVO vo = new QuoteVO();
                    vo.setStockName(item.path("name").asText());
                    vo.setCurrentPrice(item.path("close").isNumber() ? BigDecimal.valueOf(item.path("close").asDouble()) : BigDecimal.ZERO);
                    vo.setOpenPrice(item.path("open").isNumber() ? BigDecimal.valueOf(item.path("open").asDouble()) : BigDecimal.ZERO);
                    vo.setHighPrice(item.path("high").isNumber() ? BigDecimal.valueOf(item.path("high").asDouble()) : BigDecimal.ZERO);
                    vo.setLowPrice(item.path("low").isNumber() ? BigDecimal.valueOf(item.path("low").asDouble()) : BigDecimal.ZERO);
                    vo.setPreClosePrice(item.path("pre_close").isNumber() ? BigDecimal.valueOf(item.path("pre_close").asDouble()) : BigDecimal.ZERO);
                    vo.setChangePercent(item.path("pct_chg").isNumber() ? BigDecimal.valueOf(item.path("pct_chg").asDouble()) : BigDecimal.ZERO);
                    vo.setTimestamp(System.currentTimeMillis());
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse daily response: {}", e.getMessage());
        }
        return result;
    }

    private List<QuoteVO> parseSearchResult(String responseBody) {
        List<QuoteVO> result = new ArrayList<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode data = root.path("data");
            if (data.path("items").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : data.path("items")) {
                    QuoteVO vo = new QuoteVO();
                    vo.setStockCode(item.path("ts_code").asText());
                    vo.setStockName(item.path("name").asText());
                    vo.setMarket(item.path("market").asText());
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse stock_basic response: {}", e.getMessage());
        }
        return result;
    }

    private QuoteVO parseQuoteFromCache(String cached, String stockCode) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cached, QuoteVO.class);
        } catch (Exception e) {
            log.error("Failed to parse cached quote: {}", e.getMessage());
            return null;
        }
    }

    private String serializeQuote(QuoteVO quote) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(quote);
        } catch (Exception e) {
            log.error("Failed to serialize quote: {}", e.getMessage());
            return null;
        }
    }

    private String serializeSearchResult(List<QuoteVO> result) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<QuoteVO> deserializeSearchResult(String cached) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cached,
                    mapper.getTypeFactory().constructCollectionType(List.class, QuoteVO.class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private QuoteVO fetchDailyQuote(String stockCode) {
        List<QuoteVO> daily = fetchDailyKline(stockCode, null, null);
        if (!daily.isEmpty()) {
            return daily.get(0);
        }
        return null;
    }
}
