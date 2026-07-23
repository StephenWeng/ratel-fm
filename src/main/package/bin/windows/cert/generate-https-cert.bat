@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
set "BASE_DIR=%~1"
set "HTTPS_PORT=%~2"
set "CONTEXT_PATH=%~3"
set "KEYTOOL=%BASE_DIR%\runtime\jdk\bin\keytool.exe"
set "CERT_DIR=%BASE_DIR%\certs"
set "WORK_DIR=%CERT_DIR%\work"
if not exist "%KEYTOOL%" (echo keytool not found: %KEYTOOL%& exit /b 1)
if not exist "%WORK_DIR%" mkdir "%WORK_DIR%"
set "CA_STORE=%CERT_DIR%\ratel-local-ca.p12"
set "CA_CERT=%CERT_DIR%\ratel-local-ca.cer"
set "CA_PASS_FILE=%CERT_DIR%\ratel-local-ca.password"
set "SERVER_STORE=%CERT_DIR%\ratel-fm-server.p12"
set "SERVER_PASS_FILE=%CERT_DIR%\ratel-fm-server.password"
if not exist "%CA_PASS_FILE%" >"%CA_PASS_FILE%" echo ratelca%RANDOM%%RANDOM%%RANDOM%
if not exist "%SERVER_PASS_FILE%" >"%SERVER_PASS_FILE%" echo ratelsrv%RANDOM%%RANDOM%%RANDOM%
set /p CA_PASS=<"%CA_PASS_FILE%"
set /p SERVER_PASS=<"%SERVER_PASS_FILE%"
if not exist "%CA_STORE%" "%KEYTOOL%" -genkeypair -alias ratel-local-ca -keyalg RSA -keysize 4096 -validity 3650 -dname "CN=Ratel FM Local CA, OU=Ratel FM, O=ratel, L=Chengdu, ST=Sichuan, C=CN" -ext bc=ca:true -ext KeyUsage=keyCertSign,cRLSign -storetype PKCS12 -keystore "%CA_STORE%" -storepass "!CA_PASS!" -keypass "!CA_PASS!" || exit /b 1
if not exist "%CA_CERT%" "%KEYTOOL%" -exportcert -alias ratel-local-ca -keystore "%CA_STORE%" -storepass "!CA_PASS!" -rfc -file "%CA_CERT%" || exit /b 1
set "SAN=SAN=dns:localhost,dns:%COMPUTERNAME%,ip:127.0.0.1"
for /f "tokens=2 delims=:" %%I in ('ipconfig ^| findstr /i /c:"IPv4"') do for /f "tokens=*" %%J in ("%%I") do set "SAN=!SAN!,ip:%%J"
del /q "%SERVER_STORE%" "%WORK_DIR%\ratel-fm-server.csr" "%WORK_DIR%\ratel-fm-server.crt" >nul 2>nul
"%KEYTOOL%" -genkeypair -alias ratel-fm-server -keyalg RSA -keysize 2048 -validity 825 -dname "CN=%COMPUTERNAME%, OU=Ratel FM, O=ratel, L=Chengdu, ST=Sichuan, C=CN" -storetype PKCS12 -keystore "%SERVER_STORE%" -storepass "!SERVER_PASS!" -keypass "!SERVER_PASS!" || exit /b 1
"%KEYTOOL%" -certreq -alias ratel-fm-server -keystore "%SERVER_STORE%" -storepass "!SERVER_PASS!" -file "%WORK_DIR%\ratel-fm-server.csr" -ext "!SAN!" -ext EKU=serverAuth || exit /b 1
"%KEYTOOL%" -gencert -alias ratel-local-ca -keystore "%CA_STORE%" -storepass "!CA_PASS!" -infile "%WORK_DIR%\ratel-fm-server.csr" -outfile "%WORK_DIR%\ratel-fm-server.crt" -rfc -validity 825 -ext "!SAN!" -ext EKU=serverAuth -ext KeyUsage=digitalSignature,keyEncipherment || exit /b 1
"%KEYTOOL%" -importcert -alias ratel-local-ca -keystore "%SERVER_STORE%" -storepass "!SERVER_PASS!" -file "%CA_CERT%" -noprompt || exit /b 1
"%KEYTOOL%" -importcert -alias ratel-fm-server -keystore "%SERVER_STORE%" -storepass "!SERVER_PASS!" -file "%WORK_DIR%\ratel-fm-server.crt" || exit /b 1
>"%CERT_DIR%\https-info.txt" echo HTTPS URL: https://127.0.0.1:%HTTPS_PORT%%CONTEXT_PATH%
echo HTTPS certificate generated: %SERVER_STORE%
exit /b 0
