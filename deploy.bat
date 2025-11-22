@echo off
setlocal

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

if "%DB_URL%"=="" (
  echo ERROR: Environment variable DB_URL is missing.
  exit /b 1
)

if "%DB_USER%"=="" (
  echo ERROR: Environment variable DB_USER is missing.
  exit /b 1
)

if "%DB_PASSWORD%"=="" (
  echo ERROR: Environment variable DB_PASSWORD is missing.
  exit /b 1
)

if "%SERVER_PORT%"=="" (
  echo ERROR: Environment variable SERVER_PORT is missing.
  exit /b 1
)

if not exist "%DEPLOY_PATH%" (
  mkdir "%DEPLOY_PATH%" 2>nul
)

if not exist "%LOG_DIR%" (
  mkdir "%LOG_DIR%" 2>nul
)

REM =======================================================
REM 2. Copy build artifact
REM =======================================================
echo Copying JAR file...
copy /Y "%SOURCE_BUILD_DIR%\*.jar" "%DEPLOY_PATH%\%JAR_NAME%" >nul
if errorlevel 1 (
  echo ERROR: Failed to copy JAR. Check file lock or existence. Access denied?
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
  timeout /t 1 >nul
)

REM =======================================================
REM 4. Build App Parameters
REM =======================================================
set "JAVA_ARGS=-jar \"%DEPLOY_PATH%\%JAR_NAME%\" -Dspring.datasource.password=\"%DB_PASSWORD%\" -Dspring.datasource.url=\"%DB_URL%\" -Dspring.datasource.username=\"%DB_USER%\" -Dserver.port=%SERVER_PORT%"

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

REM =======================================================
REM 6. Start service
REM =======================================================
echo Starting service...
net start "%SERVICE_NAME%" 2>nul

if errorlevel 1 (
  echo WARNING: Service failed to start. Check logs:
  echo   %STDERR_LOG%
  echo   %STDOUT_LOG%
) else (
  echo Service started successfully.
)

echo Done.
endlocal
exit /b 0
