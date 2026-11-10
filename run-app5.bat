@echo off
rem App 5 - Search Interface: web page on http://localhost:8080 (calls App 4)
rem Usage: run-app5.bat
setlocal
set "MAIN_CLASS=gr.ihu.ir.ui.SearchInterface"
set "JAVA_OPTS=-Xmx256m"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
