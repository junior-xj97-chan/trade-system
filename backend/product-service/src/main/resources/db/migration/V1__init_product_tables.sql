-- =============================================================
-- V1: 初始化 trade_product 数据库表结构
-- =============================================================

-- 商品（股票）表
CREATE TABLE IF NOT EXISTS t_product (
    id             BIGINT        NOT NULL COMMENT '主键（雪花算法）',
    product_code   VARCHAR(20)   NOT NULL COMMENT '商品代码（股票代码）',
    product_name   VARCHAR(100)  NOT NULL COMMENT '商品名称（股票名称）',
    current_price  DECIMAL(10,2) DEFAULT 0    COMMENT '当前价格',
    market         VARCHAR(10)   DEFAULT 'SH' COMMENT '市场标识：SH/SZ/HK/US',
    change_percent DECIMAL(8,4)  DEFAULT 0    COMMENT '涨跌幅（%）',
    category       INT           DEFAULT 1    COMMENT '1:股票 2:基金 3:商品 4:其他',
    status         INT           DEFAULT 1    COMMENT '1:正常 0:停牌/下架',
    version        INT           DEFAULT 0    COMMENT '乐观锁版本号',
    create_time    DATETIME      NULL         COMMENT '创建时间',
    update_time    DATETIME      NULL         COMMENT '更新时间',
    deleted        INT           DEFAULT 0    COMMENT '逻辑删除 0:未删除 1:已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
