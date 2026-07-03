package com.trade.seckill.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.seckill.entity.SeckillOrder;
import com.trade.seckill.mapper.SeckillOrderMapper;
import com.trade.seckill.service.SeckillService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀超时订单自动取消定时任务（XXL-JOB 版）
 * 通过 XXL-JOB 调度中心管理，每分钟执行一次，扫描超过 timeoutMinutes 分钟未支付的秒杀订单并自动取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillTimeoutTask {

    private final SeckillOrderMapper orderMapper;
    private final SeckillService seckillService;

    @Value("${seckill.order-timeout-minutes}")
    private int orderTimeoutMinutes;

    /**
     * XXL-JOB 任务：秒杀超时订单自动取消
     * <p>
     * 调度中心配置 Cron 表达式：0 * * * * ?（每分钟）
     */
    @XxlJob("seckillTimeoutJob")
    public void cleanTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
        List<SeckillOrder> timeoutOrders = orderMapper.selectList(
            new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, 1)
                .lt(SeckillOrder::getCreateTime, deadline)
                .last("LIMIT 100")
        );

        if (timeoutOrders.isEmpty()) {
            log.info("【超时清理】本次扫描未发现超时订单");
            XxlJobHelper.handleSuccess("扫描完成，未发现超时订单");
            return;
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder failedOrders = new StringBuilder();

        for (SeckillOrder order : timeoutOrders) {
            try {
                seckillService.cancelTimeoutOrder(order.getId());
                successCount++;
            } catch (Exception e) {
                log.error("【超时清理】取消订单失败: orderId={}", order.getId(), e);
                failCount++;
                if (failedOrders.length() > 0) {
                    failedOrders.append(",");
                }
                failedOrders.append(order.getId());
            }
        }

        String resultMsg = String.format("处理完成：成功 %d 个，失败 %d 个", successCount, failCount);
        log.info("【超时清理】{}", resultMsg);

        if (failCount > 0) {
            XxlJobHelper.handleFail("部分订单取消失败：" + failedOrders);
        } else {
            XxlJobHelper.handleSuccess(resultMsg);
        }
    }
}
