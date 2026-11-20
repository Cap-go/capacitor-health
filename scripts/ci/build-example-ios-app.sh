#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"
DERIVED_DATA_PATH="${CAPGO_MAESTRO_IOS_DERIVED_DATA_PATH:-$ROOT_DIR/maestro-ios-derived-data}"
SIMULATOR_ID="${CAPGO_MAESTRO_IOS_SIMULATOR_ID:-$("$ROOT_DIR/scripts/select-ios-simulator.sh")}"

cd "$ROOT_DIR"
bun run build
cd "$EXAMPLE_DIR"
bun install --frozen-lockfile
bun run build
bunx cap sync ios
xcodebuild \
  -project ios/App/App.xcodeproj \
  -scheme App \
  -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
  -derivedDataPath "$DERIVED_DATA_PATH" \
  -configuration Debug \
  CODE_SIGNING_ALLOWED=NO \
  build