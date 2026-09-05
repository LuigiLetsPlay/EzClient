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
from backend.services.store import ProfileStore, ezclient_asset_name, has_ezclient_asset
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.services.minecraft import detect_launcher, launch_minecraft_official, launcher_install_exit_code, patch_launcher_profile, java_path, minecraft_dir
from backend.services.mod_downloader import sync_profile_mods
from backend.services.process_watcher import MinecraftWatcher
from backend.services.direct_launch import launch_minecraft_direct
from backend.services.live_log_service import LiveLogService
from backend.services.minecraft_versions import FROZEN_EZCLIENT_VERSION, is_frozen_ezclient_version
from backend.services.mod_scanner import InstalledModRegistry
from backend.services.crash_doctor import CrashDoctorService, CrashDiagnosis
from PySide6.QtWidgets import QFileDialog


class ProfileController(QObject):
    activeProfileChanged = Signal()
    inspectedProfileChanged = Signal()
    profilesChanged = Signal()
    launchStatusChanged = Signal(str, bool)
    gameCrashed = Signal(str, str, str)  # (title, shortError, fullLog)
    crashDiagnosisChanged = Signal()
    crashFixStarted = Signal()
    crashFixProgress = Signal(float, str)
    crashFixCompleted = Signal(bool, str)
    settingsChanged = Signal()
    javaRuntimesChanged = Signal()
    gameVersionFamiliesChanged = Signal()
    settingSaved = Signal(str)
    hideToTrayRequested = Signal()
    restoreFromTrayRequested = Signal()
    onboardingStepProgress = Signal(float, str, str)
    onboardingFinished = Signal(str)
    
    # Mod & Modpack signals
    modInstallStarted = Signal(str)
    modInstallFinished = Signal(str)
    modpackInstallProgress = Signal(float, str)
    modpackInstallFinished = Signal(str, bool, str)
    modUpdatesChanged = Signal()
    ezClientUpdateAvailableChanged = Signal()
    noriskProfilesChanged = Signal()
    noriskImportProgress = Signal(float, str)
    noriskImportFinished = Signal(str, bool, str)
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
        self._crash_doctor = CrashDoctorService()
        self._crash_diagnosis: CrashDiagnosis = CrashDiagnosis()
        self._is_fixing_crash: bool = False
        self._crash_fix_status: str = ""
        self._crash_fix_success: bool = False
        self._norisk_profiles: list[dict[str, Any]] = []
        self._is_launching: bool = False
        self._mod_updates: dict[str, str] = {}
        self._update_check_token: int = 0
        self._ez_client_update_available: bool = False
        self._skip_next_registry_scan: bool = False
        self._syncNeeded.connect(self._sync_models)

        # Normalize any legacy profile mod filenames and auto-update
        self._normalize_versioned_ezclient_assets()
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()
        if self._active_profile:
            threading.Thread(target=self._warm_registry_after_startup, daemon=True).start()
        threading.Thread(target=self._auto_update_ezclient_mods, daemon=True).start()
        threading.Thread(target=self._sync_managed_profiles_after_startup, daemon=True).start()

    def _normalize_versioned_ezclient_assets(self) -> None:
        """Migrate old generic filenames and detach the mod from unsupported MC versions."""
        changed = False
        for profile in self._store.profiles:
            expected = ezclient_asset_name(profile.minecraft_version)
            expected_version = FROZEN_EZCLIENT_VERSION if is_frozen_ezclient_version(profile.minecraft_version) else APP_VERSION
            available = has_ezclient_asset(profile.minecraft_version) and self._bundled_ezclient_asset(expected).is_file()
            retained = []
            for mod in profile.mods:
                is_ezclient = (mod.slug or mod.project_id or "").lower() in ("ezclient", "ezclient-core")
                if not is_ezclient:
                    retained.append(mod)
                    continue
                if available:
                    if mod.filename != expected or mod.version != expected_version:
                        mod.filename = expected
                        mod.version = expected_version
                        changed = True
                    retained.append(mod)
                else:
                    candidate = profile.mods_path / (mod.filename or expected)
                    if candidate.is_file():
                        candidate.unlink(missing_ok=True)
                    changed = True
            profile.mods = retained
            if not available:
                profile.profile_type = "raw"
                old_managed = list(profile.managed_core_mods)
                old_integrated = list(profile.integrated_mods)
                profile.managed_core_mods = [item for item in old_managed if item.lower() not in ("ezclient", "ezclient-core")]
                profile.integrated_mods = [item for item in old_integrated if item.lower() not in ("ezclient", "ezclient-core")]
                changed = changed or old_managed != profile.managed_core_mods or old_integrated != profile.integrated_mods
        if changed:
            self._store.save()

    def _sync_managed_profiles_after_startup(self) -> None:
        """Repair missing core files after the local migration has completed."""
        for profile in list(self._store.profiles):
            if profile.profile_type not in ("ezclient", "performance"):
                continue
            try:
                sync_profile_mods(profile)
            except Exception as exc:
                print(f"[ProfileController] Core sync failed for {profile.name}: {exc}")
        self._store.save()
        try:
            self._syncNeeded.emit()
        except RuntimeError:
            pass
        self._auto_update_ezclient_mods()

    def _warm_registry_after_startup(self) -> None:
        profile = self._active_profile
        if not profile:
            return
        try:
            self._installed_registry.scan_directory(profile.mods_path, profile.mods)
            self._skip_next_registry_scan = True
            try:
                self._syncNeeded.emit()
            except RuntimeError:
                pass
        except Exception as exc:
            print(f"[ProfileController] Startup registry scan error: {exc}")

    def _bundled_ezclient_asset(self, filename: str) -> Path:
        root = Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parents[2]))
        appdata = os.environ.get("APPDATA", "")
        candidates = [
            root / "backend" / "assets" / filename,
            root / filename,
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
                expected_version = FROZEN_EZCLIENT_VERSION if is_frozen_ezclient_version(profile.minecraft_version) else APP_VERSION
                for mod in profile.mods:
                    is_core = ((mod.slug or "").lower() in ("ezclient", "ezclient-core")
                               or "ezclient" in (mod.filename or "").lower())
                    if not is_core:
                        continue
                    source_name = ezclient_asset_name(profile.minecraft_version)
                    source = self._bundled_ezclient_asset(source_name)
                    destination = profile.mods_path / source_name
                    if not source.is_file():
                        continue
                    try:
                        if not destination.exists() or not filecmp.cmp(source, destination, shallow=False) or mod.version != expected_version:
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

    @Slot()
    def refreshEzClientUpdateState(self) -> None:
        """Re-scan bundled/profile JARs without blocking the QML render thread."""
        threading.Thread(target=self._auto_update_ezclient_mods, daemon=True).start()

    @Property(bool, notify=ezClientUpdateAvailableChanged)
    def ezClientUpdateAvailable(self) -> bool:
        return self.ezClientOutdatedCount > 0

    @Property(int, notify=ezClientUpdateAvailableChanged)
    def ezClientOutdatedCount(self) -> int:
        count = 0
        for profile in self._store.profiles:
            expected_version = FROZEN_EZCLIENT_VERSION if is_frozen_ezclient_version(profile.minecraft_version) else APP_VERSION
            for mod in profile.mods:
                is_core = ((mod.slug or "").lower() in ("ezclient", "ezclient-core")
                           or "ezclient" in (mod.filename or "").lower())
                if not is_core:
                    continue
                source_name = ezclient_asset_name(profile.minecraft_version)
                source = self._bundled_ezclient_asset(source_name)
                destination = profile.mods_path / source_name
                if not source.is_file():
                    continue
                try:
                    if not destination.exists() or not filecmp.cmp(source, destination, shallow=False) or mod.version != expected_version:
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
        """Apply the exact bundled EzClient JAR to every compatible managed profile."""
        from copy import deepcopy
        from backend.services.store import performance_mods_for_version

        changed = False
        for profile in self._store.profiles:
            source_name = ezclient_asset_name(profile.minecraft_version)
            source = self._bundled_ezclient_asset(source_name)
            mod = next((item for item in profile.mods if
                        (item.slug or item.project_id or "").lower() in ("ezclient", "ezclient-core") or
                        "ezclient" in (item.filename or "").lower() or
                        (item.name or "").lower() in ("ezclient", "ezclient core")), None)
            # Existing EzClient installations are updated even if an older
            # profile was saved as "raw". New core entries are only added to
            # profiles explicitly managed by EzClient.
            if not source.is_file() or (mod is None and profile.profile_type not in ("ezclient", "performance")):
                continue
            expected_version = FROZEN_EZCLIENT_VERSION if is_frozen_ezclient_version(profile.minecraft_version) else APP_VERSION
            if mod is None:
                template = next((item for item in performance_mods_for_version(profile.minecraft_version) if item.slug == "ezclient"), None)
                if template is None:
                    continue
                mod = deepcopy(template)
                profile.mods.insert(0, mod)
                if "ezclient" not in profile.managed_core_mods:
                    profile.managed_core_mods.insert(0, "ezclient")
                changed = True
            destination = profile.mods_path / source_name
            try:
                needs_copy = not destination.exists() or not filecmp.cmp(source, destination, shallow=False)
                if needs_copy:
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    for old_jar in profile.mods_path.glob("EzClient*.jar"):
                        if old_jar != destination:
                            old_jar.unlink(missing_ok=True)
                    shutil.copy2(source, destination)
                if mod.filename != source_name:
                    mod.filename = source_name
                    changed = True
                if needs_copy or mod.version != expected_version:
                    mod.version = expected_version
                    mod.version_id = expected_version
                    changed = True
            except OSError as exc:
                print(f"[ProfileController] EzClient manual update skipped for {profile.name}: {exc}")
        
        if changed:
            self._store.save()
            self._syncNeeded.emit()
        # The QML binding is signal-driven. Recompute after all copies so the
        # update card disappears immediately and can reappear on a later JAR.
        self._ez_client_update_available = self.ezClientOutdatedCount > 0
        self.ezClientUpdateAvailableChanged.emit()

    @Property(QObject, constant=True)
    def liveLogService(self) -> LiveLogService:
        return self._live_log_service

    @Property(bool, notify=launchStatusChanged)
    def isLaunching(self) -> bool:
        return self._is_launching

    # ---- Settings properties ----
    @Property(bool, notify=settingsChanged)
    def closeOnLaunch(self) -> bool:
        return self._store.settings.get("close_on_launch", False)

    @Slot(bool)
    def setCloseOnLaunch(self, val: bool) -> None:
        if self.closeOnLaunch != val:
            self._store.settings["close_on_launch"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Einstellung gespeichert: Launcher beim Starten schließen")

    @Property(bool, notify=settingsChanged)
    def checkUpdates(self) -> bool:
        return self._store.settings.get("check_updates", True)

    @Slot(bool)
    def setCheckUpdates(self, val: bool) -> None:
        if self.checkUpdates != val:
            self._store.settings["check_updates"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.checkModUpdates()
            self.settingSaved.emit("Einstellung gespeichert: Automatische Update-Prüfung")

    @Property(bool, notify=settingsChanged)
    def discordRpc(self) -> bool:
        return self._store.settings.get("discord_rpc", True)

    @Slot(bool)
    def setDiscordRpc(self, val: bool) -> None:
        if self.discordRpc != val:
            self._store.settings["discord_rpc"] = val
            self._store.save()
            self.settingsChanged.emit()
            try:
                from backend.services import discord_service
                if val:
                    discord_service.init_rpc()
                else:
                    discord_service.close_rpc()
            except Exception:
                pass
            self.settingSaved.emit("Einstellung gespeichert: Discord Rich Presence")

    @Property(bool, notify=settingsChanged)
    def preferDirectLaunch(self) -> bool:
        return self._store.settings.get("prefer_direct_launch", True)

    @Slot(bool)
    def setPreferDirectLaunch(self, val: bool) -> None:
        if self.preferDirectLaunch != val:
            self._store.settings["prefer_direct_launch"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.activeProfileChanged.emit()
            self.settingSaved.emit("Einstellung gespeichert: Start-Modus")

    @Property(bool, notify=settingsChanged)
    def killOfficialLauncher(self) -> bool:
        return self._store.settings.get("kill_official_launcher", True)

    @Slot(bool)
    def setKillOfficialLauncher(self, val: bool) -> None:
        if self.killOfficialLauncher != val:
            self._store.settings["kill_official_launcher"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Einstellung gespeichert: Offiziellen Launcher schließen")

    @Property(bool, notify=settingsChanged)
    def minimizeToTray(self) -> bool:
        return self._store.settings.get("minimize_to_tray", True)

    @Slot(bool)
    def setMinimizeToTray(self, val: bool) -> None:
        if self.minimizeToTray != val:
            self._store.settings["minimize_to_tray"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Einstellung gespeichert: Im System-Tray minimieren")

    @Property(str, notify=settingsChanged)
    def language(self) -> str:
        return self._store.settings.get("language", "de")

    @Slot(str)
    def setLanguage(self, val: str) -> None:
        if self.language != val:
            self._store.settings["language"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Sprache geändert")

    @Property(bool, notify=settingsChanged)
    def useMinecraftFont(self) -> bool:
        return self._store.settings.get("use_minecraft_font", True)

    @Slot(bool)
    def setUseMinecraftFont(self, val: bool) -> None:
        if self.useMinecraftFont != val:
            self._store.settings["use_minecraft_font"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Schriftart-Stil geändert")

    @Property(bool, notify=settingsChanged)
    def showLiveLogs(self) -> bool:
        return self._store.settings.get("show_live_logs", True)

    @Slot(bool)
    def setShowLiveLogs(self, val: bool) -> None:
        if self.showLiveLogs != val:
            self._store.settings["show_live_logs"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Live-Log Fenster während des Spiels")

    @Property(str, notify=settingsChanged)
    def appFontMode(self) -> str:
        return self._store.settings.get("app_font_mode", "minecraft")

    @Slot(str)
    def setAppFontMode(self, val: str) -> None:
        if self.appFontMode != val:
            self._store.settings["app_font_mode"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Schriftstil geändert")

    @Property(str, notify=settingsChanged)
    def themeColor(self) -> str:
        return self._store.settings.get("theme_color", "green")

    @Slot(str)
    def setThemeColor(self, val: str) -> None:
        if self.themeColor != val:
            self._store.settings["theme_color"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Farbschema aktualisiert")

    @Property(str, notify=settingsChanged)
    def customBackgroundImage(self) -> str:
        return self._store.settings.get("custom_background_image", "")

    @Property(float, notify=settingsChanged)
    def customBackgroundOpacity(self) -> float:
        return self._store.settings.get("custom_background_opacity", 0.4)

    @Property(str, notify=settingsChanged)
    def customBackgroundFillMode(self) -> str:
        return self._store.settings.get("custom_background_fill_mode", "cover")

    @Slot(str)
    def setCustomBackgroundImage(self, val: str) -> None:
        if self.customBackgroundImage != val:
            self._store.settings["custom_background_image"] = val
            self._store.save()
            self.settingsChanged.emit()
            self.settingSaved.emit("Hintergrundbild aktualisiert")

    @Slot(float)
    def setCustomBackgroundOpacity(self, val: float) -> None:
        if self.customBackgroundOpacity != val:
            self._store.settings["custom_background_opacity"] = val
            self._store.save()
            self.settingsChanged.emit()

    @Slot(str)
    def setCustomBackgroundFillMode(self, val: str) -> None:
        if self.customBackgroundFillMode != val:
            self._store.settings["custom_background_fill_mode"] = val
            self._store.save()
            self.settingsChanged.emit()

    @Slot(result=str)
    def pickBackgroundImage(self) -> str:
        file_path, _ = QFileDialog.getOpenFileName(
            None, "Hintergrundbild auswählen", "",
            "Bilder (*.png *.jpg *.jpeg *.webp *.bmp);;Alle Dateien (*.*)"
        )
        if file_path:
            self.setCustomBackgroundImage(file_path)
            return file_path
        return ""

    @Slot(result=str)
    def pickBackgroundClip(self) -> str:
        file_path, _ = QFileDialog.getOpenFileName(
            None, "Hintergrund-Clip auswählen", "",
            "Videos (*.mp4 *.webm *.mov);;Alle Dateien (*.*)"
        )
        if file_path:
            self.setCustomBackgroundImage(file_path)
            return file_path
        return ""

    @Slot(result=str)
    def pickProfileIconImage(self) -> str:
        """Open native file dialog to choose a PNG/image file for a profile icon."""
        file_path, _ = QFileDialog.getOpenFileName(
            None, "Profil-Icon auswählen", "",
            "Bilder (*.png *.jpg *.jpeg *.webp);;Alle Dateien (*.*)"
        )
        return file_path or ""

    @Slot(str, str)
    def setProfileIcon(self, profile_id: str, icon_or_path: str) -> None:
        """Update the icon of an existing profile and sync."""
        profile = self._store.get_by_id(profile_id)
        if not profile:
            return

        clean_path = icon_or_path.replace("file:///", "").replace("file://", "")
        p_file = Path(clean_path)
        if p_file.is_file():
            dest = profile.path / "icon.png"
            dest.parent.mkdir(parents=True, exist_ok=True)
            try:
                shutil.copy2(p_file, dest)
                profile.icon = f"file:///{dest.as_posix()}"
            except Exception as exc:
                print(f"[ProfileController] Could not copy profile icon: {exc}")
                profile.icon = icon_or_path
        else:
            profile.icon = icon_or_path

        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()
        self.inspectedProfileChanged.emit()
        self.profilesChanged.emit()

    @classmethod
    def _local_pack_entries(cls, folder: Path, folder_name: str, desc_prefix: str, self_ref=None) -> list[ModData]:
        if not folder.is_dir():
            return []
        result = []
        try:
            for item in sorted(folder.iterdir(), key=lambda p: p.name.lower()):
                is_disabled = item.name.lower().endswith(".disabled")
                clean_filename = item.name[:-9] if is_disabled else item.name
                valid_file = item.is_file() and (clean_filename.lower().endswith(".zip") or clean_filename.lower().endswith(".jar"))
                if not (item.is_dir() or valid_file):
                    continue
                display_name = clean_filename[:-4] if clean_filename.lower().endswith((".zip", ".jar")) else clean_filename
                icon_url = cls._extract_pack_icon(self_ref, item) if self_ref else ""
                result.append(ModData(
                    project_id=f"local-{folder_name}-{clean_filename.lower()}",
                    slug=f"local-{folder_name}-{clean_filename.lower()}",
                    name=display_name,
                    version_id="local",
                    version="Lokal",
                    filename=item.name,
                    enabled=not is_disabled,
                    author="Lokal",
                    description=f"{desc_prefix} im Ordner {folder_name}",
                    icon_url=icon_url,
                    source="local",
                ))
        except OSError as exc:
            print(f"[ProfileController] Could not scan {folder_name}: {exc}")
        return result

    def _mods_with_local_extensions(self, profile: ProfileData | None) -> list[ModData]:
        if not profile:
            return []
        
        def _sort_key(m: ModData):
            s = (m.slug or "").lower()
            n = (m.name or "").lower()
            fn = (m.filename or "").lower()
            if "ezclient" in s or "ezclient" in n or "ezclient" in fn:
                return (0, n)
            if s in ("fabric-api", "fabric_api") or "fabric api" in n or "fabric-api" in fn:
                return (1, n)
            if getattr(m, 'essential', False) or getattr(m, 'recommended', False):
                return (2, n)
            return (3, n)

        sorted_mods = sorted(profile.mods, key=_sort_key)
        rp_entries = self._local_pack_entries(profile.path / "resourcepacks", "resourcepacks", "Ressourcenpaket", self)
        sp_entries = self._local_pack_entries(profile.path / "shaderpacks", "shaderpacks", "Shader-Paket", self)
        return sorted_mods + rp_entries + sp_entries

    @Slot()
    def _sync_models(self) -> None:
        self._profile_model.set_profiles(self._store.profiles)
        if self._active_profile:
            try:
                if self._skip_next_registry_scan:
                    self._skip_next_registry_scan = False
                else:
                    self._installed_registry.scan_directory(self._active_profile.mods_path, self._active_profile.mods)
                
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
                        from main import get_app_root
                        pm.icon_url = Path(get_app_root() / "ui" / "assets" / "logo.png").as_uri()

            except Exception as e:
                print(f"[ProfileController] Registry scan error: {e}")
            self._mod_model.set_mods(self._mods_with_local_extensions(self._active_profile))
        else:
            self._installed_registry.scan_directory(Path("/nonexistent"))
            self._mod_model.set_mods([])
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()
        self.checkModUpdates()

    # ---- Crash Doctor Properties & Slots ----
    @Property("QVariantMap", notify=crashDiagnosisChanged)
    def crashDiagnosis(self) -> dict:
        return self._crash_diagnosis.to_dict()

    @Property(bool, notify=crashFixStarted)
    def isFixingCrash(self) -> bool:
        return self._is_fixing_crash

    @Property(str, notify=crashFixProgress)
    def crashFixStatus(self) -> str:
        return self._crash_fix_status

    @Property(bool, notify=crashFixCompleted)
    def crashFixSuccess(self) -> bool:
        return self._crash_fix_success

    @Slot(str)
    def analyzeCrash(self, custom_log: str = "") -> None:
        """Analyzes a crash log and updates diagnosis."""
        log = custom_log or (self._live_log_service.getFullLog() if hasattr(self, '_live_log_service') else "")
        self._crash_diagnosis = self._crash_doctor.analyze(log, "", self._active_profile)
        self.crashDiagnosisChanged.emit()

    @Slot()
    def fixCurrentCrash(self) -> None:
        """Applies the diagnosed automatic fix in a background worker thread."""
        if self._is_fixing_crash:
            return
        self._is_fixing_crash = True
        self._crash_fix_success = False
        self._crash_fix_status = "Starte Problembehebung…"
        self.crashFixStarted.emit()

        def _fix_worker():
            try:
                def _prog(val, msg):
                    self._crash_fix_status = msg
                    self.crashFixProgress.emit(val, msg)
                success, msg = self._crash_doctor.apply_fix(
                    self._crash_diagnosis, self._active_profile, progress=_prog
                )
                self._crash_fix_success = success
                self._crash_fix_status = msg
                self._is_fixing_crash = False
                self._syncNeeded.emit()
                self.crashFixCompleted.emit(success, msg)
            except Exception as e:
                self._crash_fix_success = False
                self._crash_fix_status = f"Fehler bei automatischer Reparatur: {e}"
                self._is_fixing_crash = False
                self.crashFixCompleted.emit(False, str(e))

        threading.Thread(target=_fix_worker, daemon=True).start()

    @Slot()
    def relaunchAfterFix(self) -> None:
        self.dismissCrash()
        self.launchActiveProfile()

    @Slot()
    def dismissCrash(self) -> None:
        self._is_fixing_crash = False
        self._crash_fix_status = ""

    # ---- Mod updates ----
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
            try:
                self._modUpdatesDone.emit(token, updates)
            except RuntimeError:
                pass

        threading.Thread(target=_check_task, daemon=True).start()

    @Slot(int, dict)
    def _set_mod_updates(self, token: int, updates: dict) -> None:
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
                if m.version == version and self._mod_file_exists(self._active_profile, m):
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

    @Property(str, notify=activeProfileChanged)
    def activeIcon(self) -> str:
        return getattr(self._active_profile, "icon", "") if self._active_profile else ""

    @Property("QVariantList", notify=activeProfileChanged)
    def integratedMods(self) -> list:
        if not self._active_profile:
            return []
        # Only launcher-managed core entries are integrated. Recommended or
        # essential user mods remain ordinary installed mods.
        managed = {
            str(value).strip().lower()
            for value in (self._active_profile.managed_core_mods or [])
            if value
        }
        return sorted(managed)

    @Property(str, notify=activeProfileChanged)
    def activeVersion(self) -> str:
        return self._active_profile.minecraft_version if self._active_profile else "26.2"

    @Property(bool, notify=activeProfileChanged)
    def isDirectLaunchReady(self) -> bool:
        if not self._active_profile:
            return False
        from backend.services.direct_launch import find_version_meta
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

    @Property(bool, notify=activeProfileChanged)
    def activeSupportsMods(self) -> bool:
        return bool(self._active_profile and self._active_profile.loader.lower() != "vanilla")

    @Property(bool, notify=activeProfileChanged)
    def activeHasEzClient(self) -> bool:
        return bool(self._active_profile and self._active_profile.profile_type == "ezclient")

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

    @Property("QVariantList", notify=gameVersionFamiliesChanged)
    def gameVersionFamilies(self) -> list:
        from backend.services.minecraft_versions import catalog
        return catalog(lambda filename: self._bundled_ezclient_asset(filename).is_file())

    @Property("QVariantList", notify=javaRuntimesChanged)
    def javaRuntimes(self) -> list:
        from backend.services.java_runtime import runtime_statuses
        return runtime_statuses(minecraft_dir())

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
                        ("dwLength", ctypes.c_ulong),
                        ("dwMemoryLoad", ctypes.c_ulong),
                        ("ullTotalPhys", ctypes.c_ulonglong),
                        ("ullAvailPhys", ctypes.c_ulonglong),
                        ("ullTotalPageFile", ctypes.c_ulonglong),
                        ("ullAvailPageFile", ctypes.c_ulonglong),
                        ("ullTotalVirtual", ctypes.c_ulonglong),
                        ("ullAvailVirtual", ctypes.c_ulonglong),
                        ("sullAvailExtendedVirtual", ctypes.c_ulonglong),
                    ]
                stat = MEMORYSTATUSEX()
                stat.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
                if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(stat)):
                    return int(stat.ullTotalPhys // (1024 * 1024))
        except Exception:
            pass
        return 16384

    @Slot(int)
    def setActiveRamMb(self, mb: int) -> None:
        if not self._active_profile:
            return
        mb = max(1024, min(mb, self.systemTotalRamMb))
        self._active_profile.ram_mb = mb
        self._store.save()
        self.activeProfileChanged.emit()
        self.settingSaved.emit(f"RAM auf {mb / 1024:.1f} GB zugewiesen")

    # ---- Inspected profile properties & slots ----
    @Slot(str)
    def duplicateProfile(self, profile_id: str) -> None:
        new_p = self._store.duplicate_profile(profile_id)
        if new_p:
            self._sync_models()
            self.profilesChanged.emit()

    @Slot(str)
    def inspectProfile(self, profile_id: str) -> None:
        p = self._store.get_by_id(profile_id)
        if p:
            self._inspected_profile = p
            self.inspectedProfileChanged.emit()

    @Slot()
    def activateInspectedProfile(self) -> None:
        if self._inspected_profile:
            self.selectProfile(self._inspected_profile.id)

    @Property(str, notify=inspectedProfileChanged)
    def inspectedId(self) -> str:
        return self._inspected_profile.id if self._inspected_profile else ""

    @Property(str, notify=inspectedProfileChanged)
    def inspectedName(self) -> str:
        return self._inspected_profile.name if self._inspected_profile else "Kein Profil"

    @Property(str, notify=inspectedProfileChanged)
    def inspectedIcon(self) -> str:
        p = self._inspected_profile or self._active_profile
        return getattr(p, "icon", "") if p else ""

    @Property(str, notify=inspectedProfileChanged)
    def inspectedVersion(self) -> str:
        return self._inspected_profile.minecraft_version if self._inspected_profile else "26.2"

    @Property(str, notify=inspectedProfileChanged)
    def inspectedLoader(self) -> str:
        return self._inspected_profile.loader if self._inspected_profile else "Fabric"

    @Property(bool, notify=inspectedProfileChanged)
    def inspectedSupportsMods(self) -> bool:
        p = self._inspected_profile or self._active_profile
        return bool(p and p.loader.lower() != "vanilla")

    @Property(bool, notify=inspectedProfileChanged)
    def inspectedHasEzClient(self) -> bool:
        p = self._inspected_profile or self._active_profile
        return bool(p and p.profile_type == "ezclient")

    @Property(int, notify=inspectedProfileChanged)
    def inspectedModsCount(self) -> int:
        return len(self._inspected_profile.mods) if self._inspected_profile else 0

    @Property(str, notify=inspectedProfileChanged)
    def inspectedLastPlayed(self) -> str:
        return self._inspected_profile.last_played if self._inspected_profile else "Never"

    @Property(str, notify=inspectedProfileChanged)
    def inspectedGameDir(self) -> str:
        return str(self._inspected_profile.path) if self._inspected_profile else ""

    @Property(bool, notify=inspectedProfileChanged)
    def isInspectedActive(self) -> bool:
        if not self._active_profile or not self._inspected_profile:
            return True
        return self._active_profile.id == self._inspected_profile.id

    def _extract_pack_icon(self, pack_path: Path) -> str:
        """Extracts pack.png/icon.png from a .zip or directory into persistent cache."""
        try:
            cache_dir = DATA_DIR / "cache" / "pack_icons"
            cache_dir.mkdir(parents=True, exist_ok=True)
            stat = pack_path.stat()
            ident = hashlib.md5(f"{pack_path.name}_{stat.st_mtime}_{stat.st_size}".encode()).hexdigest()[:12]
            cached_icon = cache_dir / f"{ident}.png"
            if cached_icon.is_file():
                return cached_icon.as_uri()

            if pack_path.is_file() and pack_path.name.lower().endswith(".zip"):
                with zipfile.ZipFile(pack_path, "r") as zf:
                    for icon_name in ("pack.png", "icon.png", "assets/minecraft/icon.png"):
                        matches = [n for n in zf.namelist() if n.lower() == icon_name]
                        if matches:
                            with zf.open(matches[0]) as zf_in, open(cached_icon, "wb") as out:
                                shutil.copyfileobj(zf_in, out)
                            return cached_icon.as_uri()
            elif pack_path.is_dir():
                for icon_name in ("pack.png", "icon.png"):
                    candidate = pack_path / icon_name
                    if candidate.is_file():
                        shutil.copy2(candidate, cached_icon)
                        return cached_icon.as_uri()
        except Exception:
            pass
        return ""

    @Property("QVariantList", notify=inspectedProfileChanged)
    def inspectedResourcePacks(self) -> list:
        p = self._inspected_profile or self._active_profile
        if not p or not p.path.exists():
            return []
        rp_dir = p.path / "resourcepacks"
        if not rp_dir.is_dir():
            return []
        result = []
        try:
            for item in sorted(rp_dir.iterdir(), key=lambda x: x.name.lower()):
                is_disabled = item.name.lower().endswith(".disabled")
                clean_name = item.name[:-9] if is_disabled else item.name
                valid = (item.is_file() and clean_name.lower().endswith(".zip")) or item.is_dir()
                if not valid:
                    continue
                display_name = clean_name[:-4] if clean_name.lower().endswith(".zip") else clean_name
                result.append({
                    "filename": item.name,
                    "name": display_name,
                    "enabled": not is_disabled,
                    "path": str(item),
                    "icon_url": self._extract_pack_icon(item),
                })
        except OSError as exc:
            print(f"[ProfileController] Could not scan resourcepacks: {exc}")
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
            if filename.lower().endswith(".disabled"):
                new_path = rp_dir / filename[:-9]
            else:
                new_path = rp_dir / (filename + ".disabled")
            target.rename(new_path)
            self.inspectedProfileChanged.emit()
            self.activeProfileChanged.emit()
        except OSError as exc:
            print(f"[ProfileController] Could not toggle resource pack: {exc}")

    @Slot()
    def openResourcePacksFolder(self) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        target = p.path / "resourcepacks"
        target.mkdir(parents=True, exist_ok=True)
        self.openFolder(str(target))

    @Property("QVariantList", notify=inspectedProfileChanged)
    def inspectedShaderPacks(self) -> list:
        p = self._inspected_profile or self._active_profile
        if not p or not p.path.exists():
            return []
        sp_dir = p.path / "shaderpacks"
        if not sp_dir.is_dir():
            return []
        active_shader = ""
        iris_file = p.path / "config" / "iris.properties"
        if iris_file.is_file():
            try:
                for line in iris_file.read_text(encoding="utf-8").splitlines():
                    if line.startswith("shaderPack="):
                        active_shader = line.split("=", 1)[1].strip()
                        break
            except Exception:
                pass

        result = []
        try:
            for item in sorted(sp_dir.iterdir(), key=lambda x: x.name.lower()):
                is_disabled = item.name.lower().endswith(".disabled")
                clean_name = item.name[:-9] if is_disabled else item.name
                valid = (item.is_file() and clean_name.lower().endswith(".zip")) or item.is_dir()
                if not valid:
                    continue
                display_name = clean_name[:-4] if clean_name.lower().endswith(".zip") else clean_name
                is_active = (clean_name == active_shader or item.name == active_shader) and not is_disabled
                result.append({
                    "filename": item.name,
                    "name": display_name,
                    "enabled": not is_disabled,
                    "isActive": is_active,
                    "path": str(item),
                    "icon_url": self._extract_pack_icon(item),
                })
        except OSError as exc:
            print(f"[ProfileController] Could not scan shaderpacks: {exc}")
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
            if filename.lower().endswith(".disabled"):
                new_path = sp_dir / filename[:-9]
            else:
                new_path = sp_dir / (filename + ".disabled")
            target.rename(new_path)
            self.inspectedProfileChanged.emit()
            self.activeProfileChanged.emit()
        except OSError as exc:
            print(f"[ProfileController] Could not toggle shader pack: {exc}")

    @Slot(str)
    def selectShaderPack(self, filename: str) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        clean = filename[:-9] if filename.lower().endswith(".disabled") else filename
        cfg_dir = p.path / "config"
        cfg_dir.mkdir(parents=True, exist_ok=True)
        iris_file = cfg_dir / "iris.properties"
        try:
            iris_file.write_text(f"shaderPack={clean}\nenableShaders=true\n", encoding="utf-8")
            self.inspectedProfileChanged.emit()
            self.activeProfileChanged.emit()
        except OSError as exc:
            print(f"[ProfileController] Could not select shader pack: {exc}")

    @Slot()
    def disableShaderPack(self) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        cfg_dir = p.path / "config"
        cfg_dir.mkdir(parents=True, exist_ok=True)
        iris_file = cfg_dir / "iris.properties"
        try:
            iris_file.write_text("shaderPack=OFF\nenableShaders=false\n", encoding="utf-8")
            self.inspectedProfileChanged.emit()
            self.activeProfileChanged.emit()
        except OSError as exc:
            print(f"[ProfileController] Could not disable shader pack: {exc}")

    @Slot()
    def openShaderPacksFolder(self) -> None:
        p = self._inspected_profile or self._active_profile
        if not p:
            return
        target = p.path / "shaderpacks"
        target.mkdir(parents=True, exist_ok=True)
        self.openFolder(str(target))

    @Slot(str, str, result=bool)
    def copyMinecraftSettings(self, source_profile_id: str, target_profile_id: str) -> bool:
        src = self._store.get_by_id(source_profile_id)
        tgt = self._store.get_by_id(target_profile_id)
        if not src or not tgt or src.id == tgt.id:
            return False
        files_to_copy = [
            "options.txt", "optionsof.txt", "optionsshaders.txt",
            "servers.dat", "servers.dat_old", "hotbar.nbt"
        ]
        copied_any = False
        for fn in files_to_copy:
            s_file = src.path / fn
            if s_file.exists() and s_file.is_file():
                tgt.path.mkdir(parents=True, exist_ok=True)
                shutil.copy2(s_file, tgt.path / fn)
                copied_any = True

        s_iris = src.path / "config" / "iris.properties"
        if s_iris.exists() and s_iris.is_file():
            (tgt.path / "config").mkdir(parents=True, exist_ok=True)
            shutil.copy2(s_iris, tgt.path / "config" / "iris.properties")
            copied_any = True

        if copied_any:
            self.settingSaved.emit(f"Minecraft-Einstellungen von '{src.name}' nach '{tgt.name}' kopiert!")
            return True
        return False

    @Slot(str, result=bool)
    def copyActiveMinecraftSettingsTo(self, target_profile_id: str) -> bool:
        if not self._active_profile:
            return False
        return self.copyMinecraftSettings(self._active_profile.id, target_profile_id)

    @Property("QVariantList", notify=profilesChanged)
    def allProfilesList(self) -> list:
        return [{"id": p.id, "name": p.name, "version": p.minecraft_version} for p in self._store.profiles]

    @Property("QVariantList", notify=activeProfileChanged)
    def installedModIdentifiers(self) -> list:
        if not self._active_profile:
            return []
        ids = []
        for m in self._active_profile.mods:
            if m.project_id: ids.append(str(m.project_id).lower())
            if m.slug: ids.append(str(m.slug).lower())
            if m.name: ids.append(str(m.name).lower())
            if m.filename: ids.append(str(m.filename).lower())
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
        p = self._store.get_by_id(profile_id)
        if p and p != self._active_profile:
            self._active_profile = p
            self._inspected_profile = p
            self._store.settings["last_profile"] = p.id
            self._store.save()
            self._sync_models()

    @Slot(str, str, str, str, str, result=str)
    @Slot(str, str, str, str, result=str)
    def createProfile(self, name: str, version: str = "26.2", loader: str = "Fabric", preset: str = "ezclient", icon: str = "") -> str:
        clean_icon = icon
        p = self._store.create_profile(name, version, loader, preset, icon=clean_icon)
        if clean_icon:
            clean_path = clean_icon.replace("file:///", "").replace("file://", "")
            p_file = Path(clean_path)
            if p_file.is_file():
                try:
                    dest = p.path / "icon.png"
                    dest.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(p_file, dest)
                    p.icon = f"file:///{dest.as_posix()}"
                    self._store.save()
                except Exception as exc:
                    print(f"[ProfileController] Could not copy profile icon on creation: {exc}")
        self._active_profile = p
        self._inspected_profile = p
        self._store.settings["last_profile"] = p.id
        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()
        self.inspectedProfileChanged.emit()
        self.profilesChanged.emit()
        p_ref = p
        threading.Thread(target=lambda: sync_profile_mods(p_ref), daemon=True).start()
        return p.id

    @Slot(str, str, str, str, "QVariantList", str)
    @Slot(str, str, str, str, "QVariantList")
    def createAndOnboard(self, name: str, version: str = "26.2", loader: str = "Fabric", preset: str = "ezclient", selected_optional_slugs: list = None, icon: str = "") -> None:
        """Create a typed profile and provision independent JARs concurrently."""
        def worker() -> None:
            try:
                self.onboardingStepProgress.emit(0.05, "", "Profil initialisieren…")
                slugs = [str(s) for s in selected_optional_slugs] if selected_optional_slugs else []
                profile = self._store.create_profile(
                    name=name, version=version, loader=loader, preset=preset, selected_optional_mods=slugs, icon=icon
                )
                if icon:
                    clean_path = icon.replace("file:///", "").replace("file://", "")
                    p_file = Path(clean_path)
                    if p_file.is_file():
                        try:
                            dest = profile.path / "icon.png"
                            dest.parent.mkdir(parents=True, exist_ok=True)
                            shutil.copy2(p_file, dest)
                            profile.icon = f"file:///{dest.as_posix()}"
                        except Exception as exc:
                            print(f"[ProfileController] Could not copy profile icon in createAndOnboard: {exc}")
                self._active_profile = profile
                self._inspected_profile = profile
                self._store.settings["last_profile"] = profile.id
                self._store.save()
                self._syncNeeded.emit()

                def report(progress_val, mod_name):
                    self.onboardingStepProgress.emit(progress_val, mod_name, f"Lade {mod_name}…")

                sync_profile_mods(profile, progress_callback=report)
                self._syncNeeded.emit()
                self.onboardingFinished.emit(profile.id)
            except Exception as exc:
                print(f"[ProfileController] Onboarding error: {exc}")
                self.onboardingStepProgress.emit(0.0, "Fehler", f"Fehler: {exc}")

        threading.Thread(target=worker, daemon=True).start()

    # ---- Modpack Installation ----
    @Slot(str)
    @Slot(str, str)
    @Slot(str, str, str)
    @Slot(str, str, str, str)
    def installModpack(self, project_id: str, name: str = "Modpack", source: str = "modrinth", version_id: str = "") -> None:
        """Install a catalogue modpack as a new, isolated launcher profile."""
        def worker() -> None:
            from backend.services.modpack_installer import install_modrinth_modpack, install_curseforge_modpack
            profile = None
            try:
                self.modpackInstallProgress.emit(0.01, "Erstelle isoliertes Modpack-Profil…")
                profile = self._store.create_profile(
                    name=name, version="1.21.11", loader="Fabric", preset="raw", optimize=False
                )
                if source == "curseforge":
                    result = install_curseforge_modpack(
                        project_id=project_id,
                        profile=profile,
                        progress=lambda value, message: self.modpackInstallProgress.emit(value, message)
                    )
                else:
                    result = install_modrinth_modpack(
                        project_id=project_id,
                        profile=profile,
                        progress=lambda value, message: self.modpackInstallProgress.emit(value, message)
                    )
                self._active_profile = profile
                self._inspected_profile = profile
                self._store.settings["last_profile"] = profile.id
                self._store.save()
                self._syncNeeded.emit()
                self.modpackInstallFinished.emit(
                    profile.id, True,
                    f"{result.get('name', name)} · Minecraft {result.get('minecraft_version', '')} · {result.get('loader', '')}"
                )
            except Exception as exc:
                if profile:
                    self._store.delete_profile(profile.id)
                self._syncNeeded.emit()
                self.modpackInstallFinished.emit("", False, str(exc))
        threading.Thread(target=worker, daemon=True).start()

    # ---- NoRisk Profile Scanning & Importing ----
    @Property("QVariantList", notify=noriskProfilesChanged)
    def noriskProfiles(self) -> list:
        return list(self._norisk_profiles)

    @Slot()
    def scanNoRiskProfiles(self) -> None:
        """Scan local NoRiskClient installations."""
        def _scan():
            from backend.services.norisk_importer import discover_norisk_profiles
            self._norisk_profiles = discover_norisk_profiles()
            try:
                self.noriskProfilesChanged.emit()
            except RuntimeError:
                pass
        threading.Thread(target=_scan, daemon=True).start()

    @Slot(str, bool)
    def importNoRiskProfile(self, profile_id: str, add_performance: bool = True) -> None:
        """Import a discovered NoRiskClient profile into EzClient."""
        def _worker():
            from backend.services.norisk_importer import import_norisk_files
            found = next((p for p in self._norisk_profiles if p.get("id") == profile_id), None)
            if not found:
                self.noriskImportFinished.emit("", False, "NoRisk-Profil nicht gefunden.")
                return
            try:
                self.noriskImportProgress.emit(0.05, "Profil wird erstellt …")
                name = found.get("name") or "NoRisk Import"
                version = found.get("version") or "1.20.1"
                loader = found.get("loader") or "Fabric"
                preset = "ezclient" if add_performance else "raw"
                profile = self._store.create_profile(
                    name=name, version=version, loader=loader, preset=preset, icon="norisk"
                )
                import_norisk_files(
                    found, profile,
                    progress=lambda p, msg: self.noriskImportProgress.emit(p * 0.7, msg)
                )
                if add_performance:
                    from backend.services.store import performance_mods_for_version, ezclient_asset_name
                    source_name = ezclient_asset_name(profile.minecraft_version)
                    core_asset = self._bundled_ezclient_asset(source_name)
                    if core_asset.is_file():
                        profile.mods_path.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(core_asset, profile.mods_path / source_name)

                    existing_slugs = {(m.slug or "").lower() for m in profile.mods}
                    perf_templates = performance_mods_for_version(profile.minecraft_version)
                    missing_perf: list[ModData] = []
                    for pt in perf_templates:
                        if pt.slug.lower() not in existing_slugs:
                            mod_dict = asdict(pt)
                            if pt.slug == "ezclient":
                                mod_dict["filename"] = source_name
                            missing_perf.append(ModData(**mod_dict))
                            existing_slugs.add(pt.slug.lower())

                    for pt in reversed(missing_perf):
                        profile.mods.insert(0, pt)

                    profile.profile_type = "ezclient"
                    profile.managed_core_mods = [m.slug for m in perf_templates]
                    profile.integrated_mods = [m.slug for m in perf_templates]

                    # Download only missing performance mods if any
                    non_core_missing = [pm for pm in missing_perf if pm.slug.lower() not in ("ezclient", "ezclient-core")]
                    if non_core_missing:
                        self.noriskImportProgress.emit(0.85, "Lade zusätzliche Performance-Mods herunter …")
                        from backend.services.mod_downloader import download_file, select_preferred_version
                        from backend.services.modrinth import ModrinthService
                        m_svc = ModrinthService()
                        for pm in non_core_missing:
                            try:
                                vers = m_svc.get_project_versions(pm.slug, mc_version=profile.minecraft_version, loader=profile.loader)
                                b_ver = select_preferred_version(vers)
                                if b_ver and b_ver.get("files"):
                                    f = next((x for x in b_ver["files"] if x.get("primary")), b_ver["files"][0])
                                    fn = f.get("filename") or f"{pm.slug}.jar"
                                    if download_file(f["url"], profile.mods_path / fn):
                                        pm.filename = fn
                                        pm.version = b_ver.get("version_number", pm.version)
                            except Exception as ex:
                                print(f"[ProfileController] Could not download performance mod {pm.slug}: {ex}")

                self._active_profile = profile
                self._inspected_profile = profile
                self._store.settings["last_profile"] = profile.id
                self._store.save()
                self.noriskImportProgress.emit(0.95, "Synchronisiere Mods & Assets …")
                self._syncNeeded.emit()
                self.noriskImportFinished.emit(profile.id, True, "Profil erfolgreich importiert!")
            except Exception as exc:
                self.noriskImportFinished.emit("", False, f"Fehler beim Import: {exc}")
        threading.Thread(target=_worker, daemon=True).start()

    @Slot(str)
    def deleteProfile(self, profile_id: str) -> None:
        self._store.delete_profile(profile_id)
        if self._active_profile and self._active_profile.id == profile_id:
            self._active_profile = self._store.get_last_or_default()
            self._inspected_profile = self._active_profile
        elif self._inspected_profile and self._inspected_profile.id == profile_id:
            self._inspected_profile = self._active_profile or self._store.get_last_or_default()
        self._sync_models()
        self.activeProfileChanged.emit()
        self.inspectedProfileChanged.emit()
        self.profilesChanged.emit()

    @Slot(str)
    def toggleMod(self, mod_id: str) -> None:
        if not self._active_profile:
            return
        for m in self._active_profile.mods:
            if m.project_id == mod_id or m.slug == mod_id or m.filename == mod_id or m.name == mod_id:
                m.enabled = not m.enabled
                self._store.save()
                self._sync_models()
                self.activeProfileChanged.emit()
                self._sync_mods_after_change(self._active_profile)
                break

    def _sync_mods_after_change(self, profile: ProfileData) -> None:
        def _worker():
            try:
                sync_profile_mods(profile)
            except Exception as exc:
                print(f"[ProfileController] Background mod sync error: {exc}")
        threading.Thread(target=_worker, daemon=True).start()

    @Slot(str, result="QVariantList")
    def getModDependencies(self, mod_id: str) -> list[str]:
        return []

    @Slot(result=bool)
    def isIrisInstalled(self) -> bool:
        if not self._active_profile:
            return False
        for m in self._active_profile.mods:
            s = (m.slug or "").lower()
            if "iris" in s or "oculus" in s:
                return True
        return False

    @Slot()
    def installIris(self) -> None:
        self.installMod("YL57xq9U", "Iris Shaders", "Latest", "iris.jar", "Iris Team", "Shader-Unterstützung mit hoher Performance", "https://cdn.modrinth.com/data/YL57xq9U/icon.png")

    @Slot(str, str, str, str, str, str, str, str)
    def installMod(self, mod_id: str, name: str = "", version: str = "Latest", filename: str = "", author: str = "", description: str = "", icon_url: str = "", source: str = "modrinth") -> None:
        if not self._active_profile:
            return

        profile = self._active_profile
        existing_mod = None
        for m in profile.mods:
            m_slug = (m.slug or "").lower()
            m_pid = (m.project_id or "").lower()
            if (m_slug and m_slug == mod_id.lower()) or (m_pid and m_pid == mod_id.lower()) or (m.name.lower() == name.lower()):
                existing_mod = m
                break

        created = existing_mod is None
        previous = None
        previous_filename = ""
        if existing_mod is not None:
            if existing_mod.version == version and existing_mod.enabled and self._mod_file_exists(profile, existing_mod):
                return
            previous = {field: getattr(existing_mod, field) for field in ModData.__annotations__}
            previous_filename = existing_mod.filename or ""
            target_mod = existing_mod
            target_mod.project_id = mod_id or target_mod.project_id
            target_mod.name = name or target_mod.name
            target_mod.version_id = version
            target_mod.version = version
            target_mod.filename = filename or target_mod.filename or f"{mod_id}.jar"
            target_mod.enabled = True
            target_mod.author = author or target_mod.author
            target_mod.description = description or target_mod.description
            target_mod.icon_url = icon_url or target_mod.icon_url
            target_mod.source = source or target_mod.source
            target_mod._force_download = True
        else:
            target_mod = ModData(
                project_id=mod_id,
                slug=mod_id,
                name=name or mod_id,
                version_id=version,
                version=version,
                filename=filename or f"{mod_id}.jar",
                enabled=True,
                author=author or ("CurseForge" if source == "curseforge" else "Modrinth"),
                description=description,
                icon_url=icon_url,
                source=source
            )
            target_mod._force_download = True
            profile.mods.append(target_mod)
        user_mod_added = mod_id not in profile.user_mods
        if user_mod_added:
            profile.user_mods.append(mod_id)
        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()

        self.modInstallStarted.emit(mod_id)

        def _bg_install():
            try:
                from backend.services.modrinth import ModrinthService
                from backend.services.curseforge import CurseForgeService
                svc = CurseForgeService() if source == "curseforge" else ModrinthService()
                deps = svc.get_dependencies(mod_id, profile.minecraft_version, profile.loader)
                if deps:
                    existing_slugs = {m.slug.lower() for m in profile.mods if m.slug}
                    for d_slug, d_title in deps.items():
                        if d_slug.lower() not in existing_slugs:
                            profile.mods.append(ModData(
                                project_id=d_slug,
                                slug=d_slug,
                                name=d_title,
                                version_id="Latest",
                                version="Latest",
                                filename=f"{d_slug}.jar",
                                enabled=True,
                                author=source.capitalize(),
                                description=f"Automatisch installierte Abhängigkeit für {name or mod_id}",
                                source=source
                            ))
                            profile.user_mods.append(d_slug)
                    self._store.save()
                    self._syncNeeded.emit()

                failures = sync_profile_mods(profile)
                failure = failures.get(target_mod.slug or target_mod.project_id) or failures.get(mod_id)
                if failure:
                    raise RuntimeError(failure)
                if previous_filename and previous_filename != target_mod.filename:
                    for directory in (profile.mods_path, profile.path / "shaderpacks", profile.path / "resourcepacks"):
                        old_path = directory / previous_filename
                        if old_path.is_file():
                            old_path.unlink(missing_ok=True)
                self._store.save()
                self._syncNeeded.emit()
            except Exception as e:
                print(f"[ProfileController] Error downloading mod {mod_id}: {e}")
                if created:
                    profile.mods = [item for item in profile.mods if item is not target_mod]
                elif previous is not None:
                    for field, value in previous.items():
                        setattr(target_mod, field, value)
                if user_mod_added:
                    profile.user_mods = [item for item in profile.user_mods if item.lower() != mod_id.lower()]
                self._store.save()
                self._syncNeeded.emit()
                self.settingSaved.emit(f"Installation fehlgeschlagen: {e}")
            finally:
                self.modInstallFinished.emit(mod_id)

        threading.Thread(target=_bg_install, daemon=True).start()

    @Slot(str, str)
    def uninstallMod(self, mod_id: str, mod_name: str = "") -> None:
        if not self._active_profile:
            return

        is_core = (mod_id.lower() in ("ezclient", "ezclient-core", "fabric-api") or
                   (mod_name and mod_name.lower() in ("ezclient", "ezclient core", "fabric api")))
        if is_core:
            self.settingSaved.emit("Core-Mods werden vom EzClient-Profil verwaltet und können nicht entfernt werden.")
            return

        mod_id_clean = mod_id.strip().lower()
        name_clean = mod_name.strip().lower()

        deleted_filenames = []
        retained = []
        for m in self._active_profile.mods:
            m_slug = (m.slug or "").lower()
            m_pid = (m.project_id or "").lower()
            m_name = (m.name or "").lower()
            if (m_slug and m_slug == mod_id_clean) or (m_pid and m_pid == mod_id_clean) or (name_clean and m_name == name_clean):
                if m.filename:
                    deleted_filenames.append(m.filename)
            else:
                retained.append(m)

        self._active_profile.mods = retained
        self._active_profile.user_mods = [s for s in self._active_profile.user_mods if s.lower() != mod_id_clean]
        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()

        profile_path = self._active_profile.mods_path
        def _delete_files():
            for fn in deleted_filenames:
                try:
                    p = profile_path / fn
                    if p.exists():
                        p.unlink()
                    p_dis = profile_path / (fn + ".disabled")
                    if p_dis.exists():
                        p_dis.unlink()
                except Exception as ex:
                    print(f"[ProfileController] Error unlinking mod jars: {ex}")
            self._syncNeeded.emit()

        threading.Thread(target=_delete_files, daemon=True).start()

    @Slot(str, str, str, str)
    def switchModVersion(self, mod_id: str, version_number: str, filename: str = "", download_url: str = "") -> None:
        if not self._active_profile:
            return
        slug_clean = mod_id.strip().lower()
        target_mod = None
        for m in self._active_profile.mods:
            m_slug = (m.slug or "").lower()
            m_pid = (m.project_id or "").lower()
            if m_slug == slug_clean or m_pid == slug_clean:
                target_mod = m
                break
        if not target_mod:
            return

        old_filename = target_mod.filename
        target_mod.version = version_number
        target_mod.version_id = version_number
        if filename:
            target_mod.filename = filename
        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()

        profile_ref = self._active_profile
        mod_ref = target_mod
        def _switch_worker():
            try:
                if old_filename and old_filename != mod_ref.filename:
                    old_jar = profile_ref.mods_path / old_filename
                    if old_jar.exists():
                        old_jar.unlink()
                sync_profile_mods(profile_ref)
                self._syncNeeded.emit()
            except Exception as e:
                print(f"[ProfileController] Error switching mod version: {e}")

        threading.Thread(target=_switch_worker, daemon=True).start()

    @Slot(str, str)
    def updateModVersion(self, mod_id: str, new_version: str) -> None:
        if not self._active_profile:
            return
        target_mod = None
        for m in self._active_profile.mods:
            m_slug = (m.slug or "").lower()
            m_pid = (m.project_id or "").lower()
            if m_slug == mod_id.lower() or m_pid == mod_id.lower() or m.name.lower() == mod_id.lower():
                target_mod = m
                break
        if not target_mod:
            return

        target_mod.version = new_version
        target_mod.version_id = new_version
        self._store.save()
        self._sync_models()
        self.activeProfileChanged.emit()

        profile_ref = self._active_profile
        def _bg():
            try:
                sync_profile_mods(profile_ref)
                self._syncNeeded.emit()
            except Exception as e:
                print(f"[ProfileController] Error updating mod: {e}")
        threading.Thread(target=_bg, daemon=True).start()

    @Slot()
    def updateAllMods(self) -> None:
        if not self._active_profile or not self._mod_updates:
            return
        for m in self._active_profile.mods:
            key = m.project_id or m.slug or m.name
            if key in self._mod_updates:
                m.version = self._mod_updates[key]
                m.version_id = self._mod_updates[key]
        self._mod_updates = {}
        self._store.save()
        self._sync_models()
        self.modUpdatesChanged.emit()
        self.activeProfileChanged.emit()

        profile_ref = self._active_profile
        def _bg():
            try:
                sync_profile_mods(profile_ref)
                self._syncNeeded.emit()
            except Exception as e:
                print(f"[ProfileController] Error bulk-updating mods: {e}")
        threading.Thread(target=_bg, daemon=True).start()

    @Slot()
    def updateEzClient(self) -> None:
        self.applyEzClientUpdates()

    @Slot(str, str, str, str, result=bool)
    def isModInstalled(self, project_id: str = "", slug: str = "", name: str = "", filename: str = "") -> bool:
        if not self._active_profile:
            return False
        p_id = (project_id or "").strip().lower()
        p_slug = (slug or "").strip().lower()
        p_name = (name or "").strip().lower()
        p_fn = (filename or "").strip().lower()

        for m in self._active_profile.mods:
            m_pid = (m.project_id or "").lower()
            m_slug = (m.slug or "").lower()
            m_name = (m.name or "").lower()
            m_fn = (m.filename or "").lower()
            matches = ((p_id and (m_pid == p_id or m_slug == p_id)) or
                       (p_slug and (m_slug == p_slug or m_pid == p_slug)) or
                       (p_name and m_name == p_name) or
                       (p_fn and m_fn == p_fn))
            if matches and self._mod_file_exists(self._active_profile, m):
                return True
        return False

    @staticmethod
    def _mod_file_exists(profile: ProfileData, mod: ModData) -> bool:
        filename = (mod.filename or "").strip()
        if not filename:
            return False
        candidates = [profile.mods_path / filename]
        if filename.lower().endswith(".zip"):
            candidates.extend((profile.path / "shaderpacks" / filename, profile.path / "resourcepacks" / filename))
        def valid_file(path: Path) -> bool:
            try:
                return path.is_file() and path.stat().st_size > 1024
            except OSError:
                return False

        return any(valid_file(path) or valid_file(path.with_name(path.name + ".disabled")) for path in candidates)

    @Slot()
    def launchActiveProfile(self) -> None:
        if self._is_launching:
            return
        if not self._active_profile:
            self.launchStatusChanged.emit("Kein Profil ausgewählt!", True)
            return

        self._is_launching = True
        profile = self._active_profile

        # Early initialization of LiveLogService so instance and logs are active immediately
        log_file = profile.path / "ezclient_latest_run.log"
        instance_id = self._live_log_service.begin_instance(
            log_file,
            profile.name,
            f"{profile.loader} {profile.minecraft_version}",
            "Player",
            str(profile.path),
        )
        self._live_log_service.append_system_message(
            f"Starte Vorbereitung für Profil '{profile.name}' ({profile.loader} {profile.minecraft_version})…",
            instance_id=instance_id,
        )

        self.launchStatusChanged.emit("Starte Vorbereitung…", False)

        def _launch_worker():
            def _safe_emit_status(message: str, is_err: bool = False) -> None:
                try:
                    self.launchStatusChanged.emit(message, is_err)
                except RuntimeError:
                    pass

            try:
                from backend.services.minecraft import minecraft_dir
                mc = minecraft_dir()

                # Sync mods with status logging to live log
                def _sync_status(msg: str) -> None:
                    self._live_log_service.append_system_message(msg, instance_id=instance_id)

                self._live_log_service.append_system_message("Synchronisiere Mods & Assets…", instance_id=instance_id)
                _safe_emit_status("Synchronisiere Mods & Assets…", False)
                sync_profile_mods(profile, status_callback=_sync_status)

                # Prefer direct launch
                direct_launch = self.preferDirectLaunch and self.isDirectLaunchReady
                if direct_launch:
                    def _direct_status(message: str) -> None:
                        self._live_log_service.append_system_message(message, instance_id=instance_id)
                        _safe_emit_status(message, False)

                    _safe_emit_status("Starte Minecraft direkt…", False)
                    proc = launch_minecraft_direct(profile, _direct_status, log_file)
                    if not proc:
                        self._live_log_service.append_system_message(
                            "Minecraft wurde nicht gestartet. Prüfe die obigen Meldungen.",
                            "ERROR", instance_id,
                        )
                        self._live_log_service.detach_process(instance_id)
                        raise RuntimeError("Minecraft konnte nicht gestartet werden. Details stehen in den Live-Logs.")
                    self._live_log_service.attach_process(
                        proc, log_file, profile.name,
                        f"{profile.loader} {profile.minecraft_version}", "Player",
                        str(profile.path), instance_id,
                    )
                    self._is_launching = False
                    _safe_emit_status("Minecraft läuft!", False)
                    if self.minimizeToTray:
                        try:
                            self.hideToTrayRequested.emit()
                        except RuntimeError:
                            pass
                    return

                # Official launcher fallback
                self._live_log_service.append_system_message("Patching Launcher-Profil…", instance_id=instance_id)
                _safe_emit_status("Patching Launcher-Profil…", False)
                patch_launcher_profile(profile, mc)
                self._live_log_service.append_system_message("Starte offiziellen Launcher…", instance_id=instance_id)
                _safe_emit_status("Starte offiziellen Launcher…", False)
                exit_code = launch_minecraft_official(profile, mc)
                self._is_launching = False
                _safe_emit_status("Launcher gestartet", False)
                if self.minimizeToTray:
                    try:
                        self.hideToTrayRequested.emit()
                    except RuntimeError:
                        pass
            except Exception as e:
                self._is_launching = False
                print(f"[ProfileController] Launch error: {e}")
                self._live_log_service.append_system_message(f"Fehler beim Start: {e}", "ERROR", instance_id=instance_id)
                _safe_emit_status(f"Fehler: {e}", True)
                try:
                    self.gameCrashed.emit("Startfehler", str(e), str(e))
                    self.analyzeCrash(str(e))
                except RuntimeError:
                    pass

        threading.Thread(target=_launch_worker, daemon=True).start()

    @Slot(str)
    def copyToClipboard(self, text: str) -> None:
        from PySide6.QtGui import QGuiApplication
        cb = QGuiApplication.clipboard()
        if cb:
            cb.setText(text)
            self.settingSaved.emit("In Zwischenablage kopiert!")

    @Slot(str)
    def openFolder(self, path_str: str) -> None:
        import subprocess
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

    @Slot(str, result="QVariantList")
    def getDependentMods(self, mod_id_or_slug: str) -> list[str]:
        return []

    @Slot(str, result="QVariantList")
    def checkDependentMods(self, mod_id: str) -> list[str]:
        return self.getDependentMods(mod_id)

    @Slot(bool)
    def enableAllMods(self, enable: bool) -> None:
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
        if not self._active_profile:
            return
        new_p = self._store.duplicate_profile(self._active_profile.id)
        if new_p:
            self._sync_models()
            self.profilesChanged.emit()

    @Slot()
    def openScreenshotsFolder(self) -> None:
        appdata = os.getenv("APPDATA")
        mc_p = Path(appdata) / ".minecraft" if appdata else Path.home() / ".minecraft"
        target = mc_p / "screenshots"
        if self._active_profile and (self._active_profile.path / "screenshots").exists():
            target = self._active_profile.path / "screenshots"
        self.openFolder(str(target))

    @Slot()
    def openLogsFolder(self) -> None:
        appdata = os.getenv("APPDATA")
        mc_p = Path(appdata) / ".minecraft" if appdata else Path.home() / ".minecraft"
        target = mc_p / "logs"
        if self._active_profile and (self._active_profile.path / "logs").exists():
            target = self._active_profile.path / "logs"
        self.openFolder(str(target))

    @Slot()
    def detectJava(self) -> None:
        j = java_path()
        if j:
            self.settingSaved.emit(f"Java gefunden: {Path(j).name}")
        else:
            self.settingSaved.emit("Java-Laufzeitumgebung erkannt und bereit")

    @Slot(int)
    def openJavaLocation(self, major: int = 21) -> None:
        from backend.services.java_runtime import java_executable
        j = java_executable(major, minecraft_dir())
        if j:
            self.openFolder(str(Path(j).parent))

    @Slot(int)
    def deleteJava(self, major: int = 21) -> None:
        from backend.services.java_runtime import delete_runtime
        success, msg = delete_runtime(major, minecraft_dir())
        self.javaRuntimesChanged.emit()
        self.settingSaved.emit(msg)

    @Slot(int)
    def installJava(self, major: int = 21) -> None:
        def _bg():
            from backend.services.java_runtime import ensure_runtime
            try:
                ensure_runtime(major, minecraft_dir(), progress=lambda p, msg: self.settingSaved.emit(msg))
                self.javaRuntimesChanged.emit()
                self.settingSaved.emit(f"Java {major} erfolgreich installiert!")
            except Exception as e:
                self.settingSaved.emit(f"Fehler bei Java-Installation: {e}")
        threading.Thread(target=_bg, daemon=True).start()

    @Slot(int)
    def reinstallJava(self, major: int = 21) -> None:
        self.deleteJava(major)
        self.installJava(major)
