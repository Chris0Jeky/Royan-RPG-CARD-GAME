#!/bin/sh
# Royan dev launcher: incremental build, serve on a free port, open browser.
set -eu
cd "$(dirname "$0")"
if ! command -v mvn >/dev/null 2>&1; then
  echo "[royan] mvn not found. Install Maven 3.9 plus JDK 17." >&2
  exit 1
fi
exec mvn -q compile exec:java \
  "-Dexec.mainClass=com.chris.cardgame.Main" \
  "-Dexec.args=serve 0"
