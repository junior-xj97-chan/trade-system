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
        ├── mapper/ProductMapper.java
        └── feign/ProductFeignClient.java    # 对外暴露商品查询接口
        └── feign/fallback/ProductFeignFallbackFactory.java  # Feign 降级
└── search-service/              # 搜索服务 (端口: 9006)
    └── src/main/java/com/trade/search/
        ├── controller/SearchController.java  # 搜索接口
        ├── service/
        │   ├── ProductSearchService.java      # ES 搜索服务
        │   └── DataSyncService.java           # 数据同步服务
        ├── feign/ProductFeignClient.java    # 商品服务调用（查最新数据）
        ├── feign/fallback/ProductFeignFallbackFactory.java  # Feign 降级
        ├── consumer/ProductSyncConsumer.java # MQ 消费者（实时同步）
        ├── job/DataSyncJob.java              # XXL-JOB 同步任务
        ├── config/XxlJobConfig.java           # XXL-JOB 执行器配置
        ├── document/ProductDocument.java      # ES 文档实体
        ├── repository/ProductSearchRepository.java  # ES 仓库
        ├── mapper/ProductMapper.java           # MySQL Mapper
        └── dto/
            ├── SearchRequest.java              # 搜索请求
            └── SearchResponse.java              # 搜索响应
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
| RabbitMQ | 3.x | 消息队列/异步解耦 |
| XXL-JOB | 3.4.0 | 任务调度 |
| SkyWalking | 10.4.0 | 链路追踪 |
| MyBatis-Plus | 3.5.9 | ORM框架 |
| Redis | 7.x | 缓存/会话存储 |
| Elasticsearch | 8.x | 全文搜索引擎 |
| IK Analyzer | 8.12.0 | 中文分词器 |
| Lombok | 1.18.40 | 简化代码 |
| Swagger/OpenAPI | 3.0 | 接口文档 |
| ShardingSphere | 5.x | 分库分表（预留方案，待数据量达阈值后启用） |

## 🚀 启动顺序

> **提示**：有两套启动脚本，分别适用于不同场景：
> - `start-scripts/`：本地 IDEA/命令行运行微服务（通过 `-javaagent` 加载 SkyWalking Agent）
> - `docker-scripts/`：Docker 容器化运行微服务（通过 `JAVA_TOOL_OPTIONS` 环境变量注入）

### 方式一：Docker 中间件 + 本地微服务（推荐）

```powershell
# 1. 启动所有中间件容器
cd C:\Users\13129\WorkBuddy\Claw\trade-system\docker
docker compose up -d

# 2. 等待约 2 分钟后，上传 Nacos 共享配置
cd ..\nacos-config
.\upload-config-docker.ps1

# 3. 启动本地微服务（带 SkyWalking Agent）
cd ..\start-scripts
start-all.bat
```

### 方式二：本地中间件 + 本地微服务

```
1. MySQL        Windows 服务自动启动（端口 3306）
2. Redis        Windows 服务自动启动（端口 6379）
3. RabbitMQ     Windows 服务自动启动（端口 5672，控制台 15672）
4. Nacos        D:\nacos\bin\startup.cmd -m standalone (端口 8848)
5. Seata        D:\seata\seata-server.bat (端口 8091)
6. Sentinel     D:\sentinel\start_sentinel.bat (端口 8858）
7. XXL-JOB      D:\xxl-job\start_xxl_job.bat (端口 8081)
8. SkyWalking   D:\skywalking-apm\bin\startup.bat (UI端口 8088)

微服务：trade-system/start-scripts/start-all.bat
```

### 方式三：Docker 中间件 + Docker 微服务（完全容器化）

```bash
# 1. 启动所有中间件容器
cd docker
docker compose up -d

# 2. 等待约 2 分钟后，上传 Nacos 共享配置
cd ../nacos-config
./upload-config-docker.ps1

# 3. 构建微服务镜像（Jib）
mvn clean package jib:dockerBuild -DskipTests

# 4. 启动微服务容器
cd ../docker-scripts
bash docker-start-all.sh
```

**注意**：Docker 微服务模式需要在 `docker-scripts/` 目录下放置 SkyWalking Agent，或在构建镜像时将其打包进去。

**容器访问地址：**
| 服务 | 地址 | 说明 |
|------|------|------|
| Nacos | http://localhost:8848/nacos | nacos/nacos |
| SkyWalking | http://localhost:8088 | 链路追踪 |
| Sentinel | http://localhost:8858 | sentinel/sentinel |
| XXL-JOB | http://localhost:8081 | admin/123456 |
| RabbitMQ | http://localhost:15672 | guest/guest |
| Elasticsearch | http://localhost:9200 | 搜索引擎 |
| Kibana | http://localhost:5601 | ES 可视化（汉化） |

### 方式二：本地中间件部署

```
1. MySQL        Windows 服务自动启动（端口 3306）
2. Redis        Windows 服务自动启动（端口 6379）
3. RabbitMQ     Windows 服务自动启动（端口 5672，控制台 15672）
4. Nacos        D:\nacos\bin\startup.cmd -m standalone (端口 8848)
5. Seata        D:\seata\seata-server.bat (端口 8091)
6. Sentinel     D:\sentinel\start_sentinel.bat (端口 8858）
7. XXL-JOB      D:\xxl-job\start_xxl_job.bat (端口 8081)
8. SkyWalking   D:\skywalking-apm\bin\startup.bat (UI端口 8088)

微服务：trade-system/start-scripts/start-all.bat
```

**微服务端口：**
| 服务 | 端口 | 说明 |
|------|------|------|
| gateway | 9000 | 网关入口 |
| user-service | 9001 | 用户服务 |
| order-service | 9002 | 订单服务 |
| trade-service | 9003 | 交易/持仓服务 |
| account-service | 9004 | 账户服务 |
| product-service | 9005 | 商品服务 |
| search-service | 9006 | 搜索服务（ES） |
| search-service XXL-JOB | 9007 | XXL-JOB 执行器端口 |

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

### 搜索服务
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/search` | POST | 高级搜索（多条件组合） |
| `/api/search/quick` | GET | 快速关键词搜索 |
| `/api/search/code/{productCode}` | GET | 根据代码精确查询 |
| `/api/search/index` | POST | 索引单个商品 |
| `/api/search/index/batch` | POST | 批量索引商品 |
| `/api/search/index/{id}` | DELETE | 删除商品索引 |
| `/api/search/status` | GET | 检查索引状态 |
| `/api/search/index/create` | POST | 创建索引 |
| `/api/search/index/full` | POST | 全量同步（MySQL → ES） |
| `/api/search/index/incremental` | POST | 增量同步（MySQL → ES） |

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
- **ES 数据同步**：
  - `dataFullSyncJob`：全量同步 MySQL 商品数据到 ES（每天凌晨 2 点）
  - `dataIncrementalSyncJob`：增量同步 MySQL 商品数据到 ES（每小时）
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

### 9. 分布式链路追踪（SkyWalking）
- **SkyWalking 10.4.0**：可视化链路追踪平台
- **Agent 9.6.0**：Java Agent 自动注入，无需修改代码
- **监控能力**：
  - 服务拓扑图：可视化展示微服务调用关系
  - 调用链路追踪：完整记录每个请求的调用链
  - 性能分析：接口响应时间、吞吐量、慢查询
  - 数据库追踪：自动追踪 MySQL、Redis 操作
- **Agent 配置参数**：
  ```
  -javaagent:D:\skywalking-agent\skywalking-agent.jar
  -Dskywalking.agent.service_name=服务名
  -Dskywalking.collector.backend_service=127.0.0.1:11800
  ```
- **一键启动**：start-scripts/start-all.bat 自动加载 Agent

### 9. MQ 实时数据同步
- **商品变更实时同步**：product-service 增删改商品时发送 MQ 消息
- **search-service 消费**：监听商品同步队列，实时更新 ES 索引
- **支持操作**：新增(CREATE)、修改(UPDATE)、删除(DELETE)、上架(ONLINE)、下架(OFFLINE)
- **队列配置**：`product-sync-queue`，路由键 `product.sync`
- **数据一致性保证**：
  - CREATE：直接用 MQ 消息数据创建 ES 文档
  - UPDATE/ONLINE/OFFLINE：**先通过 Feign 查询 product-service 获取数据库最新数据**，再更新 ES
  - DELETE：直接用 productId 删除 ES 文档

## 📞 后续优化方向

### ✅ 已完成

| 功能 | 说明 | 完成时间 |
|------|------|----------|
| **RabbitMQ 消息队列** | 订单支付成功后异步解耦 | 2026-04-18 |
| **Nacos 配置中心** | shared-common/sentinel/xxljob 统一管理 | 2026-04-20 |
| **接口幂等性保障** | Redis Token 机制防止重复支付、退款、充值等 | 2026-04-23 |
| **RabbitMQ 集成** | Topic 交换机 + 订单支付消息通知（order-paid-queue） | 2026-04-25 |
| **SkyWalking 链路追踪** | 可视化调用链路、服务拓扑、性能分析、数据库追踪 | 2026-04-25 |
| **Docker 容器化部署** | Docker Compose 一键启动 9 个中间件（MySQL/Redis/RabbitMQ/Nacos/Seata/SkyWalking/Sentinel/XXL-JOB） | 2026-04-26 |
| **XXL-JOB 任务调度** | 订单超时检测（每分钟扫描，30分钟自动取消）+ Web 可视化管理 | 2026-04-26 |
| **MQ 实时数据同步** | product-service 商品变更 → MQ → search-service 实时同步 ES | 2026-04-27 |
| **Elasticsearch 搜索服务** | 基于 ES + IK 中文分词器的商品搜索服务，支持关键词搜索、多条件过滤、排序分页、XXL-JOB 定时同步、MQ 实时同步 | 2026-04-27 |

### 🟢 低优先级（锦上添花）

| 功能 | 说明 |
|------|------|
| ELK 日志收集 | 集中日志管理 |
| **分库分表（ShardingSphere）** | 订单/交易表水平分片，详见下方"扩展方案"章节 |

---

## 📋 扩展方案预留：分库分表（ShardingSphere）

> **当前状态**：方案预留，代码暂无改动。  
> **触发条件**：`t_order` 或 `t_trade` 单表数据量超过 **2000 万行**，或写入 QPS 出现瓶颈时启用。

### 为什么选 ShardingSphere-JDBC

- **无代理侵入**：以 JDBC Driver 形式引入，业务代码零改动
- **与 Seata 兼容**：官方支持 Seata AT 分布式事务
- **Spring Boot 3.x 适配**：5.5.x 版本已支持

### 分片目标表

| 服务 | 表 | 分片键 | 分片数 | 策略 |
|------|----|----|----|----|
| order-service | `t_order` | `user_id` | 4 | 取模：`user_id % 4` |
| trade-service | `t_trade` | `user_id` | 4 | 取模：`user_id % 4` |

> 其余表（t_user、t_account、t_product、t_position）数据量相对小，暂不分片。

### 分片后物理表命名

```
trade_order 库：
  t_order_0、t_order_1、t_order_2、t_order_3

trade_trade 库：
  t_trade_0、t_trade_1、t_trade_2、t_trade_3
```

### 接入步骤（待执行）

**1. 添加依赖（order-service / trade-service）**
```xml
<dependency>
    <groupId>org.apache.shardingsphere</groupId>
    <artifactId>shardingsphere-jdbc</artifactId>
    <version>5.5.0</version>
</dependency>
```

**2. 修改数据源配置**
```yaml
spring:
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:sharding.yaml
```

**3. sharding.yaml 核心配置（以 order-service 为例）**
```yaml
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://127.0.0.1:3306/trade_order
    username: root
    password: root123

rules:
  - !SHARDING
    tables:
      t_order:
        actualDataNodes: ds_0.t_order_${0..3}
        tableStrategy:
          standard:
            shardingColumn: user_id
            shardingAlgorithmName: t_order_mod
    shardingAlgorithms:
      t_order_mod:
        type: INLINE
        props:
          algorithm-expression: t_order_${user_id % 4}
    keyGenerators:
      snowflake:
        type: SNOWFLAKE          # 全局唯一ID，替代自增主键

props:
  sql-show: true                 # 开发阶段开启，生产关闭
```

**4. 提前建好物理分片表**
```sql
-- 在 trade_order 库中执行
CREATE TABLE t_order_0 LIKE t_order;
CREATE TABLE t_order_1 LIKE t_order;
CREATE TABLE t_order_2 LIKE t_order;
CREATE TABLE t_order_3 LIKE t_order;
```

### 注意事项

| 问题 | 解决方案 |
|------|---------|
| 自增ID在多表中重复 | 启用 ShardingSphere Snowflake 全局ID生成 |
| 跨分片分页查询性能下降 | 查询时尽量携带 `user_id` 作为路由条件 |
| 与 Seata AT 模式兼容 | ShardingSphere 5.x 官方支持，无需额外处理 |
| 扩容（4片→8片）数据迁移 | 需提前规划，或初期直接分8片预留容量 |

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
- 全文搜索（Elasticsearch + IK 分词）
