-- =============================================================
-- V1: 初始化 trade_order 数据库表结构
-- =============================================================

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id           BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    order_no     VARCHAR(50)   NOT NULL COMMENT '订单号',
    user_id      BIGINT        NOT NULL COMMENT '用户ID',
    product_id   BIGINT        NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100)  NULL     COMMENT '商品名称',
    price        DECIMAL(10,2) NULL     COMMENT '单价',
    quantity     INT           NULL     COMMENT '数量',
    amount       DECIMAL(10,2) NULL     COMMENT '总金额',
    status       INT           DEFAULT 1 COMMENT '1:待支付 2:已支付 3:已完成 4:已取消',
    create_time  DATETIME      NULL     COMMENT '创建时间',
    update_time  DATETIME      NULL     COMMENT '更新时间',
    deleted      INT           DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- Seata AT undo_log 表（order-service 参与分布式事务）
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
