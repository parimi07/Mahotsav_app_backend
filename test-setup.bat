@echo off
echo ========================================
echo Testing Native Android App Build
echo ========================================
echo.

REM Set JAVA_HOME
set JAVA_HOME=C:\Program Files\Java\jdk-17

echo Checking project structure...
if not exist "app\src\main\java\com\eventadmin\LoginActivity.java" (
    echo ERROR: LoginActivity.java not found!
    pause
    exit /b 1
)

if not exist "app\src\main\java\com\eventadmin\DashboardActivity.java" (
    echo ERROR: DashboardActivity.java not found!
    pause
    exit /b 1
)

if not exist "app\src\main\AndroidManifest.xml" (
    echo ERROR: AndroidManifest.xml not found!
    pause
    exit /b 1
)

echo ✓ All source files present
echo.

echo Testing Gradle configuration...
call gradlew.bat tasks --all > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Gradle configuration may need sync
) else (
    echo ✓ Gradle configuration OK
)

echo.
echo ========================================
echo Project structure is valid!
echo ========================================
echo.
echo Ready to build. Run: build.bat
echo.
pause
