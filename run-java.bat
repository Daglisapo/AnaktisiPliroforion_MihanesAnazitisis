@echo off
rem ------------------------------------------------------------------
rem  Common launcher used by run-app1..5.bat and the inspect tools.
rem  The caller sets MAIN_CLASS (and optionally JAVA_OPTS) first.
rem  Uses %JAVA_HOME%\bin\java.exe if JAVA_HOME is set, else "java".
rem
rem  We "cd" into the project folder and pass RELATIVE paths to Java
rem  (target\search-engine.jar, config.properties): the project path
rem  contains Greek characters, and the Java launcher on Windows can
rem  corrupt non-ASCII command-line arguments. setlocal restores the
rem  caller's current directory when this script ends.
rem ------------------------------------------------------------------
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if not exist "target\search-engine.jar" (
  echo [!] target\search-engine.jar not found. Run build.bat first.
  exit /b 1
)
set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
"%JAVA_EXE%" %JAVA_OPTS% -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp target\search-engine.jar %MAIN_CLASS% %*
exit /b %ERRORLEVEL%
