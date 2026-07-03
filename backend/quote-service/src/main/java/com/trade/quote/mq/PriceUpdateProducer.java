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
public class PriceUpdateProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendPriceUpdate(String stockCode, double price, long timestamp) {
        String msgId = UUID.randomUUID().toString();
        PriceUpdateMessage message = new PriceUpdateMessage();
        message.setMsgId(msgId);
        message.setStockCode(stockCode);
        message.setPrice(price);
        message.setTimestamp(timestamp);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.QUOTE_EXCHANGE,
                RabbitMQConfig.QUOTE_PRICE_ROUTING_KEY,
                message
        );
        log.debug("Sent price update: msgId={}, stockCode={}, price={}", msgId, stockCode, price);
    }
}
