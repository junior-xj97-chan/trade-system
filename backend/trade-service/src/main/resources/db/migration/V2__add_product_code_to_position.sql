-- =============================================================
-- V2: t_position 表新增 product_code 字段（冗余存储股票代码）
-- =============================================================

ALTER TABLE t_position ADD COLUMN product_code VARCHAR(20) NULL COMMENT '商品代码（股票代码）' AFTER product_id;
ALTER TABLE t_position ADD INDEX idx_product_code (product_code);