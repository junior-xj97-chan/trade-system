-- ========================================
-- 持仓表 (t_position)
-- 用于记录用户的股票持仓情况
-- ========================================

CREATE TABLE IF NOT EXISTS `t_position` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID（股票代码）',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `quantity` INT NOT NULL DEFAULT 0 COMMENT '持有数量',
    `avg_cost` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '平均成本价',
    `current_price` DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '当前价格',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常 0已清仓',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_product` (`user_id`, `product_id`, `status`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓表';
