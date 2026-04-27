-- =============================================
-- trade-system 模拟数据脚本
-- 密码：统一为 123456
-- 加密：DigestUtils.md5DigestAsHex("trade:123456") = 9c0094e4f6ef629f8da3e9494584710c
-- ID：固定长整型模拟雪花 ID（方便测试时直接复用）
-- =============================================

-- =============================================
-- 1. 用户数据 trade_user.t_user
-- =============================================
USE trade_user;

-- 清空旧数据（不影响表结构）
TRUNCATE TABLE t_user;

INSERT INTO t_user (id, username, password, phone, email, status, create_time, update_time, deleted) VALUES
-- 密码统一为 123456
-- 加密逻辑：DigestUtils.md5DigestAsHex(("trade:" + "123456").getBytes())
-- 实际 MD5 值：9c0094e4f6ef629f8da3e9494584710c
(1000001, 'alice',   '9c0094e4f6ef629f8da3e9494584710c', '13800000001', 'alice@trade.com',  1, '2026-01-01 09:00:00', '2026-01-01 09:00:00', 0),
(1000002, 'bob',     '9c0094e4f6ef629f8da3e9494584710c', '13800000002', 'bob@trade.com',    1, '2026-01-02 09:00:00', '2026-01-02 09:00:00', 0),
(1000003, 'charlie', '9c0094e4f6ef629f8da3e9494584710c', '13800000003', 'charlie@trade.com',1, '2026-01-03 09:00:00', '2026-01-03 09:00:00', 0),
(1000004, 'diana',   '9c0094e4f6ef629f8da3e9494584710c', '13800000004', 'diana@trade.com',  1, '2026-01-04 09:00:00', '2026-01-04 09:00:00', 0),
(1000005, 'eve',     '9c0094e4f6ef629f8da3e9494584710c', '13800000005', NULL,               0, '2026-01-05 09:00:00', '2026-02-01 10:00:00', 0);
-- alice、bob、charlie、diana 正常用户；eve 已禁用（status=0）

SELECT '用户数据插入完成' AS msg, COUNT(*) AS total FROM t_user;

-- =============================================
-- 2. 账户数据 trade_account.t_account
-- =============================================
USE trade_account;

TRUNCATE TABLE t_account;

INSERT INTO t_account (id, user_id, balance, frozen_amount, status, version, create_time, update_time, deleted) VALUES
-- alice：余额充足，正常测试支付
(2000001, 1000001, 50000.00, 0.00,    1, 0, '2026-01-01 09:05:00', '2026-01-01 09:05:00', 0),
-- bob：余额中等，可测试部分支付后余额变化
(2000002, 1000002, 8000.00,  0.00,    1, 0, '2026-01-02 09:05:00', '2026-01-02 09:05:00', 0),
-- charlie：余额较少，用于测试余额不足回滚场景
(2000003, 1000003, 200.00,   0.00,    1, 0, '2026-01-03 09:05:00', '2026-01-03 09:05:00', 0),
-- diana：余额充足，且有冻结金额（模拟进行中订单）
(2000004, 1000004, 30000.00, 1500.00, 1, 2, '2026-01-04 09:05:00', '2026-04-01 14:00:00', 0),
-- eve：账户状态冻结（status=0）
(2000005, 1000005, 3000.00,  0.00,    0, 0, '2026-01-05 09:05:00', '2026-02-01 10:05:00', 0);

SELECT '账户数据插入完成' AS msg, COUNT(*) AS total FROM t_account;

-- =============================================
-- 3. 商品数据 trade_product.t_product
-- =============================================
USE trade_product;

TRUNCATE TABLE t_product;

INSERT INTO t_product (id, product_code, product_name, current_price, category, status, version, create_time, update_time, deleted) VALUES
-- 与订单数据保持一致
(101, '600036', '招商银行股票',    45.50,  1, 1, 0, '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(102, '601318', '中国平安股票',   1680.00, 1, 1, 0, '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(103, '600519', '贵州茅台股票',   220.00,  1, 1, 0, '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(104, '000001', '平安银行股票',   12.50,   1, 1, 0, '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(105, '601319', '中国平安股票B',  50.00,   1, 1, 0, '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0),
(106, '601398', '工商银行股票',    3.00,    1, 1, 0, '2026-01-01 08:00:00', '2026-01-01 08:00:00', 0);

SELECT '商品数据插入完成' AS msg, COUNT(*) AS total FROM t_product;

-- =============================================
-- 4. 订单数据 trade_order.t_order
-- =============================================
USE trade_order;

TRUNCATE TABLE t_order;

INSERT INTO t_order (id, order_no, user_id, product_id, product_name, price, quantity, amount, status, create_time, update_time, deleted) VALUES
-- alice 的订单
-- 状态 1：待支付（可直接调用 /order/pay/{id} 测试）
(3000001, 'ORD20260101001', 1000001, 101, '招商银行股票(600036)', 45.50,  100, 4550.00,  1, '2026-04-01 10:00:00', '2026-04-01 10:00:00', 0),
-- 状态 2：已支付
(3000002, 'ORD20260101002', 1000001, 102, '贵州茅台股票(600519)', 1680.00, 5,  8400.00,  2, '2026-04-02 10:00:00', '2026-04-02 11:00:00', 0),
-- 状态 3：已完成
(3000003, 'ORD20260101003', 1000001, 103, '比亚迪股票(002594)',   220.00, 20,  4400.00,  3, '2026-04-03 10:00:00', '2026-04-03 15:00:00', 0),

-- bob 的订单
-- 状态 1：待支付（价格 = 3000，bob 余额 8000，支付可成功）
(3000004, 'ORD20260102001', 1000002, 104, '宁德时代股票(300750)', 150.00,  20, 3000.00,  1, '2026-04-05 09:00:00', '2026-04-05 09:00:00', 0),
-- 状态 4：已取消
(3000005, 'ORD20260102002', 1000002, 101, '招商银行股票(600036)', 46.00,   50, 2300.00,  4, '2026-04-06 09:00:00', '2026-04-06 10:00:00', 0),

-- charlie 的订单
-- 状态 1：待支付（价格 = 500，charlie 余额 200，支付必然失败 → 验证 Seata 回滚）
(3000006, 'ORD20260103001', 1000003, 105, '中国平安股票(601318)', 50.00,   10, 500.00,   1, '2026-04-10 11:00:00', '2026-04-10 11:00:00', 0),

-- diana 的订单（已有冻结余额对应）
-- 状态 2：已支付（amount=1500，对应 diana 的 frozen_amount=1500）
(3000007, 'ORD20260104001', 1000004, 106, '工商银行股票(601398)', 3.00,   500, 1500.00,  2, '2026-04-01 14:00:00', '2026-04-01 15:00:00', 0);

SELECT '订单数据插入完成' AS msg, COUNT(*) AS total FROM t_order;

-- =============================================
-- 4. 交易数据 trade_trade.t_trade
-- =============================================
USE trade_trade;

TRUNCATE TABLE t_trade;

INSERT INTO t_trade (id, trade_no, order_id, user_id, product_id, price, quantity, amount, direction, status, create_time, update_time, deleted) VALUES
-- alice 的交易记录（对应已支付/已完成订单）
(4000001, 'TRD20260101001', 3000002, 1000001, 102, 1680.00, 5,   8400.00, 1, 2, '2026-04-02 11:00:00', '2026-04-02 11:00:00', 0),
(4000002, 'TRD20260101002', 3000003, 1000001, 103, 220.00,  20,  4400.00, 1, 2, '2026-04-03 15:00:00', '2026-04-03 15:00:00', 0),
-- alice 的卖出交易（无关联订单，直接交易）
(4000003, 'TRD20260101003', NULL,    1000001, 101, 46.00,   100, 4600.00, 2, 2, '2026-04-04 14:00:00', '2026-04-04 14:00:00', 0),

-- diana 的交易记录（对应已支付订单）
(4000004, 'TRD20260104001', 3000007, 1000004, 106, 3.00,   500,  1500.00, 1, 2, '2026-04-01 15:00:00', '2026-04-01 15:00:00', 0);

SELECT '交易数据插入完成' AS msg, COUNT(*) AS total FROM t_trade;

-- =============================================
-- 5. 持仓数据 trade_trade.t_position
-- =============================================
USE trade_trade;

TRUNCATE TABLE t_position;

INSERT INTO t_position (id, user_id, product_id, product_name, quantity, avg_cost, current_price, status, version, create_time, update_time, deleted) VALUES
-- alice 的持仓（对应历史买入交易）
(5000001, 1000001, 102, '中国平安股票',   5,   1680.00, 1680.00, 1, 0, '2026-04-02 11:00:00', '2026-04-03 15:00:00', 0),
(5000002, 1000001, 103, '贵州茅台股票',   20,  220.00,  220.00,  1, 0, '2026-04-03 15:00:00', '2026-04-03 15:00:00', 0),
-- diana 的持仓（对应历史买入交易）
(5000003, 1000004, 106, '工商银行股票',  500, 3.00,    3.00,   1, 0, '2026-04-01 15:00:00', '2026-04-01 15:00:00', 0);

SELECT '持仓数据插入完成' AS msg, COUNT(*) AS total FROM t_position;

-- =============================================
-- 6. 数据概览（运行后验证）
-- =============================================
SELECT '========== 数据概览 ==========' AS `separator`;

SELECT '用户表' AS tbl, COUNT(*) AS total, SUM(status=1) AS 正常, SUM(status=0) AS 禁用 FROM trade_user.t_user;
SELECT '账户表' AS tbl, COUNT(*) AS total, SUM(status=1) AS 正常, SUM(status=0) AS 冻结,
       FORMAT(SUM(balance),2) AS 总余额, FORMAT(SUM(frozen_amount),2) AS 总冻结 FROM trade_account.t_account;
SELECT '订单表' AS tbl, COUNT(*) AS total,
       SUM(status=1) AS 待支付, SUM(status=2) AS 已支付, SUM(status=3) AS 已完成, SUM(status=4) AS 已取消
FROM trade_order.t_order;
SELECT '交易表' AS tbl, COUNT(*) AS total, SUM(direction=1) AS 买入, SUM(direction=2) AS 卖出 FROM trade_trade.t_trade;
SELECT '持仓表' AS tbl, COUNT(*) AS total, SUM(status=1) AS 正常, SUM(status=0) AS 已清仓 FROM trade_trade.t_position;
SELECT '商品表' AS tbl, COUNT(*) AS total, SUM(status=1) AS 上架, SUM(status=0) AS 下架 FROM trade_product.t_product;

-- =============================================
-- 6. 测试场景速查
-- =============================================
/*
【场景 A】正常支付测试（Seata 全链路提交）
  接口：PUT /order/pay/3000001
  用户：alice（userId=1000001）
  订单：待支付，金额=4550
  账户：余额=50000，足够支付
  预期：
    t_order.status → 2
    t_account.balance 减少 4550
    t_trade 新增一条记录

【场景 B】余额不足 → Seata 全局回滚
  接口：PUT /order/pay/3000006
  用户：charlie（userId=1000003）
  订单：待支付，金额=500
  账户：余额=200，不足
  预期：
    t_order 无变化（回滚）
    t_account 无变化（回滚）
    t_trade 无新数据（回滚）

【场景 C】bob 支付测试（余额刚好够）
  接口：PUT /order/pay/3000004
  用户：bob（userId=1000002）
  订单：待支付，金额=3000
  账户：余额=8000
  预期：支付成功，余额剩 5000（deduct 扣减冻结金额）

【查询验证 SQL】
  SELECT * FROM trade_order.t_order WHERE id = 3000001;
  SELECT * FROM trade_account.t_account WHERE user_id = 1000001;
  SELECT * FROM trade_trade.t_trade WHERE user_id = 1000001;
*/
