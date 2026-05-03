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

-- Seata AT undo_log 表（account-service 参与分布式事务）
CREATE TABLE IF NOT EXISTS undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'increment id',
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT          NOT NULL COMMENT '0:normal status,1:defense status',
    log_created   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    log_modified  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';
