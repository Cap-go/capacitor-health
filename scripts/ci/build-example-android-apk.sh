#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"

cd "$ROOT_DIR"
export CAPGO_MAESTRO_E2E=1
bun run build
cd "$EXAMPLE_DIR"
bun install --frozen-lockfile
bun run build
if ! grep -rq "permission-denied:success" dist/assets/*.js 2>/dev/null; then
  echo "Maestro web bundle missing permission-denied E2E strings" >&2
  exit 1
fi
bunx cap sync android
cd android
./gradlew clean :capgo-capacitor-health:clean assembleDebug --no-build-cache