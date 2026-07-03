-- =============================================================
-- V2: t_position 表新增 product_code 字段（冗余存储股票代码）
-- 注意：MySQL 不支持 ALTER TABLE ADD COLUMN IF NOT EXISTS 语法
-- 本脚本已在 V1 或手动执行中完成，此处留空避免重复执行
-- =============================================================

-- 字段已存在：product_code
-- 索引已存在：idx_product_code
-- 无需重复执行 ALTER TABLE
