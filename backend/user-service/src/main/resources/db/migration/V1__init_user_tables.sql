-- =============================================================
-- V1: 初始化 trade_user 数据库表结构
-- =============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL COMMENT '主键（雪花算法）',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（MD5加密）',
    phone       VARCHAR(20)  NULL     COMMENT '手机号',
    email       VARCHAR(100) NULL     COMMENT '邮箱',
    status      INT          DEFAULT 1 COMMENT '1:正常 0:禁用',
    create_time DATETIME     NULL     COMMENT '创建时间',
    update_time DATETIME     NULL     COMMENT '更新时间',
    deleted     INT          DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
