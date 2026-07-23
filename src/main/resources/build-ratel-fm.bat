@echo off
setlocal EnableExtensions
rem 构建 Ratel FM 主 Jar 和便携部署包，不构建独立 Ollama/Qdrant 包。
for %%I in ("%~dp0..\..\..") do set "PROJECT_DIR=%%~fI"
if defined RATEL_BUILD_JAVA_HOME (set "JAVA_HOME=%RATEL_BUILD_JAVA_HOME%") else set "JAVA_HOME=D:\jdk\jdk-24.0.1"
if defined RATEL_BUILD_MAVEN_HOME (set "MAVEN_HOME=%RATEL_BUILD_MAVEN_HOME%") else set "MAVEN_HOME=D:\java_develop_V1.0\apache-maven-3.6.3"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
pushd "%PROJECT_DIR%" || exit /b 1
call mvn.cmd -DskipTests package
set "BUILD_EXIT=%ERRORLEVEL%"
popd
if not "%BUILD_EXIT%"=="0" exit /b %BUILD_EXIT%
echo Built target\ratel-fm.jar and target\ratel-fm-portable.zip
exit /b 0
