import json
from pathlib import Path
from typing import Any
import re
import uuid
import shutil
from dataclasses import asdict
from backend.models.types import ProfileData, ModData, STATE_PATH, PROFILES_DIR, ICON_CACHE_DIR

def ensure(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)

ensure(PROFILES_DIR)
ensure(ICON_CACHE_DIR)

def read_json(path: Path, default: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return default

def write_json(path: Path, value: Any) -> None:
    ensure(path.parent)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(path)

PERFORMANCE_MODS: list[ModData] = [
    ModData(
        project_id="ezclient", slug="ezclient", name="EzClient Core", version_id="v-core", version="1.6.4",
        filename="EzClient.jar", enabled=True, recommended=True, essential=True,
        icon_url="", author="EzClient Team", description="EzClient Core Mod – Fenstertitel 'EzClient', Icon, Narrator-Bypass & Auto-Optimierung."
    ),
    ModData(
        project_id="fabric-api", slug="fabric-api", name="Fabric API", version_id="v1", version="0.115.0",
        filename="fabric-api.jar", enabled=True, recommended=True, essential=True,
        icon_url="https://cdn.modrinth.com/data/P7dR8mSH/icon.png", author="FabricMC", description="Essenzielle Schnittstelle und Mod-Basis."
    ),
    ModData(
        project_id="modmenu", slug="modmenu", name="Mod Menu", version_id="v-modmenu", version="Latest",
        filename="modmenu.jar", enabled=True, recommended=True, essential=True,
        icon_url="https://cdn.modrinth.com/data/mOgUt4GM/5a20ed1450a0e1e79a1fe04e61bb4e5878bf1d20.png", author="Prospector", description="Fügt das interaktive 'Mods'-Menü in den Minecraft-Startbildschirm ein."
    ),

    ModData(
        project_id="sodium", slug="sodium", name="Sodium", version_id="v2", version="0.6.13",
        filename="sodium-fabric.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/AANobbMI/295862f4724dc3f78df3447ad6072b2dcd3ef0c9_96.webp", author="CaffeineMC", description="Next-Gen Rendering Engine für maximale FPS."
    ),
    ModData(
        project_id="lithium", slug="lithium", name="Lithium", version_id="v3", version="0.15.0",
        filename="lithium-fabric.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/gvQqBUqZ/bcc8686c13af0143adf4285d741256af824f70b7_96.webp", author="CaffeineMC", description="Physik-, CPU- und Chunk-Optimierung."
    ),
    ModData(
        project_id="ferrite-core", slug="ferrite-core", name="FerriteCore", version_id="v4", version="7.0.2",
        filename="ferritecore.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/uXXizFIs/222a126f26f8f9ae1eb339f3b767677f18bff31f_96.webp", author="malte0811", description="Halbiert den Arbeitsspeicher-Verbrauch."
    ),
    ModData(
        project_id="memoryleakfix", slug="memoryleakfix", name="Memory Leak Fix", version_id="v-mlf", version="Latest",
        filename="memoryleakfix.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/NRjRiSSD/a279c19f9c3574339fa90f675aa8a94f8f6cff92_96.webp", author="fxmorin", description="Behebt Speicherlecks im Minecraft-Client für stabile Frameraten."
    ),
    ModData(
        project_id="immediatelyfast", slug="immediatelyfast", name="ImmediatelyFast", version_id="v5", version="1.3.4",
        filename="immediatelyfast.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/5ZwdcRci/e57b6b451425692ac17ad322d5e14bea686a383a_96.webp", author="RaphiMC", description="HUD und GUI Rendering-Beschleunigung."
    ),
    ModData(
        project_id="entityculling", slug="entityculling", name="Entity Culling", version_id="v6", version="1.7.2",
        filename="entityculling.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/NNAgCjsB/7873452d6cede4daed12da3d7d8c193ab88b4fd6_96.webp", author="tr9zw", description="Überspringt das Rendern verdeckter Mobs."
    ),
    ModData(
        project_id="krypton", slug="krypton", name="Krypton", version_id="v-krypton", version="Latest",
        filename="krypton.jar", enabled=True, recommended=True, essential=False,
        icon_url="https://cdn.modrinth.com/data/fQEb0iXm/3ea60899d060a9286e03b87bfa9e71d0cbe2dde7_96.webp", author="astei", description="Optimiert den Minecraft-Netzwerk-Stack für minimalen Ping & flüssigen Multiplayer."
    ),
    ModData(
        project_id="fabric-language-kotlin", slug="fabric-language-kotlin", name="Fabric Language Kotlin", version_id="v-kotlin", version="Latest",
        filename="fabric-language-kotlin.jar", enabled=True, recommended=True, essential=True,
        icon_url="https://cdn.modrinth.com/data/Ha28R6CL/72c3d74aeb665e45aea93a945a01474cbce3b7da_96.webp", author="FabricMC", description="Kotlin Language Adapter für moderne Mods."
    ),
    ModData(
        project_id="yacl", slug="yacl", name="YetAnotherConfigLib (YACL)", version_id="v-yacl", version="Latest",
        filename="yet_another_config_lib.jar", enabled=True, recommended=True, essential=True,
        icon_url="https://cdn.modrinth.com/data/1eAoo2KR/08c0cd32515e260f4bb20bbc0696510041523f9a_96.webp", author="isXander", description="Konfigurations-Bibliothek für UI & Grafik."
    )
]

ESSENTIALS_MODS: list[ModData] = PERFORMANCE_MODS + [
    ModData(
        project_id="essential", slug="essential", name="Essential Mod", version_id="v7", version="1.3.4",
        filename="essential.jar", enabled=True, recommended=False, essential=False,
        icon_url="https://cdn.modrinth.com/data/k2ZPuTBm/7f7ac7cf2a46d5f02e9644372c44b3095ad61ffb_96.webp", author="SparkUniverse", description="Welten hosten, Freunde, Chat, Kosmetik & Multi-World Support."
    ),
    ModData(
        project_id="YL57xq9U", slug="iris", name="Iris Shaders", version_id="v-iris", version="Latest",
        filename="iris.jar", enabled=True, recommended=False, essential=False,
        icon_url="https://cdn.modrinth.com/data/YL57xq9U/icon.png", author="Iris Team", description="Shader-Unterstützung mit hoher Performance."
    ),
    ModData(
        project_id="9eGKb6K1", slug="simple-voice-chat", name="Simple Voice Chat", version_id="v-voice", version="Latest",
        filename="voicechat.jar", enabled=True, recommended=False, essential=False,
        icon_url="https://cdn.modrinth.com/data/9eGKb6K1/icon.png", author="Henkelmax", description="Ein leistungsstarker Voice-Chat-Mod für Minecraft."
    ),
]

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
            "graphicsMode": "0",             # Fast
            "renderDistance": "8",           # 8 Chunks (PvP-Optimum 6-10)
            "simulationDistance": "5",       # 5 Chunks (4-6)
            "entityShadows": "false",        # OFF
            "clouds": "false",               # OFF
            "cloudStatus": "false",          # OFF
            "particles": "2",                # Minimal (2)
            "biomeBlendRadius": "0",         # OFF (0)
            "maxFps": "260",                 # Max Framerate / Unlimited
            "enableVsync": "false",          # VSync OFF
            "onboardAccessibility": "false", # Skip accessibility onboarding
            "narrator": "0",                 # Narrator OFF (0)
            "skipRealmsNotifications": "true",
            "gamma": "1.0",                  # Full Brightness
            "smoothLighting": "false",       # Smooth Lighting OFF
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
                    "leaves_quality": "FAST",
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
        self.load()

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
                    mod_obj.essential = True
                    mod_obj.enabled = True
                    needs_save = True
                mods.append(mod_obj)

            # Auto-upgrade profile to include EzClient Core mod if missing
            mc_version = raw.get("minecraft_version", "")
            is_26 = mc_version.startswith("26.")
            
            has_ez = any((m.slug or "").lower() == "ezclient" or (m.filename or "").lower().startswith("ezclient") for m in mods)
            
            # Add regular version if missing
            if not any(m.filename.lower() == "ezclient.jar" for m in mods):
                ez_mod = ModData(**asdict(PERFORMANCE_MODS[0]))
                mods.insert(0, ez_mod)
                existing_slugs.add("ezclient")
                needs_save = True

            # Auto-upgrade profile with new essential performance mods if they are missing
            for default_m in PERFORMANCE_MODS:
                if default_m.essential:
                    slug_l = (default_m.slug or "").lower()
                    if slug_l == "ezclient":
                        continue
                    if slug_l not in existing_slugs and not any(m.filename.lower() == default_m.filename.lower() for m in mods):
                        mods.insert(1, ModData(**asdict(default_m)))
                        existing_slugs.add(slug_l)
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
        write_json(STATE_PATH, data)

    def get_by_id(self, profile_id: str) -> ProfileData | None:
        return next((p for p in self.profiles if p.id == profile_id), None)

    def get_last_or_default(self) -> ProfileData | None:
        last_id = self.settings.get("last_profile")
        if last_id:
            found = self.get_by_id(last_id)
            if found:
                return found
        return self.profiles[0] if self.profiles else None

    def create_profile(self, name: str, version: str, loader: str = "Fabric", preset: str = "performance", optimize: bool = True) -> ProfileData:
        slug = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-") or "profile"
        profile_mods: list[ModData] = []
        if preset == "performance":
            profile_mods = [ModData(**asdict(m)) for m in PERFORMANCE_MODS]
        elif preset == "essentials":
            profile_mods = [ModData(**asdict(m)) for m in ESSENTIALS_MODS]

        if version.startswith("26.") and version not in ["26.1", "26.2"]:
            # Only remove EzClient if it's a 26.x version that is NOT 26.1 or 26.2
            profile_mods = [m for m in profile_mods if (m.slug or "").lower() != "ezclient"]

        profile = ProfileData(
            id=f"{slug}-{uuid.uuid4().hex[:8]}",
            name=name,
            minecraft_version=version,
            loader=loader,
            optimize=optimize,
            mods=profile_mods,
            integrated_mods=list({m.slug for m in profile_mods if m.slug} | {m.project_id for m in profile_mods if m.project_id})
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
        new_name = f"{src.name} (Kopie)"
        slug = re.sub(r"[^a-z0-9]+", "-", new_name.lower()).strip("-") or "profile"
        new_id = f"{slug}-{uuid.uuid4().hex[:8]}"
        dup = ProfileData(
            id=new_id,
            name=new_name,
            minecraft_version=src.minecraft_version,
            optimize=src.optimize,
            loader=src.loader,
            ram_mb=src.ram_mb,
            jvm_args=src.jvm_args,
            mods=[ModData(**asdict(m)) for m in src.mods]
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
        if not target:
            return False
        self.profiles = [p for p in self.profiles if p.id != profile_id]
        if self.settings.get("last_profile") == profile_id:
            self.settings["last_profile"] = self.profiles[0].id if self.profiles else ""
        self.save()
        return True

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
