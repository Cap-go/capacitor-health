#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXAMPLE_DIR="$ROOT_DIR/example-app"
FLOW_PATH="${CAPGO_MAESTRO_IOS_FLOW:-$ROOT_DIR/.maestro/ios/body-composition-unsupported.yaml}"
SKIP_BUILD="${CAPGO_MAESTRO_SKIP_BUILD:-0}"
DERIVED_DATA_PATH="${CAPGO_MAESTRO_IOS_DERIVED_DATA_PATH:-$ROOT_DIR/maestro-ios-derived-data}"
APP_PATH="${CAPGO_MAESTRO_IOS_APP_PATH:-$DERIVED_DATA_PATH/Build/Products/Debug-iphonesimulator/App.app}"

export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-600000}"

SIMULATOR_ID="${CAPGO_MAESTRO_IOS_SIMULATOR_ID:-$("$ROOT_DIR/scripts/select-ios-simulator.sh")}"

cd "$ROOT_DIR"

if [[ "$SKIP_BUILD" != "1" ]]; then
  bun run build
  cd "$EXAMPLE_DIR"
  bun install --frozen-lockfile
  bun run build
  bunx cap sync ios
  xcodebuild \
    -project ios/App/App.xcodeproj \
    -scheme App \
    -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
    -derivedDataPath "$DERIVED_DATA_PATH" \
    -configuration Debug \
    CODE_SIGNING_ALLOWED=NO \
    build
fi

if [[ ! -d "$APP_PATH" ]]; then
  echo "Missing simulator app at $APP_PATH" >&2
  exit 1
fi

if [[ "${CAPGO_MAESTRO_SKIP_SIM_BOOT:-0}" == "1" ]] && xcrun simctl list devices booted | grep -Fq "$SIMULATOR_ID"; then
  echo "iOS simulator $SIMULATOR_ID already booted (skipping boot wait)"
elif xcrun simctl list devices booted | grep -Fq "$SIMULATOR_ID"; then
  echo "iOS simulator $SIMULATOR_ID already booted"
else
  xcrun simctl boot "$SIMULATOR_ID" 2>/dev/null || true
  if [[ "${CAPGO_MAESTRO_SKIP_SIM_BOOT:-0}" == "1" ]]; then
    sleep 5
  else
    xcrun simctl bootstatus "$SIMULATOR_ID" -b
  fi
fi
if [[ "${CAPGO_MAESTRO_SKIP_INSTALL:-0}" != "1" ]]; then
  if [[ "${CAPGO_MAESTRO_INSTALL_RETRY:-0}" == "1" ]]; then
    xcrun simctl uninstall "$SIMULATOR_ID" app.capgo.health 2>/dev/null || true
  fi
  echo "Installing $APP_PATH on $SIMULATOR_ID"
  xcrun simctl install "$SIMULATOR_ID" "$APP_PATH"
fi

echo "Starting Maestro on $SIMULATOR_ID"
maestro test --device "$SIMULATOR_ID" --config "$ROOT_DIR/.maestro/config.yaml" "$FLOW_PATH"