#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export CAPGO_MAESTRO_IOS_FLOW="${CAPGO_MAESTRO_IOS_FLOW:-$ROOT_DIR/.maestro/ios/ci-suite.yaml}"
export PATH="$HOME/.maestro/bin:${PATH:-}"
export MAESTRO_DRIVER_STARTUP_TIMEOUT="${MAESTRO_DRIVER_STARTUP_TIMEOUT:-120000}"

exec "$ROOT_DIR/scripts/run-maestro-ios-body-composition-unsupported.sh"