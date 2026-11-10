@echo off
rem App 4 - Query Client: sends a query to the server and prints the JSON reply to stdout
rem Usage: run-app4.bat "machine learning" 10   |   run-app4.bat   (interactive)   |   run-app4.bat --shutdown
setlocal
set "MAIN_CLASS=gr.ihu.ir.client.QueryClient"
set "JAVA_OPTS=-Xmx256m -XX:TieredStopAtLevel=1"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
