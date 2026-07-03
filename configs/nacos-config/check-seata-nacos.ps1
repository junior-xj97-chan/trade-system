# 检查 Seata Server 是否注册到 Nacos
param(
    $NacosAddr = "127.0.0.1:8848",
    $Namespace = "",
    $Group = "DEFAULT_GROUP",
    $ServiceName = "seata-server"
)

# 1. 获取 Access Token (Nacos 2.x 需要认证)
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$loginBody = "username=nacos&password=nacos"
$token = ""
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -Body $loginBody -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    $token = $loginResponse.accessToken
    Write-Host "✓ Nacos 认证成功"
} catch {
    Write-Host "✗ Nacos 认证失败: $($_.Exception.Message)"
    exit 1
}

# 2. 查询服务列表
$serviceUrl = "http://$NacosAddr/nacos/v1/ns/instance/list?serviceName=$ServiceName&groupName=$Group"
if ($token) {
    $serviceUrl += "&accessToken=$token"
}
try {
    $serviceResponse = Invoke-RestMethod -Uri $serviceUrl -TimeoutSec 10
    Write-Host "`n=== Seata Server 服务信息 ===" -ForegroundColor Cyan
    Write-Host "服务名: $ServiceName"
    Write-Host "Group: $Group"
    Write-Host "实例数: $($serviceResponse.hosts.Count)"
    if ($serviceResponse.hosts.Count -gt 0) {
        $serviceResponse.hosts | ForEach-Object {
            Write-Host "  - IP: $($_.ip), Port: $($_.port), Healthy: $($_.healthy)"
        }
    } else {
        Write-Host "  ⚠️ 没有找到 seata-server 实例" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ 查询服务失败: $($_.Exception.Message)"
}

# 3. 查询所有服务名（检查是否有 seata 相关）
Write-Host "`n=== 检查所有已注册服务 ===" -ForegroundColor Cyan
$listUrl = "http://$NacosAddr/nacos/v1/ns/service/list?pageNo=1&pageSize=100&hasHttpFilter=false"
if ($token) {
    $listUrl += "&accessToken=$token"
}
try {
    $listResponse = Invoke-RestMethod -Uri $listUrl -TimeoutSec 10
    $allServices = $listResponse.doms
    Write-Host "已注册服务数量: $($allServices.Count)"
    
    # 筛选包含 seata 或 server 的服务
    $seataServices = $allServices | Where-Object { $_ -match "seata|server" }
    if ($seataServices) {
        Write-Host "`nSeata 相关服务:" -ForegroundColor Green
        $seataServices | ForEach-Object { Write-Host "  - $_" }
    } else {
        Write-Host "`n⚠️ 没有找到 Seata 相关服务" -ForegroundColor Yellow
        Write-Host "`n前 20 个服务:" -ForegroundColor Cyan
        $allServices[0..19] | ForEach-Object { Write-Host "  - $_" }
    }
} catch {
    Write-Host "✗ 查询服务列表失败: $($_.Exception.Message)"
}
