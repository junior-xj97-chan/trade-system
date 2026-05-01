package com.trade.search.job;

import com.trade.search.service.DataSyncService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 数据同步任务（XXL-JOB 版）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncJob {

    private final DataSyncService dataSyncService;

    /**
     * 全量同步任务
     * 建议 Cron: 0 0 2 * * ?（每天凌晨 2 点）
     */
    @XxlJob("dataFullSyncJob")
    public void fullSync() {
        log.info("【XXL-JOB】开始执行全量同步任务...");
        try {
            dataSyncService.manualFullSync();
            XxlJobHelper.handleSuccess("全量同步完成");
        } catch (Exception e) {
            log.error("【XXL-JOB】全量同步失败", e);
            XxlJobHelper.handleFail("全量同步失败：" + e.getMessage());
        }
    }

    /**
     * 增量同步任务
     * 建议 Cron: 0 0 * * * ?（每小时执行）
     */
    @XxlJob("dataIncrementalSyncJob")
    public void incrementalSync() {
        log.info("【XXL-JOB】开始执行增量同步任务...");
        try {
            dataSyncService.incrementalSync();
            XxlJobHelper.handleSuccess("增量同步完成");
        } catch (Exception e) {
            log.error("【XXL-JOB】增量同步失败", e);
            XxlJobHelper.handleFail("增量同步失败：" + e.getMessage());
        }
    }
}
