@echo off
setlocal
chcp 65001 >nul
set "PID_FILE=%~dp0..\..\run\ratel-fm.pid"
if not exist "%PID_FILE%" (echo Ratel FM is not running.& exit /b 1)
set /p PID=<"%PID_FILE%"
tasklist /fi "PID eq %PID%" /fo table /nh 2>nul | findstr /i "java.exe" || (echo Ratel FM is not running: PID=%PID% not found.& exit /b 1)
echo Ratel FM is running, PID=%PID%
exit /b 0
