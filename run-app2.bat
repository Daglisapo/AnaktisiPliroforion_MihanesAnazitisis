@echo off
rem App 2 - Text Preprocessor and Lucene Indexer: Repository -> inverted index
rem Usage: run-app2.bat
setlocal
set "MAIN_CLASS=gr.ihu.ir.indexer.Indexer"
set "JAVA_OPTS=-Xmx2g --add-modules jdk.incubator.vector"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
