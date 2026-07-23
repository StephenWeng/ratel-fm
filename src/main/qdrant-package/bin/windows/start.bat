@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "BASE_DIR=%%~fI"
set "QDRANT_EXE=%BASE_DIR%\runtime\windows\qdrant\qdrant.exe"
set "RUN_DIR=%BASE_DIR%\run"
set "LOG_DIR=%BASE_DIR%\logs"
set "PID_FILE=%RUN_DIR%\qdrant.pid"
if not defined QDRANT_HOST set "QDRANT_HOST=0.0.0.0"
if not defined QDRANT_HTTP_PORT set "QDRANT_HTTP_PORT=6333"
if not defined QDRANT_GRPC_PORT set "QDRANT_GRPC_PORT=6334"
if not defined QDRANT_STORAGE_DIR set "QDRANT_STORAGE_DIR=%BASE_DIR%\storage"
if not defined QDRANT_SNAPSHOTS_DIR set "QDRANT_SNAPSHOTS_DIR=%BASE_DIR%\snapshots"
for %%D in ("%RUN_DIR%" "%LOG_DIR%" "%QDRANT_STORAGE_DIR%" "%QDRANT_SNAPSHOTS_DIR%") do if not exist "%%~D" mkdir "%%~D"
if not exist "%QDRANT_EXE%" (echo Qdrant executable not found: %QDRANT_EXE%& exit /b 1)
if exist "%PID_FILE%" (
  set /p OLD_PID=<"%PID_FILE%"
  tasklist /fi "PID eq !OLD_PID!" /fo csv /nh 2>nul | findstr /i "qdrant.exe" >nul && (echo Qdrant is already running, PID=!OLD_PID!& exit /b 0)
  del /q "%PID_FILE%" >nul 2>nul
)
for %%P in (%QDRANT_HTTP_PORT% %QDRANT_GRPC_PORT%) do (netstat -ano -p tcp | findstr /r /c:":%%P .*LISTENING" >nul && (echo Qdrant port %%P is already in use.& exit /b 1))
call :ensure_firewall %QDRANT_HTTP_PORT% "Qdrant HTTP" || exit /b 1
call :ensure_firewall %QDRANT_GRPC_PORT% "Qdrant gRPC" || exit /b 1
set "QDRANT__SERVICE__HOST=%QDRANT_HOST%"
set "QDRANT__SERVICE__HTTP_PORT=%QDRANT_HTTP_PORT%"
set "QDRANT__SERVICE__GRPC_PORT=%QDRANT_GRPC_PORT%"
set "QDRANT__STORAGE__STORAGE_PATH=%QDRANT_STORAGE_DIR%"
set "QDRANT__STORAGE__SNAPSHOTS_PATH=%QDRANT_SNAPSHOTS_DIR%"
set "QDRANT__TELEMETRY_DISABLED=true"
rem Qdrant 从当前工作目录的 static 目录提供 /dashboard，必须固定为独立包根目录启动。
if not exist "%BASE_DIR%\static\index.html" (echo Qdrant Dashboard static files not found: %BASE_DIR%\static& exit /b 1)
pushd "%BASE_DIR%" || exit /b 1
start "Qdrant" /b cmd /c ""%QDRANT_EXE%" 1>>"%LOG_DIR%\qdrant.log" 2>>"%LOG_DIR%\qdrant-error.log""
popd
for /l %%N in (1,1,20) do (
  ping.exe -n 2 127.0.0.1 >nul 2>nul
  for /f "tokens=2 delims=," %%P in ('tasklist /fi "IMAGENAME eq qdrant.exe" /fo csv /nh 2^>nul') do set "QDRANT_PID=%%~P"
  if defined QDRANT_PID goto started
)
echo Qdrant failed to start. Check %LOG_DIR%\qdrant-error.log
exit /b 1
:started
>"%PID_FILE%" echo !QDRANT_PID!
echo Qdrant started, PID=!QDRANT_PID!
curl.exe -fsS --max-time 3 "http://127.0.0.1:%QDRANT_HTTP_PORT%/" >nul 2>nul && echo Qdrant health check passed.
curl.exe -fsS --max-time 3 "http://127.0.0.1:%QDRANT_HTTP_PORT%/dashboard/" >nul 2>nul && echo Qdrant Dashboard ready: http://127.0.0.1:%QDRANT_HTTP_PORT%/dashboard/
exit /b 0

:ensure_firewall
set "FIREWALL_RULE=Ratel FM %~2 TCP %~1"
netsh advfirewall firewall show rule name="%FIREWALL_RULE%" >nul 2>nul && exit /b 0
netsh advfirewall firewall add rule name="%FIREWALL_RULE%" dir=in action=allow protocol=TCP localport=%~1 profile=any >nul 2>nul
if errorlevel 1 (echo Warning: cannot open Windows Firewall TCP %~1 for %~2; local access will continue.& exit /b 0)
echo Windows Firewall opened TCP %~1 for %~2.
exit /b 0
