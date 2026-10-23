$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & python -B -m unittest discover -s "$PSScriptRoot\tests" -v
    if ($LASTEXITCODE -ne 0) { throw 'Report verifier regressions failed.' }
    & '.\gradlew.bat' ':app:verifyDebugPackageIsolation' ':app:assembleDebugAndroidTest' ':feature:applock:assembleDebugAndroidTest' '--warning-mode=fail' '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw 'Debug package verification/build failed; device tests were not started.' }
    $classes = @(
        'com.solthariel.pawchan.DebugPackageIsolationRuntimeTest',
        'com.solthariel.pawchan.PinEditorInputRuntimeTest'
    )
    foreach ($class in $classes) {
        $started = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
        Write-Host "==== Device class: $class ===="
        $task = if ($class -eq 'com.solthariel.pawchan.DebugPackageIsolationRuntimeTest') {
            ':app:connectedDebugAndroidTest'
        } else {
            ':feature:applock:connectedDebugAndroidTest'
        }
        & '.\gradlew.bat' $task "-Pandroid.testInstrumentationRunnerArguments.class=$class" '--rerun' '--warning-mode=fail' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Connected tests failed: $class" }
        & python -B "$PSScriptRoot\verify_pin_test_report.py" --root $projectRoot --since $started --class $class
        if ($LASTEXITCODE -ne 0) { throw "Fresh complete results missing or failed: $class. Do not treat BUILD SUCCESSFUL as a test pass." }
    }
    Write-Host 'PIN AND DEBUG ISOLATION DEVICE CHECK: PASS (7 tests)'
} finally {
    Pop-Location
}
