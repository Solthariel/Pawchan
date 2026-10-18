#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$sdk" && -f ../local.properties ]]; then
    sdk="$(sed -n 's/^sdk.dir=//p' ../local.properties)"
fi
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$sdk/ndk/29.0.14206865}"
[[ -x "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/clang" ]] || { echo 'Install Linux Android NDK 29.0.14206865 and set ANDROID_NDK_HOME. This source-rebuild script is for Linux maintainers only; normal app builds use native-prebuilt.' >&2; exit 1; }
command -v meson >/dev/null || { echo 'Install meson and ninja (python -m pip install meson ninja).' >&2; exit 1; }
command -v ninja >/dev/null
export DAV1D_VERSION=1.5.4 FFMPEG_VERSION=8.1.3
export NATIVE_ABIS="${1:-arm64-v8a}"
export EXTERNAL_CORES="${EXTERNAL_CORES:-4}"
export WEBM_ARCHIVE_CACHE_DIR="${WEBM_ARCHIVE_CACHE_DIR:-$HOME/.cache/nekochan-media}"
root="$(pwd)/build/native"
mkdir -p "$root/libraries" "$root/external"
fingerprint="$( { printf '%s\n' "$NATIVE_ABIS" "$FFMPEG_VERSION" "$DAV1D_VERSION"; sha256sum scripts/*.sh; cat "$ANDROID_NDK_HOME/source.properties"; } | sha256sum | cut -d ' ' -f 1)"
valid=1
for abi in $NATIVE_ABIS; do
    [[ -s "$root/libraries/ffmpeg/$abi/libavcodec.so" && -s "$root/libraries/dav1d/$abi/libdav1d.so" && -s "$root/external/ffmpeg/include/$abi/libavformat/avformat.h" ]] || valid=0
done
if [[ "$valid" == 1 && -f "$root/.complete" && "$(cat "$root/.complete")" == "$fingerprint" ]]; then
    echo 'Verified native build inputs unchanged; reusing local outputs.'
    exit 0
fi
bash scripts/shared-prepare.sh "$root/sources"
bash scripts/shared-build.sh "$root/sources" "$root/libraries" "$root/external"
printf '%s\n' "$fingerprint" > "$root/.complete"
