# 检查并上传 Seata 配置到 Nacos
$NacosAddr = "127.0.0.1:8848"
$Group = "DEFAULT_GROUP"

# 1. 获取 Access Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$loginBody = "username=nacos&password=nacos"
$token = ""
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -Body $loginBody -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    $token = $loginResponse.accessToken
    Write-Host "Nacos 认证成功" -ForegroundColor Green
} catch {
    Write-Host "Nacos 认证失败" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== 检查 Seata 配置 ===" -ForegroundColor Cyan

# 2. 检查 vgroupMapping
$configUrl = "http://$NacosAddr/nacos/v1/cs/configs?dataId=service.vgroupMapping.trade-system-group&group=$Group&accessToken=$token"
try {
    $configResponse = Invoke-WebRequest -Uri $configUrl -UseBasicParsing -TimeoutSec 10
    if ($configResponse.Content -and $configResponse.Content.Length -gt 0 -and $configResponse.Content -ne "false") {
        Write-Host "service.vgroupMapping.trade-system-group = " -NoNewline -ForegroundColor Green
        Write-Host $configResponse.Content -ForegroundColor White
    } else {
        Write-Host "service.vgroupMapping.trade-system-group 不存在，需要上传" -ForegroundColor Yellow
    }
} catch {
    Write-Host "查询失败" -ForegroundColor Red
}

# 3. 上传 vgroupMapping
Write-Host ""
Write-Host "=== 上传 Seata 配置 ===" -ForegroundColor Cyan

$uploadUrl = "http://$NacosAddr/nacos/v1/cs/configs"
$bodyHash = @{
    dataId = "service.vgroupMapping.trade-system-group"
    group = $Group
    content = "default"
    type = "properties"
    accessToken = $token
}
try {
    $uploadResponse = Invoke-RestMethod -Uri $uploadUrl -Method POST -Body $bodyHash -TimeoutSec 10
    if ($uploadResponse -eq "true") {
        Write-Host "vgroupMapping 上传成功" -ForegroundColor Green
    } else {
        Write-Host "vgroupMapping 上传失败: $uploadResponse" -ForegroundColor Red
    }
} catch {
    Write-Host "上传失败: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== 配置完成 ===" -ForegroundColor Green
