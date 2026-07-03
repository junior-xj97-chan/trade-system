package com.trade.seckill.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SeckillRabbitMQConfig {

    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";
    public static final String SECKILL_DLX_EXCHANGE = "seckill.order.dlx.exchange";
    public static final String SECKILL_DLQ = "seckill.order.dlq";
    public static final String SECKILL_DLQ_ROUTING_KEY = "seckill.order.dlq";

    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SECKILL_DLX_EXCHANGE);
        args.put("x-dead-letter-routing-key", SECKILL_DLQ_ROUTING_KEY);
        args.put("x-message-ttl", 30000);
        return QueueBuilder.durable(SECKILL_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding seckillBinding(Queue seckillQueue, TopicExchange seckillExchange) {
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with(SECKILL_ROUTING_KEY);
    }

    @Bean
    public TopicExchange seckillDlxExchange() {
        return new TopicExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillDlq() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public Binding seckillDlqBinding(Queue seckillDlq, TopicExchange seckillDlxExchange) {
        return BindingBuilder.bind(seckillDlq).to(seckillDlxExchange).with(SECKILL_DLQ_ROUTING_KEY);
    }

}
