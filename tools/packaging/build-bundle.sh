#!/bin/sh
# Build the Royan distributable bundle: fat jar, jpackage app-image, zip.
# Usage: sh tools/packaging/build-bundle.sh [--skip-tests]
set -eu

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

SKIP_TESTS=""
if [ "${1:-}" = "--skip-tests" ]; then
  SKIP_TESTS="-DskipTests"
fi

MVN_ARGS="package"
if [ -n "$SKIP_TESTS" ]; then
  MVN_ARGS="$MVN_ARGS $SKIP_TESTS"
fi
# shellcheck disable=SC2086
mvn $MVN_ARGS

JAR="$(ls target/royan-*.jar 2>/dev/null | grep -v -- '-sources\.jar' | grep -v -- '-javadoc\.jar' | grep -v -- '-shaded\.jar' | sort | head -n 1 || true)"
if [ -z "${JAR:-}" ]; then
  echo "[royan] no fat jar found in target/ after mvn package" >&2
  exit 1
fi

APP_VERSION="$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -n 1 | cut -d- -f1)"
case "$APP_VERSION" in
  ''|*[!0-9.]*) APP_VERSION="0.1.0" ;;
esac

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
  JPACKAGE="$JAVA_HOME/bin/jpackage"
else
  JPACKAGE="$(command -v jpackage || true)"
fi
if [ -z "${JPACKAGE:-}" ]; then
  echo "[royan] jpackage not found (needs JDK 17+ on JAVA_HOME or PATH)" >&2
  exit 1
fi

mkdir -p dist
rm -rf dist/RoyanRPG

echo "[royan] jpackage app-image $APP_VERSION"
"$JPACKAGE" --type app-image --name RoyanRPG --app-version "$APP_VERSION" \
  --input "$ROOT/target" --main-jar "$(basename "$JAR")" \
  --main-class com.chris.cardgame.Main --dest "$ROOT/dist" \
  --description "Royan RPG Card Game" --vendor "Royan" \
  --java-options "-Dfile.encoding=UTF-8"

cp "$JAR" "dist/$(basename "$JAR")"

OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
case "$OS" in
  darwin) SUFFIX="mac" ;;
  linux) SUFFIX="linux" ;;
  *) SUFFIX="$OS" ;;
esac
if ! command -v zip >/dev/null 2>&1; then
  echo "[royan] 'zip' not found; app-image is at dist/RoyanRPG" >&2
  exit 1
fi
rm -f "dist/RoyanRPG-$SUFFIX.zip"
(cd dist && zip -qr "RoyanRPG-$SUFFIX.zip" RoyanRPG)
echo "[royan] bundle ready: dist/RoyanRPG-$SUFFIX.zip"
