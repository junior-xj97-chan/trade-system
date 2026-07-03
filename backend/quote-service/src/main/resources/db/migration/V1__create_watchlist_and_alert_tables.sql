-- V1__create_watchlist_and_alert_tables.sql
CREATE DATABASE IF NOT EXISTS trade_quote DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trade_quote;

CREATE TABLE IF NOT EXISTS t_watchlist (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    stock_code VARCHAR(20) NOT NULL COMMENT '股票代码（如 000001.SZ）',
    stock_name VARCHAR(50) NOT NULL COMMENT '股票名称',
    market VARCHAR(10) NOT NULL COMMENT '市场（SH/SZ/HK/US）',
    tags VARCHAR(100) DEFAULT NULL COMMENT '标签，逗号分隔',
    note VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_stock (user_id, stock_code, market),
    KEY idx_user_id (user_id),
    KEY idx_stock_code_market (stock_code, market),
    CONSTRAINT chk_market CHECK (market IN ('SH', 'SZ', 'HK', 'US'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自选股表';

CREATE TABLE IF NOT EXISTS t_price_alert (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    stock_code VARCHAR(20) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) NOT NULL COMMENT '股票名称',
    target_price DECIMAL(18,4) NOT NULL COMMENT '目标价格',
    alert_type VARCHAR(20) NOT NULL COMMENT '提醒类型：gt-大于 lt-小于 eq-等于',
    condition_desc VARCHAR(100) DEFAULT NULL COMMENT '条件描述',
    is_triggered TINYINT NOT NULL DEFAULT 0 COMMENT '是否已触发：0-否 1-是',
    triggered_at DATETIME DEFAULT NULL COMMENT '触发时间',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_stock_enabled_triggered (stock_code, is_enabled, is_triggered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格提醒表';

CREATE TABLE IF NOT EXISTS t_price_alert_log (
    id BIGINT NOT NULL COMMENT '主键（雪花算法）',
    alert_id BIGINT NOT NULL COMMENT '价格提醒ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    stock_code VARCHAR(20) NOT NULL COMMENT '股票代码（带市场后缀，如600000.SH）',
    target_price DECIMAL(18,4) NOT NULL COMMENT '目标价格',
    current_price DECIMAL(18,4) NOT NULL COMMENT '触发时价格',
    alert_type VARCHAR(20) NOT NULL COMMENT '提醒类型：gt/lt/eq',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发类型：realtime-行情驱动 fallback-兜底任务',
    triggered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
    msg_id VARCHAR(64) DEFAULT NULL COMMENT 'MQ消息ID',
    PRIMARY KEY (id),
    KEY idx_alert_id (alert_id),
    KEY idx_user_id (user_id),
    KEY idx_triggered_at (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格提醒触发日志表';

CREATE TABLE IF NOT EXISTS undo_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'increment id',
    branch_id BIGINT NOT NULL COMMENT 'branch transaction id',
    xid VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context VARCHAR(128) NOT NULL COMMENT 'undo_log context',
    rollback_info LONGBLOB NOT NULL COMMENT 'rollback info',
    log_status INT NOT NULL COMMENT '0:normal status,1:defense status',
    log_created DATETIME(6) NOT NULL COMMENT 'create datetime',
    log_modified DATETIME(6) NOT NULL COMMENT 'modify datetime',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';
