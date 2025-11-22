@echo off
setlocal

REM =======================================================
REM Configuration
REM =======================================================
set "DEPLOY_PATH=%USERPROFILE%\roadmate-backend"
set "SERVICE_NAME=RoadmateApiService"
set "JAR_NAME=roadmate-backend.jar"
set "SOURCE_BUILD_DIR=.\build\libs"
REM NSSM 실행 파일 이름을 설정합니다. 스크립트와 같은 폴더에 NSSM을 두는 것을 권장합니다.
set "NSSM_EXEC=nssm.exe"

REM Java executable - JAVA_HOME이 정의되어 있으면 사용하고, 아니면 시스템 PATH에서 찾도록 합니다.
if defined JAVA_HOME (
  set "JAVA_EXEC_PATH=%JAVA_HOME%\bin\java.exe"
) else (
  REM 시스템 PATH에 등록된 java.exe를 사용하도록 설정 (가장 유연함)
  set "JAVA_EXEC_PATH=java.exe"
)

set "LOG_DIR=%DEPLOY_PATH%\logs"
set "STDOUT_LOG=%LOG_DIR%\service.out.log"
set "STDERR_LOG=%LOG_DIR%\service.err.log"

REM =======================================================
REM 1. Check prerequisites
REM =======================================================
echo Checking prerequisites...

REM NSSM 체크: 현재 폴더 또는 PATH에서 찾습니다. (유연성 개선)
where "%NSSM_EXEC%" >nul 2>&1
if errorlevel 1 (
  echo ERROR: NSSM not found. Ensure "%NSSM_EXEC%" is in the current directory or in the system PATH.
  exit /b 1
)
set "NSSM_PATH=%NSSM_EXEC%"

REM Java 실행 파일 체크 (유연성 개선)
where "%JAVA_EXEC_PATH%" >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java executable not found. Ensure JAVA_HOME is set or "java.exe" is in the system PATH.
  exit /b 1
)

if not exist "%SOURCE_BUILD_DIR%\*.jar" (
  echo ERROR: No JAR found in "%SOURCE_BUILD_DIR%".
  exit /b 1
)

REM 환경 변수 필수 확인 (스크립트 실행 전 반드시 설정해야 함)
if not defined DB_URL ( echo ERROR: Environment variable DB_URL is missing. & exit /b 1 )
if not defined DB_USER ( echo ERROR: Environment variable DB_USER is missing. & exit /b 1 )
if not defined DB_PASSWORD ( echo ERROR: Environment variable DB_PASSWORD is missing. & exit /b 1 )
if not defined SERVER_PORT ( echo ERROR: Environment variable SERVER_PORT is missing. & exit /b 1 )

REM 디렉터리 생성
if not exist "%DEPLOY_PATH%" ( mkdir "%DEPLOY_PATH%" )
if not exist "%LOG_DIR%" ( mkdir "%LOG_DIR%" )

REM =======================================================
REM 2. Copy build artifact
REM =======================================================
echo Copying JAR file...
copy /Y "%SOURCE_BUILD_DIR%\*.jar" "%DEPLOY_PATH%\%JAR_NAME%" >nul
if errorlevel 1 (
  echo ERROR: Failed to copy JAR. Check file lock or existence.
  exit /b 1
)

REM =======================================================
REM 3. Stop and remove existing service if running
REM =======================================================
echo Checking existing service...
sc query "%SERVICE_NAME%" >nul 2>&1
if %errorlevel%==0 (
  echo Existing service found. Stopping and removing...
  net stop "%SERVICE_NAME%" /y >nul 2>&1

  REM NSSM 및 SC 삭제 명령을 실행하여 이전 구성을 완전히 정리
  "%NSSM_PATH%" remove "%SERVICE_NAME%" confirm >nul 2>&1
  sc delete "%SERVICE_NAME%" >nul 2>&1
  timeout /t 2 /nobreak >nul
)

REM =======================================================
REM 4. Build App Parameters (Simplified & Secure)
REM =======================================================
REM **보안 개선**: 민감한 정보를 제외하고 순수 실행 명령만 남깁니다.
set "JAVA_ARGS=-jar \"%DEPLOY_PATH%\%JAR_NAME%\""

REM =======================================================
REM 5. Install NSSM service and set Environment Variables (Crucial Fix)
REM =======================================================
echo Installing service...
"%NSSM_PATH%" install "%SERVICE_NAME%" "%JAVA_EXEC_PATH%"
if errorlevel 1 (
  echo ERROR: NSSM install failed.
  exit /b 1
)

REM 일반 설정
"%NSSM_PATH%" set "%SERVICE_NAME%" DisplayName "Roadmate API Service"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppDirectory "%DEPLOY_PATH%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppParameters "%JAVA_ARGS%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppStdout "%STDOUT_LOG%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppStderr "%STDERR_LOG%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppRotateFiles 1
"%NSSM_PATH%" set "%SERVICE_NAME%" Start SERVICE_AUTO_START

REM ***보안 개선***: 민감한 DB 설정 및 포트를 NSSM의 AppEnvironmentExtra를 통해 환경 변수로 설정합니다.
REM Spring Boot는 대문자 환경 변수를 자동으로 인식합니다. (예: SPRING_DATASOURCE_URL -> spring.datasource.url)
echo Setting secure environment variables for the service...
set "ENV_VARS=SPRING_DATASOURCE_URL=%DB_URL% SPRING_DATASOURCE_USERNAME=%DB_USER% SPRING_DATASOURCE_PASSWORD=%DB_PASSWORD% SERVER_PORT=%SERVER_PORT%"
"%NSSM_PATH%" set "%SERVICE_NAME%" AppEnvironmentExtra "%ENV_VARS%"

REM =======================================================
REM 6. Start service and verify status (Reliability Improvement)
REM =======================================================
echo Starting service...
net start "%SERVICE_NAME%" 2>nul
if errorlevel 1 (
  echo ERROR: Service start command failed immediately.
  echo Check logs: %STDERR_LOG% and %STDOUT_LOG%
  exit /b 1
)

REM **안정성 개선**: 서비스가 완전히 시작될 때까지 최대 10초간 대기하며 상태를 확인합니다.
echo Waiting for service to stabilize (max 10 seconds)...
for /l %%i in (1,1,10) do (
  sc query "%SERVICE_NAME%" | find "STATE" | find "RUNNING" >nul
  if not errorlevel 1 (
    echo Service started successfully.
    goto :START_SUCCESS
  )
  timeout /t 1 /nobreak >nul
)

sc query "%SERVICE_NAME%" | find "STATE" | find "RUNNING" >nul
if errorlevel 1 (
  echo WARNING: Service failed to enter the RUNNING state after timeout.
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