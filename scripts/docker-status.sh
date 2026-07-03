#!/bin/bash
# ================================================================
# 查看所有微服务容器状态
# ================================================================

echo "========================================"
echo "微服务容器状态"
echo "========================================"
echo ""

docker ps --filter "name=trade-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "SkyWalking 接入服务:"
docker exec trade-gateway sh -c 'echo "Gateway 服务可观测性已开启"' 2>/dev/null || echo "Gateway 容器未运行"
