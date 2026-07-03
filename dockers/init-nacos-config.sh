#!/bin/bash
# ================================================================
# Nacos 配置自动初始化脚本
# 等待 Nacos 启动后，自动上传 shared-configs
# ================================================================

NACOS_URL="http://localhost:8848/nacos/v1/cs/configs"
NACOS_USER="nacos"
NACOS_PASS="nacos"
GROUP="SHARED_GROUP"

echo "================================================================"
echo "  Nacos 配置初始化脚本"
echo "  请确保 Nacos 已启动：http://localhost:8848"
echo "================================================================"
echo ""

# 等待 Nacos 就绪
echo "[1/4] 等待 Nacos 启动..."
MAX_RETRY=30
RETRY=0
while ! curl -sf "http://localhost:8848/nacos/actuator/health" | grep -q "UP" 2>/dev/null; do
    RETRY=$((RETRY+1))
    if [ $RETRY -ge $MAX_RETRY ]; then
        echo "[✗] Nacos 启动超时，请手动检查"
        exit 1
    fi
    echo "    等待中... ($RETRY/$MAX_RETRY)"
    sleep 5
done
echo "[✓] Nacos 已就绪"
echo ""

# 上传共享配置的函数
upload_config() {
    local DATA_ID=$1
    local FILE=$2
    local CONTENT=$(cat "$FILE")
    
    RESPONSE=$(curl -sf -X POST "$NACOS_URL" \
        -d "dataId=${DATA_ID}&group=${GROUP}&content=$(python3 -c 'import sys,urllib.parse; print(urllib.parse.quote(sys.stdin.read()))' <<< "$CONTENT")&type=yaml" \
        2>&1)
    
    if [ "$RESPONSE" = "true" ]; then
        echo "[✓] 上传成功: $DATA_ID"
    else
        echo "[✗] 上传失败: $DATA_ID (${RESPONSE})"
    fi
}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NACOS_DIR="$(dirname "$SCRIPT_DIR")/nacos-config"

echo "[2/4] 上传 shared-common.yml..."
curl -sf -X POST "$NACOS_URL" \
    --data-urlencode "dataId=shared-common.yml" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "content@${NACOS_DIR}/shared-common.yml" \
    --data-urlencode "type=yaml" \
    -u "${NACOS_USER}:${NACOS_PASS}" && echo "[✓] shared-common.yml 上传成功" || echo "[✗] shared-common.yml 上传失败"

echo "[3/4] 上传 shared-sentinel.yml..."
curl -sf -X POST "$NACOS_URL" \
    --data-urlencode "dataId=shared-sentinel.yml" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "content@${NACOS_DIR}/shared-sentinel.yml" \
    --data-urlencode "type=yaml" \
    -u "${NACOS_USER}:${NACOS_PASS}" && echo "[✓] shared-sentinel.yml 上传成功" || echo "[✗] shared-sentinel.yml 上传失败"

echo "[4/4] 上传 shared-xxljob.yml..."
curl -sf -X POST "$NACOS_URL" \
    --data-urlencode "dataId=shared-xxljob.yml" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "content@${NACOS_DIR}/shared-xxljob.yml" \
    --data-urlencode "type=yaml" \
    -u "${NACOS_USER}:${NACOS_PASS}" && echo "[✓] shared-xxljob.yml 上传成功" || echo "[✗] shared-xxljob.yml 上传失败"

echo ""
echo "================================================================"
echo "  完成！请访问 Nacos 控制台验证："
echo "  地址: http://localhost:8848/nacos"
echo "  账号: nacos / nacos"
echo "  命名空间: public，分组: SHARED_GROUP"
echo "================================================================"
