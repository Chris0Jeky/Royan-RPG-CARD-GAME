@echo off
rem Royan dev launcher: incremental build, serve on a free port, open browser.
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME if exist "%TEMP%\royan-tools\jdk17" (
  set "JAVA_HOME=%TEMP%\royan-tools\jdk17"
  set "Path=%JAVA_HOME%\bin;%TEMP%\royan-tools\maven\bin;%Path%"
)
where mvn >nul 2>nul
if errorlevel 1 (
  echo [royan] mvn not found. Install Maven 3.9 plus JDK 17, or use the portable toolchain. 1>&2
  exit /b 1
)
mvn -q compile exec:java -Dexec.mainClass=com.chris.cardgame.Main -Dexec.args="serve 0"
