#!/bin/bash
# Start Draw & Guess Client
cd "$(dirname "$0")"
java -cp "out:lib/sqlite-jdbc.jar" client.gui.MainFrame "$@"
