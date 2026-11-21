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
if ! grep -q 'type="module"' dist/index.html 2>/dev/null; then
  echo "Maestro web bundle missing module script in dist/index.html" >&2
  exit 1
fi
if grep -q 'src="/assets/' dist/index.html 2>/dev/null; then
  echo "Maestro web bundle must use relative asset URLs (vite base: './')" >&2
  exit 1
fi
if ! grep -rq "E2E:boot:ready" dist/assets/*.js 2>/dev/null; then
  echo "Maestro web bundle missing E2E boot marker" >&2
  exit 1
fi
if ! grep -rq "permission-denied:success" dist/assets/*.js 2>/dev/null; then
  echo "Maestro web bundle missing Android permission-denied E2E strings" >&2
  exit 1
fi
bunx cap sync android
cd android
./gradlew clean :capgo-capacitor-health:clean assembleDebug --no-build-cache