param(
    [ValidateSet("patch", "minor", "major")][string]$Change = "patch",
    [Parameter(Mandatory = $true)][string]$NotesFile
)
$ErrorActionPreference = "Stop"
$notes = (Resolve-Path -LiteralPath $NotesFile).Path
& python -B "$PSScriptRoot\..\update\publishing\tools\publication_command.py" --change $Change --notes $notes
if ($LASTEXITCODE -ne 0) { throw "Release request stopped. Do not repeat blindly; inspect the output." }
