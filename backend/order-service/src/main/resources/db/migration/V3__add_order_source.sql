ALTER TABLE t_order ADD COLUMN source INT DEFAULT 1 COMMENT '1-普通订单 2-秒杀订单' AFTER status;
