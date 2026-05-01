# Upload Seata configs to Nacos (quick version - no pause)
$NACOS = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$login = Invoke-RestMethod -Uri "http://127.0.0.1:8848/nacos/v1/auth/users/login" -Method POST -Body "username=nacos&password=nacos" -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$tok = $login.accessToken

$items = @(
    "store.mode=db",
    "store.db.dbType=mysql",
    "store.db.driverClassName=com.mysql.cj.jdbc.Driver",
    "store.db.url=jdbc:mysql://172.20.0.10:3306/seata?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
    "store.db.user=root",
    "store.db.password=root123",
    "store.db.min-conn=10",
    "store.db.max-conn=100",
    "store.db.global-table=global_table",
    "store.db.branch-table=branch_table",
    "store.db.lock-table=lock_table",
    "store.db.distributed-lock-table=distributed_lock",
    "store.db.query-limit=1000",
    "store.db.max-wait=5000",
    "vgroupMapping.trade-system-group=default",
    "txServiceGroup=trade-system-group",
    "client.rm.async.committing.buffer.size=10000",
    "client.rm.lock.retry.internal=10",
    "client.rm.lock.retry.times=30",
    "client.rm.report.retry.count=5",
    "client.tm.commit.retry.count=0",
    "client.tm.rollback.retry.count=0",
    "server.max.commit.retry.timeout=-1",
    "server.max.rollback.retry.timeout=-1",
    "server.rollback.retry.timeout.unlock.enable=false",
    "server.checkAuth=false",
    "server.namespace=",
    "server.degrade=false"
)

foreach ($item in $items) {
    $parts = $item -split '=', 2
    $dataId = $parts[0]
    $content = $parts[1]
    $params = "dataId=$dataId&group=DEFAULT_GROUP&content=$([System.Uri]::EscapeDataString($content))&type=properties"
    $r = Invoke-RestMethod -Uri "$NACOS?accessToken=$tok" -Method POST -Body $params -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
    Write-Host "$dataId -> $r"
}
Write-Host "Done!"
