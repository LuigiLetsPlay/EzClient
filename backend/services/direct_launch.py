import os
import sys
import json
import shutil
import zipfile
import subprocess
import threading
from pathlib import Path
from typing import Any, Callable, Optional, Tuple, List
from backend.models.types import ProfileData, APP_VERSION
from backend.services.minecraft import minecraft_dir
from backend.services.msa_auth import get_minecraft_session, MinecraftSession
from backend.services.minecraft_versions import version_tuple

def maven_to_path(name: str) -> str:
    """Convert maven coordinates group:artifact:version[:classifier] to relative jar path."""
    parts = name.split(":")
    if len(parts) < 3:
        return ""
    group, artifact, version = parts[0], parts[1], parts[2]
    classifier = f"-{parts[3]}" if len(parts) > 3 else ""
    group_path = group.replace(".", "/")
    return f"{group_path}/{artifact}/{version}/{artifact}-{version}{classifier}.jar"


def maven_module_key(name: str) -> str:
    """Return the conflict-resolution key for Maven coordinates."""
    parts = str(name).split(":")
    if len(parts) >= 4:
        return f"{parts[0]}:{parts[1]}:{parts[3]}".lower()
    if len(parts) >= 2:
        return f"{parts[0]}:{parts[1]}".lower()
    return str(name).lower()

def _java_major(java_path: str) -> int | None:
    try:
        result = subprocess.run(
            [java_path, "-XshowSettings:properties", "-version"],
            capture_output=True,
            text=True,
            timeout=15,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
        )
        for line in (result.stdout + result.stderr).splitlines():
            if line.strip().startswith("java.version"):
                value = line.split("=", 1)[1].strip()
                parts = value.split(".")
                return int(parts[1] if parts[0] == "1" and len(parts) > 1 else parts[0])
    except (OSError, subprocess.SubprocessError, ValueError):
        pass
    return None


def find_best_java(mc_dir: Path, required_major: int = 0) -> str:
    """Finds best Java executable (checks Mojang runtime, Java 21/25, system PATH)."""
    candidates = []

    # 1. Official Mojang Bundled Runtimes in Program Files (x86) / Program Files / AppData
    roots = [
        Path(os.environ.get("PROGRAMFILES(X86)", "")) / "Minecraft Launcher" / "runtime",
        Path(os.environ.get("PROGRAMFILES", "")) / "Minecraft Launcher" / "runtime",
        Path(os.environ.get("LOCALAPPDATA", "")) / "Programs" / "Minecraft Launcher" / "runtime",
        Path(os.environ.get("LOCALAPPDATA", "")) / "Packages" / "Microsoft.4297127D64EC6_8wekyb3d8bbwe" / "LocalCache" / "Local" / "runtime",
        mc_dir / "runtime",
    ]

    for r in roots:
        if r.exists():
            # Check Java 25 (epsilon) and Java 21 (delta / gamma) first
            for tag in ["java-runtime-epsilon", "java-runtime-delta", "java-runtime-gamma", "java-runtime-beta", "java-runtime-alpha"]:
                sub = r / tag
                if sub.exists():
                    for jw in sub.rglob("javaw.exe" if sys.platform.startswith("win") else "java"):
                        candidates.append(str(jw))

    # 2. System PATH javaw / java
    sys_cand = shutil.which("javaw") or shutil.which("java")
    if sys_cand:
        candidates.append(str(sys_cand))

    # 3. Adoptium / JDK installations
    prog_files = Path(os.environ.get("PROGRAMFILES", ""))
    for jdk_dir in [prog_files / "Eclipse Adoptium", prog_files / "Java", prog_files / "Microsoft"]:
        if jdk_dir.exists():
            for jw in jdk_dir.rglob("javaw.exe" if sys.platform.startswith("win") else "java"):
                candidates.append(str(jw))

    for c in candidates:
        if Path(c).exists():
            if required_major and _java_major(c) != required_major:
                continue
            return c

    if required_major:
        try:
            from backend.services.java_runtime import install_required_java
            installed = install_required_java(mc_dir, required_major, lambda message: print(f"[DirectLaunch] {message}"))
            return str(installed)
        except Exception as exc:
            print(f"[DirectLaunch] Java-Auto-Installation fehlgeschlagen: {exc}")

    return "javaw" if sys.platform.startswith("win") else "java"

def is_rule_allowed(rule_obj: dict[str, Any]) -> bool:
    """Evaluates Minecraft library OS rules for current operating system."""
    rules = rule_obj.get("rules")
    if not rules:
        return True

    allowed = False
    for r in rules:
        action = r.get("action") == "allow"
        os_spec = r.get("os", {})
        os_name = os_spec.get("name")

        if os_name is None:
            allowed = action
        else:
            match = False
            if os_name == "windows" and sys.platform.startswith("win"):
                match = True
            elif os_name == "osx" and sys.platform == "darwin":
                match = True
            elif os_name == "linux" and sys.platform.startswith("linux"):
                match = True

            if match:
                allowed = action

    return allowed

def find_version_meta(mc_dir: Path, mc_version: str, loader: str = "Fabric", loader_version: str = "") -> Tuple[Optional[Path], dict[str, Any], Optional[Path], dict[str, Any]]:
    """
    Finds and parses the version JSONs for Fabric Loader and inherited Vanilla base version.
    Returns: (fabric_path, fabric_json, vanilla_path, vanilla_json)
    """
    versions_dir = mc_dir / "versions"
    if not versions_dir.exists():
        return None, {}, None, {}

    fabric_path = None
    fabric_data = {}

    if loader.lower() == "fabric":
        # 1. Exact match with mc_version
        candidates = list(versions_dir.glob(f"fabric-loader-*-{mc_version}"))
        if loader_version:
            candidates = [path for path in candidates if path.name == f"fabric-loader-{loader_version}-{mc_version}"]
        if candidates:
            from backend.services.game_bootstrap import _parse_loader_version
            candidates.sort(key=lambda p: _parse_loader_version(p.name), reverse=True)
            for chosen in candidates:
                json_file = chosen / f"{chosen.name}.json"
                try:
                    candidate_data = json.loads(json_file.read_text(encoding="utf-8"))
                    if candidate_data.get("inheritsFrom") != mc_version:
                        continue
                    fabric_path = json_file
                    fabric_data = candidate_data
                    break
                except (OSError, ValueError):
                    continue

    # Vanilla base version resolution
    inherits = fabric_data.get("inheritsFrom", mc_version) if fabric_data else mc_version
    vanilla_dir = versions_dir / inherits
    vanilla_json_file = vanilla_dir / f"{inherits}.json"
    vanilla_data = {}
    vanilla_path = None

    if vanilla_json_file.exists():
        try:
            vanilla_path = vanilla_json_file
            vanilla_data = json.loads(vanilla_json_file.read_text(encoding="utf-8"))
        except Exception:
            pass

    return fabric_path, fabric_data, vanilla_path, vanilla_data

def extract_natives(mc_dir: Path, libraries: list[dict[str, Any]], natives_dir: Path) -> None:
    """Extracts required native DLLs to profile natives directory."""
    natives_dir.mkdir(parents=True, exist_ok=True)
    is_win = sys.platform.startswith("win")

    for lib in libraries:
        name = lib.get("name", "")
        if not is_rule_allowed(lib):
            continue

        # Look for native library markers
        is_native = False
        native_artifact = None
        native_map = lib.get("natives", {})
        if native_map:
            from backend.services.game_bootstrap import _native_artifact

            platform_key = "windows" if is_win else ("osx" if sys.platform == "darwin" else "linux")
            if native_map.get(platform_key):
                is_native = True
                native_artifact = _native_artifact(lib)
        if is_win:
            if ":natives-windows" in name:
                if "-arm64" not in name and not (name.endswith("-x86") or ":natives-windows-x86:" in name):
                    is_native = True
        elif sys.platform == "darwin" and ":natives-macos" in name:
            is_native = True
        elif sys.platform.startswith("linux") and ":natives-linux" in name:
            is_native = True

        if is_native:
            rel = (native_artifact or {}).get("path")
            if not rel:
                rel = lib.get("downloads", {}).get("artifact", {}).get("path")
            if not rel:
                rel = maven_to_path(name)
            jar_p = mc_dir / "libraries" / rel
            if jar_p.exists():
                try:
                    with zipfile.ZipFile(jar_p, "r") as zf:
                        for member in zf.namelist():
                            if member.endswith(".dll") or member.endswith(".so") or member.endswith(".dylib"):
                                fname = Path(member).name
                                target = natives_dir / fname
                                with zf.open(member) as src, open(target, "wb") as dst:
                                    shutil.copyfileobj(src, dst)
                except Exception as e:
                    print(f"[DirectLaunch] Native extraction warning for {name}: {e}")

def ensure_profile_defaults(
    profile_path: Path,
    status_callback: Optional[Callable[[str], None]] = None,
) -> None:
    """Pre-configures options.txt and sodium-options.json to optimize PvP/Performance, skip narrator, disable tutorial, and set music to 10%."""
    from backend.services.store import preseed_optimized_profile_settings
    preseed_optimized_profile_settings(profile_path)

    options_file = profile_path / "options.txt"
    defaults = {
        "soundCategory_music": "0.05",
        "onboardAccessibility": "false",
        "narrator": "0",
        "tutorialStep": "none",
        "skipRealmsNotification": "true",
        "maxFps": "120",
        "enableVsync": "false",
        "graphicsMode": "1",
        "renderDistance": "8",
        "simulationDistance": "5",
        "entityShadows": "false",
        "clouds": "false",
        "cloudStatus": "false",
        "particles": "2",
        "biomeBlendRadius": "0"
    }
    is_new = not options_file.exists()
    if not is_new:
        # Nur Discord RPC fixen, wenn options.txt schon existiert
        pass
    else:
        existing: dict[str, str] = {}
        for k, v in defaults.items():
            existing[k] = v
        lines = [f"{k}:{v}" for k, v in existing.items()]
        options_file.parent.mkdir(parents=True, exist_ok=True)
        options_file.write_text("\n".join(lines), encoding="utf-8")

    # Disable Essential Discord RPC
    essential_config = profile_path / "essential" / "config.toml"
    if essential_config.exists():
        try:
            text = essential_config.read_text("utf-8")
            if 'discord_rpc = true' in text or 'discord_integration = true' in text or 'discordRpc = true' in text or 'discord' in text.lower():
                import re
                text = re.sub(r'(?i)discord[a-z_]*\s*=\s*true', 'discord_integration = false', text)
                essential_config.write_text(text, "utf-8")
        except Exception as e:
            message = f"Essential RPC konnte nicht deaktiviert werden: {e}"
            if status_callback:
                status_callback(message)
            else:
                print(f"[DirectLaunch] {message}")

def launch_minecraft_direct(
    profile: ProfileData,
    status_callback: Optional[Callable[[str], None]] = None,
    log_file_path: Optional[Path] = None,
    server_ip: Optional[str] = None,
) -> Optional[subprocess.Popen]:
    """
    Launches Minecraft standalone directly using Java & Knot/Fabric.
    Extracts authentication token from .minecraft automatically.
    """
    def notify(msg: str):
        if status_callback:
            status_callback(msg)
        print(f"[DirectLaunch] {msg}")

    # Deleted profiles may leave their isolated directory behind to protect
    # saves. If the same visible name is later reused, an old managed JAR must
    # never be allowed to leak into a newly-created target.
    from backend.services.store import has_ezclient_asset
    if profile.profile_type != "raw" and not has_ezclient_asset(profile.minecraft_version):
        removed_core = False
        for candidate in profile.mods_path.glob("*EzClient*.jar"):
            candidate.unlink(missing_ok=True)
            removed_core = True
        retained = []
        for mod in profile.mods:
            identity = f"{mod.slug} {mod.project_id} {mod.name} {mod.filename}".lower()
            if "ezclient" in identity:
                removed_core = True
                continue
            retained.append(mod)
        profile.mods = retained
        profile.managed_core_mods = [value for value in profile.managed_core_mods if "ezclient" not in value.lower()]
        profile.integrated_mods = [value for value in profile.integrated_mods if "ezclient" not in value.lower()]
        if profile.profile_type == "ezclient":
            profile.profile_type = "performance"
        if removed_core:
            notify(f"Inkompatibler EzClient Core für Minecraft {profile.minecraft_version} wurde vor dem Start entfernt.")

    # A raw modpack owns every option and override file. Never inject client
    # defaults or modify its content before launch.
    if profile.profile_type != "raw":
        try:
            ensure_profile_defaults(profile.path, notify)
        except Exception as e:
            notify(f"Warnung beim Anwenden der Spieleinstellungen: {e}")

    mc = minecraft_dir()
    # Run the fast readiness check on every launch. It catches an incomplete
    # asset cache even when the Minecraft client JAR already exists.
    try:
        from backend.services.game_bootstrap import ensure_game_ready
        ensure_game_ready(profile, mc, notify)
    except Exception as exc:
        notify(f"Fehler beim Minecraft-Erststart: {exc}")
        return None

    if profile.loader.lower() == "forge":
        try:
            has_missing_mods = any(
                m.enabled and not (profile.mods_path / (m.filename or f"{m.slug}.jar")).is_file()
                for m in profile.mods
                if (m.slug or m.project_id)
            )
            if profile.profile_type != "raw" or has_missing_mods:
                from backend.services.mod_downloader import sync_profile_mods
                sync_profile_mods(profile, status_callback=notify)
            import minecraft_launcher_lib
            from backend.services.java_runtime import install_required_java
            from backend.services.minecraft_versions import required_java

            session: MinecraftSession = get_minecraft_session()
            if not session.is_online:
                notify("Microsoft-Anmeldung mit einer Minecraft-Java-Lizenz ist zum Starten erforderlich.")
                return None
            requested = str(getattr(profile, "loader_version", "") or "").strip()
            from backend.services.loader_versions import installed_forge_metadata
            forge_versions = installed_forge_metadata(mc, profile.minecraft_version, requested)
            if not forge_versions:
                notify("Forge wurde nicht vollständig installiert.")
                return None
            installed_version = forge_versions[0].parent.name
            java_bin = install_required_java(mc, required_java(profile.minecraft_version), notify)
            command = minecraft_launcher_lib.command.get_minecraft_command(
                installed_version,
                str(mc),
                {
                    "username": session.username,
                    "uuid": session.uuid.replace("-", ""),
                    "token": session.access_token,
                    "executablePath": str(java_bin),
                    "jvmArguments": [f"-Xmx{getattr(profile, 'ram_mb', 4096) or 4096}M", "-Xms512M"],
                    "launcherName": "EzClient",
                    "launcherVersion": APP_VERSION,
                    "gameDirectory": str(profile.path),
                },
            )
            if server_ip:
                if version_tuple(profile.minecraft_version) >= (1, 20):
                    command.extend(["--quickPlayMultiplayer", server_ip.strip()])
                else:
                    server_parts = server_ip.split(":")
                    command.extend(["--server", server_parts[0].strip()])
                    if len(server_parts) > 1 and server_parts[1].isdigit():
                        command.extend(["--port", server_parts[1].strip()])
            log_file = log_file_path or (profile.path / "ezclient_latest_run.log")
            log_file.parent.mkdir(parents=True, exist_ok=True)
            with open(log_file, "w", encoding="utf-8") as log_handle:
                process = subprocess.Popen(
                    command, cwd=str(profile.path), stdout=log_handle,
                    stderr=subprocess.STDOUT, env=os.environ.copy(),
                )
            notify("Minecraft Forge wurde erfolgreich gestartet!")
            return process
        except Exception as exc:
            notify(f"Forge konnte nicht gestartet werden: {exc}")
            return None

    # 0. Launcher-managed profiles may be synchronized. Modpack instances use
    # exactly the files from their manifest and must remain untouched, unless
    # user added mods with missing JAR files.
    has_missing_mods = any(
        m.enabled and not (profile.mods_path / (m.filename or f"{m.slug}.jar")).is_file()
        for m in profile.mods
        if (m.slug or m.project_id)
    )
    if profile.profile_type != "raw" or has_missing_mods:
        notify("Verifiziere & synchronisiere Mods und Bibliotheken…")
        try:
            from backend.services.mod_downloader import sync_profile_mods
            sync_profile_mods(profile, status_callback=notify)
        except Exception as e:
            print(f"[DirectLaunch] Warning during mod sync: {e}")

    # 1. Version and libraries lookup
    notify(f"Suche Version {profile.minecraft_version} & Fabric-Dateien…")
    fabric_file, fabric_data, vanilla_file, vanilla_data = find_version_meta(
        mc, profile.minecraft_version, profile.loader,
        profile.loader_version if profile.profile_type == "raw" else "",
    )

    if not vanilla_data and not fabric_data:
        notify(f"Fehler: Keine Spieldateien für {profile.minecraft_version} in .minecraft/versions gefunden.")
        return None

    if profile.loader.lower() == "fabric" and not fabric_data:
        notify(f"Fehler: Fabric für Minecraft {profile.minecraft_version} wurde nicht vollständig installiert.")
        return None

    inherits = fabric_data.get("inheritsFrom", profile.minecraft_version) if fabric_data else profile.minecraft_version
    required_java_major = int(vanilla_data.get("javaVersion", {}).get("majorVersion") or 0)
    if not required_java_major:
        from backend.services.minecraft_versions import required_java
        required_java_major = required_java(inherits)
    client_jar = mc / f"versions/{inherits}/{inherits}.jar"
    if not client_jar.exists():
        notify(f"Fehler: Minecraft Basis-JAR fehlt ({inherits}.jar). Bitte einmalig im Launcher starten.")
        return None

    # 2. Build Classpath & Natives
    notify("Kompiliere Bibliotheken und Classpath…")
    natives_dir = profile.path / "natives"
    all_libs = fabric_data.get("libraries", []) + vanilla_data.get("libraries", [])
    extract_natives(mc, all_libs, natives_dir)

    cp_jars: List[Path] = []
    seen = set()
    seen_modules: set[str] = set()

    for lib in all_libs:
        if not is_rule_allowed(lib):
            continue

        name = lib.get("name", "")

        # Native-only entries belong in java.library.path, not on the Java classpath.
        if ":natives-" in name or (lib.get("natives") and not lib.get("downloads", {}).get("artifact")):
            continue

        # Fabric metadata comes first. Keep its replacement when both metadata
        # sets declare the same module, such as patched and vanilla LWJGL.
        module_key = maven_module_key(name)
        if module_key and module_key in seen_modules:
            continue

        rel = lib.get("downloads", {}).get("artifact", {}).get("path")
        if not rel and "name" in lib:
            rel = maven_to_path(lib["name"])
        if rel:
            jar_path = mc / "libraries" / rel
            if jar_path.exists() and jar_path not in seen:
                if module_key:
                    seen_modules.add(module_key)
                seen.add(jar_path)
                cp_jars.append(jar_path)

    if client_jar not in seen:
        seen.add(client_jar)
        cp_jars.append(client_jar)

    if not cp_jars:
        notify("Fehler: Classpath-Bibliotheken konnten nicht gefunden werden.")
        return None

    # 3. Authenticate Session (Microsoft Account / Cached Session)
    notify("Überprüfe Microsoft-Sitzung & Token…")
    session: MinecraftSession = get_minecraft_session()
    notify(f"Authentifiziert als {session.username} ({'Online' if session.is_online else 'Offline'})")
    if not session.is_online:
        notify("Microsoft-Anmeldung mit einer Minecraft-Java-Lizenz ist zum Starten erforderlich.")
        return None

    # 4. Assemble JVM & Game Arguments
    notify(f"Prüfe und installiere Java {required_java_major}, falls erforderlich…")
    try:
        from backend.services.java_runtime import install_required_java
        java_bin = str(install_required_java(mc, required_java_major, notify))
    except Exception as exc:
        notify(f"Fehler bei der automatischen Java-Installation: {exc}")
        return None
    ram = getattr(profile, "ram_mb", 4096) or 4096
    sep = ";" if sys.platform.startswith("win") else ":"
    classpath_str = sep.join(str(p) for p in cp_jars)

    asset_index = vanilla_data.get("assetIndex", {}).get("id", inherits)
    main_class = fabric_data.get("mainClass", "net.fabricmc.loader.impl.launch.knot.KnotClient") if fabric_data else vanilla_data.get("mainClass", "net.minecraft.client.main.Main")

    # Dynamic initial heap to avoid expensive heap resizing during mod loading
    init_ram = max(2048, min(ram, 4096)) if ram >= 2048 else ram
    # Dynamic compiler threads: on <=4 core CPUs, 2 compiler threads prevents starving worker threads
    cpu_count = os.cpu_count() or 4
    ci_compilers = max(2, min(4, cpu_count // 2 if cpu_count <= 4 else cpu_count - 2))

    # Ultra-Fast High-Performance JVM Flags (Instant-Boot + Zero Stutter)
    jvm_args = [
        java_bin,
        f"-Xmx{ram}M",
        f"-Xms{init_ram}M",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseG1GC",
        "-XX:G1NewSizePercent=20",
        "-XX:G1ReservePercent=20",
        "-XX:MaxGCPauseMillis=30",
        "-XX:+ParallelRefProcEnabled",
        "-XX:+OptimizeStringConcat",
        f"-XX:CICompilerCount={ci_compilers}",
        "-XX:+TieredCompilation",
        "-XX:+PerfDisableSharedMem",
        "-XX:+DisableExplicitGC",
        "-Djava.lang.invoke.stringConcat=BC_SB",
        "-Dlog4j2.formatMsgNoLookups=true",
        "-Dfabric.log.disableAnsi=true",
    ]
    if required_java_major >= 17:
        jvm_args.append("--enable-native-access=ALL-UNNAMED")

    jvm_args.extend([
        f"-Dfabric.modsFolder={profile.mods_path}",
        f"-Dfabric.gameVersion={profile.minecraft_version}",
        "-Dfabric.development=false",
        "-Dfabric.disableGui=true",
        "-Dfabric.system.disableGui=true",
        "-Dfabric.gui.disabled=true",
        "-Dfabric.noGui=true",
        f"-Djava.library.path={natives_dir}",
        f"-Dorg.lwjgl.system.SharedLibraryExtractPath={natives_dir}",
        f"-Dorg.lwjgl.librarypath={natives_dir}",
        "-Dminecraft.launcher.brand=EzClient",
        f"-Dminecraft.launcher.version={APP_VERSION}",
        "-cp", classpath_str,
        main_class,
    ])

    game_args = [
        "--username", session.username,
        "--version", profile.minecraft_version,
        "--gameDir", str(profile.path),
        "--assetsDir", str(mc / "assets"),
        "--assetIndex", str(asset_index),
        "--uuid", session.uuid.replace("-", ""),
        "--accessToken", session.access_token,
        "--userType", session.user_type,
        "--versionType", "EzClient"
    ]

    if server_ip:
        if version_tuple(profile.minecraft_version) >= (1, 20):
            game_args.extend(["--quickPlayMultiplayer", server_ip.strip()])
        else:
            server_parts = server_ip.split(":")
            game_args.extend(["--server", server_parts[0].strip()])
            if len(server_parts) > 1 and server_parts[1].isdigit():
                game_args.extend(["--port", server_parts[1].strip()])

    full_cmd = jvm_args + game_args

    notify(f"Starte Minecraft direkt ({session.username})…")

    creationflags = 0

    env = os.environ.copy()
    env["PATH"] = f"{natives_dir};{env.get('PATH', '')}"

    log_file = log_file_path or (profile.path / "ezclient_latest_run.log")
    log_file.parent.mkdir(parents=True, exist_ok=True)
    try:
        with open(log_file, "w", encoding="utf-8") as log_f:
            proc = subprocess.Popen(
                full_cmd,
                cwd=str(profile.path),
                stdout=log_f,
                stderr=subprocess.STDOUT,
                creationflags=creationflags,
                env=env,
            )
    except OSError as exc:
        notify(f"Minecraft-Prozess konnte nicht gestartet werden: {exc}")
        return None

    notify("Minecraft wurde erfolgreich gestartet!")

    if sys.platform == "win32":
        try:
            import ctypes
            # Grant child process permission to set foreground window
            ctypes.windll.user32.AllowSetForegroundWindow(proc.pid)
        except Exception:
            pass

        def _focus_minecraft_window():
            import time
            import ctypes
            from ctypes import wintypes
            user32 = ctypes.windll.user32
            start = time.time()
            # Wait up to 35 seconds for the Minecraft window to appear
            while time.time() - start < 35:
                if proc.poll() is not None:
                    break
                found_hwnd = None

                def enum_proc(hwnd, lparam):
                    nonlocal found_hwnd
                    if not user32.IsWindowVisible(hwnd):
                        return True
                    pid = wintypes.DWORD()
                    user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
                    if pid.value == proc.pid:
                        length = user32.GetWindowTextLengthW(hwnd)
                        if length > 0:
                            found_hwnd = hwnd
                            return False
                    return True

                WNDENUMPROC = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
                user32.EnumWindows(WNDENUMPROC(enum_proc), 0)

                if found_hwnd:
                    try:
                        ico_path = str(Path(__file__).resolve().parents[2] / "ui" / "assets" / "icon.ico")
                        if os.path.exists(ico_path):
                            hicon_big = user32.LoadImageW(None, ico_path, 1, 32, 32, 0x0010)
                            hicon_small = user32.LoadImageW(None, ico_path, 1, 16, 16, 0x0010)
                            if hicon_big:
                                user32.SendMessageW(found_hwnd, 0x0080, 1, hicon_big)
                            if hicon_small:
                                user32.SendMessageW(found_hwnd, 0x0080, 0, hicon_small)
                    except Exception:
                        pass
                    time.sleep(0.15)
                    try:
                        user32.ShowWindow(found_hwnd, 9)  # SW_RESTORE
                        user32.SetForegroundWindow(found_hwnd)
                    except Exception:
                        pass
                    break

                time.sleep(0.3)

        threading.Thread(target=_focus_minecraft_window, daemon=True, name="MinecraftFocusWatcher").start()

    return proc
