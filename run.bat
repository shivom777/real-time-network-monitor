@echo off
cd /d %~dp0
if not exist out mkdir out
javac --add-modules jdk.httpserver -d out src\main\java\com\networkmonitor\*.java
if errorlevel 1 (
  echo.
  echo Build failed. Make sure JDK 21 is installed and JAVA_HOME/PATH is configured.
  pause
  exit /b 1
)
echo.
set PORT=%1
if "%PORT%"=="" set PORT=8080
echo Starting NetPulse at http://localhost:%PORT%
start "" http://localhost:%PORT%
java --add-modules jdk.httpserver -cp out com.networkmonitor.NetworkMonitorServer src\main\resources %PORT%
pause
