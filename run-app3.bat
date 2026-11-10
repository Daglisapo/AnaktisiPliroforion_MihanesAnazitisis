@echo off
rem App 3 - Query Server: TCP/JSON server loop on port 5555 (stop: run-app4.bat --shutdown)
rem Usage: run-app3.bat
setlocal
set "MAIN_CLASS=gr.ihu.ir.server.QueryServer"
set "JAVA_OPTS=-Xmx1g --add-modules jdk.incubator.vector"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
