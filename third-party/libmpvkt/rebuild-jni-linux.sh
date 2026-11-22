#!/usr/bin/env bash
# Rebuild the Pawchan-adapted libmpvkt JNI binding. NDK r29, Linux x86_64.
set -eu
root=$(CDPATH='' cd -- "$(dirname -- "$0")/../.." && pwd)
ndk="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Android NDK 29.0.14206865}"
cxx="$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android30-clang++"
src="$root/third-party/libmpvkt/jni-source"
libs="$root/video-ffmpeg/src/main/jniLibs/arm64-v8a"
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
"$cxx" -shared -std=c++20 -O2 -DNDEBUG -Wall -Wextra -Werror -fvisibility=hidden -fvisibility-inlines-hidden \
  -Wl,-soname,libmpvkt_jni.so -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 \
  -I"$src/include" \
  "$src/mpvkt.cpp" "$src/node_codec.cpp" "$src/stream_cb.cpp" "$src/utf8.cpp" \
  -L"$libs" -l:libmpv_avcodec.so -l:libmpv.so -o "$tmp/libmpvkt_jni.so"
"$ndk/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$tmp/libmpvkt_jni.so"
install -m 644 "$tmp/libmpvkt_jni.so" "$libs/libmpvkt_jni.so"
printf 'Rebuilt %s\n' "$libs/libmpvkt_jni.so"
