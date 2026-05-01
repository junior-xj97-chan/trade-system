#!/bin/bash
# ================================================================
# MySQL 中文乱码修复脚本
# 重新初始化数据库（删除旧数据，重新导入）
# ================================================================

echo "========================================"
echo "MySQL 中文乱码修复脚本"
echo "========================================"
echo ""
echo "⚠️  警告：此操作会删除所有数据库数据！"
echo ""
read -p "确认继续？(y/n): " confirm

if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "操作已取消"
    exit 1
fi

echo ""
echo ">>> 停止 MySQL 容器..."
docker stop trade-mysql 2>/dev/null || true
docker rm trade-mysql 2>/dev/null || true

echo ">>> 删除 MySQL 数据卷..."
docker volume rm trade-system_mysql-data 2>/dev/null || true

echo ">>> 重新启动 MySQL 容器..."
cd "$(dirname "$0")"
docker compose up -d mysql

echo ">>> 等待 MySQL 启动完成（约 30 秒）..."
sleep 30

# 检查 MySQL 是否就绪
for i in {1..30}; do
    if docker exec trade-mysql mysqladmin ping -h localhost -uroot -proot123 &>/dev/null; then
        echo "MySQL 已就绪！"
        break
    fi
    echo "等待 MySQL 启动... ($i/30)"
    sleep 2
done

echo ""
echo ">>> 重新导入初始化脚本..."
docker exec -i trade-mysql mysql -uroot -proot123 < ../sql/init.sql
docker exec -i trade-mysql mysql -uroot -proot123 < ../sql/mock-data.sql

echo ""
echo "========================================"
echo "✅ 修复完成！"
echo "========================================"
echo ""
echo "验证查询（中文应该正常显示）："
docker exec trade-mysql mysql -uroot -proot123 -e "SELECT * FROM trade_product.t_product;" --default-character-set=utf8mb4
