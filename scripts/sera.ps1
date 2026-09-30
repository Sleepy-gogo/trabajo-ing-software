# En Windows: .\scripts\sera.ps1 setup
$ErrorActionPreference = 'Stop'
python (Join-Path $PSScriptRoot 'sera.py') @args
exit $LASTEXITCODE
