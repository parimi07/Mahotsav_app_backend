@echo off
echo ====================================
echo Event Admin - Native Android Build
echo ====================================
echo.

REM Set JAVA_HOME
set JAVA_HOME=C:\Program Files\Java\jdk-17
echo Using JDK: %JAVA_HOME%
echo.

REM Navigate to project directory
cd /d "%~dp0"

echo Step 1: Cleaning previous builds...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Building debug APK...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo.
echo ====================================
echo BUILD SUCCESSFUL!
echo ====================================
echo.
echo APK Location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo To install on connected device, run:
echo   gradlew.bat installDebug
echo.
pause
