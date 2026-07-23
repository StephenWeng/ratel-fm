@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "BASE_DIR=%%~fI"
set "OLLAMA_EXE=%BASE_DIR%\runtime\windows\ollama\ollama.exe"
set "RUN_DIR=%BASE_DIR%\run"
set "LOG_DIR=%BASE_DIR%\logs"
set "PID_FILE=%RUN_DIR%\ollama.pid"
if not defined OLLAMA_HOST set "OLLAMA_HOST=0.0.0.0:11434"
if not defined OLLAMA_MODELS set "OLLAMA_MODELS=%BASE_DIR%\models"
if not defined OLLAMA_KEEP_ALIVE set "OLLAMA_KEEP_ALIVE=30m"
rem 同时保留 embedding 与聊天模型，避免每次问答都从机械盘卸载、重载模型。
if not defined OLLAMA_MAX_LOADED_MODELS set "OLLAMA_MAX_LOADED_MODELS=2"
rem 笔记本部署只允许单路模型推理，防止 CPU 和内存被并发请求打满。
if not defined OLLAMA_NUM_PARALLEL set "OLLAMA_NUM_PARALLEL=1"
if not defined OPEN_WEBUI_ENABLED set "OPEN_WEBUI_ENABLED=true"
if not defined OPEN_WEBUI_HOST set "OPEN_WEBUI_HOST=0.0.0.0"
if not defined OPEN_WEBUI_PORT set "OPEN_WEBUI_PORT=8080"
if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
if not exist "%OLLAMA_MODELS%" mkdir "%OLLAMA_MODELS%"
if not exist "%OLLAMA_EXE%" (echo Ollama executable not found: %OLLAMA_EXE%& exit /b 1)
if exist "%PID_FILE%" (
  set /p OLD_PID=<"%PID_FILE%"
  tasklist /fi "PID eq !OLD_PID!" /fo csv /nh 2>nul | findstr /i "ollama.exe" >nul && (echo Ollama is already running, PID=!OLD_PID!& goto webui)
  del /q "%PID_FILE%" >nul 2>nul
)
for /f "tokens=2 delims=:" %%P in ("%OLLAMA_HOST%") do set "OLLAMA_PORT=%%P"
if not defined OLLAMA_PORT set "OLLAMA_PORT=11434"
netstat -ano -p tcp | findstr /r /c:":!OLLAMA_PORT! .*LISTENING" >nul && (echo Ollama port !OLLAMA_PORT! is already in use.& exit /b 1)
call :ensure_firewall !OLLAMA_PORT! "Ollama" || exit /b 1
if /i "%OPEN_WEBUI_ENABLED%"=="true" (
  call :ensure_firewall %OPEN_WEBUI_PORT% "Open WebUI" || exit /b 1
)
start "Ollama" /b cmd /c ""%OLLAMA_EXE%" serve 1>>"%LOG_DIR%\ollama.log" 2>>"%LOG_DIR%\ollama-error.log""
for /l %%N in (1,1,20) do (
  ping.exe -n 2 127.0.0.1 >nul 2>nul
  for /f "tokens=2 delims=," %%P in ('tasklist /fi "IMAGENAME eq ollama.exe" /fo csv /nh 2^>nul') do set "OLLAMA_PID=%%~P"
  if defined OLLAMA_PID goto ollama_started
)
echo Ollama failed to start. Check %LOG_DIR%\ollama-error.log
exit /b 1
:ollama_started
>"%PID_FILE%" echo !OLLAMA_PID!
echo Ollama started, PID=!OLLAMA_PID!
echo Host: %OLLAMA_HOST%
echo Model directory: %OLLAMA_MODELS%
curl.exe -fsS --max-time 3 "http://127.0.0.1:!OLLAMA_PORT!/api/tags" >nul 2>nul && echo Ollama health check passed.
:webui
if /i not "%OPEN_WEBUI_ENABLED%"=="true" exit /b 0
set "WEBUI_RUN=%RUN_DIR%\open-webui.pid"
if exist "%WEBUI_RUN%" exit /b 0
set "PYTHON=%BASE_DIR%\runtime\python\python.exe"
set "OPEN_WEBUI_SITE_PACKAGES=%BASE_DIR%\runtime\open-webui\site-packages"
if not exist "%PYTHON%" (echo Open WebUI skipped: bundled Python runtime not found: %PYTHON%& exit /b 0)
if not exist "%OPEN_WEBUI_SITE_PACKAGES%\open_webui" (echo Open WebUI skipped: bundled Open WebUI dependencies not found.& exit /b 0)
set "PYTHONHOME=%BASE_DIR%\runtime\python"
set "PYTHONPATH=%OPEN_WEBUI_SITE_PACKAGES%"
"%PYTHON%" -c "import sys; import open_webui; raise SystemExit(0 if sys.version_info >= (3, 11) else 1)" >nul 2>nul || (echo Open WebUI skipped: bundled runtime validation failed.& exit /b 0)
if not defined DATA_DIR set "DATA_DIR=%BASE_DIR%\data\open-webui"
set "OLLAMA_BASE_URL=http://127.0.0.1:!OLLAMA_PORT!"
rem open-webui 的控制台入口是 open_webui:app，包本身不提供可直接执行的 __main__ 模块。
start "Open WebUI" /b cmd /c ""%PYTHON%" -c "from open_webui import app; app()" serve --host %OPEN_WEBUI_HOST% --port %OPEN_WEBUI_PORT% 1>>"%LOG_DIR%\open-webui.log" 2>>"%LOG_DIR%\open-webui-error.log""
rem 首次启动需要初始化 Open WebUI 数据库，机械盘环境最多等待两分钟。
for /l %%N in (1,1,120) do (
  ping.exe -n 2 127.0.0.1 >nul 2>nul
  for /f "tokens=5" %%P in ('netstat -ano -p tcp ^| findstr /r /c:":%OPEN_WEBUI_PORT% .*LISTENING"') do set "WEBUI_PID=%%P"
  if defined WEBUI_PID goto webui_started
)
echo Open WebUI failed to start. Check %LOG_DIR%\open-webui-error.log
exit /b 0
:webui_started
>"%WEBUI_RUN%" echo !WEBUI_PID!
echo Open WebUI started, PID=!WEBUI_PID!, URL=http://127.0.0.1:%OPEN_WEBUI_PORT%
exit /b 0

:ensure_firewall
set "FIREWALL_RULE=Ratel FM %~2 TCP %~1"
netsh advfirewall firewall show rule name="%FIREWALL_RULE%" >nul 2>nul && exit /b 0
netsh advfirewall firewall add rule name="%FIREWALL_RULE%" dir=in action=allow protocol=TCP localport=%~1 profile=any >nul 2>nul
if errorlevel 1 (echo Warning: cannot open Windows Firewall TCP %~1 for %~2; local access will continue.& exit /b 0)
echo Windows Firewall opened TCP %~1 for %~2.
exit /b 0
