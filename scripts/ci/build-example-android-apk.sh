#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"

cd "$ROOT_DIR"
bun run build
cd "$EXAMPLE_DIR"
bun install --frozen-lockfile
HEALTH_PKG="$(node -p "require('path').dirname(require.resolve('@capgo/capacitor-health/package.json'))")"
rsync -a "$ROOT_DIR/android/" "$HEALTH_PKG/android/"
rsync -a "$ROOT_DIR/dist/" "$HEALTH_PKG/dist/"
bun run build
bunx cap sync android
cd android
./gradlew assembleDebug --build-cache