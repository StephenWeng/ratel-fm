@echo off
setlocal EnableExtensions
chcp 65001 >nul
set "PID_FILE=%~dp0..\..\run\ratel-fm.pid"
if not exist "%PID_FILE%" (echo Ratel FM is not running.& exit /b 0)
set /p PID=<"%PID_FILE%"
tasklist /fi "PID eq %PID%" /fo csv /nh 2>nul | findstr /i "java.exe" >nul || (del /q "%PID_FILE%" >nul 2>nul& echo Ratel FM is not running.& exit /b 0)
taskkill /pid %PID% /t >nul 2>nul
for /l %%N in (1,1,15) do (tasklist /fi "PID eq %PID%" /fo csv /nh 2>nul | findstr /i "java.exe" >nul || goto stopped& timeout /t 1 /nobreak >nul)
taskkill /f /pid %PID% /t >nul 2>nul
:stopped
del /q "%PID_FILE%" >nul 2>nul
echo Ratel FM stopped, PID=%PID%
exit /b 0
