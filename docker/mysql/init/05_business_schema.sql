-- ================================================================
-- 05_business_schema.sql
-- 业务表结构（trade_user / trade_order / trade_trade / trade_account / trade_product）
-- 含 Seata undo_log 表（每个参与分布式事务的库都需要）
-- ================================================================

-- ============================================================
-- trade_user
-- ============================================================
USE trade_user;

DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
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

DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';

-- ============================================================
-- trade_order
-- ============================================================
USE trade_order;

DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id           BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    order_no     VARCHAR(50)   NOT NULL COMMENT '订单号',
    user_id      BIGINT        NOT NULL COMMENT '用户ID',
    product_id   BIGINT        NOT NULL COMMENT '商品ID',
    product_name VARCHAR(100)  NULL     COMMENT '商品名称',
    price        DECIMAL(10,2) NULL     COMMENT '单价',
    quantity     INT           NULL     COMMENT '数量',
    amount       DECIMAL(10,2) NULL     COMMENT '总金额',
    status       INT           DEFAULT 1 COMMENT '1:待支付 2:已支付 3:已完成 4:已取消',
    direction    INT           DEFAULT 1 COMMENT '1:买入 2:卖出',
    create_time  DATETIME      NULL     COMMENT '创建时间',
    update_time  DATETIME      NULL     COMMENT '更新时间',
    deleted      INT           DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';

-- ============================================================
-- trade_trade
-- ============================================================
USE trade_trade;

DROP TABLE IF EXISTS t_trade;
CREATE TABLE t_trade (
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

DROP TABLE IF EXISTS t_position;
CREATE TABLE t_position (
    id            BIGINT         NOT NULL COMMENT '主键ID',
    user_id       BIGINT         NOT NULL COMMENT '用户ID',
    product_id    BIGINT         NOT NULL COMMENT '商品ID（股票代码）',
    product_name  VARCHAR(100)   NOT NULL COMMENT '商品名称',
    quantity      INT            NOT NULL DEFAULT 0    COMMENT '持有数量',
    avg_cost      DECIMAL(18,2)  NOT NULL DEFAULT 0.00 COMMENT '平均成本价',
    current_price DECIMAL(18,2)  NOT NULL DEFAULT 0.00 COMMENT '当前价格',
    status        TINYINT        NOT NULL DEFAULT 1    COMMENT '1正常 0已清仓',
    version       INT            NOT NULL DEFAULT 0    COMMENT '乐观锁版本号',
    create_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT        NOT NULL DEFAULT 0    COMMENT '0未删除 1已删除',
    PRIMARY KEY (id),
    INDEX idx_user_product (user_id, product_id, status),
    INDEX idx_user_id      (user_id),
    INDEX idx_product_id   (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓表';

DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';

-- ============================================================
-- trade_account
-- ============================================================
USE trade_account;

DROP TABLE IF EXISTS t_account;
CREATE TABLE t_account (
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

DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';

-- ============================================================
-- trade_product
-- ============================================================
USE trade_product;

DROP TABLE IF EXISTS t_product;
CREATE TABLE t_product (
    id             BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    product_code   VARCHAR(20)   NOT NULL COMMENT '商品代码（股票代码）',
    product_name   VARCHAR(100)  NOT NULL COMMENT '商品名称（股票名称）',
    current_price  DECIMAL(10,2) DEFAULT 0 COMMENT '当前价格',
    market         VARCHAR(10)   DEFAULT 'SH' COMMENT '市场标识：SH/SZ/HK/US',
    change_percent DECIMAL(8,4)  DEFAULT 0 COMMENT '涨跌幅（%）',
    category       INT           DEFAULT 1 COMMENT '1:股票 2:基金 3:商品 4:其他',
    status         INT           DEFAULT 1 COMMENT '1:正常 0:停牌/下架',
    version        INT           DEFAULT 0 COMMENT '乐观锁版本号',
    create_time    DATETIME      NULL     COMMENT '创建时间',
    update_time    DATETIME      NULL     COMMENT '更新时间',
    deleted        INT           DEFAULT 0 COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    branch_id     BIGINT       NOT NULL,
    xid           VARCHAR(128) NOT NULL,
    context       VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB     NOT NULL,
    log_status    INT          NOT NULL,
    log_created   DATETIME(6)  NOT NULL,
    log_modified  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo_log';

SELECT 'Business schema initialized!' AS status;
