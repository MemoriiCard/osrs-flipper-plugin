@echo off
rem Starts RuneLite with the OSRS Flipper beta plugin built in. Needs Java 11 or newer (adoptium.net).
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java isn't installed. Install Temurin 17 from https://adoptium.net, then double-click this file again.
  pause
  exit /b 1
)
java -ea -jar osrs-flipper-beta.jar
if errorlevel 1 (
  echo.
  echo The beta closed with an error. Send a screenshot of this window to whoever invited you.
  pause
)
