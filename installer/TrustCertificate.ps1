# PowerShell Helper to install EzClient Certificate into CurrentUser Root
$certPath = Join-Path $PSScriptRoot "EzClient_CodeSign.cer"
if (-not (Test-Path $certPath)) {
    $certPath = Join-Path (Split-Path $PSScriptRoot -Parent) "tools\EzClient_CodeSign.cer"
}

if (-not (Test-Path $certPath)) {
    Write-Error "Zertifikat nicht gefunden: $certPath"
    Exit 1
}

Write-Host "Importiere EzClient Zertifikat in Cert:\CurrentUser\Root ..." -ForegroundColor Cyan
Import-Certificate -FilePath $certPath -CertStoreLocation "Cert:\CurrentUser\Root" | Out-Null
Write-Host "[OK] Zertifikat wurde erfolgreich als vertrauenswuerdig registriert." -ForegroundColor Green
