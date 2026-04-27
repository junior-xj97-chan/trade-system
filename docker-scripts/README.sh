# ================================================================
# Docker 容器化微服务启动脚本
#
# 使用方式：
#   1. 先构建微服务镜像（Jib 或 Dockerfile）
#   2. 运行 docker-start-all.sh 一键启动
#
# 前置条件：
#   - 中间件已启动（docker-compose up -d）
#   - 微服务镜像已构建
# ================================================================

# 目录说明
├── common-env.sh              # 公共环境变量配置
├── docker-start-gateway.sh    # 启动 Gateway
├── docker-start-user-service.sh
├── docker-start-order-service.sh
├── docker-start-trade-service.sh
├── docker-start-account-service.sh
├── docker-start-product-service.sh
├── docker-start-all.sh       # 一键启动所有容器
├── docker-stop-all.sh        # 停止所有容器
└── docker-status.sh          # 查看容器状态

# 使用步骤

# 1. 启动中间件（如果尚未启动）
cd ../docker
docker compose up -d

# 等待中间件启动完成（约 2 分钟）
sleep 120

# 2. 构建微服务镜像（Jib）
cd ..
mvn clean package jib:dockerBuild -DskipTests

# 或使用 Dockerfile 构建
# docker build -t trade/gateway:latest ./gateway

# 3. 启动微服务容器
cd docker-scripts
bash docker-start-all.sh

# 4. 验证 SkyWalking 是否接入
# 访问 http://localhost:8088 查看 Dashboard

# 常见问题

# Q: 容器内找不到 Agent？
# A: 需要将 SkyWalking Agent 打包进镜像，或通过 volume 挂载：
#    -v /path/to/skywalking-agent:/skywalking/agent

# Q: OAP 地址如何配置？
# A: 通过 JAVA_TOOL_OPTIONS 环境变量注入，无需修改代码
