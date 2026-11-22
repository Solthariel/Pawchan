#!/usr/bin/env bash
# Linux x86-64 with the pinned NDK. Verify original bundle before replacing only the JNI bridge.
set -euo pipefail
cd "$(dirname "$0")/.."
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-${ANDROID_HOME:?Android SDK required}/ndk/29.0.14206865}"
libs=video-ffmpeg/src/main/jniLibs/arm64-v8a
manifest=video-ffmpeg/mpv-libraries.sha256
(cd "$libs" && sha256sum -c ../../../../mpv-libraries.sha256)
bash third-party/libmpvkt/rebuild-jni-linux.sh
python3 - "$libs/libmpvkt_jni.so" "$manifest" <<'PYTHON'
import hashlib, pathlib, sys
lib, manifest = map(pathlib.Path, sys.argv[1:])
rows = manifest.read_text().splitlines()
assert sum(row.endswith('  ' + lib.name) for row in rows) == 1
manifest.write_text(''.join((hashlib.sha256(lib.read_bytes()).hexdigest()+'  '+lib.name if row.endswith('  '+lib.name) else row)+'\n' for row in rows))
PYTHON
(cd "$libs" && sha256sum -c ../../../../mpv-libraries.sha256)
