# Check and upload Seata config to Nacos
$NacosAddr = "127.0.0.1:8848"
$Group = "DEFAULT_GROUP"

# 1. Get Access Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$loginBody = "username=nacos&password=nacos"
$token = ""
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -Body $loginBody -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    $token = $loginResponse.accessToken
    Write-Host "[OK] Nacos login success" -ForegroundColor Green
} catch {
    Write-Host "[FAIL] Nacos login failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== Check Seata Config ===" -ForegroundColor Cyan

# 2. Check vgroupMapping
$configUrl = "http://$NacosAddr/nacos/v1/cs/configs?dataId=service.vgroupMapping.trade-system-group&group=$Group&accessToken=$token"
try {
    $configResponse = Invoke-WebRequest -Uri $configUrl -UseBasicParsing -TimeoutSec 10
    if ($configResponse.Content -and $configResponse.Content.Length -gt 0 -and $configResponse.Content -ne "false") {
        Write-Host "[OK] service.vgroupMapping.trade-system-group = " -NoNewline -ForegroundColor Green
        Write-Host $configResponse.Content -ForegroundColor White
    } else {
        Write-Host "[WARN] service.vgroupMapping.trade-system-group NOT FOUND" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[FAIL] Query failed" -ForegroundColor Red
}

# 3. Upload vgroupMapping
Write-Host ""
Write-Host "=== Upload Seata Config ===" -ForegroundColor Cyan

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
        Write-Host "[OK] vgroupMapping uploaded" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] vgroupMapping upload failed: $uploadResponse" -ForegroundColor Red
    }
} catch {
    Write-Host "[FAIL] Upload failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
