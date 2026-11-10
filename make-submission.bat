@echo off
rem Creates submission.zip with ONLY source code, pom.xml, scripts and docs
rem (no target\, PDFs, repository or index).
setlocal
cd /d "%~dp0"
if exist submission.zip del submission.zip
powershell -NoProfile -ExecutionPolicy Bypass -Command "$items = @('src','.mvn','.vscode','pom.xml','mvnw','mvnw.cmd','config.properties','.gitignore','.gitattributes') + (Get-ChildItem -Name *.bat,*.md); Compress-Archive -Path $items -DestinationPath submission.zip -Force; $z = Get-Item submission.zip; Write-Host ('submission.zip: {0:N1} KB' -f ($z.Length/1KB))"
exit /b %ERRORLEVEL%
