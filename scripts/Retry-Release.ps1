$ErrorActionPreference = "Stop"
& python -B "$PSScriptRoot\..\update\publishing\tools\publication_command.py" --retry
if ($LASTEXITCODE -ne 0) { throw "Recovery request stopped. Inspect the output; do not force push." }
