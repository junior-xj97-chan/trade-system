-- =============================================================
-- V2: 给 t_user 新增个人资料字段：昵称、性别、头像
-- 对应 patch：V3__add_user_profile_fields.sql（已整合）
-- =============================================================

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS nickname   VARCHAR(50)  NULL COMMENT '昵称'  AFTER email,
    ADD COLUMN IF NOT EXISTS gender     TINYINT      NULL COMMENT '性别：1=男 2=女 0=未知' AFTER nickname,
    ADD COLUMN IF NOT EXISTS avatar     VARCHAR(500) NULL COMMENT '头像URL' AFTER gender;
