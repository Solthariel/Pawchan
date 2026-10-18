$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $javaExecutable = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
    $javaVersionLine = (& $javaExecutable -version 2>&1 | Select-Object -First 1).ToString()
    $gradleJvmProperty = @()
    if ($javaVersionLine -match '"(?<major>\d+)' -and [int]$Matches.major -ge 24) {
        $gradleJvmProperty +=
            "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --sun-misc-unsafe-memory-access=allow"
    }
    $log = Join-Path $projectRoot "ktlint-format.log"
    Write-Host "This explicitly rewrites Kotlin sources. Keep a backup of your local edits."
    Write-Host "==== ktlintFormat ===="
    & ".\gradlew.bat" @gradleJvmProperty ":ktlintFormat" "--stacktrace" "--warning-mode=fail" "--console=plain" 2>&1 |
        Tee-Object -FilePath $log
    $formatExitCode = $LASTEXITCODE
    if ($formatExitCode -ne 0) {
        Write-Host "Formatting did not fully succeed. Sources may have been partially formatted." -ForegroundColor Red
        Write-Host "Full log: $log"
        exit $formatExitCode
    }
    Write-Host "==== ktlintCheck ===="
    & ".\gradlew.bat" @gradleJvmProperty ":ktlintCheck" "--stacktrace" "--warning-mode=fail" "--console=plain" 2>&1 |
        Tee-Object -FilePath $log -Append
    $checkExitCode = $LASTEXITCODE
    if ($checkExitCode -ne 0) {
        Write-Host "KtLint still reports diagnostics. Full log: $log" -ForegroundColor Red
        exit $checkExitCode
    }
    Write-Host "KTLINT FORMAT AND CHECK: PASS. This is not LOCAL CHECK: PASS." -ForegroundColor Green
    Write-Host "Next: .\scripts\local-check.ps1"
    exit 0
} finally {
    Pop-Location
}
