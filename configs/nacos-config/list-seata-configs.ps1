# List all Seata-related configs in Nacos
$NacosAddr = "127.0.0.1:8848"

# 1. Get Access Token
$loginUrl = "http://$NacosAddr/nacos/v1/auth/users/login"
$loginBody = "username=nacos&password=nacos"
$token = ""
try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -Body $loginBody -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    $token = $loginResponse.accessToken
    Write-Host "Nacos login OK" -ForegroundColor Green
} catch {
    Write-Host "Nacos login failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== All Nacos Configs ===" -ForegroundColor Cyan

# List configs page by page
$pageNo = 1
$pageSize = 100
$total = 0
$hasMore = $true

while ($hasMore) {
    $listUrl = "http://$NacosAddr/nacos/v1/cs/configs?search=accurate&pageNo=$pageNo&pageSize=$pageSize&accessToken=$token"
    try {
        $listResponse = Invoke-RestMethod -Uri $listUrl -TimeoutSec 10
        $json = $listResponse | ConvertFrom-Json
        $configs = $json.pageItems
        $total = $json.totalCount
        $hasMore = ($json.pageItems.Count -eq $pageSize)
        
        foreach ($config in $configs) {
            if ($config.dataId -match "seata|service\.vgroupMapping|txServiceGroup") {
                Write-Host "  $($config.dataId) [$($config.group)]" -ForegroundColor Yellow
            }
        }
        
        $pageNo++
        if (-not $hasMore) { break }
    } catch {
        Write-Host "Failed to list configs" -ForegroundColor Red
        break
    }
}

Write-Host ""
Write-Host "Total configs: $total" -ForegroundColor Cyan
Write-Host ""

# Check specific Seata configs
Write-Host "=== Check Specific Seata Configs ===" -ForegroundColor Cyan
$configsToCheck = @(
    "service.vgroupMapping.trade-system-group",
    "txServiceGroup",
    "seataServer.properties"
)

foreach ($dataId in $configsToCheck) {
    $configUrl = "http://$NacosAddr/nacos/v1/cs/configs?dataId=$dataId&group=DEFAULT_GROUP&accessToken=$token"
    try {
        $response = Invoke-WebRequest -Uri $configUrl -UseBasicParsing -TimeoutSec 10
        $content = $response.Content.Trim()
        if ($content -and $content.Length -gt 0 -and $content -ne "false") {
            Write-Host "[OK] $dataId = $content" -ForegroundColor Green
        } else {
            Write-Host "[NOT FOUND] $dataId" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[ERROR] $dataId" -ForegroundColor Red
    }
}

# Also check in SEATA_GROUP
Write-Host ""
Write-Host "=== Check in SEATA_GROUP ===" -ForegroundColor Cyan
foreach ($dataId in $configsToCheck) {
    $configUrl = "http://$NacosAddr/nacos/v1/cs/configs?dataId=$dataId&group=SEATA_GROUP&accessToken=$token"
    try {
        $response = Invoke-WebRequest -Uri $configUrl -UseBasicParsing -TimeoutSec 10
        $content = $response.Content.Trim()
        if ($content -and $content.Length -gt 0 -and $content -ne "false") {
            Write-Host "[OK] $dataId = $content" -ForegroundColor Green
        } else {
            Write-Host "[NOT FOUND] $dataId in SEATA_GROUP" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[ERROR] $dataId in SEATA_GROUP" -ForegroundColor Red
    }
}
