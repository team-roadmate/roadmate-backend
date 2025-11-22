@echo off
setlocal enabledelayedexpansion

REM =======================================================
REM Configuration
REM =======================================================
set "DEPLOY_PATH=%USERPROFILE%\roadmate-backend"
set "SERVICE_NAME=RoadmateApiService"
set "JAR_NAME=roadmate-backend.jar"
set "SOURCE_BUILD_DIR=.\build\libs"
set "NSSM_PATH=C:\nssm-2.24\win64\nssm.exe"

REM Java executable
if defined JAVA_HOME (
  set "JAVA_EXEC_PATH=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXEC_PATH=C:\Program Files\Java\jdk-21\bin\java.exe"
)

set "LOG_DIR=%DEPLOY_PATH%\logs"
set "STDOUT_LOG=%LOG_DIR%\service.out.log"
set "STDERR_LOG=%LOG_DIR%\service.err.log"

REM =======================================================
REM 1. Check prerequisites
REM =======================================================
echo Checking prerequisites...

if not exist "%NSSM_PATH%" (
  echo ERROR: NSSM not found at "%NSSM_PATH%".
  exit /b 1
)

if not exist "%JAVA_EXEC_PATH%" (
  echo ERROR: Java executable not found at "%JAVA_EXEC_PATH%".
  exit /b 1
)

if not exist "%SOURCE_BUILD_DIR%\*.jar" (
  echo ERROR: No JAR found in "%SOURCE_BUILD_DIR%".
  exit /b 1
)

REM Environment variable check
if "%DB_URL%"=="" (
  echo ERROR: DB_URL is missing.
  exit /b 1
)
if "%DB_USER%"=="" (
  echo ERROR: DB_USER is missing.
  exit /b 1
)
if "%DB_PASSWORD%"=="" (
  echo ERROR: DB_PASSWORD is missing.
  exit /b 1
)
if "%SERVER_PORT%"=="" (
  echo ERROR: SERVER_PORT is missing.
  exit /b 1
)

REM Create directories if not exist
if not exist "%DEPLOY_PATH%" mkdir "%DEPLOY_PATH%" 2>nul
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" 2>nul

REM =======================================================
REM 2. Copy build artifact
REM =======================================================
echo Copying JAR file...
copy /Y "%SOURCE_BUILD_DIR%\*.jar" "%DEPLOY_PATH%\%JAR_NAME%" >nul
if errorlevel 1 (
  echo ERROR: Failed to copy JAR. Check file lock or permissions.
  exit /b 1
)

REM =======================================================
REM 3. Stop existing service if running
REM =======================================================
echo Checking existing service...
sc query "%SERVICE_NAME%" >nul 2>&1
if %errorlevel%==0 (
  echo Existing service found. Stopping...
  net stop "%SERVICE_NAME%" /y >nul 2>&1
  "%NSSM_PATH%" remove "%SERVICE_NAME%" confirm 2>nul
  sc delete "%SERVICE_NAME%" >nul 2>&1
  timeout /t 1 /nobreak >nul
)

REM =======================================================
REM 4. Build App Parameters (특수문자 안전 처리)
REM =======================================================
REM Escape ^ & | < > " characters in password
set "ESCAPED_DB_PASSWORD=%DB_PASSWORD%"
set "ESCAPED_DB_PASSWORD=!ESCAPED_DB_PASSWORD:^=^^!"
set "ESCAPED_DB_PASSWORD=!ESCAPED_DB_PASSWORD:&=^&!"
set "ESCAPED_DB_PASSWORD=!ESCAPED_DB_PASSWORD:|=^|!"
set "ESCAPED_DB_PASSWORD=!ESCAPED_DB_PASSWORD:<=^<!"
set "ESCAPED_DB_PASSWORD=!ESCAPED_DB_PASSWORD:>=^>!"
set "ESCAPED_DB_PASSWORD=!ESCAPED_DB_PASSWORD:"=^"!"

set "JAVA_ARGS=-jar \"%DEPLOY_PATH%\%JAR_NAME%\" -Dspring.datasource.password=!ESCAPED_DB_PASSWORD! -Dspring.datasource.url=%DB_URL% -Dspring.datasource.username=%DB_USER% -Dserver.port=%SERVER_PORT%"

REM =======================================================
REM 5. Install NSSM service
REM =======================================================
echo Installing service...
"%NSSM_PATH%" install "%SERVICE_NAME%" "%JAVA_EXEC_PATH%"
if errorlevel 1 (
  echo ERROR: NSSM install failed.
  exit /b 1
)

"%NSSM_PATH%" set "%SERVICE_NAME%" AppDirectory "%DEPLOY_PATH%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppParameters "%JAVA_ARGS%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppStdout "%STDOUT_LOG%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppStderr "%STDERR_LOG%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppRotateFiles 1
"%NSSM_PATH%" set "%SERVICE_NAME%" Start SERVICE_AUTO_START
"%NSSM_PATH%" set "%SERVICE_NAME%" DisplayName "Roadmate API Service"

REM **추가**: 종료 시 자동 재시작
"%NSSM_PATH%" set "%SERVICE_NAME%" AppRestartDelay 5000
"%NSSM_PATH%" set "%SERVICE_NAME%" AppExit 1  // Exit code 1 → 자동 재시작

REM =======================================================
REM 6. Start service and verify status
REM =======================================================
echo Starting service...
net start "%SERVICE_NAME%" 2>nul
if errorlevel 1 (
  echo WARNING: Service failed to start command. Check logs:
  echo   %STDERR_LOG%
  echo   %STDOUT_LOG%
  goto :END_SCRIPT
)

REM Wait for RUNNING state (max 20 seconds)
echo Waiting for service to stabilize (max 20 seconds)...
for /l %%i in (1,1,20) do (
  sc query "%SERVICE_NAME%" | find "STATE" | find "RUNNING" >nul
  if not errorlevel 1 (
    echo Service started successfully.
    goto :START_SUCCESS
  )
  timeout /t 1 /nobreak >nul
)

sc query "%SERVICE_NAME%" | find "STATE" | find "RUNNING" >nul
if errorlevel 1 (
  echo WARNING: Service failed to enter RUNNING state after timeout.
  echo Please check logs immediately:
  echo   %STDERR_LOG%
  echo   %STDOUT_LOG%
  goto :END_SCRIPT
)

:START_SUCCESS
echo Service "%SERVICE_NAME%" is now running.

:END_SCRIPT
echo Done.
endlocal
exit /b 0
