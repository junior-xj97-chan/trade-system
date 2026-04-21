-- 为 t_order 表添加 direction 字段，区分买入/卖出订单
-- 1: 买入（默认）
-- 2: 卖出

ALTER TABLE t_order ADD COLUMN direction INT DEFAULT 1 COMMENT '1:买入 2:卖出';

-- 为已存在的订单设置默认值（已支付的为买入订单）
UPDATE t_order SET direction = 1 WHERE direction IS NULL;
