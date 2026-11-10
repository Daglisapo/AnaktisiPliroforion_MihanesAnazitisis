@echo off
rem Tool: show df / cf of terms in the inverted index
rem Usage: inspect-index.bat network retrieval --top 15
setlocal
set "MAIN_CLASS=gr.ihu.ir.indexer.IndexInspector"
set "JAVA_OPTS=-Xmx1g --add-modules jdk.incubator.vector"
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
