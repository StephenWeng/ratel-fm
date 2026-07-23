@echo off
setlocal
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "BASE_DIR=%%~fI"
set "PID_FILE=%BASE_DIR%\run\qdrant.pid"
if not exist "%PID_FILE%" (echo Qdrant is not running.& exit /b 0)
set /p PID=<"%PID_FILE%"
taskkill /f /pid %PID% /t >nul 2>nul
del /q "%PID_FILE%" >nul 2>nul
echo Qdrant stopped, PID=%PID%
exit /b 0
