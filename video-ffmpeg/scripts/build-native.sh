#!/usr/bin/env bash
# Compatibility entry point; dependency preparation and build orchestration live in Kotlin.
set -euo pipefail
[[ $# -le 1 ]] || { echo 'Usage: build-native.sh ["arm64-v8a x86_64"]. See docs/DEVELOPMENT.md for advanced options.' >&2; exit 2; }
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
exec bash "$root/gradlew" -p "$root/tools/native-build" rebuildNative \
    "-Pnative.abis=${1:-${NATIVE_ABIS:-arm64-v8a}}" --warning-mode=fail --console=plain
