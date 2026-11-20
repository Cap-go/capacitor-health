#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"
APK_PATH="$EXAMPLE_DIR/android/app/build/outputs/apk/debug/app-debug.apk"
FLOW_PATH="${CAPGO_MAESTRO_ANDROID_FLOW:-$ROOT_DIR/.maestro/android/body-composition.yaml}"
SKIP_BUILD="${CAPGO_MAESTRO_SKIP_BUILD:-0}"

export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-300000}"

cd "$ROOT_DIR"

if [[ "$SKIP_BUILD" != "1" ]]; then
  bun run build
  cd "$EXAMPLE_DIR"
  bun install --frozen-lockfile
  bun run build
  bunx cap sync android
  cd android
  ./gradlew assembleDebug
fi

if [[ ! -f "$APK_PATH" ]]; then
  echo "Missing APK at $APK_PATH" >&2
  exit 1
fi

adb wait-for-device
until [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  sleep 1
done
adb uninstall app.capgo.health 2>/dev/null || true
adb install "$APK_PATH"

if [[ "${CAPGO_MAESTRO_INSTALL_ONLY:-0}" == "1" ]]; then
  exit 0
fi

maestro test --config "$ROOT_DIR/.maestro/config.yaml" "$FLOW_PATH"