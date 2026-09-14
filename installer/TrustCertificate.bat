@echo off
chcp 65001 >nul
echo =======================================================
echo   EzClient - Entwickler-Zertifikat vertrauen (Windows)
echo =======================================================
echo.
echo Dieses Skript fuegt das digitale Code-Signing-Zertifikat
echo von "Luigi / EzClient" zu den vertrauenswuerdigen
echo Stammzertifizierungsstellen des aktuellen Benutzers hinzu.
echo.
echo Dadurch bestaetigt Windows den Herausgeber und verhindert
echo Blockaden durch Smart App Control oder Windows Defender.
echo.

set "CERT_FILE=%~dp0EzClient_CodeSign.cer"
if not exist "%CERT_FILE%" (
    set "CERT_FILE=%~dp0..\tools\EzClient_CodeSign.cer"
)

if not exist "%CERT_FILE%" (
    echo [FEHLER] Zertifikatsdatei wurde nicht gefunden: %CERT_FILE%
    echo Bitte fuehre zuerst "python tools/sign_tool.py" aus.
    pause
    exit /b 1
)

echo Installiere Zertifikat: %CERT_FILE% ...
echo (Bestaetige die folgende Windows-Sicherheitsabfrage mit "Ja")
echo.

certutil -user -addstore TrustedPublisher "%CERT_FILE%" >nul 2>&1
certutil -user -addstore Root "%CERT_FILE%"

if %ERRORLEVEL% equ 0 (
    echo.
    echo =======================================================
    echo  [ERFOLG] Zertifikat wurde erfolgreich importiert!
    echo  Windows erkennt nun "Luigi / EzClient" als vertraut.
    echo =======================================================
) else (
    echo.
    echo [HINWEIS] Der Import wurde abgebrochen oder schlug fehl.
)

echo.
pause
