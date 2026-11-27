# Films the screens for the GitHub page on a live board.
# Films a release-speed copy (R8, not debuggable) installed as com.solthariel.pawchan.showcase and wipes ONLY that
# copy; the installed app and its data stay untouched. The copy is removed at the end.
# Usage (phone connected, adb devices shows "device"):  .\scripts\readme-shots.ps1 [-Board 2d]
param(
    [string]$Board = "2d",
    [string]$Out = "readme-shots"
)
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
$package = "com.solthariel.pawchan.showcase"
$driver = "com.solthariel.pawchan.debug"
$deviceDir = "/sdcard/Download/pawchan-readme"
# adb is often not on PATH on Windows: take it from the SDK that Gradle uses.
function Find-Adb {
    $dirs = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA "Android\Sdk"))
    $sdkLine = Get-Content "local.properties" -ErrorAction SilentlyContinue | Where-Object { $_ -like "sdk.dir=*" } | Select-Object -First 1
    if ($sdkLine) {
        $dirs = @(($sdkLine -replace "^sdk.dir=", "" -replace "\\:", ":" -replace "\\\\", "\")) + $dirs
    }
    $found = $dirs | Where-Object { $_ } | ForEach-Object { Join-Path $_ "platform-tools\adb.exe" } | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $found) {
        $command = Get-Command adb -ErrorAction SilentlyContinue
        if ($command) {
            $found = $command.Source
        }
    }
    if (-not $found) {
        throw "README SHOTS: adb not found, set ANDROID_HOME or sdk.dir in local.properties"
    }
    return $found
}
$adb = Find-Adb
function Demo([string[]]$Arguments) {
    & $adb shell am broadcast -a com.android.systemui.demo @Arguments | Out-Null
}
try {
    if (-not ((& $adb devices) -match "`tdevice$")) {
        throw "README SHOTS: no phone, check adb devices"
    }
    & ".\gradlew.bat" "-Ppawchan.showcase=true" ":app:installShowcase" ":app:installDebug" ":app:installDebugAndroidTest" "--console=plain"
    if ($LASTEXITCODE -ne 0) {
        throw "README SHOTS: build failed"
    }
    & $adb shell pm clear $package | Out-Null
    & $adb shell pm grant $package android.permission.POST_NOTIFICATIONS 2>$null | Out-Null
    & $adb shell settings put global sysui_demo_allowed 1
    Demo @("-e", "command", "enter")
    Demo @("-e", "command", "clock", "-e", "hhmm", "1200")
    Demo @("-e", "command", "battery", "-e", "level", "100", "-e", "plugged", "false")
    Demo @("-e", "command", "network", "-e", "wifi", "show", "-e", "level", "4")
    Demo @("-e", "command", "notifications", "-e", "visible", "false")
    # No pop-up notifications in the frames: Do Not Disturb for the run, restored afterwards.
    $zenBefore = "$(& $adb shell settings get global zen_mode)".Trim()
    & $adb shell cmd notification set_dnd on | Out-Null
    try {
        & $adb shell am instrument -w -e class com.solthariel.pawchan.readme.ReadmeShotsTest -e readmeShots 1 -e board $Board -e target $package "$driver.test/androidx.test.runner.AndroidJUnitRunner"
    } finally {
        Demo @("-e", "command", "exit")
        if ($zenBefore -eq "0") {
            & $adb shell cmd notification set_dnd off | Out-Null
        }
    }
    if (Test-Path $Out) {
        Remove-Item -Recurse -Force $Out
    }
    & $adb pull $deviceDir $Out | Out-Null
    & $adb pull "/sdcard/Android/data/$driver/files/readme-log.txt" "$Out/readme-log.txt" | Out-Null
    $saved = Get-Content "$Out/readme-log.txt" -ErrorAction SilentlyContinue | Where-Object { $_ -like "threadshot file: *" } | Select-Object -First 1
    if ($saved) {
        & $adb pull "/sdcard/Download/$($saved.Substring(17).Trim())" "$Out/threadshot.png" | Out-Null
    }
    & $adb shell rm -rf $deviceDir | Out-Null
    & $adb uninstall $package | Out-Null
    Compress-Archive -Path "$Out/*" -DestinationPath "$Out.zip" -Force
    Write-Host ""
    Get-Content "$Out/readme-log.txt"
    Write-Host ""
    Write-Host "README SHOTS: $Out.zip"
} finally {
    Pop-Location
}
