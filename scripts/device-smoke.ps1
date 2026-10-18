$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & ".\gradlew.bat" ":app:connectedDebugAndroidTest" ":data:database:connectedDebugAndroidTest" ":core:ui:connectedDebugAndroidTest" ":core:media:connectedDebugAndroidTest" ":feature:composer:connectedDebugAndroidTest" "--stacktrace" "--warning-mode=fail"
    if ($LASTEXITCODE -ne 0) {
        throw "DEVICE SMOKE: FAIL"
    }
    Write-Host ""
    Write-Host "DEVICE SMOKE: PASS"
} finally {
    Pop-Location
}