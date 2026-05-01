# ================================================================
# Seata 配置上传脚本 - 上传到 Nacos（seata-server 和客户端共用）
# 上传位置: group=DEFAULT_GROUP
# ================================================================
$NACOS_URL = "http://127.0.0.1:8848/nacos/v1/cs/configs"
$GROUP = "DEFAULT_GROUP"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Seata 配置上传到 Nacos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 登录获取 Token
Write-Host "正在登录 Nacos..." -ForegroundColor Yellow
$login = Invoke-RestMethod -Uri "http://127.0.0.1:8848/nacos/v1/auth/users/login" `
    -Method POST -Body @{username="nacos";password="nacos"} `
    -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
$TOKEN = $login.accessToken
Write-Host "登录成功" -ForegroundColor Green
Write-Host ""

# Seata 2.x 必需配置列表
$configs = @(
    # 存储模式
    @{dataId="store.mode";               value="db"},
    @{dataId="store.db.dbType";          value="mysql"},
    @{dataId="store.db.driverClassName"; value="com.mysql.cj.jdbc.Driver"},
    @{dataId="store.db.url";             value="jdbc:mysql://172.20.0.10:3306/seata?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"},
    @{dataId="store.db.user";            value="root"},
    @{dataId="store.db.password";        value="root123"},
    @{dataId="store.db.min-conn";        value="10"},
    @{dataId="store.db.max-conn";        value="100"},
    @{dataId="store.db.global-table";    value="global_table"},
    @{dataId="store.db.branch-table";     value="branch_table"},
    @{dataId="store.db.lock-table";       value="lock_table"},
    @{dataId="store.db.distributed-lock-table"; value="distributed_lock"},
    @{dataId="store.db.query-limit";      value="1000"},
    @{dataId="store.db.max-wait";         value="5000"},

    # 事务分组映射（客户端需要）- Seata 2.x 使用完整键名
    @{dataId="service.vgroupMapping.trade-system-group"; value="default"},

    # 客户端 RMS 配置
    @{dataId="client.rm.async.committing.buffer.size";   value="10000"},
    @{dataId="client.rm.lock.retry.internal";            value="10"},
    @{dataId="client.rm.lock.retry.times";                value="30"},
    @{dataId="client.rm.lock.retry.policy.branch-rollback-on-conflict"; value="true"},
    @{dataId="client.rm.report.retry.count";              value="5"},
    @{dataId="client.rm.table.meta.checker.class";        value="default"},
    @{dataId="client.tm.commit.retry.count";              value="0"},
    @{dataId="client.tm.rollback.retry.count";            value="0"},

    # 服务端配置
    @{dataId="server.max.commit.retry.timeout";           value="-1"},
    @{dataId="server.max.rollback.retry.timeout";         value="-1"},
    @{dataId="server.rollback.retry.timeout.unlock.enable"; value="false"},
    @{dataId="server.checkAuth";                          value="false"},
    @{dataId="server.recovery.committing-retry-period";   value="1000"},
    @{dataId="server.recovery.async-committing-retry-period"; value="1000"},
    @{dataId="server.recovery.rollbacking-retry-period";  value="1000"},
    @{dataId="server.recovery.timeout-retry-period";      value="1000"},
    @{dataId="server.namespace";                          value=""},
    @{dataId="server.store.db.data-source-proxy-enable";  value="true"},
    @{dataId="server.degrade";                            value="false"},
    @{dataId="server.serverSerialize";                    value="seata"},
    @{dataId="server.serverMode";                         value="file"},

    # TC（Transaction Coordinator）配置
    @{dataId="txServiceGroup";                            value="trade-system-group"},
    @{dataId="clientTransactionServiceGroup";              value="trade-system-group"}
)

foreach ($cfg in $configs) {
    $params = @{
        "dataId"  = $cfg.dataId
        "group"   = $GROUP
        "content" = $cfg.value
        "type"    = "properties"
    }
    try {
        $resp = Invoke-RestMethod -Uri "${NACOS_URL}?accessToken=${TOKEN}" `
            -Method POST -Body $params `
            -ContentType "application/x-www-form-urlencoded" -TimeoutSec 10
        if ($resp -eq "true") {
            Write-Host "[OK] $($cfg.dataId)" -ForegroundColor Green
        } else {
            Write-Host "[??] $($cfg.dataId) - $resp" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[FAIL] $($cfg.dataId) - $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Seata 配置上传完成！" -ForegroundColor Cyan
Write-Host "请重启 Seata 服务使配置生效" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
pause
