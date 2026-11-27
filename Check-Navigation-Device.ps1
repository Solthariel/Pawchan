$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$gradle = Join-Path $root 'gradlew.bat'
if (!(Test-Path $gradle)) { throw 'Run from the extracted project.' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$reportRoot = Join-Path ([System.IO.Path]::GetTempPath()) 'Pawchan-checks'
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null
$out = Join-Path $reportRoot "navigation-production-$stamp"
if (Test-Path $out) { throw "Output already exists: $out" }
New-Item -ItemType Directory -Path $out | Out-Null
$tests = @(
    @{ Name = 'focused-navigation'; Module = 'app'; Class = 'com.solthariel.pawchan.AppNavigationMotionRuntimeTest,com.solthariel.pawchan.AppNavigationPixelsRuntimeTest,com.solthariel.pawchan.InstantNavigationProductionRuntimeTest,com.solthariel.pawchan.navigation.TabReselectionRuntimeTest,com.solthariel.pawchan.navigation.ThreadBackStackRuntimeTest,com.solthariel.pawchan.navigation.IncomingLinkNavigationRuntimeTest' },
    @{ Name = 'full-app'; Module = 'app'; Class = '' },
    @{ Name = 'full-composer'; Module = 'feature\composer'; Class = '' }
)
$backedUp = @{}
foreach ($test in $tests) {
    $module = Join-Path $root $test.Module
    $report = Join-Path $module 'build\reports\androidTests\connected\debug'
    $results = Join-Path $module 'build\outputs\androidTest-results\connected\debug'
    if (!$backedUp.ContainsKey($test.Module)) {
        $before = Join-Path $out ('previous-' + ($test.Module -replace '\\', '-'))
        New-Item -ItemType Directory -Path $before | Out-Null
        if (Test-Path $report) { Copy-Item $report (Join-Path $before 'html') -Recurse }
        if (Test-Path $results) { Copy-Item $results (Join-Path $before 'xml') -Recurse }
        $backedUp[$test.Module] = $true
    }
    $dir = Join-Path $out $test.Name
    New-Item -ItemType Directory -Path $dir | Out-Null
    if (Test-Path $report) { Remove-Item $report -Recurse -Force }
    if (Test-Path $results) { Remove-Item $results -Recurse -Force }
    $task = ':' + ($test.Module -replace '\\', ':') + ':connectedDebugAndroidTest'
    $arguments = @($task, '--console=plain')
    if ($test.Class) { $arguments += "-Pandroid.testInstrumentationRunnerArguments.class=$($test.Class)" }
    Write-Host "Running $($test.Name)..."
    $process = Start-Process -FilePath $gradle -ArgumentList $arguments -WorkingDirectory $root -NoNewWindow -Wait -PassThru -RedirectStandardOutput (Join-Path $dir 'stdout.txt') -RedirectStandardError (Join-Path $dir 'stderr.txt')
    [IO.File]::WriteAllText((Join-Path $dir 'exit-code.txt'), [string]$process.ExitCode)
    if (Test-Path $report) { Copy-Item $report (Join-Path $dir 'html') -Recurse }
    if (Test-Path $results) { Copy-Item $results (Join-Path $dir 'xml') -Recurse }
    if (!(Test-Path (Join-Path $dir 'html\index.html'))) {
        [IO.File]::WriteAllText((Join-Path $dir 'NO-FRESH-REPORT.txt'), 'No fresh Android test report: do not count as PASS.')
    }
    Write-Host "  Exit code: $($process.ExitCode)"
}
Compress-Archive -Path (Join-Path $out '*') -DestinationPath "$out.zip" -Force
Write-Host "Send this report: $out.zip"
