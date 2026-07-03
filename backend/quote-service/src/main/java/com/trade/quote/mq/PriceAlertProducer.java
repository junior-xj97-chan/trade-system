package com.trade.quote.mq;

import com.trade.quote.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendAlertNotification(Long userId, String stockCode, String stockName,
                                       double targetPrice, double currentPrice, String alertType) {
        String msgId = UUID.randomUUID().toString();
        PriceAlertMessage message = new PriceAlertMessage();
        message.setMsgId(msgId);
        message.setUserId(userId);
        message.setStockCode(stockCode);
        message.setStockName(stockName);
        message.setTargetPrice(targetPrice);
        message.setCurrentPrice(currentPrice);
        message.setAlertType(alertType);
        message.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ALERT_EXCHANGE,
                RabbitMQConfig.ALERT_NOTIFY_ROUTING_KEY,
                message
        );
        log.info("Sent alert notification: msgId={}, userId={}, stockCode={}, alertType={}",
                msgId, userId, stockCode, alertType);
    }
}
