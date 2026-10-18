#!/usr/bin/env sh
set -eu
project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_root"
java_executable="${JAVA_HOME:+$JAVA_HOME/bin/}java"
java_major="$("$java_executable" -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p')"
gradle_jvm_property=
if [ -n "$java_major" ] && [ "$java_major" -ge 24 ]; then
    gradle_jvm_property="-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --sun-misc-unsafe-memory-access=allow"
fi
run_gradle() {
    if [ -n "$gradle_jvm_property" ]; then
        ./gradlew "$gradle_jvm_property" "$@"
    else
        ./gradlew "$@"
    fi
}
printf '%s\n' 'This explicitly rewrites Kotlin sources. Keep a backup of local edits.'
if ! run_gradle :ktlintFormat --stacktrace --warning-mode=fail --console=plain; then
    printf '%s\n' 'Formatting failed; some sources may have been rewritten. No PASS is claimed.'
    exit 1
fi
run_gradle :ktlintCheck --stacktrace --warning-mode=fail --console=plain
printf '%s\n' 'KTLINT FORMAT AND CHECK: PASS. This is not LOCAL CHECK: PASS.'
