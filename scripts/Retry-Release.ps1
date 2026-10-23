$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $project '.git'))) {
    throw 'No .git at the project root. Use the Git working folder, not a ZIP folder; do not add a drive-wide safe.directory.'
}
& python -B "$PSScriptRoot\..\update\publishing\tools\publication_command.py" --retry
if ($LASTEXITCODE -ne 0) { throw "Recovery request stopped. Inspect the output; do not force push." }
