package com.trade.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.order.entity.Order;
import com.trade.order.mapper.OrderMapper;
import com.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单补偿定时任务
 * <p>
 * 扫描处于“处理中”状态的订单，根据调用流水继续完成未成功的远程调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensateTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PAY_IDEMPOTENT_PREFIX = "order:pay:";
    private static final String SELL_IDEMPOTENT_PREFIX = "order:sell:";
    private static final String CANCEL_IDEMPOTENT_PREFIX = "order:cancel:";

    /**
     * 每 30 秒扫描一次处理中订单
     */
    @Scheduled(fixedRate = 30_000)
    public void compensate() {
        log.info("【订单补偿任务】开始扫描处理中订单");
        List<Order> processingOrders = findProcessingOrders();
        if (processingOrders.isEmpty()) {
            log.info("【订单补偿任务】暂无处理中订单");
            return;
        }

        for (Order order : processingOrders) {
            Long id = order.getId();
            try {
                log.info("【订单补偿任务】处理 orderNo={}，status={}，direction={}", order.getOrderNo(), order.getStatus(), order.getDirection());
                if (order.getStatus() == 5) {
                    // 处理中：买入/卖出
                    clearIdempotentKey(id, order.getDirection());
                    if (order.getDirection() != null && order.getDirection() == 2) {
                        orderService.sellOrder(id);
                    } else {
                        orderService.payOrder(id);
                    }
                } else if (order.getStatus() == 6) {
                    // 取消处理中
                    clearCancelIdempotentKey(id);
                    orderService.cancelOrder(id);
                }
            } catch (Exception e) {
                log.error("【订单补偿任务】补偿失败 orderId={}，异常={}", id, e.getMessage());
            }
        }
    }

    /**
     * 查询处理中且创建时间超过 10 秒的订单（给首次请求足够时间完成）
     */
    private List<Order> findProcessingOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getStatus, 5, 6)
               .lt(Order::getUpdateTime, LocalDateTime.now().minusSeconds(10))
               .orderByAsc(Order::getUpdateTime)
               .last("LIMIT 100");
        return orderMapper.selectList(wrapper);
    }

    private void clearIdempotentKey(Long orderId, Integer direction) {
        String prefix = (direction != null && direction == 2) ? SELL_IDEMPOTENT_PREFIX : PAY_IDEMPOTENT_PREFIX;
        redisTemplate.delete(prefix + orderId);
    }

    private void clearCancelIdempotentKey(Long orderId) {
        redisTemplate.delete(CANCEL_IDEMPOTENT_PREFIX + orderId);
    }
}
