@echo off
setlocal
chcp 65001 >nul
set "CA_CERT=%~1"
if not defined CA_CERT set "CA_CERT=%~dp0..\..\..\certs\ratel-local-ca.cer"
if not exist "%CA_CERT%" (echo CA certificate not found: %CA_CERT%& exit /b 1)
certutil -user -addstore Root "%CA_CERT%" || exit /b 1
echo Ratel FM local CA installed for current user.
exit /b 0
