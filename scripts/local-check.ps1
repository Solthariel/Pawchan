$ErrorActionPreference = "Stop"
$localCheckStopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$javaExecutable =
    if ($env:JAVA_HOME) {
        Join-Path $env:JAVA_HOME "bin\java.exe"
    } else {
        "java"
    }
$javaVersionLine = (& $javaExecutable -version 2>&1 | Select-Object -First 1).ToString()
$gradleJvmProperty = @()
if ($javaVersionLine -match '"(?<major>\d+)') {
    $javaMajor = [int] $Matches.major
    if ($javaMajor -ge 24) {
        $gradleJvmProperty +=
            "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --sun-misc-unsafe-memory-access=allow"
    }
}

function Write-FailureDiagnostics {
    $reports =
        Get-ChildItem `
            -Path "." `
            -Recurse `
            -File `
            -Filter "lint-results-*.txt" `
            -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match "[\\/]build[\\/]" } |
        Sort-Object FullName -Unique

    foreach ($report in $reports) {
        Write-Host ""
        Write-Host "==== FULL REPORT: $($report.FullName) ===="
        Get-Content -LiteralPath $report.FullName -Raw
    }
}

function Clear-LintReports {
    Get-ChildItem `
        -Path "." `
        -Recurse `
        -File `
        -Filter "lint-results-*.txt" `
        -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match "[\\/]build[\\/]" } |
    Remove-Item -Force
}

function Write-LocalCheckResult {
    param(
        [Parameter(Mandatory = $true)]
        [bool] $Succeeded,
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $localCheckStopwatch.Stop()
    $elapsed = $localCheckStopwatch.Elapsed
    $hours = [Math]::Floor($elapsed.TotalHours)
    $elapsedText =
        "TOTAL TIME: {0:00}:{1:00}:{2:00}" -f
            $hours,
            $elapsed.Minutes,
            $elapsed.Seconds
    $color = if ($Succeeded) { "Green" } else { "Red" }
    Write-Host ""
    Write-Host $Message -ForegroundColor $color
    Write-Host $elapsedText -ForegroundColor $color
}

function Invoke-GradleStep {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Title,
        [Parameter(Mandatory = $true)]
        [string[]] $Tasks
    )

    Write-Host ""
    Write-Host "==== $Title ===="
    & ".\gradlew.bat" @gradleJvmProperty @Tasks "--stacktrace" "--warning-mode=fail" "--console=plain"
    if ($LASTEXITCODE -ne 0) {
        Write-FailureDiagnostics
        Write-LocalCheckResult $false "LOCAL CHECK: FAIL at step '$Title'"
        exit 1
    }
}

& ".\gradlew.bat" @gradleJvmProperty "--stop"
if ($LASTEXITCODE -ne 0) {
    Write-LocalCheckResult $false "LOCAL CHECK: FAIL at daemon reset"
    exit 1
}

Invoke-GradleStep "assembleDebug" @(":app:assembleDebug")
Invoke-GradleStep "assembleRelease" @(":app:assembleRelease")
Clear-LintReports
Invoke-GradleStep "quality and tests" @("qualityCheck")

Write-LocalCheckResult $true "LOCAL CHECK: PASS"
