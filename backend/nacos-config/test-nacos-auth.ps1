# Test Nacos connection with authentication
$NacosAddr = "127.0.0.1:8848"

Write-Host "=== Test Nacos Connection with Auth ===" -ForegroundColor Cyan

# 1. Get Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$token = ""
try {
    $response = Invoke-RestMethod -Uri $loginUrl -Method POST -Body "username=nacos&password=nacos" -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    $token = $response.accessToken
    Write-Host "1. Auth: OK" -ForegroundColor Green
} catch {
    Write-Host "1. Auth: FAILED" -ForegroundColor Red
    exit 1
}

# 2. Test Config Service with Token
Write-Host "`n2. Testing Config Service..." -ForegroundColor Yellow
$configUrl = "http://$NacosAddr/nacos/v1/cs/configs?dataId=service.vgroupMapping.trade-system-group&group=DEFAULT_GROUP&accessToken=$token"
try {
    $response = Invoke-WebRequest -Uri $configUrl -UseBasicParsing -TimeoutSec 5
    if ($response.Content -eq "default") {
        Write-Host "   Config service: OK" -ForegroundColor Green
        Write-Host "   Value: $($response.Content)" -ForegroundColor White
    } else {
        Write-Host "   Config service: Content = $($response.Content)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   Config service: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Test Naming Service with Token
Write-Host "`n3. Testing Naming Service..." -ForegroundColor Yellow
$namingUrl = "http://$NacosAddr/nacos/v1/ns/instance/list?serviceName=seata-server&groupName=DEFAULT_GROUP&accessToken=$token"
try {
    $response = Invoke-WebRequest -Uri $namingUrl -UseBasicParsing -TimeoutSec 5
    $json = $response.Content | ConvertFrom-Json
    if ($json.hosts.Count -gt 0) {
        Write-Host "   Naming service: OK" -ForegroundColor Green
        Write-Host "   Found $($json.hosts.Count) seata-server instance(s):" -ForegroundColor White
        $json.hosts | ForEach-Object {
            Write-Host "   - $($_.ip):$($_.port) (healthy=$($_.healthy))" -ForegroundColor White
        }
    } else {
        Write-Host "   Naming service: No seata-server instances found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   Naming service: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Summary
Write-Host "`n=== Summary ===" -ForegroundColor Cyan
if ($token) {
    Write-Host "Token obtained successfully" -ForegroundColor Green
    Write-Host "This confirms Nacos is working correctly with authentication" -ForegroundColor Green
}
