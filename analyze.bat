@echo off
rem Tool: show what EnglishAnalyzer does to a sentence (casefolding, stopwords, stemming)
rem Usage: analyze.bat "Neural Networks and networking"
setlocal
set "MAIN_CLASS=gr.ihu.ir.indexer.AnalyzerDemo"
set "JAVA_OPTS="
call "%~dp0run-java.bat" %*
exit /b %ERRORLEVEL%
