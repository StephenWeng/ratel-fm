@echo off
setlocal
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "BASE_DIR=%%~fI"
set "PID_FILE=%BASE_DIR%\run\qdrant.pid"
if not exist "%PID_FILE%" (echo Qdrant is not running.& exit /b 1)
set /p PID=<"%PID_FILE%"
tasklist /fi "PID eq %PID%" /fo table /nh 2>nul | findstr /i "qdrant.exe" || (echo Qdrant is not running: PID=%PID% not found.& exit /b 1)
echo Qdrant is running, PID=%PID%
exit /b 0
