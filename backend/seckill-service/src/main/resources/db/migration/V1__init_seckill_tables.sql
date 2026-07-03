CREATE TABLE IF NOT EXISTS seckill_activity (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    activity_name VARCHAR(200) NOT NULL COMMENT '活动名称',
    start_date   DATETIME      NOT NULL COMMENT '活动开始时间',
    end_date     DATETIME      NOT NULL COMMENT '活动结束时间',
    status       TINYINT       NOT NULL DEFAULT 0 COMMENT '0-未开始 1-进行中 2-已结束',
    preheated    TINYINT       NOT NULL DEFAULT 0 COMMENT '0-未预热 1-已预热',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

CREATE TABLE IF NOT EXISTS seckill_goods (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    activity_id   BIGINT        NOT NULL COMMENT '关联活动ID',
    product_id    BIGINT        NOT NULL COMMENT '关联 t_product 的商品ID',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock_count   INT           NOT NULL DEFAULT 0 COMMENT '秒杀库存',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_activity_id (activity_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

CREATE TABLE IF NOT EXISTS seckill_order (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    activity_id     BIGINT        NOT NULL COMMENT '秒杀活动ID',
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    goods_id        BIGINT        NOT NULL COMMENT '秒杀商品ID',
    product_id      BIGINT        NOT NULL COMMENT '关联 t_product 的商品ID',
    order_no        VARCHAR(64)   NOT NULL COMMENT '订单号',
    seckill_price   DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    status          TINYINT       NOT NULL DEFAULT 1 COMMENT '1-待支付 2-已支付 3-超时作废',
    pay_retry_count INT           NOT NULL DEFAULT 0 COMMENT '支付重试次数',
    trade_order_id  BIGINT        DEFAULT NULL COMMENT '关联的正式订单ID',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_user_goods (activity_id, user_id, goods_id),
    KEY idx_order_no (order_no),
    KEY idx_status (status),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

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
