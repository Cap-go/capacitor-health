#!/usr/bin/env bash
set -euo pipefail

APP_ID="${CAPGO_HEALTH_APP_ID:-app.capgo.health}"

grant() {
  adb shell pm grant "$APP_ID" "$1" 2>/dev/null || true
}

for permission in \
  android.permission.health.READ_LEAN_BODY_MASS \
  android.permission.health.WRITE_LEAN_BODY_MASS \
  android.permission.health.READ_BONE_MASS \
  android.permission.health.WRITE_BONE_MASS \
  android.permission.health.READ_BODY_WATER_MASS \
  android.permission.health.WRITE_BODY_WATER_MASS; do
  grant "$permission"
done