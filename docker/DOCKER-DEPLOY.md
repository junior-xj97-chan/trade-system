# Docker 中间件部署指南

## 概览

本方案使用 Docker Compose 将所有中间件容器化，微服务（Java 程序）仍运行在本地 JVM，通过固定 IP 访问 Docker 容器中的中间件。

```
本地 JVM 微服务 ←──→ Docker Bridge Network (172.20.0.0/16) ←──→ 各中间件容器
```

## 容器 IP 分配

| 容器 | 服务 | IP | 端口 |
|------|------|-----|------|
| trade-mysql | MySQL 8.0 | 172.20.0.10 | 3306 |
| trade-redis | Redis 7.2 | 172.20.0.11 | 6379 |
| trade-rabbitmq | RabbitMQ 3.12 | 172.20.0.12 | 5672 / 15672 |
| trade-nacos | Nacos 2.3.2 | 172.20.0.13 | 8848 / 9848 |
| trade-seata | Seata 2.1.0 | 172.20.0.14 | 8091 / 7091 |
| trade-skywalking-oap | SkyWalking OAP 9.6.0 | 172.20.0.15 | 11800 / 12800 |
| trade-skywalking-ui | SkyWalking UI 9.6.0 | 172.20.0.16 | 8088 |
| trade-sentinel | Sentinel 1.8.8 | 172.20.0.17 | 8858 |
| trade-xxljob | XXL-JOB 2.4.0 | 172.20.0.18 | 8081 |

---

## 前置检查

1. **停止本地中间件服务**（避免端口冲突）：
   - 停止 Nacos：关闭 startup.cmd 窗口
   - 停止 Seata：关闭 seata-server.bat 窗口
   - 停止 SkyWalking：关闭 startup.bat 窗口
   - 停止 Sentinel：关闭 Sentinel Dashboard 窗口
   - 停止 XXL-JOB：关闭对应窗口
   - 停止 MySQL：`services.msc` → MySQL 服务 → 停止（或 `net stop MySQL80`）
   - 停止 Redis：`services.msc` → Redis 服务 → 停止（或 `net stop Redis`）
   - 停止 RabbitMQ：`services.msc` → RabbitMQ 服务 → 停止

2. **确认 Docker Desktop 已启动**（任务栏有鲸鱼图标且无报错）

---

## 部署步骤

### 第一步：启动所有容器

```powershell
cd C:\Users\13129\WorkBuddy\Claw\trade-system\docker
docker compose up -d
```

首次启动会拉取镜像（约 2~5 GB），需要等待 5~10 分钟，取决于网络速度。

**查看启动进度：**
```powershell
docker compose ps
# 所有服务 Status 显示 healthy 或 running 即为正常
```

**查看某个服务日志：**
```powershell
docker compose logs -f nacos
docker compose logs -f seata
docker compose logs -f mysql
```

---

### 第二步：验证各中间件启动

等待约 2 分钟后，逐一验证：

| 服务 | 验证方式 |
|------|---------|
| MySQL | `docker exec trade-mysql mysqladmin ping -uroot -proot123` |
| Redis | `docker exec trade-redis redis-cli -a 123456 ping` |
| RabbitMQ | 浏览器打开 http://localhost:15672 (guest/guest) |
| Nacos | 浏览器打开 http://localhost:8848/nacos (nacos/nacos) |
| Seata | `docker logs trade-seata` 看到 "Server started" |
| SkyWalking | 浏览器打开 http://localhost:8088 |
| Sentinel | 浏览器打开 http://localhost:8858 (sentinel/sentinel) |
| XXL-JOB | 浏览器打开 http://localhost:8081 (admin/123456) |

---

### 第三步：上传 Nacos 共享配置

Nacos 启动后，需要上传 Docker 版的共享配置（使用容器 IP）：

```powershell
cd C:\Users\13129\WorkBuddy\Claw\trade-system\nacos-config
.\upload-config-docker.ps1
```

> ⚠️ **注意**：必须上传 Docker 版配置（`upload-config-docker.ps1`），不能用旧的 `upload-config.ps1`，因为 Docker 版配置中 Redis/Sentinel/XXL-JOB 地址已改为容器 IP。

上传完毕后，登录 Nacos 控制台验证：
- 进入 `配置管理` → `配置列表`
- 选择 `SHARED_GROUP` 分组
- 应看到 3 个配置：`shared-common.yml`、`shared-sentinel.yml`、`shared-xxljob.yml`

---

### 第四步：启动微服务

中间件全部就绪后，使用原有启动脚本启动微服务：

```bat
cd C:\Users\13129\WorkBuddy\Claw\trade-system\start-scripts
start-all.bat
```

或单独启动（按顺序）：
1. gateway（9000）
2. user-service（9001）
3. account-service（9004）
4. product-service（9005）
5. trade-service（9003）
6. order-service（9002）

---

## 常用命令

```powershell
# 启动所有容器
docker compose up -d

# 停止所有容器（保留数据）
docker compose down

# 停止并删除数据卷（全部重置，慎用！）
docker compose down -v

# 查看容器状态
docker compose ps

# 查看实时日志
docker compose logs -f [服务名]

# 重启单个服务
docker compose restart nacos

# 进入容器
docker exec -it trade-mysql bash
docker exec -it trade-redis redis-cli -a 123456
```

---

## 数据持久化

所有数据通过 Docker volume 持久化，停止容器后数据不丢失：

| 数据卷 | 说明 |
|--------|------|
| mysql-data | 所有 MySQL 数据库 |
| redis-data | Redis 持久化数据 |
| rabbitmq-data | RabbitMQ 消息和配置 |
| nacos-data | Nacos 配置（辅助存储，主要在 MySQL） |
| seata-data | Seata 日志 |
| xxljob-data | XXL-JOB 应用日志 |

---

## 常见问题

### Q: Nacos 启动慢，显示 unhealthy？
A: Nacos 依赖 MySQL 完全就绪后才能初始化，首次启动需要 60~90 秒。可用 `docker compose logs -f nacos` 查看进度。

### Q: Seata 连接不上 Nacos？
A: 确保 Nacos 已完全启动（health 显示 UP），Seata 会重试注册。可用 `docker compose restart seata` 重启。

### Q: 微服务启动报错连不上 MySQL/Redis？
A: 检查 Docker 容器是否都在运行：`docker compose ps`。确保 MySQL 已 healthy 状态。

### Q: SkyWalking 无数据？
A: 微服务需要通过 SkyWalking Agent 启动才有追踪数据。确保 `start-scripts/` 中的脚本包含 `-javaagent` 参数，且 OAP 地址已改为 `172.20.0.15:11800`。

### Q: 微服务启动报错 Nacos serverAddr='null'？
A: 这是因为 `bootstrap.yml` 未被加载（Spring Boot 3.x 默认关闭 bootstrap 上下文）。修复方法：各服务的 `bootstrap.yml` 中已添加 `spring.cloud.bootstrap.enabled: true`，此配置**无需改动**。如果 IDE 里看不到效果，尝试重新导入 Maven 项目（Reload All Maven Projects）。

### Q: 想恢复本地（非 Docker）启动方式？
A: 执行 `docker compose down`，然后：
1. 把各服务 `bootstrap.yml` 中的 `172.20.0.13:8848` 改回 `127.0.0.1:8848`
2. 把各服务 `application.yml` 中数据库地址改回 `localhost:3306`
