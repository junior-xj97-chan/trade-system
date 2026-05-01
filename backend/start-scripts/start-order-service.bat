@echo off
chcp 65001 >nul
echo ========================================
echo Order Service 启动 (带 SkyWalking Agent)
echo ========================================

set SERVICE_NAME=order-service
set SERVICE_PORT=9002
set AGENT_PATH=D:\skywalking-agent\skywalking-agent.jar
set OAP_SERVER=127.0.0.1:11800

set "JAVA_OPTS=-javaagent:%AGENT_PATH% -Dskywalking.agent.service_name=%SERVICE_NAME% -Dskywalking.collector.backend_service=%OAP_SERVER%"
set "SERVER_OPTS=--spring.profiles.active=dev"

cd /d %~dp0..\order-service
java %JAVA_OPTS% -jar target\order-service-1.0.0.jar %SERVER_OPTS%
