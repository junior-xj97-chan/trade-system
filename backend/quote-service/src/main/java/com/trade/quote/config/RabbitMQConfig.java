package com.trade.quote.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String QUOTE_EXCHANGE = "quote.exchange";
    public static final String ALERT_EXCHANGE = "alert.exchange";
    public static final String QUOTE_PRICE_ROUTING_KEY = "quote.price.updated";
    public static final String ALERT_NOTIFY_ROUTING_KEY = "alert.notify";
    public static final String QUOTE_DLQ = "quote.price.dlq";

    @Bean
    public TopicExchange quoteExchange() {
        return new TopicExchange(QUOTE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange(ALERT_EXCHANGE, true, false);
    }

    @Bean
    public Queue priceUpdateQueue() {
        return QueueBuilder.durable("quote.price.update.queue")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUOTE_DLQ)
                .build();
    }

    @Bean
    public Binding priceUpdateBinding() {
        return BindingBuilder.bind(priceUpdateQueue())
                .to(quoteExchange())
                .with(QUOTE_PRICE_ROUTING_KEY);
    }

    @Bean
    public Queue alertNotifyQueue() {
        return QueueBuilder.durable("alert.notify.queue")
                .build();
    }

    @Bean
    public Binding alertNotifyBinding() {
        return BindingBuilder.bind(alertNotifyQueue())
                .to(alertExchange())
                .with(ALERT_NOTIFY_ROUTING_KEY);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUOTE_DLQ).build();
    }
}
