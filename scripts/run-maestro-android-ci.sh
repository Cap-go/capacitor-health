#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DENIED_FLOW="$ROOT_DIR/.maestro/android/permission-denied.yaml"
BODY_FLOW="$ROOT_DIR/.maestro/android/body-composition-and-kill.yaml"
CONFIG="$ROOT_DIR/.maestro/config.yaml"
RETRIES="${CAPGO_MAESTRO_TEST_RETRIES:-2}"

export CAPGO_MAESTRO_ANDROID_FLOW="${CAPGO_MAESTRO_ANDROID_FLOW:-$ROOT_DIR/.maestro/android/ci-suite.yaml}"
export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-120000}"

run_maestro_flow() {
  maestro test --config "$CONFIG" "$1"
}

install_apk() {
  CAPGO_MAESTRO_SKIP_BUILD=1 CAPGO_MAESTRO_INSTALL_ONLY=1 \
    bash "$ROOT_DIR/scripts/run-maestro-android-body-composition.sh"
}

run_suite() {
  install_apk

  if [[ "${CAPGO_MAESTRO_ANDROID_SINGLE_FLOW:-}" == "1" ]]; then
    run_maestro_flow "$CAPGO_MAESTRO_ANDROID_FLOW"
    return
  fi

  run_maestro_flow "$DENIED_FLOW"
  bash "$ROOT_DIR/scripts/ci/grant-health-connect-e2e-permissions.sh"
  run_maestro_flow "$BODY_FLOW"
}

for attempt in $(seq 1 "$RETRIES"); do
  if run_suite; then
    exit 0
  fi
  if [[ "$attempt" -lt "$RETRIES" ]]; then
  echo "Maestro Android CI suite failed on attempt $attempt, retrying..." >&2
  fi
done

exit 1