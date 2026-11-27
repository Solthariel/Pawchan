# Films the screens for the GitHub page on a live board.
# Films a release-speed copy (R8, not debuggable) installed as com.solthariel.pawchan.showcase and wipes ONLY that
# copy; the installed app and its data stay untouched. The copy is removed at the end.
# Usage (phone connected, adb devices shows "device"):
#   .\scripts\readme-shots.ps1 [-Board 2d] [-Thread 3976996] [-Attach "https://.../a.mp4,https://.../b.jpg"]
# Without -Thread the script picks a lively thread with a video and no crude words in its first posts.
# -Attach lists the files for the composer (up to 4, comma-separated); without it light pictures of the thread go in.
# For the composer the files go to the clipboard for the run; the clipboard is cleared at the end.
param(
    [string]$Board = "2d",
    [string]$Thread = "",
    [string]$Attach = "",
    [string]$Out = "readme-shots"
)
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
$package = "com.solthariel.pawchan.showcase"
$driver = "com.solthariel.pawchan.debug"
$deviceDir = "/sdcard/Download/pawchan-readme"
# Files the test puts there for the composer; removed after the run.
# Shared storage ignores letter case, so this must not be the shots folder under another spelling.
$picturesDir = "/sdcard/Download/Pawchan-attach"
if ($picturesDir -eq $deviceDir) {
    throw "README SHOTS: the files folder is the shots folder"
}
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
    # The filming test is always compiled anew: a reused build of an older flow would film the wrong screens.
    & ".\gradlew.bat" "-Ppawchan.showcase=true" ":app:installShowcase" ":app:installDebug" ":app:compileDebugAndroidTestKotlin" "--rerun" ":app:installDebugAndroidTest" "--console=plain"
    if ($LASTEXITCODE -ne 0) {
        throw "README SHOTS: build failed"
    }
    & $adb shell pm clear $package | Out-Null
    & $adb shell rm -rf $picturesDir | Out-Null
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
    $chosen = @()
    if ($Thread) {
        $chosen += @("-e", "thread", $Thread)
    }
    if ($Attach) {
        $chosen += @("-e", "attach", ($Attach -replace "\s", ""))
    }
    try {
        & $adb shell am instrument -w -e class com.solthariel.pawchan.readme.ReadmeShotsTest -e readmeShots 1 -e board $Board @chosen -e target $package "$driver.test/androidx.test.runner.AndroidJUnitRunner"
    } finally {
        & $adb shell rm -rf $picturesDir | Out-Null
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
