$ErrorActionPreference = 'Stop'
python (Join-Path $PSScriptRoot 'sera.py') @args
exit $LASTEXITCODE
