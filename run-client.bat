@echo off
REM Start Draw & Guess Client (Windows)
cd /d "%~dp0"
java -cp "out;lib/sqlite-jdbc.jar" client.gui.MainFrame %*
