@echo off
setlocal EnableExtensions
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "BASE_DIR=%%~fI"
call :stop_pid "%BASE_DIR%\run\open-webui.pid" "Open WebUI"
call :stop_pid "%BASE_DIR%\run\ollama.pid" "Ollama"
exit /b 0
:stop_pid
if not exist "%~1" (echo %~2 is not running.& exit /b 0)
set /p PID=<"%~1"
taskkill /f /pid %PID% /t >nul 2>nul
del /q "%~1" >nul 2>nul
echo %~2 stopped, PID=%PID%
exit /b 0
