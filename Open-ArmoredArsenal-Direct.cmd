@echo off
setlocal

cd /d "%~dp0"

if not exist logs mkdir logs

set "JAVA_HOME=C:\Program Files\Java\jdk-25"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_OPTS=-Dorg.gradle.daemon=false -Dorg.gradle.internal.http.connectionTimeout=30000 -Dorg.gradle.internal.http.socketTimeout=30000"
set "GRADLE=%~dp0gradlew.bat"
set "LOG=%~dp0logs\direct-launch-latest.log"

echo Launching Armored Arsenal with Java 25...
echo Log file: %LOG%
echo.

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Java 25 was not found at "%JAVA_HOME%".
    pause
    exit /b 1
)

if not exist "%GRADLE%" (
    echo The project Gradle wrapper was not found at "%GRADLE%".
    pause
    exit /b 1
)

echo Start time: %DATE% %TIME% > "%LOG%"
echo Using Java: >> "%LOG%"
"%JAVA_HOME%\bin\java.exe" -version >> "%LOG%" 2>&1
echo. >> "%LOG%"
echo Running Gradle offline runClient... >> "%LOG%"

call "%GRADLE%" --no-daemon --offline --console=plain runClient --stacktrace >> "%LOG%" 2>&1
set "EXIT_CODE=%ERRORLEVEL%"

if "%EXIT_CODE%"=="0" goto launch_finished

findstr /C:"No cached version available for offline mode" "%LOG%" >nul
if errorlevel 1 goto launch_finished

echo.
echo The offline Gradle cache is incomplete. Retrying once online...
echo. >> "%LOG%"
echo Offline cache incomplete; retrying online... >> "%LOG%"
call "%GRADLE%" --no-daemon --console=plain runClient --stacktrace >> "%LOG%" 2>&1
set "EXIT_CODE=%ERRORLEVEL%"

:launch_finished
echo.
echo Minecraft launcher finished with exit code %EXIT_CODE%.
echo Last log lines:
echo ------------------------------------------------------------
powershell.exe -NoProfile -Command "Get-Content -LiteralPath '%LOG%' -Tail 80"
echo ------------------------------------------------------------
pause
exit /b %EXIT_CODE%
