# Check Seata Server instance details - using raw content
$NacosAddr = "127.0.0.1:8848"

# Get Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$response = Invoke-RestMethod -Uri $loginUrl -Method POST -Body "username=nacos&password=nacos" -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$token = $response.accessToken

Write-Host "=== Seata Server Instance Check ===" -ForegroundColor Cyan

# Query seata-server instances
$url = "http://$NacosAddr/nacos/v1/ns/instance/list?serviceName=seata-server&groupName=DEFAULT_GROUP&accessToken=$token"
try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
    $content = $response.Content
    
    Write-Host "Raw Response:" -ForegroundColor White
    Write-Host $content -ForegroundColor Gray
    
    # Parse manually
    $json = $content | ConvertFrom-Json
    
    Write-Host ""
    Write-Host "Parsed Details:" -ForegroundColor Yellow
    Write-Host "  Cluster: $($json.clusterName)"
    Write-Host "  Instance Count: $($json.hosts.Count)"
    
    if ($json.hosts.Count -gt 0) {
        foreach ($h in $json.hosts) {
            Write-Host "  Instance: $($h.ip):$($h.port)"
            Write-Host "    ClusterName: $($h.clusterName)"
            Write-Host "    Healthy: $($h.healthy)"
        }
    }
} catch {
    Write-Host "Failed: $($_.Exception.Message)" -ForegroundColor Red
}
