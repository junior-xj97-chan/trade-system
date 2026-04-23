# 金融交易系统 - 微服务版

> Spring Cloud Alibaba 2023 微服务架构实践项目 | 股票买入/卖出/持仓管理

## 📁 项目结构

```
trade-system/
├── pom.xml                      # 父工程
├── README.md                     # 项目文档
├── sql/                          # 数据库脚本
│   ├── user.sql                  # 用户表
│   ├── order.sql                 # 订单表
│   ├── account.sql               # 账户表
│   ├── trade.sql                 # 交易表
│   ├── product.sql               # 商品表
│   └── position.sql              # 持仓表
├── nacos-config/                 # Nacos 共享配置
│   ├── shared-common.yml         # 公共配置（Redis/数据库）
│   ├── shared-sentinel.yml       # Sentinel 配置
│   └── shared-xxljob.yml         # XXL-JOB 配置
├── docs/                         # 开发文档
│   └── SEATA-联调指南.md        # Seata 分布式事务联调测试文档
├── common/                       # 公共模块
│   └── src/main/java/com/trade/common/
│       ├── R.java               # 统一响应封装
│       ├── BizCode.java          # 业务错误码枚举（1xxx~8xxx）
│       ├── BusinessException.java # 业务异常
│       ├── GlobalExceptionHandler.java # 全局异常处理
│       ├── PageResult.java       # 分页结果封装
│       └── RedisConfig.java      # Redis 配置
├── gateway/                      # API网关 (端口: 9000)
│   └── src/main/java/com/trade/gateway/
│       └── filter/AuthFilter.java # 全局鉴权过滤器
├── user-service/                # 用户服务 (端口: 9001)
│   └── src/main/java/com/trade/user/
│       ├── controller/UserController.java
│       ├── service/UserService.java
│       ├── entity/User.java
│       └── mapper/UserMapper.java
├── order-service/               # 订单服务 (端口: 9002)
│   └── src/main/java/com/trade/order/
│       ├── controller/OrderController.java
│       ├── service/OrderService.java
│       ├── entity/Order.java
│       ├── mapper/OrderMapper.java
│       ├── feign/AccountFeignClient.java    # 账户服务调用
│       ├── feign/TradeFeignClient.java       # 交易服务调用
│       ├── feign/PositionFeignClient.java   # 持仓服务调用
│       └── task/OrderTimeoutTask.java        # 订单超时任务
├── trade-service/               # 交易服务 (端口: 9003)
│   └── src/main/java/com/trade/trade/
│       ├── controller/
│       │   ├── TradeController.java    # 交易管理
│       │   └── PositionController.java # 持仓管理
│       ├── service/
│       │   ├── TradeService.java
│       │   └── PositionService.java   # 持仓服务
│       ├── entity/
│       │   ├── Trade.java
│       │   └── Position.java
│       └── mapper/
│           ├── TradeMapper.java
│           └── PositionMapper.java
├── account-service/             # 账户服务 (端口: 9004)
│   └── src/main/java/com/trade/account/
│       ├── controller/AccountController.java
│       ├── service/AccountService.java
│       ├── entity/Account.java
│       └── mapper/AccountMapper.java
└── product-service/             # 商品服务 (端口: 9005)
    └── src/main/java/com/trade/product/
        ├── controller/ProductController.java
        ├── service/ProductService.java
        ├── entity/Product.java
        └── mapper/ProductMapper.java
```

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 运行环境 |
| Spring Boot | 3.3.5 | 基础框架 |
| Spring Cloud | 2023.0.3 | 微服务生态 |
| Spring Cloud Alibaba | 2023.0.3.2 | 阿里生态组件 |
| Nacos | 2.x | 服务注册/配置中心 |
| OpenFeign | 4.x | 服务间调用 |
| Seata | 2.x | 分布式事务（AT模式） |
| Sentinel | 2.x | 流量控制/熔断降级 |
| XXL-JOB | 3.4.0 | 任务调度 |
| MyBatis-Plus | 3.5.9 | ORM框架 |
| Redis | 7.x | 缓存/会话存储 |
| Lombok | 1.18.40 | 简化代码 |
| Swagger/OpenAPI | 3.0 | 接口文档 |

## 🚀 启动顺序

```
1. Nacos        sh startup.sh -m standalone (Linux) / cmd startup.cmd -m standalone (Windows)
2. Seata Server seata-server.bat (Windows) / sh seata-server.sh (Linux)
3. Sentinel     java -Dserver.port=8858 -jar sentinel-dashboard.jar (端口 8858)
4. Redis        redis-server
5. MySQL        创建数据库和表
6. XXL-JOB      java -jar xxl-job-admin-3.4.0.jar (端口 8080)

7. 微服务（按顺序启动）
   ├── gateway          9000  (网关入口)
   ├── user-service     9001  (用户服务)
   ├── account-service  9004  (账户服务)
   ├── order-service    9002  (订单服务)
   ├── trade-service    9003  (交易服务)
   └── product-service  9005  (商品服务)
```

## 📝 数据库设计

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ trade_user  │     │ trade_order │     │ trade_trade │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ t_user      │     │ t_order     │     │ t_trade     │
└─────────────┘     └─────────────┘     │ t_position  │
                      ↑                  │ undo_log    │
                      │ 订单关联          └─────────────┘
┌─────────────┐       │                      ↑
│trade_account│      │                      │ 持仓关联
├─────────────┤       │                      │
│ t_account   │       │                      │
│ undo_log    │───────┘ 支付/退款             │
└─────────────┘                              │
                                              │
┌─────────────┐       ┌─────────────┐         │
│trade_product│       │ Seata TC    │◄────────┘
├─────────────┤       │ (事务协调)  │   分布式事务
│ t_product   │       └─────────────┘
└─────────────┘
```

### 表结构说明

| 数据库 | 表名 | 说明 |
|--------|------|------|
| trade_user | t_user | 用户信息 |
| trade_order | t_order | 订单（买入/卖出） |
| trade_account | t_account | 账户余额/冻结资金 |
| trade_trade | t_trade | 交易记录 |
| trade_trade | t_position | 持仓记录 |
| trade_product | t_product | 商品/股票 |

### 核心字段设计

```sql
-- 账户表：余额 + 冻结资金（两阶段支付）
balance       DECIMAL(15,2)  -- 可用余额
frozen_amount DECIMAL(15,2)  -- 冻结金额（支付中）

-- 订单表：支持买入和卖出
direction  INT  -- 1:买入 2:卖出
status     INT  -- 1:待支付 2:已支付 3:已完成 4:已取消

-- 持仓表：买入加仓/卖出减仓
quantity    INT           -- 持有数量
avg_cost    DECIMAL(18,2)-- 平均成本价
current_price DECIMAL     -- 当前价格

-- 所有表：乐观锁 + 逻辑删除
version  INT  -- 乐观锁版本号
deleted  INT  -- 逻辑删除（0未删除 1已删除）
```

## 📚 API 接口

### 用户服务
| 接口 | 方法 | 说明 |
|------|------|------|
| `/user/register` | POST | 用户注册 |
| `/user/login` | POST | 用户登录（返回 Token） |
| `/user/{id}` | GET | 获取用户信息 |
| `/user` | GET | 分页查询用户（管理员） |

### 账户服务
| 接口 | 方法 | 说明 |
|------|------|------|
| `/account/create` | POST | 创建账户 |
| `/account/{userId}` | GET | 查询账户 |
| `/account/freeze` | POST | 冻结资金（买入时） |
| `/account/unfreeze` | POST | 解冻资金（退款时） |
| `/account/deduct` | POST | 扣减余额（支付成功） |
| `/account/recharge` | POST | 充值 |
| `/account/refund` | POST | 退款（取消订单） |
| `/account/sellReceive` | POST | 卖出收款（增加余额） |

### 订单服务
| 接口 | 方法 | 说明 |
|------|------|------|
| `/order/create` | POST | 创建订单 |
| `/order/{id}` | GET | 查询订单 |
| `/order/pay/{id}` | PUT | 支付订单（买入） |
| `/order/sell/{id}` | PUT | 卖出订单 |
| `/order/cancel/{id}` | PUT | 取消订单 |

### 交易服务
| 接口 | 方法 | 说明 |
|------|------|------|
| `/trade/execute` | POST | 执行交易 |
| `/trade/refund` | POST | 退款交易 |
| `/trade/{id}` | GET | 查询交易 |
| `/trade/no/{tradeNo}` | GET | 根据交易单号查询 |
| `/position/user/{userId}` | GET | 查询用户持仓 |
| `/position/user/{userId}/product/{productId}` | GET | 查询单只持仓 |
| `/position/page` | GET | 分页持仓 |
| `/position/updatePrice` | POST | 更新持仓价格 |

### 商品服务
| 接口 | 方法 | 说明 |
|------|------|------|
| `/product/page` | GET | 分页查询（带缓存） |
| `/product/code/{code}` | GET | 按代码查询 |
| `/product/{id}` | GET | 按ID查询 |

## 🔥 业务流程

### 买入流程（分布式事务）
```
用户下单 → 冻结余额 → 扣减余额 → 创建交易 → 更新持仓 → 完成
     │          │          │          │          │
     └──────────┴──────────┴──────────┴────Seata TC────┘
                              任意失败 → 全局回滚
```

### 卖出流程（分布式事务）
```
用户下单 → 卖出收款 → 创建交易 → 更新持仓 → 完成
     │          │          │          │
     └──────────┴──────────┴────Seata TC────┘
```

### 取消订单流程
```
待支付订单 → 直接取消
已支付订单 → 退款到账户 → 退款交易 → 更新订单
```

## ✨ 核心亮点

### 1. 微服务架构
- **6 个微服务**：Gateway + User + Order + Trade + Account + Product
- **Nacos 服务注册发现**：自动注册、健康检查
- **Nacos 配置中心**：shared-configs 共享配置

### 2. 分布式事务（Seata AT 模式）
- **支付订单**：`@GlobalTransactional` 覆盖 5 个步骤
- **卖出订单**：`@GlobalTransactional` 覆盖 4 个步骤
- **取消订单**：支持部分回滚（待支付直接取消，已支付全额退款）
- **undo_log 自动补偿**：失败时逆向恢复数据

### 3. Gateway 统一鉴权
- **全局 Token 校验**：从 Redis 验证用户登录状态
- **白名单放行**：登录/注册/Swagger 文档
- **Token 续期**：每次访问自动延长过期时间
- **userId 透传**：注入到 Header 传递给下游服务

### 4. 持仓管理
- **买入加仓**：自动计算加权平均成本
- **卖出减仓**：校验持仓数量是否充足
- **清仓处理**：全部卖出时标记已清仓
- **行情同步**：支持批量更新持仓价格

### 5. 流量保护（Sentinel）
- **@SentinelResource**：资源级别限流
- **BlockHandler**：限流/熔断降级处理
- **FallbackFactory**：Feign 客户端熔断降级

### 6. 任务调度（XXL-JOB）
- **订单超时检测**：每分钟扫描待支付订单
- **超时自动取消**：超过 30 分钟自动取消
- **Web 可视化管理**：动态修改 Cron 表达式

### 7. 统一异常处理
- **BusinessException**：带错误码的业务异常
- **BizCode 枚举**：1xxx~8xxx 分类管理
- **GlobalExceptionHandler**：统一响应格式

### 8. 接口幂等性保障
- **Redis Token 去重**：防止重复支付、退款、充值等操作
- **统一错误码**：`DUPLICATE_REQUEST(8001)` 请求已处理
- **幂等 Key 规则**：
  - 订单类：`order:pay:{id}`、`order:sell:{id}`、`order:cancel:{id}`
  - 账户类：`account:{操作}:{userId}:{amount}`
  - 持仓类：`position:{操作}:{userId}:{productId}:{quantity}`
  - 交易类：`trade:{操作}:{orderId}`
- **30分钟过期**：允许用户稍后重新发起请求

## 📞 后续优化方向

### 🔴 高优先级（面试重点）

| 功能 | 说明 | 状态 |
|------|------|------|
| **SkyWalking 链路追踪** | 可视化调用链路、性能分析 | ⏳ 待实现 |

### ✅ 已完成

| 功能 | 说明 | 完成时间 |
|------|------|----------|
| **RabbitMQ 消息队列** | 订单支付成功后异步解耦（seckill-system 实现） | 2026-04-18 |
| **Redis + RabbitMQ 秒杀系统** | 高并发场景，Redis预减库存 + MQ异步下单，500并发零超卖 | 2026-04-18 |
| **JMeter 压测** | 压测报告 QPS:75.8 / 响应时间:13ms / 超卖率:0% | 2026-04-18 |
| **Nacos 配置中心** | shared-common/sentinel/xxljob 统一管理 | 2026-04-20 |
| **接口幂等性保障** | Redis Token 机制防止重复支付、退款、充值等 | 2026-04-23 |

### 🟢 低优先级（锦上添花）

| 功能 | 说明 |
|------|------|
| Docker 容器化部署 | K8s 编排 |
| ELK 日志收集 | 集中日志管理 |
| 定时任务管理表 | XXL-JOB 任务可视化 |

---

## 📊 压测目标（参考）

```
目标：支撑 1000+ QPS

测试场景：
├── 登录接口      目标 QPS: 500
├── 商品查询       目标 QPS: 1000 (Redis 缓存)
├── 下单接口       目标 QPS: 200 (分布式事务限制)
└── 支付接口       目标 QPS: 100 (Seata 事务链)
```

---

## 📞 联系方式

项目用于简历展示，涵盖微服务核心知识点：
- 服务治理（注册发现、负载均衡）
- 分布式事务（Seata AT 模式）
- 流量防护（Sentinel 熔断降级）
- 异步解耦（RabbitMQ）
- 缓存优化（Redis）
- 任务调度（XXL-JOB）
