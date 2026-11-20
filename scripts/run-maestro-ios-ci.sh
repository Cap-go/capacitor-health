#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RETRIES="${CAPGO_MAESTRO_TEST_RETRIES:-2}"

export CAPGO_MAESTRO_IOS_FLOW="${CAPGO_MAESTRO_IOS_FLOW:-$ROOT_DIR/.maestro/ios/ci-suite.yaml}"
export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-120000}"

for attempt in $(seq 1 "$RETRIES"); do
  if bash "$ROOT_DIR/scripts/run-maestro-ios-body-composition-unsupported.sh"; then
    exit 0
  fi
  if [[ "$attempt" -lt "$RETRIES" ]]; then
    echo "Maestro iOS CI suite failed on attempt $attempt, retrying..." >&2
  fi
done

exit 1