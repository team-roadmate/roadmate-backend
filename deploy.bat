@echo off
REM 설정: 프로젝트 폴더와 JAR 파일명을 지정합니다.
set REPOSITORY="C:\roadmate-backend"
set PROJECT_NAME="roadmate-backend"
set JAR_FILE=%PROJECT_NAME%.jar

REM ★★★ 중요: 이 경로는 GitHub Runner의 빌드 결과물 경로입니다. ★★★
REM roadmate-backend 폴더 내에서 빌드됩니다.
set WORKSPACE_NAME="roadmate-backend"
set BUILD_DIR="C:\actions-runner\_work\%WORKSPACE_NAME%\%WORKSPACE_NAME%\build\libs"

echo ">>> 빌드 파일 복사 및 이름 변경"
REM Runner가 빌드한 JAR 파일을 현재 프로젝트 디렉토리로 복사합니다.
copy %BUILD_DIR%\*.jar %REPOSITORY%\%JAR_FILE% /y

echo ">>> 기존 프로세스 종료"
REM 현재 실행 중인 Spring 애플리케이션 (JAR 파일명 기준)을 찾아서 종료합니다.
FOR /F "tokens=2" %%i IN ('tasklist /nh /fi "imagename eq java.exe" /v ^| findstr /i "%PROJECT_FILE%"') DO (
    echo Stopping PID: %%i
    taskkill /pid %%i /f
)
timeout /t 5 /nobreak

echo ">>> 새 애플리케이션 시작"
REM javaw를 사용하여 백그라운드에서 실행하고 콘솔 창이 뜨는 것을 방지합니다.
start javaw -jar %REPOSITORY%\%JAR_FILE%
