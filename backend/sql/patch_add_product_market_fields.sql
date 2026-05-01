-- ============================================================
-- 补丁：t_product 表新增 market、change_percent 字段
-- 适用于：已存在的数据库（非初始化）
-- 执行方式：在 trade_product 数据库下执行
-- ============================================================

USE trade_product;

-- 添加市场标识字段（如已存在会报错，可忽略）
ALTER TABLE t_product
    ADD COLUMN market         VARCHAR(10)  DEFAULT 'SH'  COMMENT '市场标识：SH/SZ/HK/US' AFTER current_price,
    ADD COLUMN change_percent DECIMAL(8,4) DEFAULT 0     COMMENT '涨跌幅（%）'             AFTER market;

-- 更新现有数据的 market（根据商品代码范围简单推断）
UPDATE t_product SET market = 'SH' WHERE product_code IN ('600036','601318','600519','601319','601398');
UPDATE t_product SET market = 'SZ' WHERE product_code IN ('000001','000002','300001');

SELECT '补丁执行完成' AS msg, COUNT(*) AS total FROM t_product;
