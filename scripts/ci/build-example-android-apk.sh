#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"

cd "$ROOT_DIR"
bun run build
cd "$EXAMPLE_DIR"
bun install --frozen-lockfile
HEALTH_PKG="$(node -p "require('path').dirname(require.resolve('@capgo/capacitor-health/package.json'))")"
rm -rf "$HEALTH_PKG/android" "$HEALTH_PKG/dist"
cp -a "$ROOT_DIR/android" "$HEALTH_PKG/"
cp -a "$ROOT_DIR/dist" "$HEALTH_PKG/"
echo "Synced capacitor-health into $HEALTH_PKG for example APK"
bun run build
bunx cap sync android
cd android
./gradlew assembleDebug --build-cache