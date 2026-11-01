$ErrorActionPreference = "Stop"
Set-Location -LiteralPath $PSScriptRoot
if (-not (Test-Path -LiteralPath ".\gradlew.bat")) { throw "The full project is required." }
if (-not (Test-Path -LiteralPath ".\local.properties")) {
    $sdk = Join-Path $HOME "Android\sdk"
    if (Test-Path -LiteralPath $sdk) {
        $sdkPath = $sdk.Replace("\", "/")
        Set-Content -LiteralPath ".\local.properties" -Value "sdk.dir=$sdkPath" -Encoding ASCII
    }
}
$python = Get-Command python -ErrorAction Stop
& $python.Source -X utf8 -c "import sys; assert sys.version_info.major == 3"
if ($LASTEXITCODE -ne 0) { throw "Python 3 is required." }
$log = Join-Path $PSScriptRoot "interface-v11-full-check.log"
Start-Transcript -LiteralPath $log
try {
    Write-Host "Full checks only. No release publication or push."
    Write-Host "Connect an unlocked Android device with authorized USB debugging."
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\local-check.ps1"
    if ($LASTEXITCODE -ne 0) { throw "LOCAL CHECK failed." }
    foreach ($suite in @("scripts/tests", "update/publishing/tests", "update/publishing/tools/tests")) {
        & $python.Source -X utf8 -B -m unittest discover -s $suite -v
        if ($LASTEXITCODE -ne 0) { throw "Python tests failed: $suite" }
    }
    Write-Host "Device tests install the isolated debug and test APKs."
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\scripts\device-smoke.ps1"
    if ($LASTEXITCODE -ne 0) { throw "DEVICE SMOKE failed." }
    Write-Host "Inspect fresh XML/HTML reports in all 8 AndroidTest modules."
    Write-Host "Missing, failed or skipped reports do not count as a full PASS."
    Write-Host "Visual verification and release performance measurement remain separate."
} finally {
    Stop-Transcript
    Write-Host "Transcript: $log"
}
