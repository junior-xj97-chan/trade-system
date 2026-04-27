# ================================================================
# MySQL 中文乱码修复脚本 (PowerShell)
# 重新初始化数据库（删除旧数据，重新导入）
# ================================================================

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MySQL 中文乱码修复脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  警告：此操作会删除所有数据库数据！" -ForegroundColor Yellow
Write-Host ""

$confirm = Read-Host "确认继续？(y/n)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "操作已取消" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host ">>> 停止 MySQL 容器..." -ForegroundColor Green

# 停止并删除旧容器
docker stop trade-mysql 2>$null
docker rm trade-mysql 2>$null

# 删除旧数据卷
Write-Host ">>> 删除 MySQL 数据卷..." -ForegroundColor Green
docker volume rm trade-system_mysql-data 2>$null

# 获取当前脚本目录
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# 重新启动 MySQL
Write-Host ">>> 重新启动 MySQL 容器..." -ForegroundColor Green
docker compose up -d mysql

# 等待 MySQL 启动
Write-Host ">>> 等待 MySQL 启动完成（约 30 秒）..." -ForegroundColor Green
Start-Sleep -Seconds 30

# 检查 MySQL 是否就绪
$maxRetries = 30
for ($i = 1; $i -le $maxRetries; $i++) {
    $result = docker exec trade-mysql mysqladmin ping -h localhost -uroot -proot123 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "MySQL 已就绪！" -ForegroundColor Green
        break
    }
    Write-Host "等待 MySQL 启动... ($i/$maxRetries)" -ForegroundColor Yellow
    Start-Sleep -Seconds 2
}

# 重新导入数据
Write-Host ""
Write-Host ">>> 重新导入初始化脚本..." -ForegroundColor Green

$sqlDir = Join-Path $scriptDir "..\sql"
$initSql = Join-Path $sqlDir "init.sql"
$mockData = Join-Path $sqlDir "mock-data.sql"

docker exec -i trade-mysql mysql -uroot -proot123 < $initSql
docker exec -i trade-mysql mysql -uroot -proot123 < $mockData

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✅ 修复完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "请在 Navicat/DBeaver 中重新连接数据库测试中文显示" -ForegroundColor Yellow
