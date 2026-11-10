@echo off
rem App 1 - Store Module: PDF -> plain text -> Repository (JSON Lines, 128 MB files)
rem Usage: run-app1.bat [--limit N] [--threads T]
setlocal
set "MAIN_CLASS=gr.ihu.ir.store.StoreModule"
set "JAVA_OPTS=-Xmx3g"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
