-- =============================================================
-- V1: 初始化 trade_trade 数据库表结构
-- =============================================================

-- 持仓表
CREATE TABLE IF NOT EXISTS t_position (
    `id`           BIGINT        NOT NULL COMMENT '主键ID',
    `user_id`      BIGINT        NOT NULL COMMENT '用户ID',
    `product_id`   BIGINT        NOT NULL COMMENT '商品ID（股票代码）',
    `product_name` VARCHAR(100)  NOT NULL COMMENT '商品名称',
    `quantity`     INT           NOT NULL DEFAULT 0    COMMENT '持有数量',
    `avg_cost`     DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '平均成本价',
    `current_price`DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '当前价格',
    `status`       TINYINT       NOT NULL DEFAULT 1    COMMENT '状态：1正常 0已清仓',
    `version`      INT           NOT NULL DEFAULT 0    COMMENT '乐观锁版本号',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP            COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `idx_user_product` (`user_id`, `product_id`, `status`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓表';

-- 交易记录表
CREATE TABLE IF NOT EXISTS t_trade (
    id           BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    trade_no     VARCHAR(50)   NOT NULL COMMENT '交易单号',
    order_id     BIGINT        NULL     COMMENT '关联订单ID',
    user_id      BIGINT        NOT NULL COMMENT '用户ID',
    product_id   BIGINT        NULL     COMMENT '商品ID',
    price        DECIMAL(10,2) NULL     COMMENT '成交价格',
    quantity     INT           NULL     COMMENT '成交数量',
    amount       DECIMAL(10,2) NULL     COMMENT '成交金额',
    direction    INT           NULL     COMMENT '1:买入 2:卖出 3:退款',
    status       INT           DEFAULT 1 COMMENT '1:成交中 2:已完成 3:失败',
    create_time  DATETIME      NULL     COMMENT '创建时间',
    update_time  DATETIME      NULL     COMMENT '更新时间',
    deleted      INT           DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_no (trade_no),
    KEY idx_user_id (user_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易表';

-- Seata AT undo_log 表（trade-service 参与分布式事务）
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
