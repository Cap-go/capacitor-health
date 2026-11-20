#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SIMULATOR_ID="${CAPGO_IOS_TEST_SIMULATOR_ID:-$("$ROOT_DIR/scripts/select-ios-simulator.sh")}"

xcodebuild test \
  -scheme CapgoCapacitorHealth \
  -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
  -quiet