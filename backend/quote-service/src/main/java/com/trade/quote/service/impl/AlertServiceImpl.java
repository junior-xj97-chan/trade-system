package com.trade.quote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trade.quote.dto.PriceAlertDTO;
import com.trade.quote.entity.PriceAlert;
import com.trade.quote.entity.PriceAlertLog;
import com.trade.quote.mapper.PriceAlertLogMapper;
import com.trade.quote.mapper.PriceAlertMapper;
import com.trade.quote.mq.PriceAlertProducer;
import com.trade.quote.service.AlertService;
import com.trade.quote.vo.PriceAlertVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl extends ServiceImpl<PriceAlertMapper, PriceAlert> implements AlertService {

    private final StringRedisTemplate stringRedisTemplate;
    private final PriceAlertProducer priceAlertProducer;
    private final PriceAlertLogMapper priceAlertLogMapper;

    private static final String ALERT_CACHE_PREFIX = "quote:alert:";

    @Override
    public List<PriceAlertVO> listByUserId(Long userId) {
        LambdaQueryWrapper<PriceAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceAlert::getUserId, userId)
                .orderByDesc(PriceAlert::getCreateTime);

        List<PriceAlert> list = list(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(Long userId, PriceAlertDTO dto) {
        PriceAlert alert = new PriceAlert();
        alert.setUserId(userId);
        alert.setStockCode(dto.getStockCode());
        alert.setStockName(dto.getStockName());
        alert.setTargetPrice(dto.getTargetPrice());
        alert.setAlertType(dto.getAlertType());
        alert.setConditionDesc(dto.getConditionDesc());
        alert.setIsTriggered(0);
        alert.setIsEnabled(1);
        save(alert);

        cacheAlert(alert);
        log.info("Created price alert: userId={}, stockCode={}, targetPrice={}",
                userId, dto.getStockCode(), dto.getTargetPrice());
        return alert.getId();
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        LambdaQueryWrapper<PriceAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceAlert::getId, id).eq(PriceAlert::getUserId, userId);
        PriceAlert alert = getOne(wrapper);
        if (alert == null) {
            throw new RuntimeException("Alert not found");
        }
        removeById(id);
        removeAlertFromCache(alert.getStockCode(), alert.getId());
        log.info("Deleted price alert: id={}", id);
    }

    @Override
    @Transactional
    public void update(Long userId, Long id, PriceAlertDTO dto) {
        LambdaQueryWrapper<PriceAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceAlert::getId, id).eq(PriceAlert::getUserId, userId);
        PriceAlert alert = getOne(wrapper);
        if (alert == null) {
            throw new RuntimeException("Alert not found");
        }
        removeAlertFromCache(alert.getStockCode(), alert.getId());

        alert.setTargetPrice(dto.getTargetPrice());
        alert.setAlertType(dto.getAlertType());
        alert.setConditionDesc(dto.getConditionDesc());
        alert.setIsTriggered(0);
        updateById(alert);

        cacheAlert(alert);
    }

    @Override
    public void checkAndTrigger(String stockCode, double currentPrice) {
        String cacheKey = ALERT_CACHE_PREFIX + stockCode;
        var entries = stringRedisTemplate.opsForHash().entries(cacheKey);

        if (entries.isEmpty()) {
            return;
        }

        for (var entry : entries.entrySet()) {
            try {
                String alertJson = entry.getValue().toString();
                PriceAlert alert = parseAlert(alertJson);

                if (alert.getIsEnabled() != 1 || alert.getIsTriggered() == 1) {
                    continue;
                }

                boolean triggered = isTriggered(alert, currentPrice);
                if (triggered) {
                    triggerAlert(alert, currentPrice, "realtime");
                }
            } catch (Exception e) {
                log.error("Failed to check alert for stockCode={}: {}", stockCode, e.getMessage());
            }
        }
    }

    private boolean isTriggered(PriceAlert alert, double currentPrice) {
        BigDecimal target = alert.getTargetPrice();
        BigDecimal current = BigDecimal.valueOf(currentPrice);

        switch (alert.getAlertType().toLowerCase()) {
            case "gt":
                return current.compareTo(target) > 0;
            case "lt":
                return current.compareTo(target) < 0;
            case "eq":
                return current.compareTo(target) == 0;
            default:
                return false;
        }
    }

    private void triggerAlert(PriceAlert alert, double currentPrice, String triggerType) {
        alert.setIsTriggered(1);
        alert.setTriggeredAt(LocalDateTime.now());
        updateById(alert);

        removeAlertFromCache(alert.getStockCode(), alert.getId());

        String msgId = UUID.randomUUID().toString();
        priceAlertProducer.sendAlertNotification(
                alert.getUserId(),
                alert.getStockCode(),
                alert.getStockName(),
                alert.getTargetPrice().doubleValue(),
                currentPrice,
                alert.getAlertType()
        );

        PriceAlertLog logEntry = new PriceAlertLog();
        logEntry.setAlertId(alert.getId());
        logEntry.setUserId(alert.getUserId());
        logEntry.setStockCode(alert.getStockCode());
        logEntry.setTargetPrice(alert.getTargetPrice());
        logEntry.setCurrentPrice(BigDecimal.valueOf(currentPrice));
        logEntry.setAlertType(alert.getAlertType());
        logEntry.setTriggerType(triggerType);
        logEntry.setMsgId(msgId);
        logEntry.setTriggeredAt(LocalDateTime.now());
        priceAlertLogMapper.insert(logEntry);

        log.info("Alert triggered: alertId={}, stockCode={}, targetPrice={}, currentPrice={}",
                alert.getId(), alert.getStockCode(), alert.getTargetPrice(), currentPrice);
    }

    private void cacheAlert(PriceAlert alert) {
        String cacheKey = ALERT_CACHE_PREFIX + alert.getStockCode();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(alert);
            stringRedisTemplate.opsForHash().put(cacheKey, alert.getId().toString(), json);
            stringRedisTemplate.expire(cacheKey, 30, java.util.concurrent.TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to cache alert: {}", e.getMessage());
        }
    }

    private void removeAlertFromCache(String stockCode, Long alertId) {
        String cacheKey = ALERT_CACHE_PREFIX + stockCode;
        stringRedisTemplate.opsForHash().delete(cacheKey, alertId.toString());
    }

    private PriceAlertVO toVO(PriceAlert entity) {
        PriceAlertVO vo = new PriceAlertVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private PriceAlert parseAlert(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, PriceAlert.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse alert: " + e.getMessage());
        }
    }
}
