# ================================================================
# Nacos 配置初始化 PowerShell 脚本（Windows 版）
# 等待 Nacos 启动后，自动上传 shared-configs
# 使用方法：右键 -> 使用 PowerShell 运行
# ================================================================

$NACOS_URL = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$USERNAME  = "nacos"
$PASSWORD  = "nacos"
$GROUP     = "SHARED_GROUP"

$SCRIPT_DIR   = Split-Path -Parent $MyInvocation.MyCommand.Definition
$NACOS_DIR    = Join-Path (Split-Path -Parent $SCRIPT_DIR) "nacos-config"

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  Nacos 配置初始化脚本 (Docker 版)" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# 等待 Nacos 就绪
Write-Host "[Step 1] 等待 Nacos 启动..." -ForegroundColor Yellow
$maxRetry = 30
$retry = 0
do {
    Start-Sleep -Seconds 5
    $retry++
    Write-Host "  等待中... ($retry/$maxRetry)" -NoNewline
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:8848/nacos/actuator/health" -TimeoutSec 3 -ErrorAction Stop
        if ($health.status -eq "UP") { break }
    } catch {
        Write-Host " (未就绪)"
    }
} while ($retry -lt $maxRetry)

if ($retry -ge $maxRetry) {
    Write-Host "[✗] Nacos 启动超时，请检查容器状态：docker ps" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[✓] Nacos 已就绪！" -ForegroundColor Green
Write-Host ""

# 上传配置的函数
function Upload-Config {
    param([string]$DataId, [string]$FilePath)
    
    if (-not (Test-Path $FilePath)) {
        Write-Host "[!] 文件不存在: $FilePath" -ForegroundColor Yellow
        return
    }
    
    $content = Get-Content $FilePath -Raw -Encoding UTF8
    $params = @{
        "dataId"  = $DataId
        "group"   = $GROUP
        "content" = $content
        "type"    = "yaml"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $NACOS_URL -Method POST -Body $params `
                      -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
        if ($response -eq "true") {
            Write-Host "[✓] 上传成功: $DataId" -ForegroundColor Green
        } else {
            Write-Host "[✗] 上传失败: $DataId - $response" -ForegroundColor Red
        }
    } catch {
        Write-Host "[✗] 请求失败: $DataId - $_" -ForegroundColor Red
    }
}

Write-Host "[Step 2] 上传 Nacos 共享配置..." -ForegroundColor Yellow
Write-Host ""

Upload-Config "shared-common.yml"   (Join-Path $NACOS_DIR "shared-common.yml")
Upload-Config "shared-sentinel.yml" (Join-Path $NACOS_DIR "shared-sentinel.yml")
Upload-Config "shared-xxljob.yml"   (Join-Path $NACOS_DIR "shared-xxljob.yml")

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  完成！" -ForegroundColor Green
Write-Host "  Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)" -ForegroundColor Cyan
Write-Host "  确认 SHARED_GROUP 下有 3 个配置文件" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""
pause
