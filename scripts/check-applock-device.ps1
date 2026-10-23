$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & python -B -m unittest discover -s "$PSScriptRoot\tests" -v
    if ($LASTEXITCODE -ne 0) { throw 'Device-report verifier regressions failed.' }
    & '.\gradlew.bat' ':app:verifyDebugPackageIsolation' ':app:assembleDebugAndroidTest' ':feature:applock:assembleDebugAndroidTest' '--warning-mode=fail' '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw 'Device APK compilation/isolation failed. Tests were not started.' }
    $runs = @(
        @{ Module = 'app'; Class = 'com.solthariel.pawchan.DebugPackageIsolationRuntimeTest' },
        @{ Module = 'feature:applock'; Class = 'com.solthariel.pawchan.PinEditorInputRuntimeTest' },
        @{ Module = 'feature:applock'; Class = 'com.solthariel.pawchan.PinUnlockLayoutRuntimeTest' },
        @{ Module = 'feature:applock'; Class = 'com.solthariel.pawchan.AppLockStoreRuntimeTest' },
        @{ Module = 'app'; Class = 'com.solthariel.pawchan.SecuritySettingsLayoutRuntimeTest' }
    )
    foreach ($run in $runs) {
        $started = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        $task = ":$($run.Module):connectedDebugAndroidTest"
        Write-Host "==== $($run.Class) ===="
        & '.\gradlew.bat' $task "-Pandroid.testInstrumentationRunnerArguments.class=$($run.Class)" '--rerun' '--warning-mode=fail' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Device test failed: $($run.Class)" }
        & python -B "$PSScriptRoot\verify_pin_test_report.py" --root $projectRoot --since $started --class $run.Class
        if ($LASTEXITCODE -ne 0) { throw "Fresh complete results missing: $($run.Class)" }
    }
    Write-Host 'APP LOCK DEVICE CHECK: PASS (18 tests, no failures/errors/skips)'
    Write-Host 'Real fingerprint enrollment and picker/Home/screen-off behavior still require manual device checks.'
} finally {
    Pop-Location
}
