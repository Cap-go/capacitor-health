#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
simulator_id="${CAPGO_MAESTRO_IOS_SIMULATOR_ID:?Missing CAPGO_MAESTRO_IOS_SIMULATOR_ID}"
artifact_name="${CAPGO_MAESTRO_IOS_ARTIFACT_NAME:-maestro-ios-app}"
run_id="${GITHUB_RUN_ID:?Missing GITHUB_RUN_ID}"

xcrun simctl boot "$simulator_id" 2>/dev/null || true
(
  xcrun simctl bootstatus "$simulator_id" -b
) &
boot_pid=$!

gh run download "$run_id" --name "$artifact_name" --dir "$ROOT_DIR"

wait "$boot_pid"

tar -xzf "$ROOT_DIR/maestro-ios-app.tar.gz" -C "$ROOT_DIR"

if [[ ! -d "$ROOT_DIR/App.app" ]]; then
  echo "Missing App.app after extracting $artifact_name" >&2
  exit 1
fi