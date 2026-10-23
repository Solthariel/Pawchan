[CmdletBinding(DefaultParameterSetName = "New")]
param(
    [Parameter(ParameterSetName = "New")]
    [ValidateSet("patch", "minor", "major")][string]$Change = "patch",
    [Parameter(Mandatory = $true, ParameterSetName = "New")][string]$NotesFile,
    [Parameter(Mandatory = $true, ParameterSetName = "Resume")][switch]$Resume
)
$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $project '.git'))) {
    throw 'No .git at the project root. Open the Git working folder, not a ZIP folder. Do not add safe.directory for C:/.'
}
if ($Resume) {
    & python -B "$PSScriptRoot\..\update\publishing\tools\publication_command.py" --resume
} else {
    $notes = (Resolve-Path -LiteralPath $NotesFile).Path
    & python -B "$PSScriptRoot\..\update\publishing\tools\publication_command.py" --change $Change --notes $notes
}
if ($LASTEXITCODE -ne 0) { throw "Release request stopped. Inspect output; use -Resume only for an already prepared new release." }
