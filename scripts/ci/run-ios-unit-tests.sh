#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SIMULATOR_ID="${CAPGO_MAESTRO_IOS_SIMULATOR_ID:-$("$ROOT_DIR/scripts/select-ios-simulator.sh")}"
RESULT_PATH="$ROOT_DIR/build/ios-unit-tests.xcresult"

mkdir -p "$ROOT_DIR/build"
rm -rf "$RESULT_PATH"

xcrun simctl boot "$SIMULATOR_ID" 2>/dev/null || true
xcrun simctl bootstatus "$SIMULATOR_ID" -b

cd "$ROOT_DIR"
xcodebuild test \
  -scheme CapgoCapacitorHealth \
  -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
  -resultBundlePath "$RESULT_PATH" \
  CODE_SIGNING_ALLOWED=NO