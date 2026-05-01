#!/bin/bash
# ================================================================
# Account Service Docker 容器启动脚本
# ================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common-env.sh"

SERVICE_NAME="account-service"
CONTAINER_NAME="${CONTAINER_PREFIX}-${SERVICE_NAME}"
IMAGE_NAME="trade/${SERVICE_NAME}:latest"

echo "========================================"
echo "启动 Account Service 容器 (Docker)"
echo "========================================"

docker rm -f ${CONTAINER_NAME} 2>/dev/null || true

docker run -d \
    --name ${CONTAINER_NAME} \
    --network trade-net \
    --ip 172.20.0.24 \
    -p 9004:9004 \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Dskywalking.agent.service_name=${SERVICE_NAME}" \
    ${IMAGE_NAME}

echo "Account Service 容器已启动！"
