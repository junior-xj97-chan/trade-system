# ================================================================
# Nacos 配置上传脚本（Docker 版 - 使用容器 IP）
# 使用方法：右键 -> 使用 PowerShell 运行
# ================================================================

$NACOS_URL = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$GROUP     = "SHARED_GROUP"

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Definition

Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "Nacos 配置上传脚本 (Docker 版)" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

$configs = @(
    @{dataId="shared-common.yml";   file="shared-common-docker.yml"},
    @{dataId="shared-xxljob.yml";   file="shared-xxljob-docker.yml"},
    @{dataId="shared-sentinel.yml"; file="shared-sentinel-docker.yml"}
)

# 登录获取 Token（Nacos 开启鉴权时必须）
Write-Host "正在登录 Nacos..." -ForegroundColor Yellow
$token_resp = Invoke-RestMethod -Uri "http://127.0.0.1:8848/nacos/v1/auth/users/login" `
    -Method POST -Body @{username="nacos";password="nacos"} `
    -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$ACCESS_TOKEN = $token_resp.accessToken
Write-Host "登录成功" -ForegroundColor Green
Write-Host ""

foreach ($config in $configs) {
    $filePath = Join-Path $SCRIPT_DIR $config.file
    if (Test-Path $filePath) {
        $content = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
        $params = @{
            "dataId"  = $config.dataId
            "group"   = $GROUP
            "content" = $content
            "type"    = "yaml"
        }
        try {
            $response = Invoke-RestMethod -Uri "${NACOS_URL}?accessToken=${ACCESS_TOKEN}" -Method POST -Body $params `
                          -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
            if ($response -eq "true") {
                Write-Host "[✓] 上传成功: $($config.dataId)" -ForegroundColor Green
            } else {
                Write-Host "[✗] 上传失败: $($config.dataId) - $response" -ForegroundColor Red
            }
        } catch {
            Write-Host "[✗] 请求失败: $($config.dataId) - $_" -ForegroundColor Red
        }
    } else {
        Write-Host "[!] 文件不存在: $filePath" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "上传完成！" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
