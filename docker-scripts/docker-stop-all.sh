#!/bin/bash
# ================================================================
# 停止所有微服务容器
# ================================================================

echo "========================================"
echo "停止所有微服务容器"
echo "========================================"

CONTAINERS=(
    "trade-gateway"
    "trade-user-service"
    "trade-order-service"
    "trade-trade-service"
    "trade-account-service"
    "trade-product-service"
)

for container in "${CONTAINERS[@]}"; do
    echo ">>> 停止 ${container}..."
    docker stop ${container} 2>/dev/null && docker rm ${container} 2>/dev/null
done

echo ""
echo "所有微服务容器已停止！"
