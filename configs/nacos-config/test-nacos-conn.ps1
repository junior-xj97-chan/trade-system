# Test Nacos connection from microservice perspective
$NacosAddr = "127.0.0.1:8848"

Write-Host "=== Test Nacos Connection ===" -ForegroundColor Cyan

# 1. Test Config Service
Write-Host "`n1. Testing Config Service..." -ForegroundColor Yellow
$configUrl = "http://$NacosAddr/nacos/v1/cs/configs?dataId=service.vgroupMapping.trade-system-group&group=DEFAULT_GROUP"
try {
    $response = Invoke-WebRequest -Uri $configUrl -UseBasicParsing -TimeoutSec 5
    if ($response.Content -eq "default") {
        Write-Host "   Config service: OK" -ForegroundColor Green
    } else {
        Write-Host "   Config service: Content mismatch" -ForegroundColor Yellow
        Write-Host "   Response: $($response.Content)" -ForegroundColor Gray
    }
} catch {
    Write-Host "   Config service: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Test Naming Service (Service Registration)
Write-Host "`n2. Testing Naming Service..." -ForegroundColor Yellow
$namingUrl = "http://$NacosAddr/nacos/v1/ns/instance/list?serviceName=seata-server&groupName=DEFAULT_GROUP"
try {
    $response = Invoke-WebRequest -Uri $namingUrl -UseBasicParsing -TimeoutSec 5
    $json = $response.Content | ConvertFrom-Json
    if ($json.hosts.Count -gt 0) {
        Write-Host "   Naming service: OK (found $($json.hosts.Count) instances)" -ForegroundColor Green
        $json.hosts | ForEach-Object {
            Write-Host "   - $($_.ip):$($_.port) (healthy=$($_.healthy))" -ForegroundColor White
        }
    } else {
        Write-Host "   Naming service: No instances found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   Naming service: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Test Authentication
Write-Host "`n3. Testing Authentication..." -ForegroundColor Yellow
$authUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
try {
    $response = Invoke-RestMethod -Uri $authUrl -Method POST -Body "username=nacos&password=nacos" -ContentType "application/x-www-form-urlencoded" -TimeoutSec 5
    if ($response.accessToken) {
        Write-Host "   Auth: OK (token obtained)" -ForegroundColor Green
    } else {
        Write-Host "   Auth: Failed - no token" -ForegroundColor Red
    }
} catch {
    Write-Host "   Auth: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Test RPC connection (gRPC)
Write-Host "`n4. Testing gRPC port (9848)..." -ForegroundColor Yellow
$grpcUrl = "http://$NacosAddr:9848/"
try {
    $response = Invoke-WebRequest -Uri $grpcUrl -UseBasicParsing -TimeoutSec 3
    Write-Host "   gRPC port: Response received" -ForegroundColor Green
} catch {
    Write-Host "   gRPC port: Not reachable (this is normal if no HTTP service on 9848)" -ForegroundColor Yellow
}

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "If all tests passed but microservices still fail, the issue is likely:" -ForegroundColor White
Write-Host "  - Timing: Microservices start before Nacos client fully connects" -ForegroundColor White
Write-Host "  - Solution: Add startup delay or retry logic" -ForegroundColor White
