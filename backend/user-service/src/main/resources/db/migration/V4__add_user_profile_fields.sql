-- =============================================================
-- V4: 给 t_user 新增个人资料字段：昵称、性别、头像
-- =============================================================

ALTER TABLE t_user ADD COLUMN nickname VARCHAR(50)  NULL COMMENT '昵称' AFTER email;
ALTER TABLE t_user ADD COLUMN gender   TINYINT     NULL COMMENT '性别：1男 2女 0未知' AFTER nickname;
ALTER TABLE t_user ADD COLUMN avatar   VARCHAR(255) NULL COMMENT '头像URL' AFTER gender;
