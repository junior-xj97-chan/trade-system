-- =============================================
-- V2__add_product_code_to_position.sql
-- 给 t_position 表加 product_code 字段（冗余存储股票代码）
-- =============================================

USE trade_trade;

-- 新增 product_code 字段
ALTER TABLE t_position
    ADD COLUMN product_code VARCHAR(20) NULL COMMENT '商品代码（股票代码，冗余存储）' AFTER product_id;

-- 为已有数据回填 product_code（通过关联 t_product 表）
UPDATE t_position p
    JOIN t_product d ON p.product_id = d.id
SET p.product_code = d.product_code
WHERE p.product_code IS NULL;

-- 为 product_code 建索引（查询性能）
ALTER TABLE t_position
    ADD INDEX idx_product_code (product_code);
