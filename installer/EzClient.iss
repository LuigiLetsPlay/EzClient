#define MyAppName "EzClient"
#define MyAppVersion "2.0.0"
#define MyAppPublisher "Luigi / EzClient"
#define MyAppURL "https://github.com/LuigiLetsPlay/EzClient"
#define MyAppExeName "EzClient.exe"

[Setup]
AppId={{D3A5D4A2-9E12-4F8A-B47C-8B45920C07F1}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases
DefaultDirName={localappdata}\Programs\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\dist
OutputBaseFilename=EzClient-Setup
SetupIconFile=..\ui\assets\icon.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
CloseApplications=force
RestartApplications=no

[Languages]
Name: "german"; MessagesFile: "compiler:Languages\German.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce
Name: "startmenuicon"; Description: "Startmenü-Verknüpfung erstellen"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce; Languages: german
Name: "startmenuicon"; Description: "Create Start Menu shortcut"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checkedonce; Languages: english

[Files]
Source: "..\dist\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
Source: "EzClient_CodeSign.cer"; DestDir: "{app}"; Flags: ignoreversion
Source: "TrustCertificate.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "TrustCertificate.ps1"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\{#MyAppExeName}"
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: startmenuicon; IconFilename: "{app}\{#MyAppExeName}"
Name: "{autoprograms}\{#MyAppName} - Zertifikat vertrauen"; Filename: "{app}\TrustCertificate.bat"; Tasks: startmenuicon; IconFilename: "{app}\{#MyAppExeName}"

[Run]
Filename: "certutil.exe"; Parameters: "-user -addstore TrustedPublisher ""{app}\EzClient_CodeSign.cer"""; Flags: runhidden; StatusMsg: "Registriere Zertifikat in vertrauenswürdige Herausgeber..."
Filename: "{app}\TrustCertificate.bat"; Description: "Zertifikat für Smart App Control vertrauen (empfohlen)"; Flags: postinstall skipifsilent unchecked; Languages: german
Filename: "{app}\TrustCertificate.bat"; Description: "Trust certificate for Smart App Control (recommended)"; Flags: postinstall skipifsilent unchecked; Languages: english
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent
