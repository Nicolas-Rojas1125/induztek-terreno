@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME (
  if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
if not defined JAVA_HOME (
  echo No se encontro Java 21. Instale JDK 21 o Android Studio.
  pause
  exit /b 1
)

echo Iniciando Eureka...
start "eureka-server" cmd /k "cd /d %~dp0eurekaserver && mvnw.cmd spring-boot:run"
timeout /t 40 /nobreak >nul

echo Iniciando microservicios...
start "ms-usuarios" cmd /k "cd /d %~dp0ms-usuarios && mvnw.cmd spring-boot:run"
start "ms-activos" cmd /k "cd /d %~dp0ms-activos && mvnw.cmd spring-boot:run"
start "ms-protocolos" cmd /k "cd /d %~dp0ms-protocolos && mvnw.cmd spring-boot:run"
timeout /t 40 /nobreak >nul

echo Iniciando API Gateway...
start "api-gateway" cmd /k "cd /d %~dp0apigateway && mvnw.cmd spring-boot:run"

echo.
echo Listo. Revise http://localhost:8761 en 1 minuto.
echo La app usa http://10.0.2.2:8081/
pause
