#!/bin/bash
# ================================================================
# 一键启动所有微服务容器 (Docker 模式)
# 适用于微服务也容器化的场景
# ================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================"
echo "一键启动所有微服务容器 (Docker)"
echo "========================================"
echo ""
echo "启动顺序："
echo "  1. Gateway (9000)"
echo "  2. User Service (9001)"
echo "  3. Account Service (9004)"
echo "  4. Product Service (9005)"
echo "  5. Trade Service (9003)"
echo "  6. Order Service (9002)"
echo ""

# 依次启动（间隔 5 秒确保健康检查通过）
echo ">>> 启动 Gateway..."
bash "${SCRIPT_DIR}/docker-start-gateway.sh"

echo ">>> 启动 User Service..."
bash "${SCRIPT_DIR}/docker-start-user-service.sh"

echo ">>> 启动 Account Service..."
bash "${SCRIPT_DIR}/docker-start-account-service.sh"

echo ">>> 启动 Product Service..."
bash "${SCRIPT_DIR}/docker-start-product-service.sh"

echo ">>> 启动 Trade Service..."
bash "${SCRIPT_DIR}/docker-start-trade-service.sh"

echo ">>> 启动 Order Service..."
bash "${SCRIPT_DIR}/docker-start-order-service.sh"

echo ""
echo "========================================"
echo "所有微服务容器已启动！"
echo "========================================"
echo ""
echo "访问地址："
echo "  Gateway:     http://localhost:9000"
echo "  User:        http://localhost:9001"
echo "  Account:     http://localhost:9004"
echo "  Product:     http://localhost:9005"
echo "  Trade:       http://localhost:9003"
echo "  Order:       http://localhost:9002"
echo "  SkyWalking:  http://localhost:8088"
echo ""
echo "查看容器状态: docker ps"
echo "查看日志:     docker logs -f <容器名>"
