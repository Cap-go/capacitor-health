#!/usr/bin/env bash
set -euo pipefail

APP_ID="${CAPGO_HEALTH_APP_ID:-app.capgo.health}"

revoke() {
  adb shell pm revoke "$APP_ID" "$1" 2>/dev/null || true
}

for permission in \
  android.permission.health.READ_VO2_MAX \
  android.permission.health.WRITE_VO2_MAX; do
  revoke "$permission"
done