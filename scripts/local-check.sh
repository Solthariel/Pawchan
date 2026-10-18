#!/usr/bin/env sh
set -eu

local_check_started_at=$(date +%s)
color_green='\033[32m'
color_red='\033[31m'
color_reset='\033[0m'

java_executable="${JAVA_HOME:+$JAVA_HOME/bin/}java"
java_major="$("$java_executable" -version 2>&1 | sed -n '1s/.*version \"\\([0-9][0-9]*\\).*/\\1/p')"
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

print_failure_diagnostics() {
    find . -type f -path '*/build/*' -name 'lint-results-*.txt' -print |
        sort -u |
        while IFS= read -r report; do
            printf '\n==== FULL REPORT: %s ====\n' "$report"
            cat "$report"
        done
}

clear_lint_reports() {
    find . -type f -path '*/build/*' -name 'lint-results-*.txt' -delete
}

print_local_check_result() {
    succeeded=$1
    message=$2
    elapsed=$(($(date +%s) - local_check_started_at))
    hours=$((elapsed / 3600))
    minutes=$(((elapsed % 3600) / 60))
    seconds=$((elapsed % 60))
    if [ "$succeeded" = true ]; then
        color=$color_green
    else
        color=$color_red
    fi
    printf '\n%b%s%b\n' "$color" "$message" "$color_reset"
    printf '%bTOTAL TIME: %02d:%02d:%02d%b\n' \
        "$color" "$hours" "$minutes" "$seconds" "$color_reset"
}

run_step() {
    title=$1
    shift
    printf '\n==== %s ====\n' "$title"
    if ! run_gradle "$@" --stacktrace --warning-mode=fail --console=plain; then
        print_failure_diagnostics
        print_local_check_result false "LOCAL CHECK: FAIL at step '$title'"
        exit 1
    fi
}

if ! run_gradle --stop; then
    print_local_check_result false "LOCAL CHECK: FAIL at daemon reset"
    exit 1
fi
run_step assembleDebug :app:assembleDebug
run_step assembleRelease :app:assembleRelease
clear_lint_reports
run_step "quality and tests" qualityCheck

print_local_check_result true "LOCAL CHECK: PASS"
