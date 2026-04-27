# Check Seata Server cluster configuration in Nacos
$NacosAddr = "127.0.0.1:8848"

# Get Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$response = Invoke-RestMethod -Uri $loginUrl -Method POST -Body "username=nacos&password=nacos" -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$token = $response.accessToken

Write-Host "=== Seata Server Cluster Check ===" -ForegroundColor Cyan

# Query seata-server service details
$serviceUrl = "http://$NacosAddr/nacos/v1/ns/service?serviceName=seata-server&groupName=DEFAULT_GROUP&accessToken=$token"
try {
    $response = Invoke-RestMethod -Uri $serviceUrl -TimeoutSec 10
    $service = $response | ConvertFrom-Json
    
    Write-Host "Service Name: $($service.name)"
    Write-Host "Group Name: $($service.groupName)"
    Write-Host "Cluster Name: $($service.clusterName)"
    Write-Host "Instance Count: $($service.hosts.Count)"
    
    if ($service.hosts.Count -gt 0) {
        Write-Host ""
        Write-Host "Instances:" -ForegroundColor Yellow
        foreach ($host in $service.hosts) {
            Write-Host "  - IP: $($host.ip)" -ForegroundColor White
            Write-Host "    Port: $($host.port)" -ForegroundColor White
            Write-Host "    Cluster: $($host.clusterName)" -ForegroundColor White
            Write-Host "    Healthy: $($host.healthy)" -ForegroundColor White
            Write-Host "    Enabled: $($host.enabled)" -ForegroundColor White
            Write-Host ""
        }
    }
} catch {
    Write-Host "Failed to query service: $($_.Exception.Message)" -ForegroundColor Red
}

# Check if seata-server is registered under different cluster
Write-Host ""
Write-Host "=== Checking cluster names ===" -ForegroundColor Cyan
$clustersUrl = "http://$NacosAddr/nacos/v1/ns/cluster?serviceName=seata-server&groupName=DEFAULT_GROUP&accessToken=$token"
try {
    $response = Invoke-RestMethod -Uri $clustersUrl -TimeoutSec 10
    Write-Host "Clusters: $response" -ForegroundColor White
} catch {
    Write-Host "Failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "If clusterName is empty or different from expected," -ForegroundColor White
Write-Host "you may need to check Seata Server configuration." -ForegroundColor White
