-- =============================================================
-- V1: 初始化 trade_account 数据库表结构
-- =============================================================

-- 账户表
CREATE TABLE IF NOT EXISTS t_account (
    id            BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    balance       DECIMAL(15,2) DEFAULT 0 COMMENT '可用余额',
    frozen_amount DECIMAL(15,2) DEFAULT 0 COMMENT '冻结金额',
    status        INT           DEFAULT 1 COMMENT '1:正常 0:冻结',
    version       INT           DEFAULT 0 COMMENT '乐观锁版本号',
    create_time   DATETIME      NULL     COMMENT '创建时间',
    update_time   DATETIME      NULL     COMMENT '更新时间',
    deleted       INT           DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';
