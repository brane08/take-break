@echo off
@setlocal
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found. Install Java 21+ from adoptium.net
    pause
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%v
)
set JAVA_VER=%JAVA_VER:"=%
for /f "delims=." %%m in ("%JAVA_VER%") do set JAVA_MAJOR=%%m
if %JAVA_MAJOR% LSS 21 (
    echo ERROR: Java 21+ required. Found: %JAVA_VER%
    pause
    exit /b 1
)
set JAVA_PATH=javaw
if defined JAVA_HOME set JAVA_PATH="%JAVA_HOME%\bin\javaw"
endlocal & %JAVA_PATH% -Dprism.allowhidpi=true -jar "%~dp0take-break-app.jar"
