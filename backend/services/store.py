import json
from pathlib import Path
from typing import Any
import re
import uuid
import shutil
import sys
import os
import stat
import subprocess
import time
from dataclasses import asdict
from backend.models.types import APP_VERSION, ProfileData, ModData, STATE_PATH, PROFILES_DIR, ICON_CACHE_DIR


def ezclient_asset_name(minecraft_version: str) -> str:
    from backend.services.minecraft_versions import FROZEN_EZCLIENT_VERSION, is_frozen_ezclient_version
    product_version = FROZEN_EZCLIENT_VERSION if is_frozen_ezclient_version(minecraft_version) else APP_VERSION
    # Every 26.x target must use an exact build. The Java sources are shared,
    # while mappings, Mixins and metadata are compiled per Minecraft version.
    return f"EzClient-{product_version}+{minecraft_version}.jar"


def has_ezclient_asset(minecraft_version: str) -> bool:
    if not minecraft_version.startswith("26."):
        return False
    filename = ezclient_asset_name(minecraft_version)
    roots = [Path(__file__).resolve().parent.parent / "assets"]
    if hasattr(sys, "_MEIPASS"):
        roots.insert(0, Path(sys._MEIPASS) / "backend" / "assets")
    return any((root / filename).is_file() for root in roots)


def ensure(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


ensure(PROFILES_DIR)
ensure(ICON_CACHE_DIR)


def _remove_readonly(func, path, excinfo):
    try:
        os.chmod(path, stat.S_IWRITE)
        func(path)
    except Exception:
        pass


def clean_trash_folders(base_dir: Path) -> None:
    """Safely purge any orphaned .trash_* folders left from earlier renames."""
    if not base_dir.exists():
        return
    try:
        for item in base_dir.iterdir():
            if item.is_dir() and item.name.startswith(".trash_"):
                try:
                    shutil.rmtree(item, onerror=_remove_readonly)
                except Exception:
                    pass
    except Exception:
        pass


def robust_rmtree(path: Path, max_retries: int = 5) -> bool:
    """Robustly delete a directory tree on Windows, handling locks, read-only files, and fallbacks."""
    if not path.exists():
        return True

    # 1. Standard rmtree with readonly attribute stripping
    for attempt in range(max_retries):
        try:
            shutil.rmtree(path, onerror=_remove_readonly)
            if not path.exists():
                return True
        except Exception:
            pass
        time.sleep(0.08 * (attempt + 1))

    # 2. Windows shell fallback via cmd /c rmdir /s /q
    if path.exists() and sys.platform == "win32":
        try:
            subprocess.run(
                f'cmd /c rmdir /s /q "{path}"',
                shell=True,
                capture_output=True,
                timeout=5
            )
            if not path.exists():
                return True
        except Exception:
            pass

    # 3. If still locked (e.g. background antivirus or process), rename it to .trash_*
    # so that the profile name/folder on disk is immediately free!
    if path.exists():
        try:
            trash_name = f".trash_{path.name}_{uuid.uuid4().hex[:8]}"
            trash_path = path.parent / trash_name
            path.rename(trash_path)
            try:
                shutil.rmtree(trash_path, onerror=_remove_readonly)
            except Exception:
                pass
            return True
        except Exception as e:
            print(f"[ProfileStore] Failed to rename locked folder {path.name}: {e}")

    return not path.exists()


def read_json(path: Path, default: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return default

def write_json(path: Path, value: Any) -> None:
    ensure(path.parent)
    temporary = path.parent / f".{path.name}.{uuid.uuid4().hex[:8]}.tmp"
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    for attempt in range(5):
        try:
            os.replace(temporary, path)
            return
        except OSError:
            time.sleep(0.04 * (attempt + 1))
    try:
        shutil.move(str(temporary), str(path))
    except Exception:
        temporary.unlink(missing_ok=True)
        raise

PERFORMANCE_MODS: list[ModData] = [
    ModData(
        project_id="ezclient", slug="ezclient", name="EzClient Core", version_id="v-core", version="2.0.1",
        filename=ezclient_asset_name("26.2"), enabled=True, recommended=True, essential=True,
        icon_url="assets/logo.png", author="EzClient Team", description="EzClient Core Mod – Fenstertitel 'EzClient', Icon, Narrator-Bypass & Auto-Optimierung."
    ),
    ModData(
        project_id="AANobbMI", slug="sodium", name="Sodium", version_id="v2", version="Latest",
        filename="sodium-fabric.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/AANobbMI/295862f4724dc3f78df3447ad6072b2dcd3ef0c9_96.webp", author="CaffeineMC", description="Next-Gen Rendering Engine für maximale FPS."
    ),
    ModData(
        project_id="gvQqBUqZ", slug="lithium", name="Lithium", version_id="v3", version="Latest",
        filename="lithium-fabric.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/gvQqBUqZ/bcc8686c13af0143adf4285d741256af824f70b7_96.webp", author="CaffeineMC", description="Physik-, CPU- und Chunk-Optimierung."
    ),
    ModData(
        project_id="YL57xq9U", slug="iris", name="Iris Shaders", version_id="v-iris", version="Latest",
        filename="iris.jar", enabled=True, recommended=True, essential=True,
        icon_url="https://cdn.modrinth.com/data/YL57xq9U/icon.png", author="Iris Team", description="Shader-Unterstützung mit hoher Performance."
    ),
    ModData(
        project_id="NNAgCjsB", slug="entityculling", name="Entity Culling", version_id="v-culling", version="Latest",
        filename="entityculling-fabric.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/NNAgCjsB/icon.png", author="tr7zw",
        description="Überspringt unsichtbare Entities und Block-Entities für stabilere FPS."
    ),
]

RECOMMENDED_MODS: list[ModData] = [
    ModData(
        project_id="9eGKb6K1", slug="simple-voice-chat", name="Simple Voice Chat", version_id="v-voice", version="Latest",
        filename="voicechat.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/9eGKb6K1/icon.png", author="Henkelmax", description="Proximity Voice Chat im Spiel."
    ),
    ModData(
        project_id="essential", slug="essential", name="Essential Mod", version_id="v7", version="Latest",
        filename="essential.jar", enabled=True, recommended=False, essential=False,
        icon_url="https://cdn.modrinth.com/data/k2ZPuTBm/7f7ac7cf2a46d5f02e9644372c44b3095ad61ffb_96.webp", author="SparkUniverse", description="Welten hosten, Freunde, Chat, Kosmetik & Multi-World Support."
    ),
]

ESSENTIALS_MODS: list[ModData] = PERFORMANCE_MODS + RECOMMENDED_MODS


def performance_mods_for_version(minecraft_version: str) -> list[ModData]:
    """Return only launcher-managed mods that actually support this target."""
    try:
        parsed = tuple(int(part) for part in minecraft_version.split("."))
    except ValueError:
        parsed = (0,)
    templates: list[ModData] = []
    if has_ezclient_asset(minecraft_version):
        templates.append(PERFORMANCE_MODS[0])
    # Sodium, Lithium and Iris have no Fabric releases for the classic
    # Legacy-Fabric targets. Their managed stack starts at Minecraft 1.16.5.
    if parsed >= (1, 16, 5):
        templates.extend(PERFORMANCE_MODS[1:])
    return templates

def preseed_optimized_profile_settings(profile_dir: Path) -> None:
    """Pre-seeds options.txt and config/sodium-options.json with competitive PvP & performance settings."""
    try:
        ensure(profile_dir)
        options_file = profile_dir / "options.txt"
        config_dir = profile_dir / "config"
        ensure(config_dir)

        options: dict[str, str] = {}
        if options_file.exists():
            try:
                for line in options_file.read_text(encoding="utf-8", errors="ignore").splitlines():
                    if ":" in line:
                        k, v = line.split(":", 1)
                        options[k.strip()] = v.strip()
            except Exception:
                pass

        defaults = {
            "graphicsMode": "1",             # Fancy (clean tree leaves)
            "renderDistance": "8",           # 8 Chunks (PvP-Optimum 6-10)
            "simulationDistance": "5",       # 5 Chunks (4-6)
            "entityShadows": "false",        # OFF
            "clouds": "false",               # OFF
            "cloudStatus": "false",          # OFF
            "particles": "2",                # Minimal (2)
            "biomeBlendRadius": "0",         # OFF (0)
            "maxFps": "120",                 # Balanced default; user settings are preserved
            "enableVsync": "false",          # VSync OFF
            "onboardAccessibility": "false", # Skip accessibility onboarding
            "narrator": "0",                 # Narrator OFF (0)
            "skipRealmsNotifications": "true",
            "gamma": "1.0",                  # Full Brightness
            "smoothLighting": "false",       # Smooth Lighting OFF
            "soundCategory_music": "0.05",   # 5% Music Volume on initial profile creation
        }
        for k, v in defaults.items():
            if k not in options:
                options[k] = v

        out_content = "\n".join(f"{k}:{v}" for k, v in options.items()) + "\n"
        options_file.write_text(out_content, encoding="utf-8")

        sodium_file = config_dir / "sodium-options.json"
        if not sodium_file.exists():
            sodium_json = {
                "quality": {
                    "graphics_quality": "DEFAULT",
                    "weather_quality": "FAST",
                    "leaves_quality": "FANCY",
                    "cloud_quality": "OFF",
                    "particles_quality": "MINIMAL",
                    "smooth_lighting": "OFF",
                    "biome_blend": 0,
                    "entity_shadows": False,
                    "vignette": False
                },
                "performance": {
                    "chunk_builder_threads": 0,
                    "always_defer_chunk_updates": True,
                    "use_compact_vertex_format": True,
                    "animate_only_visible_textures": True
                },
                "advanced": {
                    "use_early_z": True
                },
                "notifications": {
                    "hide_donation_prompts": True
                }
            }
            sodium_file.write_text(json.dumps(sodium_json, indent=2), encoding="utf-8")
    except Exception as e:
        print(f"[ProfileStore] Warning: Could not pre-seed profile settings: {e}")

class ProfileStore:
    def __init__(self) -> None:
        self.settings: dict[str, Any] = {
            "close_on_launch": True,
            "check_updates": True,
            "discord_rpc": True,
            "prefer_direct_launch": True,
            "kill_official_launcher": True,
            "minimize_to_tray": True,
            "language": "de",
            "use_minecraft_font": True,
            "last_profile": ""
        }
        self.profiles: list[ProfileData] = []
        clean_trash_folders(PROFILES_DIR)
        self.load()
        # Disk cleanup is local, idempotent and version-gated. Network repair is
        # deliberately deferred to ProfileController's startup worker.
        from backend.services.profile_migration import ProfileMigrationService
        report = ProfileMigrationService(self).run_if_needed()
        if report.changed:
            self.save()
        for error in report.errors:
            print(f"[ProfileMigration] {error}")

    def load(self) -> None:
        data = read_json(STATE_PATH, {})
        self.settings.update(data.get("settings", {}))
        self.profiles = []
        needs_save = False
        icon_map = {pm.slug.lower(): pm.icon_url for pm in ESSENTIALS_MODS}

        seen_ids = set()
        for raw in data.get("profiles", []):
            prof_id = raw.get("id")
            if prof_id in seen_ids:
                needs_save = True
                continue
            seen_ids.add(prof_id)
            raw_mods = list(raw.get("mods", []))
            mods = []
            existing_slugs = set()
            for mod_dict in raw.pop("mods", []):
                valid_keys = {k: v for k, v in mod_dict.items() if k in ModData.__annotations__}
                mod_obj = ModData(**valid_keys)
                slug_l = (mod_obj.slug or "").lower()
                existing_slugs.add(slug_l)
                if slug_l in icon_map and icon_map[slug_l] != mod_obj.icon_url:
                    mod_obj.icon_url = icon_map[slug_l]
                    needs_save = True
                # Make sure ezclient core mod is marked essential
                if slug_l == "ezclient" or (mod_obj.filename and mod_obj.filename.lower() == "ezclient.jar"):
                    if not mod_obj.essential or not mod_obj.enabled:
                        mod_obj.essential = True
                        mod_obj.enabled = True
                        needs_save = True
                mods.append(mod_obj)

            integrated = {str(value).lower() for value in raw.get("integrated_mods", [])}
            profile_type = str(raw.get("profile_type", "")).lower()
            if profile_type not in {"ezclient", "raw"}:
                originally_managed = bool({"ezclient", "ezclient-core"} & integrated)
                had_managed_stack = any(
                    str(item.get("slug", "")).lower() in {"sodium", "lithium", "iris"}
                    and bool(item.get("recommended") or item.get("essential"))
                    for item in raw_mods
                )
                profile_type = "ezclient" if originally_managed or had_managed_stack else "raw"
                raw["profile_type"] = profile_type
                needs_save = True

            if profile_type == "ezclient":
                expected_templates = performance_mods_for_version(str(raw.get("minecraft_version", "")))
                expected_slugs = {mod.slug.lower() for mod in expected_templates}
                old_managed = {str(value).lower() for value in raw.get("managed_core_mods", [])}
                incompatible = old_managed - expected_slugs
                if incompatible:
                    retained = []
                    for mod in mods:
                        slug_l = (mod.slug or mod.project_id or "").lower()
                        if slug_l in incompatible:
                            for path in (PROFILES_DIR / str(raw.get("id", "")) / "mods").glob(f"{Path(mod.filename).stem}*.jar*"):
                                path.unlink(missing_ok=True)
                            needs_save = True
                            continue
                        retained.append(mod)
                    mods = retained
                    existing_slugs = {(mod.slug or "").lower() for mod in mods}
                for core_mod in expected_templates:
                    slug_l = core_mod.slug.lower()
                    if slug_l not in existing_slugs:
                        mods.append(ModData(**asdict(core_mod)))
                        existing_slugs.add(slug_l)
                        needs_save = True
                expected_managed = [m.slug for m in expected_templates]
                if raw.get("managed_core_mods") != expected_managed:
                    raw["managed_core_mods"] = expected_managed
                    needs_save = True
            else:
                raw["managed_core_mods"] = []

            if "user_mods" not in raw:
                managed = {str(value).lower() for value in raw.get("managed_core_mods", [])}
                raw["user_mods"] = [
                    m.slug or m.project_id for m in mods
                    if (m.slug or m.project_id).lower() not in managed
                ]
                needs_save = True

            p_keys = {k: v for k, v in raw.items() if k in ProfileData.__annotations__}
            prof = ProfileData(**p_keys, mods=mods)
            self.profiles.append(prof)

        if needs_save:
            self.save()

    def save(self) -> None:
        data = {
            "settings": self.settings,
            "profiles": [asdict(p) for p in self.profiles]
        }
        try:
            write_json(STATE_PATH, data)
        except OSError as exc:
            print(f"[ProfileStore] Could not persist state.json: {exc}")
        for profile in self.profiles:
            try:
                write_json(profile.path / "profile.json", {
                    "schema_version": 1,
                    "id": profile.id,
                    "name": profile.name,
                    "minecraft_version": profile.minecraft_version,
                    "loader": profile.loader,
                    "profile_type": profile.profile_type,
                    "managed_core_mods": list(profile.managed_core_mods),
                    "user_mods": list(profile.user_mods),
                    "icon": getattr(profile, "icon", "") or "",
                })
            except OSError as exc:
                print(f"[ProfileStore] Could not write {profile.id}/profile.json: {exc}")

    def get_by_id(self, profile_id: str) -> ProfileData | None:
        return next((p for p in self.profiles if p.id == profile_id), None)

    def get_last_or_default(self) -> ProfileData | None:
        last_id = self.settings.get("last_profile")
        if last_id:
            found = self.get_by_id(last_id)
            if found:
                return found
        return self.profiles[0] if self.profiles else None

    def _unique_profile_name(self, requested: str) -> str:
        base = requested.strip() or "Profil"
        existing = {profile.name.casefold() for profile in self.profiles}
        def occupied(candidate: str) -> bool:
            return candidate.casefold() in existing or (PROFILES_DIR / self._profile_folder_name(candidate)).exists()
        if not occupied(base):
            return base
        number = 2
        while occupied(f"{base} ({number})"):
            number += 1
        return f"{base} ({number})"

    @staticmethod
    def _profile_folder_name(name: str) -> str:
        # Keep the visible profile name as the folder name; replace only
        # characters Windows cannot represent in a directory name.
        cleaned = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "-", name).rstrip(" .")
        return cleaned or "Profil"

    def create_profile(self, name: str, version: str, loader: str = "Fabric", preset: str = "ezclient",
                       optimize: bool = True, selected_optional_mods: list[str] | None = None, icon: str = "") -> ProfileData:
        name = self._unique_profile_name(name)
        profile_mods: list[ModData] = []
        managed_templates: list[ModData] = []
        supports_core = loader.lower() == "fabric" and has_ezclient_asset(version)
        profile_type = "ezclient" if preset == "ezclient" and supports_core else ("performance" if loader.lower() == "fabric" and preset in ("performance", "ezclient") else "raw")
        if profile_type in ("ezclient", "performance"):
            managed_templates = performance_mods_for_version(version)
            profile_mods = [ModData(**asdict(m)) for m in managed_templates]
            for mod in profile_mods:
                if mod.slug == "ezclient":
                    mod.filename = ezclient_asset_name(version)
        selected = {str(value).lower() for value in (selected_optional_mods or [])}
        profile_mods.extend(
            ModData(**asdict(m)) for m in RECOMMENDED_MODS
            if m.slug.lower() in selected
        )

        profile = ProfileData(
            id=self._profile_folder_name(name),
            name=name,
            minecraft_version=version,
            loader=loader,
            optimize=optimize,
            mods=profile_mods,
            integrated_mods=[m.slug for m in managed_templates] if profile_type in ("ezclient", "performance") else [],
            profile_type=profile_type,
            managed_core_mods=[m.slug for m in managed_templates] if profile_type in ("ezclient", "performance") else [],
            user_mods=[
                m.slug for m in profile_mods
                if m.slug and m.slug not in {core.slug for core in managed_templates}
            ],
            icon=icon,
        )
        ensure(profile.mods_path)
        ensure(profile.path / "config")
        
        # Pre-seed competitive PvP & Sodium settings immediately
        preseed_optimized_profile_settings(profile.path)

        self.profiles.insert(0, profile)
        self.settings["last_profile"] = profile.id
        self.save()
        return profile

    def duplicate_profile(self, profile_id: str) -> ProfileData | None:
        src = self.get_by_id(profile_id)
        if not src:
            return None
        new_name = self._unique_profile_name(src.name)
        new_id = self._profile_folder_name(new_name)
        dup = ProfileData(
            id=new_id,
            name=new_name,
            minecraft_version=src.minecraft_version,
            optimize=src.optimize,
            loader=src.loader,
            ram_mb=src.ram_mb,
            jvm_args=src.jvm_args,
            mods=[ModData(**asdict(m)) for m in src.mods],
            integrated_mods=list(src.integrated_mods),
            profile_type=src.profile_type,
            managed_core_mods=list(src.managed_core_mods),
            user_mods=list(src.user_mods),
        )
        ensure(dup.mods_path)
        ensure(dup.path / "config")
        preseed_optimized_profile_settings(dup.path)
        self.profiles.insert(0, dup)
        self.settings["last_profile"] = dup.id
        self.save()
        return dup

    def delete_profile(self, profile_id: str) -> bool:
        target = self.get_by_id(profile_id)
        target_path = target.path if target else (PROFILES_DIR / self._profile_folder_name(profile_id))
        if not target and not target_path.exists():
            target_path = PROFILES_DIR / profile_id

        # Coordinate with any active background sync/downloads
        lock = None
        locked = False
        try:
            from backend.services.mod_downloader import _profile_sync_lock
            target_obj = target or ProfileData(id=profile_id, name=profile_id, minecraft_version="26.2")
            lock = _profile_sync_lock(target_obj)
            locked = lock.acquire(timeout=2.0)
        except Exception:
            locked = False
            lock = None

        try:
            if target:
                self.profiles = [p for p in self.profiles if p.id != profile_id]
                if self.settings.get("last_profile") == profile_id:
                    self.settings["last_profile"] = self.profiles[0].id if self.profiles else ""
                self.save()

            success = True
            if target_path.exists():
                success = robust_rmtree(target_path)

            if target:
                alt_path = PROFILES_DIR / self._profile_folder_name(target.name)
                if alt_path.exists() and alt_path != target_path:
                    robust_rmtree(alt_path)

            clean_trash_folders(PROFILES_DIR)
            return success or (target is not None)
        finally:
            if locked and lock:
                lock.release()

    def toggle_mod(self, profile_id: str, mod_id: str) -> bool:
        p = self.get_by_id(profile_id)
        if not p:
            return False
        mid = str(mod_id).strip().lower()
        if mid in ("fabric-api", "fabric api", "p7dr8msh", "ezclient", "ezclient core", "ezclient.jar"):
            return False
        for m in p.mods:
            m_slug = (m.slug or "").lower()
            m_name = (m.name or "").lower()
            m_proj = (m.project_id or "").lower()
            m_file = (m.filename or "").lower()
            if mid in (m_slug, m_name, m_proj, m_file):
                if getattr(m, 'essential', False) or m_slug in ("fabric-api", "ezclient"):
                    return False
                m.enabled = not m.enabled
                self.save()
                return True
        return False

