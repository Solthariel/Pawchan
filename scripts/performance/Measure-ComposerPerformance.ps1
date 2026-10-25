[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ProjectPath,
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [string]$WorkRoot,
    [ValidateSet('BaselineFirst', 'CandidateFirst')][string]$MeasurementOrder = 'BaselineFirst',
    [switch]$Diagnostics,
    [switch]$Research,
    [switch]$NonInteractive
)
$ErrorActionPreference = 'Stop'
if ($NonInteractive -and -not $Research) { throw 'NonInteractive is only allowed for the registered research protocol.' }
if ($Research) { $Diagnostics = $true }
$measurementVariants = if ($MeasurementOrder -eq 'CandidateFirst') { @('candidate', 'baseline') } else { @('baseline', 'candidate') }
$project = (Resolve-Path -LiteralPath $ProjectPath).Path
$output = [IO.Path]::GetFullPath($OutputPath)
if (Test-Path -LiteralPath $output) { throw 'Performance output must be a new directory.' }
New-Item -ItemType Directory -Path $output -Force | Out-Null
$work = $null
if (-not $WorkRoot) { $WorkRoot = Join-Path $env:USERPROFILE 'pwc' }
$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
$adb = if ($adbCommand) { $adbCommand.Source } else { $null }
if (-not $adb) {
    $localSdk = $null
    $localProperties = Join-Path $project 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkLine = Get-Content -LiteralPath $localProperties | Where-Object { $_ -match '^sdk\.dir\s*=' } | Select-Object -First 1
        if ($sdkLine) { $localSdk = ($sdkLine -replace '^sdk\.dir\s*=\s*', '').Trim().Replace('\:', ':').Replace('\\', '\') }
    }
    foreach ($sdk in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, $localSdk, (Join-Path $env:LOCALAPPDATA 'Android/Sdk'))) {
        if ($sdk -and (Test-Path -LiteralPath (Join-Path $sdk 'platform-tools/adb.exe'))) {
            $adb = Join-Path $sdk 'platform-tools/adb.exe'; break
        }
    }
}
if (-not $adb) { throw 'adb not found. Add Android SDK platform-tools to PATH.' }
$devices = @(& $adb devices)
if ($LASTEXITCODE -ne 0) { throw 'adb devices failed.' }
$serials = @($devices | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
$unready = @($devices | Where-Object { $_ -match '\s+(unauthorized|offline)$' })
if ($serials.Count -ne 1 -or $unready.Count -ne 0) { throw 'Connect exactly one authorized physical device; disconnect emulators/other phones.' }
$serial = $serials[0]
function Invoke-AdbText([string[]]$Arguments) {
    $text = @(& $adb '-s' $serial @Arguments)
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $Arguments" }
    return ($text -join "`n")
}
function Save-DeviceState([string]$Directory) {
    New-Item -ItemType Directory -Path $Directory -Force | Out-Null
    $commands = [ordered]@{
        properties = @('shell', 'getprop'); display = @('shell', 'dumpsys', 'display')
        thermal = @('shell', 'dumpsys', 'thermalservice'); battery = @('shell', 'dumpsys', 'battery')
        power = @('shell', 'dumpsys', 'power'); settings = @('shell', 'settings', 'list', 'system')
    }
    foreach ($key in $commands.Keys) {
        [IO.File]::WriteAllText((Join-Path $Directory ($key + '.txt')), (Invoke-AdbText $commands[$key]))
    }
    [DateTime]::UtcNow.ToString('o') | Set-Content -LiteralPath (Join-Path $Directory 'utc.txt') -Encoding UTF8
}
function Wait-CoolDevice {
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $thermal = Invoke-AdbText @('shell', 'dumpsys', 'thermalservice')
        if ($thermal -notmatch 'Thermal Status:\s*(\d+)') { throw 'Cannot read thermal status. No benchmark run.' }
        if ([int]$Matches[1] -le 1) { return }
        Write-Host 'Device is warm/hot; waiting 30 seconds. No thermal protections are disabled.'
        Start-Sleep -Seconds 30
    }
    throw 'Device did not cool sufficiently within ten minutes.'
}
$summary = [ordered]@{
    startedUtc = [DateTime]::UtcNow.ToString('o'); status = 'RUNNING'; deviceSerial = $serial
    performancePass = $false; published = $false; diagnostics = [bool]$Diagnostics; research = [bool]$Research; order = @($measurementVariants); measurementOrder = $MeasurementOrder
    protocol = 'Full AOT; two warmup cycles before each of five measured iterations; ten error/scroll cycles per iteration.'
    scope = 'Offline isolated composer, not full application/startup. Baseline reverts only align(Top).'
    thresholds = 'NOT_AGREED: results and traces require owner review; no automatic performance PASS.'
}
$diagnosticArguments = @()
if ($Diagnostics) {
    $diagnosticArguments = @('-Pandroid.testInstrumentationRunnerArguments.pawDiagnostics=true', '-Pandroid.testInstrumentationRunnerArguments.additionalTestOutputDir=/sdcard/Android/media/com.solthariel.pawchan.performance.test/additional_test_output')
    $summary.protocol = 'DIAGNOSTIC_TRACE_MARKERS; observer overhead; NOT comparable with original protocol. ' + $summary.protocol
    Write-Host 'DIAGNOSTIC ONLY: target trace observers enabled identically in both variants. Not performance acceptance.'
}
if ($Research) {
    $diagnosticArguments += '-Pandroid.testInstrumentationRunnerArguments.pawResearch=true'
    $summary.protocol = 'PAW_ALIGNMENT_RESEARCH_V1; fixed paired study; no release performance PASS. ' + $summary.protocol
}
# The project pins Gradle's daemon toolchain to JDK 25, as in local-check.ps1.
$gradleJvmProperty = '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 --sun-misc-unsafe-memory-access=allow'
$oldSerial = $env:ANDROID_SERIAL
try {
    if ((Invoke-AdbText @('shell', 'getprop', 'ro.kernel.qemu')).Trim() -eq '1') { throw 'Physical phone required.' }
    if ([int](Invoke-AdbText @('shell', 'getprop', 'ro.build.version.sdk')).Trim() -lt 31) { throw 'Android 12+ required for frameOverrunMs.' }
    $env:ANDROID_SERIAL = $serial
    $model = (Invoke-AdbText @('shell', 'getprop', 'ro.product.model')).Trim()
    $release = (Invoke-AdbText @('shell', 'getprop', 'ro.build.version.release')).Trim()
    $planFile = Join-Path $output 'workspace-plan.json'
    & python -B (Join-Path $PSScriptRoot 'performance_paths.py') --root ([IO.Path]::GetFullPath($WorkRoot)) --project $project --output $output --model $model --android $release --plan-file $planFile
    if ($LASTEXITCODE -ne 0) { throw 'Unsafe artifact path length. No build or measurement started.' }
    $work = (Get-Content -LiteralPath $planFile -Raw -Encoding UTF8 | ConvertFrom-Json).workspace
    $ancestor = [IO.Path]::GetFullPath($WorkRoot)
    while ($ancestor) {
        if ((Test-Path -LiteralPath $ancestor) -and (((Get-Item -LiteralPath $ancestor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0)) { throw "Reparse-point workspace refused: $ancestor" }
        $parent = Split-Path -Parent $ancestor
        if ($parent -eq $ancestor) { break }
        $ancestor = $parent
    }
    & python -B (Join-Path $PSScriptRoot 'prepare_comparison.py') --project $project --output $work
    if ($LASTEXITCODE -ne 0) { throw 'Failed to prepare controlled source pair.' }
    Copy-Item -LiteralPath (Join-Path $work 'source-pair.json') -Destination $output
    # Compile both copies before cooldown/measurement so a clean build does not heat one run differently.
    foreach ($variant in @('baseline', 'candidate')) {
        Push-Location (Join-Path $work $variant)
        try {
            & '.\gradlew.bat' ':performance:target:assembleBenchmark' ':performance:macrobenchmark:assembleBenchmark' '--stacktrace' '--warning-mode=fail' $gradleJvmProperty
            if ($LASTEXITCODE -ne 0) { throw "Benchmark build failed: $variant" }
        } finally { Pop-Location }
    }
    Write-Host ('Measurement order: ' + ($measurementVariants -join ' -> '))
    Write-Host 'Keep the phone unlocked, screen on, same refresh/brightness and no other interaction.'
    Write-Host 'No normal Pawchan app is uninstalled. The separate performance package uses synthetic offline data.'
    foreach ($variant in $measurementVariants) {
        if ($NonInteractive) {
            Write-Host "Research cooldown: 60 seconds before $variant. Keep phone unlocked and untouched."
            Start-Sleep -Seconds 60
        } else {
            Read-Host "Ready for ${variant}? Let the phone cool, keep identical display/power settings, then press Enter" | Out-Null
        }
        Wait-CoolDevice
        if ($Research -and (Invoke-AdbText @('shell', 'dumpsys', 'power')) -notmatch 'mWakefulness=Awake') { throw 'Screen not awake. Study stopped; no automatic retry/unlock.' }
        $dest = Join-Path $output $variant
        Save-DeviceState (Join-Path $dest 'device-before')
        Push-Location (Join-Path $work $variant)
        try {
            & '.\gradlew.bat' ':performance:macrobenchmark:connectedBenchmarkAndroidTest' @diagnosticArguments '--stacktrace' '--warning-mode=fail' $gradleJvmProperty
            $code = $LASTEXITCODE
        } finally {
            Pop-Location
            $build = Join-Path $work "$variant/performance/macrobenchmark/build"
            # Flatten extra artifacts instead of reproducing the deep AGP export directory.
            New-Item -ItemType Directory -Path $dest -Force | Out-Null
            $additional = Join-Path $build 'outputs/connected_android_test_additional_output'
            $raw = Join-Path $dest 'raw'
            $copyMap = @()
            if (Test-Path -LiteralPath $additional) {
                New-Item -ItemType Directory -Path $raw -Force | Out-Null
                foreach ($file in @(Get-ChildItem -LiteralPath $additional -Recurse -File)) {
                    $to = Join-Path $raw $file.Name
                    if (Test-Path -LiteralPath $to) { throw "Duplicate artifact basename refused: $($file.Name)" }
                    if ($to.Length -gt 240) { throw "Artifact destination too long: $to" }
                    Copy-Item -LiteralPath $file.FullName -Destination $to
                    $copyMap += [ordered]@{ filename = $file.Name; source = $file.FullName; sha256 = (Get-FileHash -LiteralPath $to -Algorithm SHA256).Hash }
                }
            }
            $copyMap | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $dest 'artifact-copy-map.json') -Encoding UTF8
            foreach ($relative in @('outputs/androidTest-results', 'reports')) {
                $source = Join-Path $build $relative
                if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination $dest -Recurse -Force }
            }
            $mapping = Join-Path $work "$variant/performance/target/build/outputs/mapping/benchmark"
            if (Test-Path -LiteralPath $mapping) { Copy-Item -LiteralPath $mapping -Destination (Join-Path $dest 'r8-mapping') -Recurse -Force }
            Save-DeviceState (Join-Path $dest 'device-after')
        }
        if ($code -ne 0) { throw "Macrobenchmark instrumentation failed: $variant" }
        [ordered]@{ diagnostics = [bool]$Diagnostics; research = [bool]$Research; protocol = $(if ($Diagnostics) { 'PAW_DIAGNOSTIC_V1' } else { 'ORIGINAL' }) } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $dest 'measurement-mode.json') -Encoding UTF8
        $validationArguments = @()
        if ($Diagnostics) { $validationArguments = @('--diagnostic') }
        & python -B (Join-Path $PSScriptRoot 'verify_benchmark.py') --reports $dest --output (Join-Path $dest 'validated-metrics.json') @validationArguments
        if ($LASTEXITCODE -ne 0) { throw "Missing/invalid raw metrics or traces: $variant" }
    }
    $summary.status = 'COLLECTED_REVIEW_REQUIRED'
} catch {
    $summary.status = 'FAILED_OR_INCOMPLETE'
    $summary.error = $_.Exception.Message
    throw
} finally {
    $env:ANDROID_SERIAL = $oldSerial
    $summary.finishedUtc = [DateTime]::UtcNow.ToString('o')
    $summary | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $output 'performance-summary.json') -Encoding UTF8
    Write-Host "Comparison work copies retained for diagnosis: $work"
    Write-Host 'No performance PASS, commit, push or publication. Review report, thermal/display logs and Perfetto traces.'
}
