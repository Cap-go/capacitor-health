#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CI_FLOW="$ROOT_DIR/.maestro/android/ci-suite.yaml"
CONFIG="$ROOT_DIR/.maestro/config.yaml"
GRANT_SCRIPT="$ROOT_DIR/scripts/ci/grant-health-connect-e2e-permissions.sh"
RETRIES="${CAPGO_MAESTRO_TEST_RETRIES:-1}"

export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-90000}"

install_apk() {
  CAPGO_MAESTRO_SKIP_BUILD=1 CAPGO_MAESTRO_INSTALL_ONLY=1 \
    bash "$ROOT_DIR/scripts/run-maestro-android-body-composition.sh"
}

APP_ID="${CAPGO_HEALTH_APP_ID:-app.capgo.health}"
HEALTH_E2E_PERMISSIONS=(
  android.permission.health.READ_LEAN_BODY_MASS
  android.permission.health.WRITE_LEAN_BODY_MASS
  android.permission.health.READ_BONE_MASS
  android.permission.health.WRITE_BONE_MASS
  android.permission.health.READ_BODY_WATER_MASS
  android.permission.health.WRITE_BODY_WATER_MASS
)

revoke_health_permissions() {
  for permission in "${HEALTH_E2E_PERMISSIONS[@]}"; do
    adb shell pm revoke --user 0 "$APP_ID" "$permission" 2>/dev/null || \
      adb shell pm revoke "$APP_ID" "$permission" 2>/dev/null || true
  done
}

start_app_for_maestro() {
  adb shell am force-stop "$APP_ID" 2>/dev/null || true
  adb shell am start -W -n "$APP_ID/.MainActivity" >/dev/null
  sleep 5
}

grant_when_permission_marker_logged() {
  local timeout_seconds="${1:-180}"
  local started
  started="$(date +%s)"
  adb logcat -c 2>/dev/null || true
  while (( $(date +%s) - started < timeout_seconds )); do
    if adb logcat -d 2>/dev/null | grep -q 'E2E:permission-denied:success'; then
      bash "$GRANT_SCRIPT"
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for E2E:permission-denied:success before granting Health Connect perms" >&2
  return 1
}

run_android_e2e() {
  revoke_health_permissions
  adb shell cmd healthconnect reset-app-permissions "$APP_ID" 2>/dev/null || true
  if adb shell dumpsys package "$APP_ID" 2>/dev/null | grep -q "android.permission.health.WRITE_LEAN_BODY_MASS: granted=true"; then
    echo "WRITE_LEAN_BODY_MASS still granted after revoke; permission-denied E2E cannot run" >&2
    return 1
  fi
  start_app_for_maestro

  local started grant_pid=0
  started="$(date +%s)"
  grant_when_permission_marker_logged 180 &
  grant_pid=$!

  if ! maestro test --config "$CONFIG" "$CI_FLOW"; then
    kill "$grant_pid" 2>/dev/null || true
    wait "$grant_pid" 2>/dev/null || true
    echo "logcat tail after Maestro failure (ci-suite):" >&2
    adb logcat -d 2>/dev/null | grep -iE 'app\\.capgo\\.health|Capacitor|chromium|Console' | tail -n 200 >&2 || \
      adb logcat -d 2>/dev/null | tail -n 200 >&2 || true
    return 1
  fi

  if ! wait "$grant_pid"; then
    echo "Health Connect grant watcher failed after Maestro success" >&2
    return 1
  fi

  echo "Maestro Android CI suite finished in $(( $(date +%s) - started ))s"
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