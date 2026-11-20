#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"

cd "$ROOT_DIR"
bun run build
cd "$EXAMPLE_DIR"
bun install --frozen-lockfile
bun run build
bunx cap sync android
cd android
./gradlew clean :capgo-capacitor-health:clean assembleDebug --no-build-cache