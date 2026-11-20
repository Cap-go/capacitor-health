#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PERMISSION_FLOW="$ROOT_DIR/.maestro/android/permission-denied.yaml"
BODY_FLOW="$ROOT_DIR/.maestro/android/body-composition-kill.yaml"
CONFIG="$ROOT_DIR/.maestro/config.yaml"
RETRIES="${CAPGO_MAESTRO_TEST_RETRIES:-1}"

export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-90000}"

install_apk() {
  CAPGO_MAESTRO_SKIP_BUILD=1 CAPGO_MAESTRO_INSTALL_ONLY=1 \
    bash "$ROOT_DIR/scripts/run-maestro-android-body-composition.sh"
}

run_flow() {
  local label="$1"
  local flow_path="$2"
  local started
  started="$(date +%s)"
  if ! maestro test --config "$CONFIG" "$flow_path"; then
    return 1
  fi
  echo "Maestro Android flow ${label} finished in $(( $(date +%s) - started ))s"
}

run_android_e2e() {
  bash "$ROOT_DIR/scripts/ci/prepare-android-health-connect-emulator.sh"
  if ! run_flow permission-denied "$PERMISSION_FLOW"; then
    return 1
  fi
  bash "$ROOT_DIR/scripts/ci/grant-health-connect-e2e-permissions.sh"
  if ! run_flow body-composition-kill "$BODY_FLOW"; then
    return 1
  fi
}

for attempt in $(seq 1 "$RETRIES"); do
  install_apk
  if run_android_e2e; then
    exit 0
  fi
  if [[ "$attempt" -lt "$RETRIES" ]]; then
    echo "Maestro Android CI suite failed on attempt $attempt, retrying..." >&2
  fi
done

exit 1