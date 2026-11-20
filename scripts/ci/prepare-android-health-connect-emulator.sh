#!/usr/bin/env bash
set -euo pipefail

APP_ID="${CAPGO_HEALTH_APP_ID:-app.capgo.health}"

adb wait-for-device
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  sleep 1
done

revoke() {
  adb shell pm revoke "$APP_ID" "$1" 2>/dev/null || true
}

for permission in \
  android.permission.health.READ_LEAN_BODY_MASS \
  android.permission.health.WRITE_LEAN_BODY_MASS \
  android.permission.health.READ_BONE_MASS \
  android.permission.health.WRITE_BONE_MASS \
  android.permission.health.READ_BODY_WATER_MASS \
  android.permission.health.WRITE_BODY_WATER_MASS; do
  revoke "$permission"
done

if adb shell pm path com.google.android.apps.healthdata >/dev/null 2>&1; then
  adb shell am start -n com.google.android.apps.healthdata/.ui.deeplink.HomeDeepLinkActivity 2>/dev/null || \
    adb shell am start -a android.health.connect.action.HEALTH_CONNECT_SETTINGS 2>/dev/null || \
    adb shell monkey -p com.google.android.apps.healthdata -c android.intent.category.LAUNCHER 1 2>/dev/null || true
  sleep 2
  adb shell input keyevent KEYCODE_HOME 2>/dev/null || true
fi

# Warm Health Connect client binding so saveSample does not block getOrCreate on a cold emulator.
adb shell am start -W -n "${APP_ID}/.MainActivity" >/dev/null 2>&1 || \
  adb shell monkey -p "$APP_ID" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
sleep 3
adb shell input keyevent KEYCODE_HOME 2>/dev/null || true
adb shell am force-stop "$APP_ID" 2>/dev/null || true

for permission in \
  android.permission.health.READ_LEAN_BODY_MASS \
  android.permission.health.WRITE_LEAN_BODY_MASS \
  android.permission.health.READ_BONE_MASS \
  android.permission.health.WRITE_BONE_MASS \
  android.permission.health.READ_BODY_WATER_MASS \
  android.permission.health.WRITE_BODY_WATER_MASS; do
  revoke "$permission"
done