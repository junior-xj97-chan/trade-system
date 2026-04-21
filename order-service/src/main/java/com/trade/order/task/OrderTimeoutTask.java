package com.trade.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.order.entity.Order;
import com.trade.order.mapper.OrderMapper;
import com.trade.order.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务（XXL-JOB 版）
 * 通过 XXL-JOB 调度中心管理，每分钟执行一次，扫描超过 timeoutMinutes 分钟未支付的订单并自动取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 订单超时时间（分钟），默认 30 分钟
     */
    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    /**
     * XXL-JOB 任务：订单超时自动取消
     * <p>
     * 调度中心配置 Cron 表达式：0 * * * * ?（每分钟）
     */
    @XxlJob("orderTimeoutJob")
    public void cancelExpiredOrders() {
        // 获取任务参数（可选，用于动态调整超时分钟数）
        String jobParam = XxlJobHelper.getJobParam();
        int effectiveTimeout = timeoutMinutes;
        if (jobParam != null && !jobParam.isEmpty()) {
            try {
                effectiveTimeout = Integer.parseInt(jobParam);
            } catch (NumberFormatException e) {
                log.warn("【XXL-JOB】无效的任务参数 jobParam={}，使用默认值 timeoutMinutes={}", jobParam, timeoutMinutes);
            }
        }

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(effectiveTimeout);

        // 查询所有待支付且超时的订单
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 1) // 1=待支付
               .lt(Order::getCreateTime, deadline);

        List<Order> expiredOrders = orderMapper.selectList(wrapper);

        if (expiredOrders.isEmpty()) {
            log.info("【XXL-JOB】本次扫描未发现超时订单");
            XxlJobHelper.handleSuccess("扫描完成，未发现超时订单");
            return;
        }

        log.info("【XXL-JOB】发现 {} 个超时未支付订单，即将自动取消（超时阈值={}分钟）", expiredOrders.size(), effectiveTimeout);

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

        String resultMsg = String.format("处理完成：成功 %d 个，失败 %d 个", successCount, failCount);
        log.info("【XXL-JOB】{}", resultMsg);

        if (failCount > 0) {
            XxlJobHelper.handleFail("部分订单取消失败：" + failedOrders);
        } else {
            XxlJobHelper.handleSuccess(resultMsg);
        }
    }
}
