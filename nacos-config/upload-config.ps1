# Nacos 配置上传脚本
# 使用方法: 右键 -> 使用 PowerShell 运行

$NACOS_URL = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$USERNAME = "nacos"
$PASSWORD = "nacos"
$GROUP = "SHARED_GROUP"

# 配置文件列表
$configs = @(
    @{dataId="shared-common.yml"; file="shared-common.yml"},
    @{dataId="shared-xxljob.yml"; file="shared-xxljob.yml"},
    @{dataId="shared-sentinel.yml"; file="shared-sentinel.yml"}
)

Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "Nacos 配置上传脚本" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""

foreach ($config in $configs) {
    $filePath = Join-Path $PSScriptRoot $config.file
    
    if (Test-Path $filePath) {
        $content = Get-Content $filePath -Raw -Encoding UTF8
        
        $params = @{
            "dataId" = $config.dataId
            "group" = $GROUP
            "content" = $content
            "type" = "yaml"
        }
        
        try {
            $response = Invoke-RestMethod -Uri $NACOS_URL -Method POST -Body $params -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
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
Write-Host "请登录 Nacos 控制台验证配置" -ForegroundColor Cyan
Write-Host "地址: http://127.0.0.1:8848/nacos" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
