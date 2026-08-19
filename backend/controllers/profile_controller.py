import sys
import os
from typing import Any
from pathlib import Path
from dataclasses import asdict
import threading
import time
from PySide6.QtCore import QObject, Signal, Slot, Property
from backend.models.types import ProfileData, ModData
from backend.services.store import ProfileStore
from backend.models.profile_model import ProfileModel
from backend.models.mod_model import ModModel
from backend.services.minecraft import detect_launcher, launch_minecraft_official, patch_launcher_profile, java_path
from backend.services.mod_downloader import sync_profile_mods
from backend.services.process_watcher import MinecraftWatcher
from backend.services.direct_launch import launch_minecraft_direct
from backend.services.live_log_service import LiveLogService
from backend.services.mod_scanner import InstalledModRegistry
from PySide6.QtWidgets import QFileDialog

class ProfileController(QObject):
    activeProfileChanged = Signal()
    profilesChanged = Signal()
    launchStatusChanged = Signal(str, bool)
    gameCrashed = Signal(str, str, str)  # (title, shortError, fullLog)
    settingsChanged = Signal()
    settingSaved = Signal(str)
    hideToTrayRequested = Signal()
    restoreFromTrayRequested = Signal()
    onboardingStepProgress = Signal(float, str, str)
    onboardingFinished = Signal(str)
    _syncNeeded = Signal()

    def __init__(self, store: ProfileStore, profile_model: ProfileModel, mod_model: ModModel, parent=None):
        super().__init__(parent)
        self._store = store
        self._profile_model = profile_model
        self._mod_model = mod_model
        self._live_log_service = LiveLogService(self)
        self._active_profile: ProfileData | None = self._store.get_last_or_default()
        self._installed_registry = InstalledModRegistry()
        self._is_launching: bool = False
        self._syncNeeded.connect(self._sync_models)
        self._sync_models()
        if self._active_profile:
            threading.Thread(target=lambda: sync_profile_mods(self._active_profile), daemon=True).start()

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
            "Hintergrundbild auswählen",
            "",
            "Bilder (*.png *.jpg *.jpeg *.webp);;Alle Dateien (*.*)"
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
                self._installed_registry.scan_directory(self._active_profile.mods_path, self._active_profile.mods)
            except Exception as e:
                print(f"[ProfileController] Registry scan error: {e}")
            self._mod_model.set_mods(self._active_profile.mods)
        else:
            self._installed_registry.scan_directory(Path("/nonexistent"))
            self._mod_model.set_mods([])
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()

    @Property("QVariantList", notify=activeProfileChanged)
    def installedMods(self) -> list:
        return self._installed_registry.installed_mods

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

    @Slot(int)
    def setActiveRamMb(self, mb: int) -> None:
        if not self._active_profile:
            return
        self._active_profile.ram_mb = mb
        self._store.save()
        self.activeProfileChanged.emit()
        self.settingSaved.emit(f"RAM auf {mb // 1024} GB zugewiesen (gespeichert)")

    @Slot(str)
    def duplicateProfile(self, profile_id: str) -> None:
        dup = self._store.duplicate_profile(profile_id)
        if dup:
            self._active_profile = dup
            self._sync_models()
            self.profilesChanged.emit()
            self.activeProfileChanged.emit()

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
    def createProfile(self, name: str, version: str, loader: str = "Fabric", preset: str = "performance") -> None:
        profile = self._store.create_profile(name, version, preset=preset)
        profile.loader = loader
        self._store.save()
        self._active_profile = profile
        self._profile_model.set_profiles(self._store.profiles)
        self._mod_model.set_mods(profile.mods)
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()
        threading.Thread(target=lambda: sync_profile_mods(profile), daemon=True).start()

    @Slot(str, str, str, str)
    def createProfileWithLiveDownloads(self, name: str, version: str, loader: str, preset: str) -> None:
        """Creates a profile and runs live mod downloads in background thread with progress feedback."""
        from backend.services.mod_downloader import download_file, sync_profile_mods
        from backend.services.modrinth import ModrinthService

        def worker():
            import shutil
            import time
            profile = self._store.create_profile(name, version, loader=loader, preset=preset)
            svc = ModrinthService()
            try:
                from backend.services.direct_launch import ensure_profile_defaults
                ensure_profile_defaults(profile.path)
            except Exception:
                pass

            total = len(profile.mods)
            for idx, m in enumerate(profile.mods):
                p = idx / max(1, total)
                self.onboardingStepProgress.emit(p, m.name, f"Richte {m.name} ein… ({idx+1}/{total})")

                is_ezclient = (m.slug and m.slug.lower() in ("ezclient", "ezclient-core")) or (m.filename and m.filename.lower() == "ezclient.jar") or ("ezclient" in m.name.lower())
                if is_ezclient:
                    # Resolve EzClient.jar across all candidate locations
                    candidates = [
                        Path(sys._MEIPASS) / "backend" / "assets" / "EzClient.jar" if hasattr(sys, "_MEIPASS") else None,
                        Path(sys._MEIPASS) / "assets" / "EzClient.jar" if hasattr(sys, "_MEIPASS") else None,
                        Path(__file__).resolve().parent.parent / "assets" / "EzClient.jar",
                        Path(__file__).resolve().parent / "assets" / "EzClient.jar",
                        Path(sys.executable).parent / "backend" / "assets" / "EzClient.jar",
                        Path.cwd() / "backend" / "assets" / "EzClient.jar",
                    ]
                    copied = False
                    for c in candidates:
                        if c and c.exists() and c.is_file() and c.stat().st_size > 100:
                            try:
                                profile.mods_path.mkdir(parents=True, exist_ok=True)
                                shutil.copy2(c, profile.mods_path / "EzClient.jar")
                                copied = True
                                break
                            except Exception as ex:
                                print(f"[EzClient Copy] Error: {ex}")
                    if not copied:
                        # Ensure empty file exists as fallback
                        try:
                            (profile.mods_path / "EzClient.jar").touch(exist_ok=True)
                        except Exception:
                            pass
                    p_after = (idx + 1) / max(1, total)
                    self.onboardingStepProgress.emit(p_after, m.name, f"✓ {m.name} bereitgestellt ({idx+1}/{total})")
                    time.sleep(0.08)
                    continue

                try:
                    vers = svc.get_project_versions(m.slug or m.project_id, mc_version=version, loader=loader)
                    if not vers:
                        vers = svc.get_project_versions(m.slug or m.project_id, loader=loader)
                    if not vers:
                        vers = svc.get_project_versions(m.slug or m.project_id)
                    if vers:
                        best = next((v for v in vers if v.get("version_type") == "release"), vers[0])
                        files = best.get("files", [])
                        primary = next((f for f in files if f.get("primary")), files[0] if files else None)
                        if primary and primary.get("url"):
                            dest = profile.mods_path / primary.get("filename", f"{m.slug}.jar")
                            download_file(primary["url"], dest)
                            m.filename = primary.get("filename", m.filename)
                except Exception as e:
                    print(f"[Onboarding Download] Error for {m.name}: {e}")

                p_after = (idx + 1) / max(1, total)
                self.onboardingStepProgress.emit(p_after, m.name, f"✓ {m.name} installiert ({idx+1}/{total})")
                time.sleep(0.04)

            # Final dependency sync pass to ensure 100% completeness
            try:
                sync_profile_mods(profile, svc)
            except Exception as ex:
                print(f"[Sync Profile Mods] Handled: {ex}")

            self._active_profile = profile
            self._store.save()
            self._syncNeeded.emit()
            self.onboardingStepProgress.emit(1.0, "Fertig", "Profil erfolgreich eingerichtet & optimiert!")
            time.sleep(0.4)
            self.onboardingFinished.emit(profile.id)

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
                m.version = version or m.version
                m.enabled = True
                self._store.save()
                self._sync_models()
                self.profilesChanged.emit()
                self.activeProfileChanged.emit()
                self.settingSaved.emit(f"Mod aktualisiert: {m.name}")
                p_ref = self._active_profile
                threading.Thread(target=lambda: sync_profile_mods(p_ref), daemon=True).start()
                return

        new_mod = ModData(
            project_id=mod_id,
            slug=mod_id,
            name=name,
            version_id="",
            version=version or "Latest",
            filename=filename or f"{mod_id}.jar",
            enabled=True,
            essential=False,
            icon_url=icon_url,
            author=author or ("CurseForge" if source == "curseforge" else "Modrinth"),
            description=description,
            source=source
        )
        self._active_profile.mods.append(new_mod)

        # Check and auto-install required dependencies from Modrinth if Modrinth mod
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
                        dep_names.append(d.get("name", d_slug))
                        existing_slugs.add(d_slug)
            except Exception as e:
                print(f"[ProfileController] Dependency check error for {name}: {e}")

        self._store.save()
        self._sync_models()
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()

        if dep_names:
            self.settingSaved.emit(f"✓ {name} & {len(dep_names)} benötigte Abhängigkeiten ({', '.join(dep_names)}) installiert!")
        else:
            self.settingSaved.emit(f"✓ Mod installiert: {name}")

        # Download jar and all dependencies in background immediately
        p_ref = self._active_profile
        threading.Thread(target=lambda: sync_profile_mods(p_ref), daemon=True).start()

    @Slot(str)
    @Slot(str, str)
    def uninstallMod(self, mod_id: str, mod_name: str = "") -> None:
        if not self._active_profile:
            return
        targets = {str(mod_id).strip().lower()}
        if mod_name:
            targets.add(str(mod_name).strip().lower())
        targets.discard("")
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
        self._store.save()

        # Remove jar from disk
        try:
            for target in targets:
                for f in self._active_profile.mods_path.glob(f"*{target}*"):
                    try:
                        f.unlink(missing_ok=True)
                    except Exception:
                        pass
        except Exception as ex:
            print(f"[ProfileController] Error unlinking mod jars: {ex}")

        self._sync_models()
        self.profilesChanged.emit()
        self.activeProfileChanged.emit()
        if removed_name:
            self.settingSaved.emit(f"Mod gelöscht: {removed_name}")

    @Slot(str, str)
    def updateModVersion(self, mod_id: str, new_version: str) -> None:
        if not self._active_profile:
            return
        mid = str(mod_id).strip().lower()
        for m in self._active_profile.mods:
            if (m.project_id and m.project_id.lower() == mid) or \
               (m.slug and m.slug.lower() == mid) or \
               (m.name and m.name.lower() == mid):
                m.version = new_version
                self._store.save()
                self._sync_models()
                self.profilesChanged.emit()
                self.activeProfileChanged.emit()
                self.settingSaved.emit(f"Version geändert: {m.name} ({new_version})")
                p_ref = self._active_profile
                threading.Thread(target=lambda: sync_profile_mods(p_ref), daemon=True).start()
                return

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
            try:
                # 1. Sync all enabled mods to profile.mods_path
                sync_profile_mods(self._active_profile, status_callback=lambda s: self.launchStatusChanged.emit(s, False))
                
                # 2. Check if Direct Launch is preferred
                proc = None
                if self.preferDirectLaunch:
                    self.launchStatusChanged.emit("Direktstart wird vorbereitet (Token & Java)…", False)
                    proc = launch_minecraft_direct(
                        self._active_profile,
                        status_callback=lambda s: self.launchStatusChanged.emit(s, False)
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
                    self.launchStatusChanged.emit(f"Minecraft läuft (Direktstart) · {self._active_profile.name}", False)

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

                # 3. Fallback: Official Minecraft Launcher with Autokill
                self.launchStatusChanged.emit("Minecraft Launcher wird gestartet…", False)
                patch_launcher_profile(self._active_profile)
                launch_minecraft_official()

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
            self.selectProfile(new_p.id)

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
