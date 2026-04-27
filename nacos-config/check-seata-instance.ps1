# Check Seata Server instance details
$NacosAddr = "127.0.0.1:8848"

# Get Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$response = Invoke-RestMethod -Uri $loginUrl -Method POST -Body "username=nacos&password=nacos" -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$token = $response.accessToken

Write-Host "=== Seata Server Instance Details ===" -ForegroundColor Cyan

# Query seata-server instances
$url = "http://$NacosAddr/nacos/v1/ns/instance/list?serviceName=seata-server&groupName=DEFAULT_GROUP&accessToken=$token"
try {
    $response = Invoke-RestMethod -Uri $url -TimeoutSec 10
    $json = $response | ConvertFrom-Json
    
    Write-Host "Service: seata-server"
    Write-Host "Group: DEFAULT_GROUP"
    Write-Host "Cluster: $($json.clusterName)"
    Write-Host "Instance Count: $($json.hosts.Count)"
    
    if ($json.hosts.Count -gt 0) {
        Write-Host ""
        foreach ($host in $json.hosts) {
            Write-Host "Instance Details:" -ForegroundColor Yellow
            Write-Host "  IP: $($host.ip)"
            Write-Host "  Port: $($host.port)"
            Write-Host "  Cluster Name: $($host.clusterName)"
            Write-Host "  Healthy: $($host.healthy)"
            Write-Host "  Enabled: $($host.enabled)"
            Write-Host "  Weight: $($host.weight)"
            Write-Host "  Ephemeral: $($host.ephemeral)"
        }
    }
} catch {
    Write-Host "Failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Key Check ===" -ForegroundColor Cyan
Write-Host "The clusterName returned should match 'default'" -ForegroundColor White
Write-Host "because seata config has: vgroupMapping.trade-system-group = default" -ForegroundColor White
