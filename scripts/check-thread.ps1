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
    Write-Host "Focused check: KtLint, all thread unit tests, thread Detekt. Not a full LOCAL CHECK."
    & ".\gradlew.bat" @gradleJvmProperty `
        ":ktlintCheck" `
        ":feature:thread:testDebugUnitTest" `
        ":feature:thread:detekt" `
        "--stacktrace" "--warning-mode=fail" "--console=plain"
    $checkExitCode = $LASTEXITCODE
    if ($checkExitCode -ne 0) {
        Write-Host "THREAD CHECK: FAIL" -ForegroundColor Red
        exit $checkExitCode
    }
    Write-Host "THREAD CHECK: PASS. This is not LOCAL CHECK: PASS." -ForegroundColor Green
    Write-Host "Full verification still requires: .\scripts\local-check.ps1"
    exit 0
} finally {
    Pop-Location
}
