package com.trade.seckill.task;

import com.trade.seckill.service.SeckillPreheatService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 秒杀库存自动预热定时任务（XXL-JOB 版）
 * 通过 XXL-JOB 调度中心管理，每5分钟执行一次，预热30分钟内开始的活动
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillPreheatTask {

    private final SeckillPreheatService preheatService;

    /**
     * XXL-JOB 任务：库存自动预热
     * <p>
     * 调度中心配置 Cron 表达式：0 * / 5 * * * ?（每5分钟）
     */
    @XxlJob("seckillPreheatJob")
    public void autoPreheat() {
        log.info("【自动预热】开始扫描待预热活动");
        try {
            preheatService.autoPreheatScheduled();
            XxlJobHelper.handleSuccess("自动预热完成");
        } catch (Exception e) {
            log.error("【自动预热】执行失败", e);
            XxlJobHelper.handleFail("自动预热失败：" + e.getMessage());
        }
    }
}
