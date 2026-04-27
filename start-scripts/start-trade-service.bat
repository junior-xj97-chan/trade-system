@echo off
chcp 65001 >nul
echo ========================================
echo Trade Service 启动 (带 SkyWalking Agent)
echo ========================================

set SERVICE_NAME=trade-service
set SERVICE_PORT=9003
set AGENT_PATH=D:\skywalking-agent\skywalking-agent.jar
set OAP_SERVER=172.20.0.15:11800

set "JAVA_OPTS=-javaagent:%AGENT_PATH% -Dskywalking.agent.service_name=%SERVICE_NAME% -Dskywalking.collector.backend_service=%OAP_SERVER%"
set "SERVER_OPTS=--spring.profiles.active=dev"

cd /d %~dp0..\trade-service
java %JAVA_OPTS% -jar target\trade-service-1.0.0.jar %SERVER_OPTS%
