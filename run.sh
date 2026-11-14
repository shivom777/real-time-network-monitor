#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
mkdir -p out
javac --add-modules jdk.httpserver -d out src/main/java/com/networkmonitor/*.java
PORT="${1:-8080}"
echo "Starting NetPulse at http://localhost:${PORT}"
java --add-modules jdk.httpserver -cp out com.networkmonitor.NetworkMonitorServer src/main/resources "$PORT"
