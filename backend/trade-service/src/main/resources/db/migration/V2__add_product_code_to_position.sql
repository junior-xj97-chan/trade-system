-- =============================================================
-- V2: t_position 表新增 product_code 字段（冗余存储股票代码）
-- 对应 patch：V2__add_product_code_to_position.sql（已整合）
-- =============================================================

ALTER TABLE t_position
    ADD COLUMN IF NOT EXISTS product_code VARCHAR(20) NULL
        COMMENT '商品代码（股票代码，冗余存储）' AFTER product_id;

ALTER TABLE t_position
    ADD INDEX IF NOT EXISTS idx_product_code (product_code);
