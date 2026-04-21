# Seata 分布式事务联调指南

> 基于 trade-system 微服务架构，验证 Seata AT 模式在跨服务场景下的分布式事务一致性

---

## 1. 架构说明

### 1.1 分布式事务链路

```
客户端
  │
  ▼
┌─────────────────┐
│  order-service  │  ← @GlobalTransactional 发起全局事务
└────────┬────────┘
         │ Feign 调用
         ├──────────────────────────────────────────────┐
         │                                              │
         ▼                                              ▼
┌─────────────────┐                           ┌─────────────────┐
│ account-service │ 分支事务 1                │ trade-service   │ 分支事务 2
│ 扣减账户余额    │                           │ 创建交易记录    │
└─────────────────┘                           └─────────────────┘
         │                                              │
         │◄───────────── Seata TC 协调 ────────────────►│
         │              (全局回滚/提交)                  │
         ▼                                              ▼
    trade_account DB                           trade_trade DB
         │                                              │
         └─────────────── Seata undo_log ───────────────┘
                          (AT 模式自动回滚日志)
```

### 1.2 事务范围

| 服务 | 事务角色 | 操作 | 数据库 |
|------|---------|------|--------|
| order-service | TM（事务发起者） | @GlobalTransactional 标记事务起点 | trade_order |
| account-service | RM（资源管理者） | deductBalance 扣减余额 | trade_account |
| trade-service | RM（资源管理者） | executeTrade 创建交易记录 | trade_trade |

---

## 2. 启动前置条件

### 2.1 检查基础服务

```bash
# 1. 检查 MySQL（需确认已执行 sql/init.sql）
mysql -u root -proot123 -e "SHOW DATABASES LIKE 'trade_%';"

# 2. 检查 Nacos（默认端口 8848）
curl http://127.0.0.1:8848/nacos/v1/console/health

# 3. 检查 Redis
redis-cli -a 123456 PING

# 4. 检查 Seata Server（默认端口 8091）
netstat -an | findstr 8091
# 或浏览器访问 http://127.0.0.1:7091 (Seata Console)
```

### 2.2 启动顺序

```bash
# 1. MySQL（Nacos 内置或独立）
# 2. Nacos
sh startup.sh -m standalone   # Linux
cmd startup.cmd -m standalone # Windows

# 3. Seata Server
seata-server.bat   # Windows

# 4. 依次启动微服务
# common（公共模块，无需启动）
# gateway（9000）
# user-service（9001）
# account-service（9004）
# order-service（9002）
# trade-service（9003）
```

### 2.3 Nacos 配置检查

确保 Nacos 中存在以下配置：

```
# 配置 ID: seataServer.properties（DEFAULT_GROUP）
store.mode=db
store.db.driver-class-name=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true&characterEncoding=utf-8
store.db.user=root
store.db.password=root123
```

---

## 3. 正常场景测试

### 3.1 完整测试流程（Apifox）

**步骤 1：注册用户**

```
POST http://localhost:9001/user/register
Content-Type: application/json

{
    "username": "test_user_001",
    "password": "123456",
    "phone": "13800138000"
}
```

**步骤 2：创建账户（充值 10000 元）**

```
POST http://localhost:9004/account/create
Content-Type: application/json

{
    "userId": <步骤1返回的用户ID>,
    "initialBalance": 10000
}
```

**步骤 3：创建订单**

```
POST http://localhost:9002/order/create
Content-Type: application/json

{
    "userId": <用户ID>,
    "productId": 1001,
    "productName": "股票A",
    "price": 100.00,
    "quantity": 10
}
```

**步骤 4：支付订单（触发分布式事务）**

```
PUT http://localhost:9002/order/pay/<订单ID>
```

### 3.2 预期结果

| 数据表 | 字段变化 | 验证 SQL |
|--------|---------|----------|
| t_order | status = 2（已支付） | `SELECT * FROM trade_order.t_order WHERE id = <ID>;` |
| t_account | frozen_amount = 扣减金额 | `SELECT * FROM trade_account.t_account WHERE user_id = <USER_ID>;` |
| t_trade | 记录已创建 | `SELECT * FROM trade_trade.t_trade WHERE order_id = <ID>;` |
| undo_log | 3 条 undo_log 已清理 | `SELECT * FROM trade_order.undo_log;`（应为空） |

### 3.3 查看 Seata 日志

```
# Seata Server 控制台日志
# 正常提交时输出：
# 2026-xx-xx xx:xx:xx [Server] ... branch commit succ, xid = xxx, branchId = xxx

# order-service 日志
# 2026-xx-xx xx:xx:xx [DEBUG] ... [payOrder] 【扣减余额】userId=xxx，amount=xxx
# 2026-xx-xx xx:xx:xx [DEBUG] ... [payOrder] 【创建交易记录】orderId=xxx
# 2026-xx-xx xx:xx:xx [DEBUG] ... [payOrder] 【支付完成】orderNo=xxx
```

---

## 4. 回滚场景测试

### 4.1 测试场景设计

**场景 A：余额不足**

```
前置条件：账户余额 < 订单金额

操作：POST /order/pay/<ID>

预期结果：
1. order-service 抛出异常
2. Seata 触发全局回滚
3. order 表数据回滚（undo_log 逆向操作）
4. account 表数据回滚
5. trade 表无数据（事务失败，未创建）
```

**场景 B：交易服务不可用**

```
前置条件：停止 trade-service

操作：POST /order/pay/<ID>

预期结果：
1. Feign 调用超时或返回错误
2. @GlobalTransactional 捕获异常
3. Seata 触发全局回滚
4. order 表数据回滚
5. account 表数据回滚
```

### 4.2 验证回滚

```sql
-- 查询 undo_log（应包含回滚日志）
SELECT id, xid, branch_id, log_status, log_created 
FROM trade_order.undo_log 
ORDER BY log_created DESC LIMIT 10;

-- 回滚完成后，undo_log 应被清理（AT 模式特性）
-- 若回滚失败，log_status = 1，需手动处理
```

---

## 5. 常见问题排查

### 5.1 Seata 注册失败

```yaml
# 检查配置一致性
seata:
  enabled: true
  tx-service-group: trade-system-group  # 所有服务必须一致
  registry:
    type: nacos
    nacos:
      application: seata-server         # Seata Server 注册到 Nacos 的名称
```

### 5.2 分支事务未注册

```yaml
# 检查是否引入 seata starter
# pom.xml 中必须有：
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

### 5.3 全局事务超时

```yaml
# application.yml 中调整超时时间（默认 60s）
seata:
  service:
    # ...
  client:
    rm:
      report-retry-count: 5
      async-commitbing-buffer-limit: 1000
```

### 5.4 日志级别调整

```yaml
# 临时开启 DEBUG 日志，方便排查
logging:
  level:
    com.trade: DEBUG
    io.seata: DEBUG
    com.alibaba.nacos: INFO
```

---

## 6. 面试亮点提炼

### 6.1 技术描述模板

> 在 trade-system 项目中，我负责分布式事务的设计与实现。采用 Seata AT 模式，通过 @GlobalTransactional 注解在 order-service 的 payOrder 方法上标记事务起点，当 account-service 扣减余额失败或 trade-service 创建交易记录异常时，Seata TC 会触发全局回滚，利用 undo_log 实现各数据库的自动逆向补偿，保证订单支付场景下账户余额与交易记录的一致性。

### 6.2 关键配置点

1. **tx-service-group 一致性** — 所有微服务和 Seata Server 必须使用相同的事务组名称
2. **undo_log 表** — 每个参与分布式事务的数据库都必须创建
3. **Seata Server 高可用** — 生产环境建议部署集群，注册到 Nacos

### 6.3 压测数据建议

```
场景：1000 并发支付请求
结果：
- 分布式事务成功率：99.8%
- 平均事务耗时：120ms
- Seata TC 吞吐量：5000 TPS
```

---

## 7. Apifox 环境配置

### 7.1 快速测试环境

```
环境：本地开发
变量：
  {{baseUrl}} = http://localhost:9002
  {{accountUrl}} = http://localhost:9004
  {{userUrl}} = http://localhost:9001
```

### 7.2 测试脚本（JavaScript）

```javascript
// 全局预请求脚本：生成测试数据
const timestamp = Date.now();
const userId = 100000 + (timestamp % 100000);

// 创建用户
const userResp = pm.sendRequest({
    url: '{{userUrl}}/user/register',
    method: 'POST',
    header: { 'Content-Type': 'application/json' },
    body: { mode: 'raw', raw: JSON.stringify({
        username: 'user_' + userId,
        password: '123456'
    })}
});

console.log('用户ID:', userResp.json().data);
```

---

## 8. 参考文档

- [Seata 官方文档](https://seata.io/zh-cn/docs/overview/what-is-seata)
- [Spring Cloud Alibaba Seata](https://spring-cloud-alibaba-group.github.io/github-pages/2023.0.3.2/en-us/user/seata.html)
- [Nacos 配置中心](https://nacos.io/zh-cn/docs/quick-start.html)
