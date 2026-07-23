@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
set "BASE_DIR=%~dp0..\.."
for %%I in ("%BASE_DIR%") do set "BASE_DIR=%%~fI"
set "JAVA=%BASE_DIR%\runtime\jdk\bin\java.exe"
set "JPS=%BASE_DIR%\runtime\jdk\bin\jps.exe"
set "JAR=%BASE_DIR%\app\ratel-fm.jar"
set "CONFIG=%BASE_DIR%\config"
set "LOG_DIR=%BASE_DIR%\logs"
set "RUN_DIR=%BASE_DIR%\run"
set "DATA_DIR=%BASE_DIR%\data"
set "FILES_DIR=%BASE_DIR%\files"
set "PID_FILE=%RUN_DIR%\ratel-fm.pid"
set "JPS_OUTPUT=%RUN_DIR%\jps-output.tmp"
if not defined SERVER_PORT set "SERVER_PORT=38000"
if not defined SERVER_SERVLET_CONTEXT_PATH set "SERVER_SERVLET_CONTEXT_PATH=/ratel/fm"
if not defined RATEL_HTTPS_ENABLED set "RATEL_HTTPS_ENABLED=true"
if not defined RATEL_HTTPS_PORT set "RATEL_HTTPS_PORT=38443"
if not defined RATEL_JVM_XMS set "RATEL_JVM_XMS=1g"
if not defined RATEL_JVM_XMX set "RATEL_JVM_XMX=2g"
if not defined RATEL_JVM_MAX_METASPACE set "RATEL_JVM_MAX_METASPACE=512m"
for %%D in ("%LOG_DIR%" "%RUN_DIR%" "%DATA_DIR%" "%FILES_DIR%" "%BASE_DIR%\backup" "%BASE_DIR%\uploads\avatars") do if not exist "%%~D" mkdir "%%~D"
if not exist "%DATA_DIR%\ratel-fm.mv.db" if exist "%BASE_DIR%\database-template\ratel-fm.mv.db" copy /y "%BASE_DIR%\database-template\ratel-fm.mv.db" "%DATA_DIR%\ratel-fm.mv.db" >nul
if not exist "%JAVA%" (echo Bundled Java not found: %JAVA%& exit /b 1)
if not exist "%JAR%" (echo Application jar not found: %JAR%& exit /b 1)
if exist "%PID_FILE%" (
  set /p OLD_PID=<"%PID_FILE%"
  tasklist /fi "PID eq !OLD_PID!" /fo csv /nh 2>nul | findstr /i "java.exe" >nul && (echo Ratel FM is already running, PID=!OLD_PID!& exit /b 0)
  del /q "%PID_FILE%" >nul 2>nul
)
netstat -ano -p tcp | findstr /r /c:":%SERVER_PORT% .*LISTENING" >nul && (echo Port %SERVER_PORT% is already in use.& exit /b 1)
set "HTTPS_ARGS="
if /i "%RATEL_HTTPS_ENABLED%"=="true" (
  call "%~dp0cert\generate-https-cert.bat" "%BASE_DIR%" "%RATEL_HTTPS_PORT%" "%SERVER_SERVLET_CONTEXT_PATH%" || exit /b 1
  set /p KEYSTORE_PASSWORD=<"%BASE_DIR%\certs\ratel-fm-server.password"
  set "HTTPS_ARGS=--server.port=%RATEL_HTTPS_PORT% --server.ssl.enabled=true --server.ssl.key-store="%BASE_DIR%\certs\ratel-fm-server.p12" --server.ssl.key-store-password=!KEYSTORE_PASSWORD! --server.ssl.key-store-type=PKCS12 --server.ssl.key-alias=ratel-fm-server --app.https.http-enabled=true --app.https.http-port=%SERVER_PORT%"
)
set "CONFIG_URI=%CONFIG:\=/%"
start "Ratel FM" /b cmd /c ""%JAVA%" -Xms%RATEL_JVM_XMS% -Xmx%RATEL_JVM_XMX% -XX:MaxMetaspaceSize=%RATEL_JVM_MAX_METASPACE% -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%LOG_DIR%" %RATEL_JAVA_OPTS% -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai -DLOG_HOME="%LOG_DIR%" -DFM_ATTACHMENT_BASE_DIR="%FILES_DIR%" -jar "%JAR%" --spring.config.additional-location="file:!CONFIG_URI!/" --logging.config="%CONFIG%\logback-spring.xml" !HTTPS_ARGS! 1>>"%LOG_DIR%\console.log" 2>>"%LOG_DIR%\console-error.log""
for /l %%N in (1,1,30) do (
  ping.exe -n 2 127.0.0.1 >nul 2>nul
  rem 先落盘再解析，避免 for /f 子命令对带空格的 jps 完整路径进行二次引号解析。
  "%JPS%" -l >"!JPS_OUTPUT!" 2>nul
  for /f "usebackq tokens=1,*" %%P in ("!JPS_OUTPUT!") do echo %%Q | findstr /i /c:"ratel-fm.jar" >nul && set "APP_PID=%%P"
  if defined APP_PID goto started
)
del /q "%JPS_OUTPUT%" >nul 2>nul
echo Ratel FM failed to start. Check %LOG_DIR%\console-error.log
exit /b 1
:started
del /q "%JPS_OUTPUT%" >nul 2>nul
>"%PID_FILE%" echo !APP_PID!
echo Ratel FM started, PID=!APP_PID!
echo HTTP URL: http://127.0.0.1:%SERVER_PORT%%SERVER_SERVLET_CONTEXT_PATH%
if /i "%RATEL_HTTPS_ENABLED%"=="true" echo HTTPS URL: https://127.0.0.1:%RATEL_HTTPS_PORT%%SERVER_SERVLET_CONTEXT_PATH%
exit /b 0
