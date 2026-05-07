#!/bin/bash
# Start Draw & Guess Server
cd "$(dirname "$0")"
javac -cp "src:lib/sqlite-jdbc.jar" -d out $(find src -name "*.java") 2>&1
java -cp "out:lib/sqlite-jdbc.jar" server.GameServer "$@"
