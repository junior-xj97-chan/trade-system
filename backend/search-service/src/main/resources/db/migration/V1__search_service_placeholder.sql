-- =============================================================
-- V1: search-service 使用 trade_product 数据库
-- search-service 本身不创建额外业务表，依赖 product-service 的库
-- 此脚本仅确保 Flyway schema_history 表正常初始化
-- 注意：实际业务表由 product-service 的迁移脚本负责
-- =============================================================

-- 占位脚本，确保 Flyway 版本链完整
SELECT 1;
