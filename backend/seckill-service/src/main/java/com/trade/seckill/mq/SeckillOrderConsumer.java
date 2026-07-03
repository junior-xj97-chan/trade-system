package com.trade.seckill.mq;

import com.rabbitmq.client.Channel;
import com.trade.seckill.config.SeckillRabbitMQConfig;
import com.trade.seckill.dto.SeckillMessage;
import com.trade.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final SeckillService seckillService;

    @RabbitListener(queues = SeckillRabbitMQConfig.SECKILL_QUEUE, concurrency = "5-10")
    public void handleSeckillOrder(SeckillMessage message, Channel channel, Message amqpMessage) {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        try {
            log.info("【秒杀消费】处理秒杀订单: activityId={}, goodsId={}, userId={}",
                message.getActivityId(), message.getGoodsId(), message.getUserId());

            seckillService.processSeckillOrder(message);

            channel.basicAck(deliveryTag, false);
            log.info("【秒杀消费】ACK成功: userId={}, goodsId={}", message.getUserId(), message.getGoodsId());
        } catch (Exception e) {
            int retryCount = getRetryCount(amqpMessage);
            log.warn("【秒杀消费】处理失败: userId={}, goodsId={}, retry={}, error={}",
                message.getUserId(), message.getGoodsId(), retryCount, e.getMessage());

            if (retryCount < 3) {
                try {
                    channel.basicNack(deliveryTag, false, true);
                } catch (IOException ex) {
                    log.error("【秒杀消费】NACK失败", ex);
                }
            } else {
                log.error("【秒杀消费】重试次数已达上限，转入DLQ: userId={}, goodsId={}",
                    message.getUserId(), message.getGoodsId());
                try {
                    channel.basicNack(deliveryTag, false, false);
                } catch (IOException ex) {
                    log.error("【秒杀消费】NACK失败", ex);
                }
            }
        }
    }

    private int getRetryCount(Message message) {
        Object retryHeader = message.getMessageProperties().getHeader("x-retry-count");
        if (retryHeader instanceof Integer) {
            return (Integer) retryHeader + 1;
        }
        return 0;
    }
}
