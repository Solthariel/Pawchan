# Run from the project root. Runs the checked-in synthetic fixtures on a phone.
$ErrorActionPreference = 'Stop'
$adb = (Get-Command adb -ErrorAction SilentlyContinue).Source
if (-not $adb) { $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }
if (-not (Test-Path -LiteralPath $adb -PathType Leaf)) { throw 'adb.exe not found. Install Android SDK Platform-Tools.' }
& $adb get-state
if ($LASTEXITCODE -ne 0) { throw 'Connect and unlock one phone, and allow USB debugging.' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$logPath = Join-Path $PWD "paw-mpv-fixtures-$stamp.log"
$errPath = Join-Path $PWD "paw-mpv-fixtures-adb-errors-$stamp.log"
$log = Start-Process -FilePath $adb -ArgumentList @('logcat','-v','time','-T','1','-s','PawMpvFixtures:I','PawMpv:I','AndroidRuntime:E','DEBUG:*','*:S') -RedirectStandardOutput $logPath -RedirectStandardError $errPath -NoNewWindow -PassThru
$code = -1
try {
    Start-Sleep -Seconds 1
    if ($log.HasExited) { throw "adb logcat failed; see $errPath" }
    Write-Host 'Testing synthetic videos in SW and AUTO-HW. Keep the phone unlocked and screen on.'
    $ErrorActionPreference = 'Continue'
    & .\gradlew.bat :core:media:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.solthariel.pawchan.MpvSyntheticFixturesRuntimeTest' '-Pandroid.testInstrumentationRunnerArguments.timeout_msec=3600000'
    $code = $LASTEXITCODE
} finally {
    $ErrorActionPreference = 'Stop'
    Start-Sleep -Seconds 2
    if (-not $log.HasExited) { Stop-Process -Id $log.Id -ErrorAction SilentlyContinue }
    $log.WaitForExit()
    Write-Host "Video log: $logPath"
    Write-Host "adb errors: $errPath"
}
if ($code -ne 0) { throw 'Test failed. Send the video log and the console error.' }
Write-Host 'Test command succeeded. Send the video log for inspection.'
