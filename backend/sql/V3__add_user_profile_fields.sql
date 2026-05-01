-- 给 t_user 新增个人资料字段：昵称、性别、头像
-- 2026-05-01

USE trade_user;

ALTER TABLE t_user
    ADD COLUMN nickname   VARCHAR(50)  NULL COMMENT '昵称'  AFTER email,
    ADD COLUMN gender     TINYINT      NULL COMMENT '性别：1=男 2=女 0=未知' AFTER nickname,
    ADD COLUMN avatar     VARCHAR(500) NULL COMMENT '头像URL' AFTER gender;