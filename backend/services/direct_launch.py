import os
import sys
import json
import shutil
import zipfile
import subprocess
from pathlib import Path
from typing import Any, Callable, Optional, Tuple, List
from backend.models.types import ProfileData
from backend.services.minecraft import minecraft_dir
from backend.services.msa_auth import get_minecraft_session, MinecraftSession

def maven_to_path(name: str) -> str:
    """Convert maven coordinates group:artifact:version[:classifier] to relative jar path."""
    parts = name.split(":")
    if len(parts) < 3:
        return ""
    group, artifact, version = parts[0], parts[1], parts[2]
    classifier = f"-{parts[3]}" if len(parts) > 3 else ""
    group_path = group.replace(".", "/")
    return f"{group_path}/{artifact}/{version}/{artifact}-{version}{classifier}.jar"

def find_best_java(mc_dir: Path) -> str:
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

    # Test candidate executability
    for c in candidates:
        if Path(c).exists():
            return c

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

def find_version_meta(mc_dir: Path, mc_version: str, loader: str = "Fabric") -> Tuple[Optional[Path], dict[str, Any], Optional[Path], dict[str, Any]]:
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
        if not candidates:
            # 2. General fabric match
            candidates = list(versions_dir.glob("fabric-loader-*"))
        if candidates:
            chosen = max(candidates, key=lambda p: p.stat().st_mtime)
            json_file = chosen / f"{chosen.name}.json"
            if json_file.exists():
                try:
                    fabric_path = json_file
                    fabric_data = json.loads(json_file.read_text(encoding="utf-8"))
                except Exception:
                    pass

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
        if is_win:
            if ":natives-windows" in name:
                if "-arm64" not in name and not (name.endswith("-x86") or ":natives-windows-x86:" in name):
                    is_native = True
        elif sys.platform == "darwin" and ":natives-macos" in name:
            is_native = True
        elif sys.platform.startswith("linux") and ":natives-linux" in name:
            is_native = True

        if is_native:
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
                                if not target.exists():
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
        "soundCategory_music": "0.1",
        "onboardAccessibility": "false",
        "narrator": "0",
        "tutorialStep": "none",
        "skipRealmsNotification": "true",
        "maxFps": "260",
        "enableVsync": "false",
        "graphicsMode": "0",
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
    status_callback: Optional[Callable[[str], None]] = None
) -> Optional[subprocess.Popen]:
    """
    Launches Minecraft standalone directly using Java & Knot/Fabric.
    Extracts authentication token from .minecraft automatically.
    """
    def notify(msg: str):
        if status_callback:
            status_callback(msg)
        print(f"[DirectLaunch] {msg}")

    # Configure optimal audio/accessibility defaults (10% music, skip narrator)
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

    # 0. Sync and verify all profile mods and dependencies
    notify("Verifiziere & synchronisiere Mods und Bibliotheken…")
    try:
        from backend.services.mod_downloader import sync_profile_mods
        sync_profile_mods(profile, status_callback=notify)
    except Exception as e:
        print(f"[DirectLaunch] Warning during mod sync: {e}")

    # 1. Version and libraries lookup
    notify(f"Suche Version {profile.minecraft_version} & Fabric-Dateien…")
    fabric_file, fabric_data, vanilla_file, vanilla_data = find_version_meta(
        mc, profile.minecraft_version, profile.loader
    )

    if not vanilla_data and not fabric_data:
        notify(f"Fehler: Keine Spieldateien für {profile.minecraft_version} in .minecraft/versions gefunden.")
        return None

    inherits = fabric_data.get("inheritsFrom", profile.minecraft_version) if fabric_data else profile.minecraft_version
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

    for lib in all_libs:
        if not is_rule_allowed(lib):
            continue

        rel = lib.get("downloads", {}).get("artifact", {}).get("path")
        if not rel and "name" in lib:
            rel = maven_to_path(lib["name"])
        if rel:
            jar_path = mc / "libraries" / rel
            if jar_path.exists() and jar_path not in seen:
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
    java_bin = find_best_java(mc)
    ram = getattr(profile, "ram_mb", 4096) or 4096
    sep = ";" if sys.platform.startswith("win") else ":"
    classpath_str = sep.join(str(p) for p in cp_jars)

    asset_index = vanilla_data.get("assetIndex", {}).get("id", inherits)
    main_class = fabric_data.get("mainClass", "net.fabricmc.loader.impl.launch.knot.KnotClient") if fabric_data else vanilla_data.get("mainClass", "net.minecraft.client.main.Main")

    # High-Performance JVM Flags (Aggressive ZGC zero-stutter flags)
    jvm_args = [
        java_bin,
        f"-Xmx{ram}M",
        f"-Xms{ram}M",
        "-XX:+AlwaysPreTouch",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseZGC",
        "-XX:+ZProactive",
        "-XX:ZUncommitDelay=60",
        "-XX:+PerfDisableSharedMem",
        "-XX:+DisableExplicitGC",
        "--enable-native-access=ALL-UNNAMED",
        "-DFabricMcEmu=net.minecraft.client.main.Main",
        f"-Dfabric.modsFolder={profile.mods_path}",
        f"-Dfabric.gameVersion={profile.minecraft_version}",
        "-Dfabric.development=false",
        f"-Djava.library.path={natives_dir}",
        f"-Dorg.lwjgl.system.SharedLibraryExtractPath={natives_dir}",
        f"-Dorg.lwjgl.librarypath={natives_dir}",
        "-Dminecraft.launcher.brand=EzClient",
        "-Dminecraft.launcher.version=2.0.0",
        "-cp", classpath_str,
        main_class,
    ]

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

    full_cmd = jvm_args + game_args

    notify(f"Starte Minecraft direkt ({session.username})…")

    creationflags = 0

    env = os.environ.copy()
    env["PATH"] = f"{natives_dir};{env.get('PATH', '')}"

    log_file = profile.path / "ezclient_latest_run.log"
    try:
        log_f = open(log_file, "w", encoding="utf-8")
    except Exception:
        log_f = subprocess.DEVNULL

    proc = subprocess.Popen(
        full_cmd,
        cwd=str(profile.path),
        stdout=log_f,
        stderr=subprocess.STDOUT,
        creationflags=creationflags,
        env=env
    )

    notify("Minecraft wurde erfolgreich gestartet!")
    return proc
