-- =============================================================
-- V6: 创建定时任务执行日志表，用于任务调度轨迹落库与问题排查
-- =============================================================

CREATE TABLE IF NOT EXISTS t_task_log (
    id            BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    task_name     VARCHAR(64)   NOT NULL COMMENT '任务名称',
    trigger_time  DATETIME      NOT NULL COMMENT '任务触发时间',
    end_time      DATETIME      NULL     COMMENT '任务结束时间',
    status        INT           DEFAULT 0 COMMENT '0:失败 1:成功',
    total_count   INT           DEFAULT 0 COMMENT '扫描/处理总数',
    success_count INT           DEFAULT 0 COMMENT '成功数量',
    fail_count    INT           DEFAULT 0 COMMENT '失败数量',
    failed_nos    VARCHAR(2048) NULL     COMMENT '失败的业务编号列表',
    message       VARCHAR(2048) NULL     COMMENT '执行结果描述',
    create_time   DATETIME      NULL     COMMENT '创建时间',
    update_time   DATETIME      NULL     COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_task_name_time (task_name, trigger_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务执行日志表';
