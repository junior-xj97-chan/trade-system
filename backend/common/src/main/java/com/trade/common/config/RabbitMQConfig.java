package com.trade.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 消息队列配置
 * <p>
 * 交换机类型：Topic（主题交换机，支持通配符匹配）
 * 队列：
 * - order-paid-queue：订单支付成功队列
 * - order-canceled-queue：订单取消队列
 * - account-update-queue：账户余额更新队列
 * <p>
 * 路由键规则：order.{event} 或 account.{event}
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 交换机 ====================
    public static final String TRADE_EXCHANGE = "trade-exchange";

    // ==================== 队列 ====================
    public static final String ORDER_PAID_QUEUE = "order-paid-queue";
    public static final String ORDER_CANCELED_QUEUE = "order-canceled-queue";
    public static final String ACCOUNT_UPDATE_QUEUE = "account-update-queue";
    public static final String PRODUCT_SYNC_QUEUE = "product-sync-queue";  // 商品同步队列

    // ==================== 路由键 ====================
    public static final String ORDER_PAID_KEY = "order.paid";
    public static final String ORDER_CANCELED_KEY = "order.canceled";
    public static final String ACCOUNT_UPDATE_KEY = "account.update";
    public static final String PRODUCT_SYNC_KEY = "product.sync";  // 商品同步路由键

    // ==================== 交换机 Bean ====================
    @Bean
    public TopicExchange tradeExchange() {
        return new TopicExchange(TRADE_EXCHANGE, true, false);
    }

    // ==================== 队列 Bean ====================
    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE).build();
    }

    @Bean
    public Queue orderCanceledQueue() {
        return QueueBuilder.durable(ORDER_CANCELED_QUEUE).build();
    }

    @Bean
    public Queue accountUpdateQueue() {
        return QueueBuilder.durable(ACCOUNT_UPDATE_QUEUE).build();
    }

    @Bean
    public Queue productSyncQueue() {
        return QueueBuilder.durable(PRODUCT_SYNC_QUEUE).build();
    }

    // ==================== 绑定 Bean ====================
    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange tradeExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(tradeExchange).with(ORDER_PAID_KEY);
    }

    @Bean
    public Binding orderCanceledBinding(Queue orderCanceledQueue, TopicExchange tradeExchange) {
        return BindingBuilder.bind(orderCanceledQueue).to(tradeExchange).with(ORDER_CANCELED_KEY);
    }

    @Bean
    public Binding accountUpdateBinding(Queue accountUpdateQueue, TopicExchange tradeExchange) {
        return BindingBuilder.bind(accountUpdateQueue).to(tradeExchange).with(ACCOUNT_UPDATE_KEY);
    }

    @Bean
    public Binding productSyncBinding(Queue productSyncQueue, TopicExchange tradeExchange) {
        return BindingBuilder.bind(productSyncQueue).to(tradeExchange).with(PRODUCT_SYNC_KEY);
    }

    // ==================== 消息转换器 ====================
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
