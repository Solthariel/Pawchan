#!/usr/bin/env sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_root"
./gradlew :app:connectedDebugAndroidTest :data:database:connectedDebugAndroidTest :core:ui:connectedDebugAndroidTest :core:media:connectedDebugAndroidTest :feature:composer:connectedDebugAndroidTest :feature:thread:connectedDebugAndroidTest :feature:applock:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.timeout_msec=180000 --stacktrace --warning-mode=fail
printf '\nDEVICE SMOKE: PASS\n'