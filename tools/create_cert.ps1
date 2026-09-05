$ErrorActionPreference = "Stop"
$certDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$pfxPath = Join-Path $certDir "EzClient_CodeSign.pfx"
$cerPath = Join-Path $certDir "EzClient_CodeSign.cer"
$password = "EzClient2026"

Write-Host "[Cert] Generating EzClient Code Signing Certificate..."
$cert = New-SelfSignedCertificate `
    -Type CodeSigningCert `
    -Subject "CN=Luigi / EzClient, O=EzClient" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -NotAfter (Get-Date).AddYears(5) `
    -KeyUsage DigitalSignature `
    -FriendlyName "EzClient Code Signing"

$securePwd = ConvertTo-SecureString -String $password -Force -AsPlainText
Export-PfxCertificate -Cert $cert -FilePath $pfxPath -Password $securePwd | Out-Null
Export-Certificate -Cert $cert -FilePath $cerPath | Out-Null

Write-Host "[Cert] Successfully created: $pfxPath and $cerPath"
