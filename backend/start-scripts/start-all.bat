@echo off
chcp 65001 >nul
echo ========================================
echo 一键启动所有微服务 (带 SkyWalking Agent)
echo ========================================
echo.
echo 启动顺序：
echo   1. Gateway (9000)
echo   2. User Service (9001)
echo   3. Account Service (9004)
echo   4. Product Service (9005)
echo   5. Trade Service (9003)
echo   6. Order Service (9002)
echo   7. Search Service (9006)
echo.

cd /d %~dp0

start "Gateway" cmd /k "call start-gateway.bat"
timeout /t 3 /nobreak >nul

start "User-Service" cmd /k "call start-user-service.bat"
timeout /t 3 /nobreak >nul

start "Account-Service" cmd /k "call start-account-service.bat"
timeout /t 3 /nobreak >nul

start "Product-Service" cmd /k "call start-product-service.bat"
timeout /t 3 /nobreak >nul

start "Trade-Service" cmd /k "call start-trade-service.bat"
timeout /t 3 /nobreak >nul

start "Order-Service" cmd /k "call start-order-service.bat"
timeout /t 3 /nobreak >nul

start "Search-Service" cmd /k "call start-search-service.bat"

echo.
echo 所有服务已启动！请访问 SkyWalking UI: http://localhost:8088
pause
