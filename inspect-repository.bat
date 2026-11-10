@echo off
rem Tool: read back the Repository (files, sizes, document count, sample text)
rem Usage: inspect-repository.bat [document-id]
setlocal
set "MAIN_CLASS=gr.ihu.ir.store.RepositoryInspector"
set "JAVA_OPTS=-Xmx1g"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
