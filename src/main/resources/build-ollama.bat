@echo off
setlocal EnableExtensions
rem 构建 Ollama 独立包；首次构建会在工程目录准备便携 Python 和 Open WebUI 依赖。
for %%I in ("%~dp0..\..\..") do set "PROJECT_DIR=%%~fI"
set "PYTHON_RUNTIME=%PROJECT_DIR%\src\main\ollama-package\runtime\python"
set "OPEN_WEBUI_SITE=%PROJECT_DIR%\src\main\ollama-package\runtime\open-webui\site-packages"
set "PYTHON_PACKAGE=%PROJECT_DIR%\tmp\python-3.11.9-embed-amd64.zip"
if not exist "%PYTHON_RUNTIME%\python.exe" call :prepare_python || exit /b 1
if not exist "%OPEN_WEBUI_SITE%\open_webui" call :prepare_open_webui || exit /b 1
if defined RATEL_BUILD_JAVA_HOME (set "JAVA_HOME=%RATEL_BUILD_JAVA_HOME%") else set "JAVA_HOME=D:\jdk\jdk-24.0.1"
if defined RATEL_BUILD_MAVEN_HOME (set "MAVEN_HOME=%RATEL_BUILD_MAVEN_HOME%") else set "MAVEN_HOME=D:\java_develop_V1.0\apache-maven-3.6.3"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
pushd "%PROJECT_DIR%" || exit /b 1
call mvn.cmd -DskipTests package -Pwith-ollama
set "BUILD_EXIT=%ERRORLEVEL%"
popd
if not "%BUILD_EXIT%"=="0" exit /b %BUILD_EXIT%
echo Built target\ratel-fm-ollama.zip with bundled Python and Open WebUI.
exit /b 0

:prepare_python
rem Python 官方 embeddable ZIP 是可复制运行时，不执行安装器、不写注册表，也不会成为部署机系统 Python。
if not exist "%PROJECT_DIR%\tmp" mkdir "%PROJECT_DIR%\tmp"
curl.exe -L --fail --retry 10 --retry-delay 2 -o "%PYTHON_PACKAGE%" "https://www.python.org/ftp/python/3.11.9/python-3.11.9-embed-amd64.zip" || exit /b 1
if not exist "%PYTHON_RUNTIME%" mkdir "%PYTHON_RUNTIME%"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%PYTHON_PACKAGE%' -DestinationPath '%PYTHON_RUNTIME%' -Force" || exit /b 1
>"%PYTHON_RUNTIME%\python311._pth" echo python311.zip
>>"%PYTHON_RUNTIME%\python311._pth" echo .
>>"%PYTHON_RUNTIME%\python311._pth" echo ..\open-webui\site-packages
>>"%PYTHON_RUNTIME%\python311._pth" echo ..\open-webui\site-packages\win32
>>"%PYTHON_RUNTIME%\python311._pth" echo ..\open-webui\site-packages\win32\lib
>>"%PYTHON_RUNTIME%\python311._pth" echo ..\open-webui\site-packages\pythonwin
>>"%PYTHON_RUNTIME%\python311._pth" echo ..\open-webui\site-packages\pywin32_system32
>>"%PYTHON_RUNTIME%\python311._pth" echo import site
"%PYTHON_RUNTIME%\python.exe" --version || exit /b 1
exit /b 0

:prepare_open_webui
rem 依赖只写入部署包目录，部署机启动时不再执行 pip 或访问互联网。
if not exist "%OPEN_WEBUI_SITE%" mkdir "%OPEN_WEBUI_SITE%"
curl.exe -L --fail --retry 5 -o "%PROJECT_DIR%\tmp\get-pip.py" "https://bootstrap.pypa.io/get-pip.py" || exit /b 1
"%PYTHON_RUNTIME%\python.exe" "%PROJECT_DIR%\tmp\get-pip.py" --no-warn-script-location || exit /b 1
"%PYTHON_RUNTIME%\python.exe" -m pip install --upgrade --target "%OPEN_WEBUI_SITE%" torch==2.6.0 open-webui==0.10.2 || exit /b 1
"%PYTHON_RUNTIME%\python.exe" -c "import open_webui" || exit /b 1
exit /b 0
