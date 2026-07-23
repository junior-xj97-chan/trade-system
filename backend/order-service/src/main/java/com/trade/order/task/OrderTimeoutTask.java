package com.trade.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.order.entity.Order;
import com.trade.order.entity.TaskLog;
import com.trade.order.mapper.OrderMapper;
import com.trade.order.mapper.TaskLogMapper;
import com.trade.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时自动取消定时任务
 * <p>
 * 改造点：
 * 1. 基于 Redis 分布式锁保证集群环境下同一时刻只有一个实例执行
 * 2. 任务执行结果落库（t_task_log），便于后续审计与排查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final TaskLogMapper taskLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 订单超时时间（分钟），默认 30 分钟
     */
    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    private static final String LOCK_KEY = "task:order-timeout:lock";
    private static final long LOCK_EXPIRE_MINUTES = 5;
    private static final String TASK_NAME = "OrderTimeoutTask";

    /**
     * 每分钟扫描一次超时未支付订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelExpiredOrders() {
        LocalDateTime triggerTime = LocalDateTime.now();
        String lockValue = UUID.randomUUID().toString();

        // ========== 1. 获取 Redis 分布式锁 ==========
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, lockValue, LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            log.debug("【{}】未获取到分布式锁，本次跳过", TASK_NAME);
            return;
        }

        TaskLog taskLog = new TaskLog();
        taskLog.setTaskName(TASK_NAME);
        taskLog.setTriggerTime(triggerTime);
        taskLog.setStatus(1);
        taskLog.setTotalCount(0);
        taskLog.setSuccessCount(0);
        taskLog.setFailCount(0);
        taskLog.setCreateTime(triggerTime);
        taskLog.setUpdateTime(triggerTime);

        try {
            int effectiveTimeout = timeoutMinutes;
            LocalDateTime deadline = triggerTime.minusMinutes(effectiveTimeout);

            // 查询所有待支付且超时的订单
            LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Order::getStatus, 1) // 1=待支付
                   .lt(Order::getCreateTime, deadline);

            List<Order> expiredOrders = orderMapper.selectList(wrapper);
            taskLog.setTotalCount(expiredOrders.size());

            if (expiredOrders.isEmpty()) {
                log.info("【定时任务】本次扫描未发现超时订单");
                taskLog.setMessage("本次扫描未发现超时订单");
                return;
            }

            log.info("【定时任务】发现 {} 个超时未支付订单，即将自动取消（超时阈值={}分钟）", expiredOrders.size(), effectiveTimeout);

            int successCount = 0;
            int failCount = 0;
            StringBuilder failedOrders = new StringBuilder();

            for (Order order : expiredOrders) {
                try {
                    log.info("【超时取消订单】orderNo={}，创建时间={}，超时阈值={}分钟",
                            order.getOrderNo(), order.getCreateTime(), effectiveTimeout);
                    orderService.cancelOrder(order.getId());
                    successCount++;
                } catch (Exception e) {
                    log.error("【超时取消失败】orderNo={}，原因={}", order.getOrderNo(), e.getMessage());
                    failCount++;
                    if (failedOrders.length() > 0) {
                        failedOrders.append(",");
                    }
                    failedOrders.append(order.getOrderNo());
                }
            }

            taskLog.setSuccessCount(successCount);
            taskLog.setFailCount(failCount);
            taskLog.setFailedNos(failedOrders.length() > 0 ? failedOrders.toString() : null);

            String resultMsg = String.format("处理完成：成功 %d 个，失败 %d 个", successCount, failCount);
            if (failCount > 0) {
                taskLog.setStatus(0);
                taskLog.setMessage(resultMsg + "，失败订单=" + failedOrders);
                log.warn("【定时任务】{}，失败订单={}", resultMsg, failedOrders);
            } else {
                taskLog.setMessage(resultMsg);
                log.info("【定时任务】{}", resultMsg);
            }
        } catch (Exception e) {
            taskLog.setStatus(0);
            taskLog.setMessage("任务执行异常：" + e.getMessage());
            log.error("【{}】执行异常", TASK_NAME, e);
        } finally {
            taskLog.setEndTime(LocalDateTime.now());
            taskLog.setUpdateTime(LocalDateTime.now());
            try {
                taskLogMapper.insert(taskLog);
            } catch (Exception ex) {
                log.error("【{}】任务日志落库失败", TASK_NAME, ex);
            }

            // ========== 释放 Redis 分布式锁（仅当持有的是自己加的锁时释放） ==========
            try {
                Object currentValue = redisTemplate.opsForValue().get(LOCK_KEY);
                if (lockValue.equals(currentValue)) {
                    redisTemplate.delete(LOCK_KEY);
                }
            } catch (Exception ex) {
                log.error("【{}】释放分布式锁失败", TASK_NAME, ex);
            }
        }
    }
}
