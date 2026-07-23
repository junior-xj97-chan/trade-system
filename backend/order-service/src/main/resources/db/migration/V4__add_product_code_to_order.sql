-- =============================================================
-- V4: t_order 表新增 product_code 字段（冗余存储股票代码）
-- =============================================================

ALTER TABLE t_order ADD COLUMN product_code VARCHAR(20) NULL COMMENT '商品代码（股票代码）' AFTER product_name;
ALTER TABLE t_order ADD INDEX idx_product_code (product_code);
