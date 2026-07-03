#!/bin/bash
# ================================================================
# Gateway Docker 容器启动脚本
# ================================================================
set -e

# 加载公共配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common-env.sh"

SERVICE_NAME="gateway"
CONTAINER_NAME="${CONTAINER_PREFIX}-${SERVICE_NAME}"
IMAGE_NAME="trade/${SERVICE_NAME}:latest"

echo "========================================"
echo "启动 Gateway 容器 (Docker)"
echo "========================================"
echo "服务名: ${SERVICE_NAME}"
echo "容器名: ${CONTAINER_NAME}"
echo "Agent:  ${AGENT_PATH}"
echo "OAP:    ${OAP_SERVER}"
echo ""

# 停止并删除已存在的容器
docker rm -f ${CONTAINER_NAME} 2>/dev/null || true

# 启动容器
docker run -d \
    --name ${CONTAINER_NAME} \
    --network trade-net \
    --ip 172.20.0.20 \
    -p 9000:9000 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e SKYWALKING_COLLECTOR_BACKEND_SERVICE=${OAP_SERVER} \
    -e JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dskywalking.agent.service_name=${SERVICE_NAME}" \
    ${IMAGE_NAME}

echo ""
echo "Gateway 容器已启动！"
echo "访问地址: http://localhost:9000"
