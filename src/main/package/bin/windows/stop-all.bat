@echo off
setlocal EnableExtensions
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "RATEL_DIR=%%~fI"
for %%I in ("%RATEL_DIR%\..") do set "DEPLOY_ROOT=%%~fI"
call "%~dp0stop.bat"
if exist "%DEPLOY_ROOT%\ratel-fm-qdrant\bin\windows\stop.bat" call "%DEPLOY_ROOT%\ratel-fm-qdrant\bin\windows\stop.bat"
if exist "%DEPLOY_ROOT%\ratel-fm-ollama\bin\windows\stop.bat" call "%DEPLOY_ROOT%\ratel-fm-ollama\bin\windows\stop.bat"
exit /b 0
