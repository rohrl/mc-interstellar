@echo off
setlocal
title Interstellar Minecraft
if not defined JAVA_HOME if exist "C:\Portable\jdks\temurin-21.0.12.1\bin\java.exe" set "JAVA_HOME=C:\Portable\jdks\temurin-21.0.12.1"
cd /d "%~dp0"
echo Launching the current checkout. Do not open the same world in a second client.
call gradlew.bat runClient
if errorlevel 1 (
  echo Launch failed. Configure JAVA_HOME for a complete JDK 21. See the output above.
  pause
)
