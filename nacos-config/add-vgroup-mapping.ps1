# 配置上传脚本
$NACOS_URL = "http://127.0.0.1:8848/nacos/v1/cs/configs"

# 登录获取 Token
$login = Invoke-RestMethod -Uri "http://127.0.0.1:8848/nacos/v1/auth/users/login" `
    -Method POST -Body @{username="nacos";password="nacos"} `
    -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$TOKEN = $login.accessToken

# 添加 service.vgroupMapping.trade-system-group 配置
$params = @{
    "dataId"  = "service.vgroupMapping.trade-system-group"
    "group"   = "DEFAULT_GROUP"
    "content" = "default"
    "type"    = "properties"
}
$resp = Invoke-RestMethod -Uri "${NACOS_URL}?accessToken=${TOKEN}" `
    -Method POST -Body $params `
    -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
Write-Host "service.vgroupMapping.trade-system-group: $resp"
