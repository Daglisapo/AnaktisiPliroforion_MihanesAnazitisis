@echo off
rem Build the fat jar target\search-engine.jar (Maven Wrapper downloads Maven if needed).
cd /d "%~dp0"
call "%~dp0mvnw.cmd" -q -B package
if errorlevel 1 exit /b 1
echo Build OK: target\search-engine.jar
