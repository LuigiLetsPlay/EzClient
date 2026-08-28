import sys
import os
from typing import Any
from pathlib import Path
from dataclasses import asdict
import threading
import time
import shutil
import filecmp
import zipfile
import hashlib
from PySide6.QtCore import QObject, Signal, Slot, Property
from backend.models.types import ProfileData, ModData, DATA_DIR, APP_VERSION
from backend.services.store import ProfileStore
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.services.minecraft import detect_launcher, launch_minecraft_official, launcher_install_exit_code, patch_launcher_profile, java_path
from backend.services.mod_downloader import sync_profile_mods
from backend.services.process_watcher import MinecraftWatcher
from backend.services.direct_launch import launch_minecraft_direct
from backend.services.live_log_service import LiveLogService
from backend.services.mod_scanner import InstalledModRegistry
from PySide6.QtWidgets import QFileDialog

class ProfileController(QObject):
    activeProfileChanged = Signal()
    inspectedProfileChanged = Signal()
    profilesChanged = Signal()
    launchStatusChanged = Signal(str, bool)
    gameCrashed = Signal(str, str, str)  # (title, shortError, fullLog)
    settingsChanged = Signal()
    settingSaved = Signal(str)
    hideToTrayRequested = Signal()
    restoreFromTrayRequested = Signal()
    onboardingStepProgress = Signal(float, str, str)
    onboardingFinished = Signal(str)
    
    # New signals for instant install loading status
    modInstallStarted = Signal(str)
    modInstallFinished = Signal(str)
    modUpdatesChanged = Signal()
    ezClientUpdateAvailableChanged = Signal()
    _syncNeeded = Signal()
    _modUpdatesDone = Signal(int, dict)

    def __init__(self, store: ProfileStore, profile_model: ProfileModel, mod_model: ModModel, parent=None):
        super().__init__(parent)
        self._store = store
        self._profile_model = profile_model
        self._mod_model = mod_model
        self._live_log_service = LiveLogService(self)
        self._active_profile: ProfileData | None = self._store.get_last_or_default()
        self._inspected_profile: ProfileData | None = self._active_profile
        self._installed_registry = InstalledModRegistry()
        self._is_launching: bool = False
        self._mod_updates: dict[str, str] = {}
        self._update_check_token: int = 0
        self._ez_client_update_available: bool = False
        self._skip_next_registry_scan: bool = False
        self._syncNeeded.connect(self._sync_models)
        self._modUpdatesDone.connect(self._set_mod_updates)
        # Show the launcher immediately. Reading every JAR (often hundreds of
        # MB) is deferred to a worker instead of blocking the splash screen.
        self._profile_model.set_profiles(self._store.profiles)
        self._mod_model.set_mods(self._active_profile.mods if self._active_profile else [])
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()
        if self._active_profile:
            threading.Thread(target=self._warm_registry_after_startup, daemon=True).start()
        # A launcher update carries the matching EzClient mod. Refresh it in
        # every profile on startup, before any game can be launched.
        threading.Thread(target=self._auto_update_ezclient_mods, daemon=True).start()
        threading.Thread(target=self._sync_managed_profiles_after_startup, daemon=True).start()

    def _sync_managed_profiles_after_startup(self) -> None:
        """Repair missing core files after the local migration has completed."""
        for profile in list(self._store.profiles):
            if profile.profile_type != "ezclient":
                continue
            try:
                sync_profile_mods(profile)
            except Exception as exc:
                print(f"[ProfileController] Core sync failed for {profile.name}: {exc}")
        self._store.save()
        self._syncNeeded.emit()

    def _warm_registry_after_startup(self) -> None:
        profile = self._active_profile
        if not profile:
            return
        try:
            self._installed_registry.scan_directory(profile.mods_path, profile.mods)
            self._skip_next_registry_scan = True
            self._syncNeeded.emit()
        except Exception as exc:
            print(f"[ProfileController] Startup registry scan error: {exc}")

    def _bundled_ezclient_asset(self, filename: str) -> Path:
        root = Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parents[2]))
        appdata = os.environ.get("APPDATA", "")
        candidates = [
            root / "backend" / "assets" / filename,
            root / filename,
            root / "client_mod" / "build" / "libs" / "EzClient-1.8.2.jar",
            Path(appdata) / ".ezclient" / "assets" / filename if appdata else Path.home() / ".ezclient" / "assets" / filename
        ]
        for c in candidates:
            if c.is_file():
                return c
        return root / "backend" / "assets" / filename

    def _auto_update_ezclient_mods(self) -> None:
        """Check in background if installed EzClient JAR differs from the canonical build."""
        try:
            needs_update = False
            for profile in self._store.profiles:
                for mod in profile.mods:
                    is_core = ((mod.slug or "").lower() in ("ezclient", "ezclient-core")
                               or "ezclient" in (mod.filename or "").lower())
                    if not is_core:
                        continue
                    source_name = "EzClient-Lite.jar" if "lite" in (mod.filename or "").lower() else "EzClient.jar"
                    source = self._bundled_ezclient_asset(source_name)
                    destination = profile.mods_path / (mod.filename or source_name)
                    if not source.is_file():
                        continue
                    try:
                        if not destination.exists() or not filecmp.cmp(source, destination, shallow=False) or mod.version != APP_VERSION:
                            needs_update = True
                            break
                    except OSError:
                        pass
                if needs_update:
                    break
            
            if needs_update != self._ez_client_update_available:
                self._ez_client_update_available = needs_update
                self.ezClientUpdateAvailableChanged.emit()
        except Exception as e:
            print(f"[ProfileController] Background update check error: {e}")

    @Property(bool, notify=ezClientUpdateAvailableChanged)
    def ezClientUpdateAvailable(self) -> bool:
        return self.ezClientOutdatedCount > 0

    @Property(int, notify=ezClientUpdateAvailableChanged)
    def ezClientOutdatedCount(self) -> int:
        count = 0
        for profile in self._store.profiles:
            for mod in profile.mods:
                is_core = ((mod.slug or "").lower() in ("ezclient", "ezclient-core")
                           or "ezclient" in (mod.filename or "").lower())
                if not is_core:
                    continue
                source_name = "EzClient-Lite.jar" if "lite" in (mod.filename or "").lower() else "EzClient.jar"
                source = self._bundled_ezclient_asset(source_name)
                destination = profile.mods_path / (mod.filename or source_name)
                if not source.is_file():
                    continue
                try:
                    if not destination.exists() or not filecmp.cmp(source, destination, shallow=False) or mod.version != APP_VERSION:
                        count += 1
                        break
                except OSError:
                    pass
        return count

    @Property(str, constant=True)
    def ezClientLatestVersion(self) -> str:
        return APP_VERSION

    @Slot()
    def applyEzClientUpdates(self) -> None:
        """Apply the bundled EzClient JAR to all profiles."""
        changed = False
        for profile in self._store.profiles:
            for mod in profile.mods:
                is_core = ((mod.slug or "").lower() in ("ezclient", "ezclient-core")
                           or "ezclient" in (mod.filename or "").lower())
                if not is_core:
                    continue
                source_name = "EzClient-Lite.jar" if "lite" in (mod.filename or "").lower() else "EzClient.jar"
                source = self._bundled_ezclient_asset(source_name)
                destination = profile.mods_path / (mod.filename or source_name)
                if not source.is_file():
                    continue
                try:
                    needs_copy = (not destination.exists() or not filecmp.cmp(source, destination, shallow=False))
                    if needs_copy:
                        destination.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(source, destination)
                    if needs_copy or mod.version != APP_VERSION:
                        mod.version = APP_VERSION
                        changed = True
                except OSError as exc:
                    print(f"[ProfileController] EzClient manual update skipped for {profile.name}: {exc}")
        
        if changed:
            self._store.save()
            self._syncNeeded.emit()
            
        self._ez_client_update_available = False
        self.ezClientUpdateAvailableChanged.emit()

    @Property(QObject, constant=True)
    def liveLogService(self) -> QObject:
        return self._live_log_service

    @Property(bool, notify=launchStatusChanged)
    def isLaunching(self) -> bool:
        return self._is_launching

    # ---- Global Settings Properties & Slots ----
    @Property(bool, notify=settingsChanged)
    def closeOnLaunch(self) -> bool:
        return self._store.settings.get("close_on_launch", False)

    @Slot(bool)
    def setCloseOnLaunch(self, val: bool) -> None:
        self._store.settings["close_on_launch"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Launcher-Verhalten gespeichert")

    @Property(bool, notify=settingsChanged)
    def checkUpdates(self) -> bool:
        return self._store.settings.get("check_updates", True)

    @Slot(bool)
    def setCheckUpdates(self, val: bool) -> None:
        self._store.settings["check_updates"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Mod-Update-Einstellung gespeichert")

    @Property(bool, notify=settingsChanged)
    def discordRpc(self) -> bool:
        return self._store.settings.get("discord_rpc", True)

    @Slot(bool)
    def setDiscordRpc(self, val: bool) -> None:
        self._store.settings["discord_rpc"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Discord RPC gespeichert")

    @Property(bool, notify=settingsChanged)
    def preferDirectLaunch(self) -> bool:
        return self._store.settings.get("prefer_direct_launch", True)

    @Slot(bool)
    def setPreferDirectLaunch(self, val: bool) -> None:
        self._store.settings["prefer_direct_launch"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.activeProfileChanged.emit()
        self.settingSaved.emit("Direktstart-Einstellung gespeichert")

    @Property(bool, notify=settingsChanged)
    def killOfficialLauncher(self) -> bool:
        return self._store.settings.get("kill_official_launcher", True)

    @Slot(bool)
    def setKillOfficialLauncher(self, val: bool) -> None:
        self._store.settings["kill_official_launcher"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Launcher-Autokill gespeichert")

    @Property(bool, notify=settingsChanged)
    def minimizeToTray(self) -> bool:
        return self._store.settings.get("minimize_to_tray", True)

    @Slot(bool)
    def setMinimizeToTray(self, val: bool) -> None:
        self._store.settings["minimize_to_tray"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Infobereich-Einstellung gespeichert")

    @Property(str, notify=settingsChanged)
    def language(self) -> str:
        return self._store.settings.get("language", "de")

    @Slot(str)
    def setLanguage(self, val: str) -> None:
        val = str(val).strip().lower()
        if val not in ("de", "en"):
            val = "de"
        self._store.settings["language"] = val
        self._store.save()
        self.settingsChanged.emit()
        msg = "Sprache geändert: Deutsch" if val == "de" else "Language set to English"
        self.settingSaved.emit(msg)

    @Property(bool, notify=settingsChanged)
    def useMinecraftFont(self) -> bool:
        return self._store.settings.get("use_minecraft_font", True)

    @Slot(bool)
    def setUseMinecraftFont(self, val: bool) -> None:
        self._store.settings["use_minecraft_font"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Schriftart-Einstellung gespeichert")

    @Property(bool, notify=settingsChanged)
    def showLiveLogs(self) -> bool:
        return self._store.settings.get("show_live_logs", True)

    @Slot(bool)
    def setShowLiveLogs(self, val: bool) -> None:
        self._store.settings["show_live_logs"] = val
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Live-Log Anzeige gespeichert")

    @Property(str, notify=settingsChanged)
    def appFontMode(self) -> str:
        return self._store.settings.get("app_font_mode", "mixed")

    @Slot(str)
    def setAppFontMode(self, mode: str) -> None:
        self._store.settings["app_font_mode"] = mode
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Schriftart gespeichert")

    @Property(str, notify=settingsChanged)
    def themeColor(self) -> str:
        return str(self._store.settings.get("theme_color", "green"))

    @Slot(str)
    def setThemeColor(self, color: str) -> None:
        if color not in {"green", "purple", "blue", "rose", "orange"}:
            return
        self._store.settings["theme_color"] = color
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Theme-Farbe gespeichert")

    @Property(str, notify=settingsChanged)
    def customBackgroundImage(self) -> str:
        return self._store.settings.get("custom_background_image", "")

    @Property(float, notify=settingsChanged)
    def customBackgroundOpacity(self) -> float:
        return float(self._store.settings.get("custom_background_opacity", 0.60))

    @Property(str, notify=settingsChanged)
    def customBackgroundFillMode(self) -> str:
        return str(self._store.settings.get("custom_background_fill_mode", "PreserveAspectCrop"))

    @Slot(str)
    def setCustomBackgroundImage(self, path_or_url: str) -> None:
        self._store.settings["custom_background_image"] = str(path_or_url).strip()
        self._store.save()
        self.settingsChanged.emit()
        self.settingSaved.emit("Hintergrundbild aktualisiert")

    @Slot(float)
    def setCustomBackgroundOpacity(self, opacity: float) -> None:
        self._store.settings["custom_background_opacity"] = max(0.05, min(1.0, float(opacity)))
        self._store.save()
        self.settingsChanged.emit()

    @Slot(str)
    def setCustomBackgroundFillMode(self, mode: str) -> None:
        self._store.settings["custom_background_fill_mode"] = str(mode)
        self._store.save()
        self.settingsChanged.emit()

    @Slot(result=str)
    def pickBackgroundImage(self) -> str:
        file_path, _ = QFileDialog.getOpenFileName(
            None,
            "Hintergrundbild oder Clip auswählen",
            "",
            "Bilder und Clips (*.png *.jpg *.jpeg *.webp *.mp4 *.webm *.mov);;Alle Dateien (*.*)"
        )
        if file_path:
            self.setCustomBackgroundImage(file_path)
            return file_path
        return ""

    @Slot(result=str)
    def pickBackgroundClip(self) -> str:
        """Choose a supported local video for the Home background."""
        file_path, _ = QFileDialog.getOpenFileName(
            None, "Hintergrund-Clip auswählen", "",
            "Videos (*.mp4 *.webm *.mov);;Alle Dateien (*.*)"
        )
        if file_path:
            self.setCustomBackgroundImage(file_path)
            return file_path
        return ""

    @Slot()
    def _sync_models(self) -> None:
        self._profile_model.set_profiles(self._store.profiles)
        if self._active_profile:
            try:
                if self._skip_next_registry_scan:
                    self._skip_next_registry_scan = False
                else:
                    self._installed_registry.scan_directory(self._active_profile.mods_path, self._active_profile.mods)
                
                # Update descriptions and authors from the scanned JAR metadata
                # so we don't rely on translated/custom descriptions saved in profile.json
                for pm in self._active_profile.mods:
                    pm_slug = (pm.slug or "").lower()
                    pm_fn = (pm.filename or "").lower()
                    for scanned in self._installed_registry.installed_mods:
                        sc_slug = scanned.get("slug", "").lower()
                        sc_fn = scanned.get("filename", "").lower()
                        if (pm_slug and pm_slug == sc_slug) or (pm_fn and pm_fn == sc_fn):
                            if scanned.get("description"):
                                pm.description = scanned["description"]
                            if scanned.get("authors"):
                                pm.author = scanned["authors"]
                            if scanned.get("icon_url") and not pm.icon_url:
                                pm.icon_url = scanned["icon_url"]
                            break
                    
                    if pm.name == "EzClient" or pm_slug == "ezclient":
                        import os
                        from main import get_app_root
                        pm.icon_url = Path(get_app_root() / "ui" / "assets" / "logo.png").as_uri()

            except Exception as e:
                print(f"[ProfileController] Registry scan error: {e}")
            self._mod_model.set_mods(self._active_profile.mods)
        else:
            self._installed_registry.scan_directory(Path("/nonexistent"))
            self._mod_model.set_mods([])
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()
        self.checkModUpdates()

    @Property("QVariantMap", notify=modUpdatesChanged)
    def modUpdates(self) -> dict:
        return dict(self._mod_updates)

    @Property(bool, notify=modUpdatesChanged)
    def hasModUpdates(self) -> bool:
        return bool(self._mod_updates)

    @Slot()
    def checkModUpdates(self) -> None:
        """Fetch the newest compatible build for installed non-core mods."""
        if not self._active_profile or not self.checkUpdates:
            if self._mod_updates:
                self._mod_updates = {}
                self.modUpdatesChanged.emit()
            return
        profile = self._active_profile
        mods = list(profile.mods)
        self._update_check_token += 1
        token = self._update_check_token

        def _check_task():
            from backend.services.modrinth import ModrinthService, select_preferred_version
            from backend.services.curseforge import CurseForgeService
            modrinth = ModrinthService()
            curseforge = CurseForgeService()
            updates: dict[str, str] = {}
            for mod in mods:
                key = mod.project_id or mod.slug or mod.name
                is_ezclient_core = (
                    (mod.slug or "").lower() in ("ezclient", "ezclient-core")
                    or "ezclient" in (mod.filename or "").lower()
                    or (mod.name or "").strip().lower() == "ezclient"
                )
                if not key or is_ezclient_core or (mod.slug or "").lower() == "fabric-api":
                    continue
                if str(mod.version or "").strip().lower() == "latest":
                    continue
                try:
                    versions = []
                    if getattr(mod, "source", "modrinth") == "curseforge":
                        versions = curseforge.get_project_versions(key, mc_version=profile.minecraft_version, loader=profile.loader)
                    if not versions:
                        versions = modrinth.get_project_versions(key, mc_version=profile.minecraft_version, loader=profile.loader)
                    if versions:
                        preferred = select_preferred_version(versions)
                        newest = str((preferred or {}).get("version_number", ""))
                        if newest and newest != str(mod.version or ""):
                            updates[key] = newest
                except Exception as exc:
                    print(f"[ProfileController] Update check skipped for {key}: {exc}")
            self._modUpdatesDone.emit(token, updates)

        threading.Thread(target=_check_task, daemon=True).start()

    @Slot(int, dict)
    def _set_mod_updates(self, token: int, updates: dict) -> None:
        # Several model refreshes may trigger checks while onboarding is still
        # downloading. Ignore stale responses so old data cannot create a row
        # full of phantom Update buttons.
        if token != self._update_check_token:
            return
        self._mod_updates = dict(updates)
        self.modUpdatesChanged.emit()

    @Property("QVariantList", notify=activeProfileChanged)
    def installedMods(self) -> list:
        return self._installed_registry.installed_mods

    @Slot(str, str, result=bool)
    def hasModVersion(self, slug: str, version: str) -> bool:
        if not self._active_profile:
            return False
        slug_clean = slug.strip().lower()
        for m in self._active_profile.mods:
            m_slug = (m.slug or "").lower()
            m_pid = (m.project_id or "").lower()
            if m_slug == slug_clean or m_pid == slug_clean:
                if m.version == version:
                    return True
        return False

    @Property(str, notify=activeProfileChanged)
    def activeId(self) -> str:
        return self._active_profile.id if self._active_profile else ""

    @Property(str, notify=activeProfileChanged)
    def activeProfilePath(self) -> str:
        return str(self._active_profile.path) if self._active_profile else ""

    @Property(str, notify=activeProfileChanged)
    def activeName(self) -> str:
        return self._active_profile.name if self._active_profile else "No Profile"

    @Property("QVariantList", notify=activeProfileChanged)
    def integratedMods(self) -> list:
        from backend.services.store import PERFORMANCE_MODS
        base_slugs = {pm.slug.lower() for pm in PERFORMANCE_MODS if pm.slug} | {pm.project_id.lower() for pm in PERFORMANCE_MODS if pm.project_id}
        base_slugs.update({"ezclient", "ezclient-core", "sodium", "lithium", "essential", "iris", "simple-voice-chat", "zoomify"})
        if self._active_profile:
            saved = set(s.lower() for s in (self._active_profile.integrated_mods or []))
            for m in self._active_profile.mods:
                if getattr(m, 'essential', False) or getattr(m, 'recommended', False):
                    if m.slug: saved.add(m.slug.lower())
                    if m.project_id: saved.add(m.project_id.lower())
            return list(base_slugs | saved)
        return list(base_slugs)

    @Property(str, notify=activeProfileChanged)
    def activeVersion(self) -> str:
        return self._active_profile.minecraft_version if self._active_profile else "26.2"

    @Property(bool, notify=activeProfileChanged)
    def isDirectLaunchReady(self) -> bool:
        if not self._active_profile:
            return False
        from backend.services.direct_launch import find_version_meta
        from backend.services.minecraft import minecraft_dir
        mc = minecraft_dir()
        if not mc.exists():
            return False
        fabric_path, fabric_data, vanilla_path, vanilla_data = find_version_meta(
            mc, self._active_profile.minecraft_version, self._active_profile.loader
        )
        return bool(fabric_data or vanilla_data)

    @Property(str, notify=activeProfileChanged)
    def launchModeName(self) -> str:
        return "Direktstart" if self.isDirectLaunchReady else "Launcher"

    @Property(str, notify=activeProfileChanged)
    def activeLoader(self) -> str:
        return self._active_profile.loader if self._active_profile else "Fabric"

    @Property(int, notify=activeProfileChanged)
    def activeModsCount(self) -> int:
        return len(self._active_profile.mods) if self._active_profile else 0

    @Property(str, notify=activeProfileChanged)
    def activeLastPlayed(self) -> str:
        return self._active_profile.last_played if self._active_profile else "Never"

    @Property(str, notify=activeProfileChanged)
    def activeGameDir(self) -> str:
        return str(self._active_profile.path) if self._active_profile else ""

    @Property(str, notify=activeProfileChanged)
    def javaRuntime(self) -> str:
        return java_path() or "Java 21 (System Standard)"

    @Property(int, notify=activeProfileChanged)
    def activeRamMb(self) -> int:
        return self._active_profile.ram_mb if (self._active_profile and hasattr(self._active_profile, 'ram_mb')) else 4096

    @Property(int, constant=True)
    def systemTotalRamMb(self) -> int:
        return self._get_system_ram_mb()

    @Property(int, constant=True)
    def systemTotalRamGb(self) -> int:
        return max(4, round(self._get_system_ram_mb() / 1024))

    def _get_system_ram_mb(self) -> int:
        try:
            if sys.platform.startswith("win"):
                import ctypes
                class MEMORYSTATUSEX(ctypes.Structure):
                    _fields_ = [
                        ('dwLength', ctypes.c_ulong),
                        ('dwMemoryLoad', ctypes.c_ulong),
                        ('ullTotalPhys', ctypes.c_ulonglong),
                        ('ullAvailPhys', ctypes.c_ulonglong),
                        ('ullTotalPageFile', ctypes.c_ulonglong),
                        ('ullAvailPageFile', ctypes.c_ulonglong),
                        ('ullTotalVirtual', ctypes.c_ulonglong),
                        ('ullAvailVirtual', ctypes.c_ulonglong),
                        ('sullAvailExtendedVirtual', ctypes.c_ulonglong),
                    ]
                stat = MEMORYSTATUSEX()
                stat.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
                if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(stat)):
                    return int(stat.ullTotalPhys / (1024 * 1024))
        except Exception:
            pass
        try:
            import psutil
            return int(psutil.virtual_memory().total / (1024 * 1024))
        except Exception:
            pass
        return 16384

    @Slot(int)
    def setActiveRamMb(self, mb: int) -> None:
        if not self._active_profile:
            return
        self._active_profile.ram_mb = mb
        self._store.save()
        self.activeProfileChanged.emit()
        self.settingSaved.emit(f"RAM auf {round(mb / 1024, 1)} GB zugewiesen")

    @Slot(str)
    def duplicateProfile(self, profile_id: str) -> None:
        dup = self._store.duplicate_profile(profile_id)
        if dup:
            self._sync_models()
            self.profilesChanged.emit()

    # ─────────────────────────────────────────────────────────
    # INSPECTED PROFILE (Detail View without changing Active Play Profile)
    # ─────────────────────────────────────────────────────────
    @Slot(str)
    def inspectProfile(self, profile_id: str) -> None:
        target = self._store.get_by_id(profile_id)
        if target:
            self._inspected_profile = target
            self._mod_model.set_mods(target.mods)
            self.inspectedProfileChanged.emit()

    @Slot()
    def activateInspectedProfile(self) -> None:
        if self._inspected_profile:
            self.selectProfile(self._inspected_profile.id)

    @Property(str, notify=inspectedProfileChanged)
    def inspectedId(self) -> str:
        p = self._inspected_profile or self._active_profile
        return p.id if p else ""

    @Property(str, notify=inspectedProfileChanged)
    def inspectedName(self) -> str:
        p = self._inspected_profile or self._active_profile
        return p.name if p else "Kein Profil"

    @Property(str, notify=inspectedProfileChanged)
    def inspectedVersion(self) -> str:
        p = self._inspected_profile or self._active_profile
        return p.minecraft_version if p else "26.2"

    @Property(str, notify=inspectedProfileChanged)
    def inspectedLoader(self) -> str:
        p = self._inspected_profile or self._active_profile
        return p.loader if p else "Fabric"

    @Property(int, notify=inspectedProfileChanged)
    def inspectedModsCount(self) -> int:
        p = self._inspected_profile or self._active_profile
        return len(p.mods) if p else 0

    @Property(str, notify=inspectedProfileChanged)
    def inspectedLastPlayed(self) -> str:
        p = self._inspected_profile or self._active_profile
        return p.last_played if p else "Never"

    @Property(str, notify=inspectedProfileChanged)
    def inspectedGameDir(self) -> str:
        p = self._inspected_profile or self._active_profile
        return str(p.path) if p else ""

    @Property(bool, notify=inspectedProfileChanged)
    def isInspectedActive(self) -> bool:
        if not self._inspected_profile or not self._active_profile:
            return True
        return self._inspected_profile.id == self._active_profile.id

    def _extract_pack_icon(self, pack_path: Path) -> str:
        """Extracts pack.png/icon.png from a .zip or directory into a persistent cache and returns file:/// URL."""
        try:
            cache_dir = DATA_DIR / "cache" / "pack_icons"
            cache_dir.mkdir(parents=True, exist_ok=True)

            if pack_path.is_dir():
                for name in ("pack.png", "icon.png", "preview.png"):
                    icon_file = pack_path / name
                    if icon_file.is_file():
                        return icon_file.as_uri()
                return ""

            if pack_path.is_file() and (pack_path.suffix.lower() == ".zip" or pack_path.name.endswith(".disabled")):
                stat = pack_path.stat()
                cache_name = hashlib.md5(f"{pack_path.name}_{stat.st_mtime}".encode("utf-8")).hexdigest() + ".png"
                cached_path = cache_dir / cache_name
                if cached_path.is_file():
                    return cached_path.as_uri()

                with zipfile.ZipFile(pack_path, "r") as z:
                    target_file = None
                    for n in z.namelist():
                        if n.lower() in ("pack.png", "icon.png", "preview.png"):
                            target_file = n
                            break
                    if target_file:
                        raw = z.read(target_file)
                        cached_path.write_bytes(raw)
                        return cached_path.as_uri()
        except Exception:
            pass
        return ""

    # ─────────────────────────────────────────────────────────
    # RESOURCE PACKS
    # ─────────────────────────────────────────────────────────
    @Property("QVariantList", notify=inspectedProfileChanged)
    def inspectedResourcePacks(self) -> list:
        p = self._inspected_profile or self._active_profile
        if not p:
            return []
        rp_dir = p.path / "resourcepacks"
        if not rp_dir.is_dir():
            return []
        result = []
        try:
            for item in sorted(rp_dir.iterdir(), key=lambda x: x.name.lower()):
                if (item.is_file() and (item.suffix.lower() == ".zip" or item.suffix.lower() == ".disabled")) or item.is_dir():
                    enabled = not item.name.endswith(".disabled")
                    clean_name = item.name[:-9] if item.name.endswith(".disabled") else item.name
                    if clean_name.lower().endswith(".zip"):
                        clean_name = clean_name[:-4]
                    result.append({
                        "filename": item.name,
                        "name": clean_name,
                        "enabled": enabled,
                        "path": str(item),
                        "icon_url": self._extract_pack_icon(item)
                    })
        except Exception:
            pass
        return result

    @Property("QVariantList", notify=activeProfileChanged)
    def activeResourcePacks(self) -> list:
        return self.inspectedResourcePacks

    @Slot(str)
    def toggleResourcePack(self, filename: str) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        rp_dir = p.path / "resourcepacks"
        target = rp_dir / filename
        if not target.exists():
            return
        try:
            if filename.endswith(".disabled"):
                new_target = rp_dir / filename[:-9]
                target.rename(new_target)
            else:
                new_target = rp_dir / (filename + ".disabled")
                target.rename(new_target)
            self.inspectedProfileChanged.emit()
            self.activeProfileChanged.emit()
        except Exception as e:
            print(f"[ProfileController] Could not toggle resource pack: {e}")

    @Slot()
    def openResourcePacksFolder(self) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        rp_dir = p.path / "resourcepacks"
        rp_dir.mkdir(parents=True, exist_ok=True)
        self._open_local_path(rp_dir)

    # ─────────────────────────────────────────────────────────
    # SHADER PACKS
    # ─────────────────────────────────────────────────────────
    @Property("QVariantList", notify=inspectedProfileChanged)
    def inspectedShaderPacks(self) -> list:
        p = self._inspected_profile or self._active_profile
        if not p:
            return []
        sp_dir = p.path / "shaderpacks"
        if not sp_dir.is_dir():
            return []

        active_shader = ""
        iris_cfg = p.path / "config" / "iris.properties"
        if iris_cfg.is_file():
            try:
                for line in iris_cfg.read_text(encoding="utf-8", errors="ignore").splitlines():
                    if line.strip().startswith("shaderPack="):
                        active_shader = line.split("=", 1)[1].strip()
            except Exception:
                pass
        if not active_shader:
            opt_s = p.path / "optionsshaders.txt"
            if opt_s.is_file():
                try:
                    for line in opt_s.read_text(encoding="utf-8", errors="ignore").splitlines():
                        if line.strip().startswith("currentShaderPack="):
                            active_shader = line.split("=", 1)[1].strip()
                except Exception:
                    pass
        if active_shader.lower() in ("off", "none", "(internal)"):
            active_shader = ""

        result = []
        try:
            for item in sorted(sp_dir.iterdir(), key=lambda x: x.name.lower()):
                if (item.is_file() and (item.suffix.lower() == ".zip" or item.suffix.lower() == ".disabled")) or item.is_dir():
                    is_disabled = item.name.endswith(".disabled")
                    clean_name = item.name[:-9] if is_disabled else item.name
                    if clean_name.lower().endswith(".zip"):
                        clean_name = clean_name[:-4]
                    is_active = (item.name == active_shader or clean_name == active_shader or item.name == (active_shader + ".zip"))
                    result.append({
                        "filename": item.name,
                        "name": clean_name,
                        "enabled": not is_disabled,
                        "isActive": is_active,
                        "path": str(item),
                        "icon_url": self._extract_pack_icon(item)
                    })
        except Exception:
            pass
        return result

    @Slot(str)
    def toggleShaderPack(self, filename: str) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        sp_dir = p.path / "shaderpacks"
        target = sp_dir / filename
        if not target.exists():
            return
        try:
            if filename.endswith(".disabled"):
                new_target = sp_dir / filename[:-9]
                target.rename(new_target)
            else:
                new_target = sp_dir / (filename + ".disabled")
                target.rename(new_target)
            self.inspectedProfileChanged.emit()
            self.activeProfileChanged.emit()
        except Exception as e:
            print(f"[ProfileController] Could not toggle shader pack: {e}")

    @Slot(str)
    def selectShaderPack(self, filename: str) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        iris_cfg = p.path / "config" / "iris.properties"
        try:
            iris_cfg.parent.mkdir(parents=True, exist_ok=True)
            text = f"shaderPack={filename}\nenableShaders=true\n"
            iris_cfg.write_text(text, encoding="utf-8")
        except Exception:
            pass
        self.inspectedProfileChanged.emit()
        self.activeProfileChanged.emit()
        self.settingSaved.emit(f"Shader '{filename}' aktiviert!")

    @Slot()
    def disableShaderPack(self) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        iris_cfg = p.path / "config" / "iris.properties"
        try:
            iris_cfg.parent.mkdir(parents=True, exist_ok=True)
            text = "shaderPack=OFF\nenableShaders=false\n"
            iris_cfg.write_text(text, encoding="utf-8")
        except Exception:
            pass
        self.inspectedProfileChanged.emit()
        self.activeProfileChanged.emit()
        self.settingSaved.emit("Shader deaktiviert")

    @Slot()
    def openShaderPacksFolder(self) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        sp_dir = p.path / "shaderpacks"
        sp_dir.mkdir(parents=True, exist_ok=True)
        self._open_local_path(sp_dir)

    @Slot(str, str)
    def copyMinecraftSettings(self, source_profile_id: str, target_profile_id: str) -> bool:
        src = self._store.get_by_id(source_profile_id)
        dst = self._store.get_by_id(target_profile_id)
        if not src or not dst:
            return False
        
        files_to_copy = ["options.txt", "optionsshaders.txt", "optionsof.txt"]
        configs_to_copy = ["sodium-options.json", "iris.properties", "ezclient.json"]
        
        dst.path.mkdir(parents=True, exist_ok=True)
        for f in files_to_copy:
            src_f = src.path / f
            dst_f = dst.path / f
            if src_f.is_file():
                try:
                    shutil.copy2(src_f, dst_f)
                except Exception:
                    pass
                
        src_cfg = src.path / "config"
        dst_cfg = dst.path / "config"
        if src_cfg.is_dir():
            dst_cfg.mkdir(parents=True, exist_ok=True)
            for c in configs_to_copy:
                src_c = src_cfg / c
                dst_c = dst_cfg / c
                if src_c.is_file():
                    try:
                        shutil.copy2(src_c, dst_c)
                    except Exception:
                        pass
                    
        self.settingSaved.emit(f"Minecraft-Einstellungen von '{src.name}' nach '{dst.name}' übertragen!")
        return True

    @Slot(str)
    def copyActiveMinecraftSettingsTo(self, target_profile_id: str) -> bool:
        if not self._active_profile:
            return False
        return self.copyMinecraftSettings(self._active_profile.id, target_profile_id)

    @Property("QVariantList", notify=profilesChanged)
    def allProfilesList(self) -> list:
        cur_id = self._active_profile.id if self._active_profile else ""
        return [
            {"id": p.id, "name": p.name, "version": p.minecraft_version, "loader": p.loader, "isActive": p.id == cur_id}
            for p in self._store.profiles
        ]

    @Property("QVariantList", notify=activeProfileChanged)
    def installedModIdentifiers(self) -> list:
        if not self._active_profile:
            return []
        ids = []
        for m in self._active_profile.mods:
            if m.project_id:
                ids.append(m.project_id.lower())
            if m.slug:
                ids.append(m.slug.lower())
            if m.name:
                ids.append(m.name.lower())
        return ids

    @Property(bool, notify=profilesChanged)
    def hasProfiles(self) -> bool:
        return len(self._store.profiles) > 0

    @Property(int, notify=profilesChanged)
    def profilesCount(self) -> int:
        return len(self._store.profiles)

    @Property(QObject, constant=True)
    def profileModel(self) -> ProfileModel:
        return self._profile_model

    @Property(QObject, constant=True)
    def modModel(self) -> ModModel:
        return self._mod_model

    @Slot(str)
    def selectProfile(self, profile_id: str) -> None:
        target = self._store.get_by_id(profile_id)
        if target:
            self._active_profile = target
            self._store.settings["last_profile"] = target.id
            self._store.save()
            self._mod_model.set_mods(target.mods)
            self.activeProfileChanged.emit()

    @Slot(str, str, str)
    @Slot(str, str, str, str)
    def createProfile(self, name: str, version: str, loader: str = "Fabric", preset: str = "ezclient") -> None:
        profile = self._store.create_profile(name, version, preset=preset)
        profile.loader = loader
        self._store.save()
        self._profile_model.set_profiles(self._store.profiles)
        self.profilesChanged.emit()
        threading.Thread(target=lambda: sync_profile_mods(profile), daemon=True).start()

    @Slot(str, str, str, str, "QVariantList")
    def createAndOnboard(self, name: str, version: str, loader: str, preset: str,
                         selected_optional_slugs: list | None = None) -> None:
        """Create a typed profile and provision independent JARs concurrently."""
        from backend.services.mod_downloader import provision_profile_mods_parallel
        from backend.services.modrinth import ModrinthService

        selected = [str(value) for value in (selected_optional_slugs or [])]

        def worker():
            profile = None
            try:
                profile = self._store.create_profile(
                    name, version, loader=loader, preset=preset,
                    selected_optional_mods=selected,
                )
                from backend.services.direct_launch import ensure_profile_defaults
                ensure_profile_defaults(profile.path)
                service = ModrinthService()

                def progress(done: int, total: int, mod: ModData, status: str) -> None:
                    self.onboardingStepProgress.emit(done / max(1, total + 1), mod.name, status)

                failures = provision_profile_mods_parallel(profile, service, progress)
                self.onboardingStepProgress.emit(0.9, "Abhängigkeiten", "Prüfe Mod-Abhängigkeiten…")
                sync_profile_mods(profile, service)
                self._active_profile = profile
                self._store.save()
                self._syncNeeded.emit()
                if failures:
                    names = ", ".join(sorted(failures))
                    self.onboardingStepProgress.emit(1.0, "Mit Hinweisen abgeschlossen", f"Nicht installiert: {names}")
                else:
                    self.onboardingStepProgress.emit(1.0, "Fertig", "Profil erfolgreich eingerichtet!")
                time.sleep(0.35)
                self.onboardingFinished.emit(profile.id)
            except Exception as exc:
                print(f"[Onboarding] Profile setup failed: {exc}")
                self.onboardingStepProgress.emit(1.0, "Fehler", f"Profil konnte nicht eingerichtet werden: {exc}")
                if profile is not None:
                    self._store.save()

        threading.Thread(target=worker, daemon=True).start()

    @Slot(str)
    def deleteProfile(self, profile_id: str) -> None:
        self._store.delete_profile(profile_id)
        self._active_profile = self._store.get_last_or_default()
        self._sync_models()
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()

    @Slot(str)
    def toggleMod(self, mod_id: str) -> None:
        if not self._active_profile:
            return
        self._store.toggle_mod(self._active_profile.id, mod_id)
        self._sync_models()
        self.activeProfileChanged.emit()
        # Renaming/enabling JARs is disk work. Keep the toggle immediate and
        # reconcile files asynchronously so the QML scene never stalls.
        profile_ref = self._active_profile
        threading.Thread(
            target=lambda: self._sync_mods_after_change(profile_ref), daemon=True
        ).start()

    def _sync_mods_after_change(self, profile: ProfileData) -> None:
        try:
            sync_profile_mods(profile)
            self._store.save()
            self._syncNeeded.emit()
        except Exception as exc:
            print(f"[ProfileController] Background mod sync error: {exc}")

    @Slot(str, result=list)
    def getModDependencies(self, mod_id: str) -> list:
        if not self._active_profile:
            return []
        svc = ModrinthService()
        return svc.get_dependencies(mod_id, mc_version=self._active_profile.minecraft_version, loader=self._active_profile.loader)

    @Slot(result=bool)
    def isIrisInstalled(self) -> bool:
        """Returns True if Iris or Oculus shader loader mod is installed in active profile."""
        if not self._active_profile:
            return False
        for m in self._active_profile.mods:
            slug = (m.slug or "").lower()
            name = (m.name or "").lower()
            pid = (m.project_id or "").lower()
            if "iris" in slug or "iris" in name or "iris" in pid or "oculus" in slug or "oculus" in name:
                return True
        return False

    @Slot()
    def installIris(self) -> None:
        """Auto-installs Iris Shaders into the active profile."""
        self.installMod("YL57xq9U", "Iris Shaders", "Latest", "iris.jar", "Iris Team", "Shader-Unterstützung mit hoher Performance", "https://cdn.modrinth.com/data/YL57xq9U/icon.png")

    @Slot(str, str, str, str, str, str, str)
    @Slot(str, str, str, str, str, str, str, str)
    def installMod(self, mod_id: str, name: str, version: str, filename: str, author: str, description: str, icon_url: str, source: str = "modrinth") -> None:
        if not self._active_profile:
            return
        mid = str(mod_id).strip().lower()
        name_clean = str(name).strip().lower()
        clean_fn = str(filename).strip().lower() if filename else ""

        for m in self._active_profile.mods:
            if (m.project_id and m.project_id.lower() == mid) or \
               (m.slug and m.slug.lower() == mid) or \
               (m.name and m.name.lower() == name_clean) or \
               (clean_fn and m.filename and m.filename.lower() == clean_fn):
                # Store "Latest" so the background sync fetches the newest
                # compatible build instead of merely re-enabling an old JAR.
                m.version = "Latest"
                m.enabled = True
                identity = m.slug or m.project_id
                if identity and identity.lower() not in {value.lower() for value in self._active_profile.managed_core_mods}:
                    if identity not in self._active_profile.user_mods:
                        self._active_profile.user_mods.append(identity)
                self._store.save()
                self._sync_models()
                self.profilesChanged.emit()
                self.activeProfileChanged.emit()
                self._mod_updates.pop(m.project_id or m.slug or m.name, None)
                self.modUpdatesChanged.emit()
                self.settingSaved.emit(f"Mod wird auf die neueste Version aktualisiert: {m.name}")
                p_ref = self._active_profile
                threading.Thread(target=lambda: sync_profile_mods(p_ref), daemon=True).start()
                return

        new_mod = ModData(
            project_id=mod_id,
            slug=mod_id,
            name=name,
            version_id="",
            version="Latest",
            filename=filename or f"{mod_id}.jar",
            enabled=True,
            essential=False,
            icon_url=icon_url,
            author=author or ("CurseForge" if source == "curseforge" else "Modrinth"),
            description=description,
            source=source
        )
        self._active_profile.mods.append(new_mod)
        if mod_id not in self._active_profile.user_mods:
            self._active_profile.user_mods.append(mod_id)

        # Check and auto-install required dependencies from Modrinth if Modrinth mod
        def _install_task():
            self.modInstallStarted.emit(mod_id)
            dep_names = []
            if source == "modrinth":
                from backend.services.modrinth import ModrinthService
                svc = ModrinthService()
                try:
                    deps = svc.get_dependencies(mod_id, mc_version=self._active_profile.minecraft_version, loader=self._active_profile.loader)
                    existing_slugs = {(m.slug or "").lower() for m in self._active_profile.mods} | {(m.project_id or "").lower() for m in self._active_profile.mods}
                    for d in deps:
                        d_slug = (d.get("slug") or d.get("project_id") or "").lower()
                        if d_slug and d_slug not in existing_slugs and not self._installed_registry.is_installed(slug=d_slug):
                            dep_mod = ModData(
                                project_id=d.get("project_id", d_slug),
                                slug=d.get("slug", d_slug),
                                name=d.get("name", d_slug),
                                version_id="",
                                version="Latest",
                                filename=f"{d_slug}.jar",
                                enabled=True,
                                essential=False,
                                icon_url=d.get("icon_url", ""),
                                author=d.get("author", "Modrinth"),
                                description=d.get("description", "Automatisch installierte Abhängigkeit"),
                                source="modrinth"
                            )
                            self._active_profile.mods.append(dep_mod)
                            dep_identity = dep_mod.slug or dep_mod.project_id
                            if dep_identity and dep_identity not in self._active_profile.user_mods:
                                self._active_profile.user_mods.append(dep_identity)
                            dep_names.append(d.get("name", d_slug))
                            existing_slugs.add(d_slug)
                except Exception as e:
                    print(f"[ProfileController] Dependency check error for {name}: {e}")

            self._store.save()
            
            # Emit signals to main thread instead of calling _sync_models directly
            # to prevent UI blocking and thread crashes
            self._syncNeeded.emit()
            
            if dep_names:
                self.settingSaved.emit(f"✓ {name} & {len(dep_names)} benötigte Abhängigkeiten ({', '.join(dep_names)}) installiert!")
            else:
                self.settingSaved.emit(f"✓ Mod installiert: {name}")

            # Download jar and all dependencies in background immediately
            p_ref = self._active_profile
            try:
                from backend.services.mod_downloader import sync_profile_mods
                sync_profile_mods(p_ref)
                self._store.save()
                self._syncNeeded.emit()
            except Exception as e:
                print(f"[ProfileController] Instant download error: {e}")
                
            self.modInstallFinished.emit(mod_id)

        threading.Thread(target=_install_task, daemon=True).start()

    @Slot(str)
    @Slot(str, str)
    def uninstallMod(self, mod_id: str, mod_name: str = "") -> None:
        if not self._active_profile:
            return
        targets = {str(mod_id).strip().lower()}
        if mod_name:
            targets.add(str(mod_name).strip().lower())
        targets.discard("")
        managed = {str(value).lower() for value in self._active_profile.managed_core_mods}
        if targets & managed:
            self.settingSaved.emit("Core-Mods werden vom EzClient-Profil verwaltet und können nicht entfernt werden.")
            return
        removed_name = ""
        new_mods = []
        for m in self._active_profile.mods:
            m_identifiers = {
                (m.project_id or "").strip().lower(),
                (m.slug or "").strip().lower(),
                (m.name or "").strip().lower(),
                (m.filename or "").strip().lower()
            }
            m_identifiers.discard("")
            if m_identifiers & targets:
                removed_name = m.name
            else:
                new_mods.append(m)
        self._active_profile.mods = new_mods
        self._active_profile.user_mods = [
            value for value in self._active_profile.user_mods
            if str(value).lower() not in targets
        ]
        self._store.save()

        # Remove jar from disk in background to prevent UI hang
        def _uninstall_task():
            try:
                for target in targets:
                    for f in self._active_profile.mods_path.glob(f"*{target}*"):
                        try:
                            f.unlink(missing_ok=True)
                        except Exception:
                            pass
            except Exception as ex:
                print(f"[ProfileController] Error unlinking mod jars: {ex}")

            self._syncNeeded.emit()
            if removed_name:
                self.settingSaved.emit(f"Mod gelöscht: {removed_name}")

        threading.Thread(target=_uninstall_task, daemon=True).start()

    @Slot(str, str, str)
    @Slot(str, str, str, str)
    def switchModVersion(self, mod_id: str, version_number: str, filename: str = "", download_url: str = "") -> None:
        if not self._active_profile:
            return
        mid = str(mod_id).strip().lower()
        target_mod = None
        for m in self._active_profile.mods:
            if (m.project_id and m.project_id.lower() == mid) or \
               (m.slug and m.slug.lower() == mid) or \
               (m.name and m.name.lower() == mid):
                target_mod = m
                break
        if not target_mod:
            return

        old_fn = target_mod.filename
        target_mod.version = version_number
        if filename:
            target_mod.filename = filename
        self._store.save()
        self._syncNeeded.emit()

        def _switch_task():
            import urllib.request
            try:
                if old_fn and filename and old_fn != filename:
                    old_path = self._active_profile.mods_path / old_fn
                    if old_path.exists():
                        old_path.unlink(missing_ok=True)
                
                if download_url and filename:
                    dest = self._active_profile.mods_path / filename
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    req = urllib.request.Request(download_url, headers={"User-Agent": "EzClient/1.8.2"})
                    with urllib.request.urlopen(req, timeout=30) as resp:
                        dest.write_bytes(resp.read())
                else:
                    from backend.services.mod_downloader import sync_profile_mods
                    sync_profile_mods(self._active_profile)
                self.settingSaved.emit(f"✓ {target_mod.name} auf Version {version_number} gewechselt!")
            except Exception as ex:
                print(f"[ProfileController] Error switching mod version: {ex}")
                self.settingSaved.emit(f"Fehler beim Versionswechsel: {ex}")
            finally:
                self._syncNeeded.emit()

        threading.Thread(target=_switch_task, daemon=True).start()

    @Slot(str, str)
    def updateModVersion(self, mod_id: str, new_version: str) -> None:
        if not self._active_profile:
            return
        mid = str(mod_id).strip().lower()
        for m in self._active_profile.mods:
            if (m.project_id and m.project_id.lower() == mid) or \
               (m.slug and m.slug.lower() == mid) or \
               (m.name and m.name.lower() == mid):
                m.version = "Latest"
                self._store.save()
                self._mod_updates.pop(m.project_id or m.slug or m.name, None)
                self.modUpdatesChanged.emit()
                
                def _update_task():
                    p_ref = self._active_profile
                    old_jar = p_ref.mods_path / m.filename
                    try:
                        if old_jar.exists():
                            old_jar.unlink()
                    except OSError as exc:
                        print(f"[ProfileController] Could not replace {old_jar.name}: {exc}")
                    sync_profile_mods(p_ref)
                    self._store.save()
                    self._syncNeeded.emit()
                    self.settingSaved.emit(f"Aktualisiert: {m.name} ({m.version})")

                threading.Thread(target=_update_task, daemon=True).start()
                return

    @Slot()
    def updateAllMods(self) -> None:
        """Replace every non-core mod with the newest compatible build."""
        if not self._active_profile:
            return
        update_keys = set(self._mod_updates)
        mods = [m for m in self._active_profile.mods
                if (m.project_id or m.slug or m.name) in update_keys
                and (m.slug or "").lower() not in ("ezclient", "fabric-api")]
        if not mods:
            return
        for mod in mods:
            mod.version = "Latest"
        self._store.save()
        self._mod_updates = {}
        self.modUpdatesChanged.emit()

        def _update_all_task():
            for mod in mods:
                try:
                    old_jar = self._active_profile.mods_path / mod.filename
                    if old_jar.exists():
                        old_jar.unlink()
                except OSError:
                    pass
            sync_profile_mods(self._active_profile)
            self._store.save()
            self._syncNeeded.emit()
            self.settingSaved.emit(f"{len(mods)} Mods auf die neuesten Versionen aktualisiert")

        threading.Thread(target=_update_all_task, daemon=True).start()

    @Slot()
    def updateEzClient(self) -> None:
        """Refresh the bundled EzClient JAR in every launcher profile that uses it."""
        def worker() -> None:
            updated = 0
            for profile in self._store.profiles:
                for mod in profile.mods:
                    if (mod.slug or "").lower() not in ("ezclient", "ezclient-core") and "ezclient" not in (mod.filename or "").lower():
                        continue
                    source_name = "EzClient-Lite.jar" if "lite" in (mod.filename or "").lower() else "EzClient.jar"
                    source = self._bundled_ezclient_asset(source_name)
                    if source.is_file():
                        profile.mods_path.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(source, profile.mods_path / (mod.filename or source_name))
                        mod.version = APP_VERSION
                        updated += 1
            self._store.save()
            self._syncNeeded.emit()
            self.settingSaved.emit(f"EzClient in {updated} Profil(en) aktualisiert")
        threading.Thread(target=worker, daemon=True).start()

    @Slot(str, result=bool)
    @Slot(str, str, result=bool)
    @Slot(str, str, str, result=bool)
    @Slot(str, str, str, str, result=bool)
    def isModInstalled(self, project_id: str = "", slug: str = "", name: str = "", filename: str = "") -> bool:
        """Cross-platform check: returns True if this mod is installed in active profile."""
        if not self._active_profile:
            return False
        return self._installed_registry.is_installed(
            project_id=project_id,
            slug=slug,
            name=name,
            filename=filename
        )

    @Slot()
    def launchActiveProfile(self) -> None:
        if not self._active_profile:
            self.launchStatusChanged.emit("Kein Profil ausgewählt.", True)
            return

        self._is_launching = True
        self.launchStatusChanged.emit("Mods werden synchronisiert & geprüft…", False)

        def worker():
            def launch_status(message: str, error: bool = False) -> None:
                self.launchStatusChanged.emit(message, error)
                self._live_log_service.append_system_message(
                    message, "ERROR" if error else "INFO"
                )

            try:
                # 1. Sync all enabled mods to profile.mods_path
                sync_profile_mods(self._active_profile, status_callback=launch_status)
                
                # 2. Check if Direct Launch is preferred
                proc = None
                if self.preferDirectLaunch:
                    self.launchStatusChanged.emit("Minecraft wird vorbereitet (Token & Java)…", False)
                    proc = launch_minecraft_direct(
                        self._active_profile,
                        status_callback=launch_status
                    )

                if proc is not None:
                    # Direct Launch started!
                    self._live_log_service.attach_process(
                        proc,
                        self._active_profile.path / "ezclient_latest_run.log",
                        instance_name=self._active_profile.name,
                        loader_version=f"{self._active_profile.loader} {self._active_profile.minecraft_version}"
                    )

                    if self.minimizeToTray:
                        self.hideToTrayRequested.emit()
                    self.launchStatusChanged.emit(f"Minecraft läuft · {self._active_profile.name}", False)

                    start_time = time.time()
                    while proc.poll() is None:
                        time.sleep(0.5)

                    duration = time.time() - start_time
                    exit_code = proc.returncode
                    self._is_launching = False
                    was_intentional_stop = self._live_log_service.intentional_stop
                    self._live_log_service.detach_process()

                    # Always bring the launcher window back to front when game exits
                    self.restoreFromTrayRequested.emit()

                    # Check for fresh crash reports generated during this specific run
                    crash_reports_dir = self._active_profile.path / "crash-reports"
                    fresh_crash_files = []
                    if crash_reports_dir.exists():
                        for f in crash_reports_dir.glob("crash-*.txt"):
                            try:
                                if f.stat().st_mtime >= (start_time - 1.0):
                                    fresh_crash_files.append(f)
                            except Exception:
                                pass
                        fresh_crash_files.sort(key=lambda f: f.stat().st_mtime, reverse=True)

                    # Only treat as crash if:
                    # 1. User did not deliberately stop the instance
                    # 2. AND either a fresh crash report exists, or duration was under 4s (failed to launch), or fatal exit code
                    is_crash = False
                    if not was_intentional_stop:
                        if fresh_crash_files:
                            is_crash = True
                        elif duration < 4.0 and exit_code != 0:
                            is_crash = True
                        elif exit_code not in (0, 1, 130, 143, -15, -9, 259) and duration < 10.0:
                            is_crash = True

                    if is_crash:
                        error_title = "Minecraft konnte nicht gestartet werden" if duration < 4.0 else "Minecraft ist abgestürzt"
                        full_log = ""
                        short_err = f"Prozess wurde mit Fehlercode {exit_code} beendet."

                        if fresh_crash_files:
                            try:
                                full_log = fresh_crash_files[0].read_text(encoding="utf-8", errors="replace")
                                for line in full_log.splitlines():
                                    if "Description:" in line or "java.lang." in line or "error:" in line.lower():
                                        short_err = line.strip()
                                        break
                            except Exception:
                                pass

                        if not full_log:
                            log_file = self._active_profile.path / "ezclient_latest_run.log"
                            if log_file.exists():
                                try:
                                    full_log = log_file.read_text(encoding="utf-8", errors="replace")
                                    lines = [l for l in full_log.strip().splitlines() if l.strip()]
                                    short_err = "\n".join(lines[-4:])
                                except Exception:
                                    pass

                        self.launchStatusChanged.emit(f"Minecraft Absturz (Code {exit_code})", True)
                        self.gameCrashed.emit(error_title, short_err, full_log or f"Prozess wurde nach {duration:.1f}s mit Exit-Code {exit_code} beendet.")
                        return

                    self.launchStatusChanged.emit(f"Spiel beendet ({self._active_profile.name})", False)
                    return

                # Direct mode is self-contained. Never fall back to installing
                # or opening the official launcher if its preparation failed.
                if self.preferDirectLaunch:
                    self._is_launching = False
                    launch_status("Spielstart konnte nicht vorbereitet werden. Bitte Microsoft-Anmeldung und Logs prüfen.", True)
                    return

                # 3. Legacy fallback: Official Minecraft Launcher with Autokill
                self.launchStatusChanged.emit("Minecraft Launcher wird gestartet…", False)
                patch_launcher_profile(self._active_profile)
                def launcher_status(message: str, error: bool = False) -> None:
                    self.launchStatusChanged.emit(message, error)
                    self._live_log_service.append_system_message(message, "ERROR" if error else "INFO")

                launcher_started = launch_minecraft_official(
                    status_callback=launcher_status
                )
                if not launcher_started:
                    # The MSI is intentionally silent.  Wait briefly and then
                    # open the newly installed launcher automatically instead
                    # of presenting Windows' minecraft:-protocol dialog.
                    self.launchStatusChanged.emit("Warte auf die Launcher-Installation…", False)
                    self._live_log_service.append_system_message("Warte auf die Launcher-Installation…")
                    for _ in range(90):
                        time.sleep(2)
                        exit_code = launcher_install_exit_code()
                        if exit_code is not None and exit_code != 0:
                            raise RuntimeError(
                                f"Die Minecraft-Launcher-Installation ist fehlgeschlagen (MSI-Code {exit_code})."
                            )
                        installed, _, _ = detect_launcher()
                        if installed:
                            self.launchStatusChanged.emit("Minecraft Launcher wird gestartet…", False)
                            launch_minecraft_official()
                            launcher_started = True
                            break
                    if not launcher_started:
                        self._is_launching = False
                        self.launchStatusChanged.emit(
                            "Die Launcher-Installation benötigt noch Zeit. Bitte nach Abschluss erneut auf Starten drücken.",
                            False,
                        )
                        return

                if self.minimizeToTray:
                    self.hideToTrayRequested.emit()

                def on_game_started():
                    self.launchStatusChanged.emit(f"Minecraft läuft ({self._active_profile.name})", False)

                def on_game_exited():
                    self._is_launching = False
                    self.restoreFromTrayRequested.emit()
                    self.launchStatusChanged.emit(f"Spiel beendet ({self._active_profile.name})", False)

                # Watch for game start, autokill official launcher window, restore on exit
                watcher = MinecraftWatcher(
                    on_started=on_game_started,
                    on_exited=on_game_exited,
                    kill_launcher=self.killOfficialLauncher
                )
                watcher.start(timeout_seconds=180)

            except Exception as exc:
                print(f"[Launch Error] {exc}")
                self._is_launching = False
                if self.minimizeToTray:
                    self.restoreFromTrayRequested.emit()
                self.launchStatusChanged.emit(f"Fehler: {exc}", True)
                self.gameCrashed.emit("Start-Fehler", str(exc), str(exc))

        t = threading.Thread(target=worker, daemon=True)
        t.start()

    @Slot(str)
    def copyToClipboard(self, text: str) -> None:
        """Copies given text to Windows / OS clipboard and shows toast."""
        try:
            from PySide6.QtGui import QGuiApplication
            clipboard = QGuiApplication.clipboard()
            if clipboard:
                clipboard.setText(text)
                self.settingSaved.emit("Fehler in Zwischenablage kopiert!")
        except Exception as e:
            print(f"[ProfileController] Clipboard error: {e}")

    @Slot(str)
    def openFolder(self, path_str: str) -> None:
        """Opens folder in Windows Explorer / OS File Manager."""
        try:
            p = Path(path_str)
            if not p.exists():
                p.mkdir(parents=True, exist_ok=True)
            if sys.platform == "win32":
                os.startfile(str(p))
            elif sys.platform == "darwin":
                import subprocess
                subprocess.Popen(["open", str(p)])
            else:
                import subprocess
                subprocess.Popen(["xdg-open", str(p)])
        except Exception as e:
            print(f"[ProfileController] openFolder error: {e}")

    @Slot(str, result="QVariantList")
    def getDependentMods(self, mod_id_or_slug: str) -> list[str]:
        """Check if deleting this mod would break dependencies of other installed mods using deep metadata inspection."""
        if not self._active_profile:
            return []
        return self._installed_registry.get_dependent_mods(mod_id_or_slug)

    @Slot(str, result="QVariantList")
    def checkDependentMods(self, mod_id: str) -> list[str]:
        """Compatibility alias for getDependentMods."""
        return self.getDependentMods(mod_id)

    @Slot(bool)
    def enableAllMods(self, enable: bool) -> None:
        """Bulk enable or disable all non-core mods in the active profile."""
        if not self._active_profile:
            return
        for m in self._active_profile.mods:
            m_slug = (m.slug or "").lower()
            if not getattr(m, 'essential', False) and m_slug not in ("fabric-api", "fabric api"):
                m.enabled = enable
        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()
        p_ref = self._active_profile
        threading.Thread(target=lambda: sync_profile_mods(p_ref), daemon=True).start()

    @Slot()
    def duplicateActiveProfile(self) -> None:
        """Duplicate the active profile."""
        if not self._active_profile:
            return
        new_p = self._store.duplicate_profile(self._active_profile.id)
        if new_p:
            self._sync_models()
            self.profilesChanged.emit()

    @Slot(str)
    def openFolder(self, path_str: str) -> None:
        import os, sys, subprocess
        p = path_str if path_str else (str(self._active_profile.path) if self._active_profile else "")
        if not p:
            return
        os.makedirs(p, exist_ok=True)
        if sys.platform.startswith("win"):
            os.startfile(p)
        elif sys.platform == "darwin":
            subprocess.Popen(["open", p])
        else:
            subprocess.Popen(["xdg-open", p])

    @Slot()
    def openScreenshotsFolder(self) -> None:
        import os
        appdata = os.getenv("APPDATA")
        mc_p = Path(appdata) / ".minecraft" if appdata else Path.home() / ".minecraft"
        target = mc_p / "screenshots"
        if self._active_profile and (self._active_profile.path / "screenshots").exists():
            target = self._active_profile.path / "screenshots"
        self.openFolder(str(target))

    @Slot()
    def openLogsFolder(self) -> None:
        import os
        appdata = os.getenv("APPDATA")
        mc_p = Path(appdata) / ".minecraft" if appdata else Path.home() / ".minecraft"
        target = mc_p / "logs"
        if self._active_profile and (self._active_profile.path / "logs").exists():
            target = self._active_profile.path / "logs"
        self.openFolder(str(target))

    @Slot()
    def detectJava(self) -> None:
        from backend.services.minecraft import java_path
        j = java_path()
        if j:
            self.settingSaved.emit(f"Java gefunden: {Path(j).name}")
        else:
            self.settingSaved.emit("Java-Laufzeitumgebung erkannt und bereit")
