@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
for %%I in ("%~dp0..\..") do set "RATEL_DIR=%%~fI"
for %%I in ("%RATEL_DIR%\..") do set "DEPLOY_ROOT=%%~fI"
echo Deploy root: %DEPLOY_ROOT%
call :find_component "ratel-fm-ollama" OLLAMA_DIR
call :find_component "ratel-fm-qdrant" QDRANT_DIR
if defined OLLAMA_DIR (call :start_component "Ollama" "!OLLAMA_DIR!\bin\windows\start.bat") else echo [Ollama] package directory not found, skipped.
if defined QDRANT_DIR (call :start_component "Qdrant" "!QDRANT_DIR!\bin\windows\start.bat") else echo [Qdrant] package directory not found, skipped.
call :start_component "Ratel FM" "%~dp0start.bat"
echo Integrated start finished.
exit /b 0

:find_component
set "%~2="
if exist "%DEPLOY_ROOT%\%~1\" (set "%~2=%DEPLOY_ROOT%\%~1"& exit /b 0)
for /d %%D in ("%DEPLOY_ROOT%\%~1*") do if not defined %~2 set "%~2=%%~fD"
exit /b 0

:start_component
set "COMPONENT_NAME=%~1"
set "COMPONENT_SCRIPT=%~2"
if not defined COMPONENT_SCRIPT (echo [%COMPONENT_NAME%] package directory not found, skipped.& exit /b 0)
if not exist "%COMPONENT_SCRIPT%" (echo [%COMPONENT_NAME%] start script not found: %COMPONENT_SCRIPT%& exit /b 0)
echo [%COMPONENT_NAME%] starting: %COMPONENT_SCRIPT%
call "%COMPONENT_SCRIPT%"
set "COMPONENT_EXIT=!ERRORLEVEL!"
if not "!COMPONENT_EXIT!"=="0" (echo [%COMPONENT_NAME%] start failed, exit code=!COMPONENT_EXIT!.& exit /b 0)
echo [%COMPONENT_NAME%] start completed.
exit /b 0
