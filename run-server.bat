@echo off
REM Start Draw & Guess Server (Windows)
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -cp "src;lib/sqlite-jdbc.jar" -d out @sources.txt
del sources.txt
java -cp "out;lib/sqlite-jdbc.jar" server.GameServer %*
