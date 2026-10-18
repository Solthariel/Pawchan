$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $requiredVariables =
        @(
            "PAWCHAN_KEYSTORE",
            "PAWCHAN_KEY_ALIAS",
            "PAWCHAN_STORE_PASSWORD",
            "PAWCHAN_KEY_PASSWORD",
            "PAWCHAN_UPDATE_MANIFEST_URL"
        )
    foreach ($variable in $requiredVariables) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($variable))) {
            throw "RELEASE PACKAGE: missing environment variable '$variable'"
        }
    }
    $updateManifestUrl = [Environment]::GetEnvironmentVariable("PAWCHAN_UPDATE_MANIFEST_URL")
    $parsedUpdateManifestUrl = $null
    if (
        -not [Uri]::TryCreate(
            $updateManifestUrl,
            [UriKind]::Absolute,
            [ref] $parsedUpdateManifestUrl
        ) -or
        $parsedUpdateManifestUrl.Scheme -ne "https" -or
        [string]::IsNullOrWhiteSpace($parsedUpdateManifestUrl.Host)
    ) {
        throw "RELEASE PACKAGE: PAWCHAN_UPDATE_MANIFEST_URL must be an absolute HTTPS URL"
    }

    & ".\scripts\local-check.ps1"
    if ($LASTEXITCODE -ne 0) {
        throw "RELEASE PACKAGE: FAIL during local check"
    }

    & ".\gradlew.bat" `
        ":app:assembleRelease" `
        "-Ppawchan.updateManifestUrl=$updateManifestUrl" `
        "--no-configuration-cache" `
        "--warning-mode=fail"
    if ($LASTEXITCODE -ne 0) {
        throw "RELEASE PACKAGE: FAIL during signed release build"
    }

    $source = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"
    $dist = Join-Path $projectRoot "dist"
    $versions =
        Get-Content `
            -LiteralPath (Join-Path $projectRoot "update\publishing\versions.json") `
            -Raw |
        ConvertFrom-Json
    $latestVersion = $versions.versions | Sort-Object code -Descending | Select-Object -First 1
    $versionName = $latestVersion.name
    $apkName = "Pawchan-$versionName.apk"
    $target = Join-Path $dist $apkName
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    Copy-Item -Force $source $target
    & python ".\update\publishing\tools\update_manifest.py" $target
    if ($LASTEXITCODE -ne 0) {
        throw "RELEASE PACKAGE: FAIL while updating release manifest"
    }
    $hash = (Get-FileHash -Algorithm SHA256 $target).Hash.ToLowerInvariant()

    Write-Host ""
    Write-Host "RELEASE PACKAGE: PASS"
    Write-Host "APK: $target"
    Write-Host "SHA-256: $hash"
} finally {
    Pop-Location
}
