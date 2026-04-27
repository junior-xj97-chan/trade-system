#!/bin/bash
# ================================================================
# Docker 容器化微服务启动脚本
# 使用 JAVA_TOOL_OPTIONS 环境变量注入 SkyWalking Agent
# ================================================================

# 公共配置
export AGENT_PATH=/skywalking/agent/skywalking-agent.jar
export OAP_SERVER=172.20.0.15:11800

# 公共 JVM 参数（通过 JAVA_TOOL_OPTIONS 注入，无需 -javaagent 在启动命令中）
export JAVA_TOOL_OPTIONS="-javaagent:${AGENT_PATH} -Dskywalking.collector.backend_service=${OAP_SERVER}"

# Nacos 配置
export NACOS_SERVER=172.20.0.13:8848

# Seata 配置
export SEATA_SERVER=172.20.0.14:8091

# 公共数据库配置
export DB_HOST=172.20.0.10
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=root123

# Redis 配置
export REDIS_HOST=172.20.0.11
export REDIS_PORT=6379
export REDIS_PASSWORD=123456

# 服务端口映射（容器端口:宿主机端口）
declare -A SERVICE_PORTS
SERVICE_PORTS[gateway]="9000:9000"
SERVICE_PORTS[user-service]="9001:9001"
SERVICE_PORTS[order-service]="9002:9002"
SERVICE_PORTS[trade-service]="9003:9003"
SERVICE_PORTS[account-service]="9004:9004"
SERVICE_PORTS[product-service]="9005:9005"

# 容器名称前缀
CONTAINER_PREFIX="trade"

# 获取容器 IP（用于服务间调用）
get_container_ip() {
    local service=$1
    local ip=""
    case $service in
        gateway) ip="172.20.0.20" ;;
        user-service) ip="172.20.0.21" ;;
        order-service) ip="172.20.0.22" ;;
        trade-service) ip="172.20.0.23" ;;
        account-service) ip="172.20.0.24" ;;
        product-service) ip="172.20.0.25" ;;
    esac
    echo $ip
}
