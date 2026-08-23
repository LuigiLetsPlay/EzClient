import os
import sys
import threading
import webbrowser
from pathlib import Path
from PySide6.QtCore import QObject, Signal, Slot, Property, QCoreApplication, QTimer
from backend.models.types import APP_VERSION, GITHUB_REPO
from backend.services.updater import check_for_updates, download_update_file, run_installer_and_exit


class UpdateController(QObject):
    updateStateChanged = Signal()
    downloadProgressChanged = Signal(float, str)
    updateReadyToInstall = Signal(str)
    checkFinished = Signal(bool, str)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._current_version: str = APP_VERSION
        self._latest_version: str = APP_VERSION
        self._update_available: bool = False
        self._release_name: str = ""
        self._changelog: str = ""
        self._published_at: str = ""
        self._download_url: str = ""
        self._asset_name: str = ""
        self._asset_size_mb: float = 0.0
        self._html_url: str = f"https://github.com/{GITHUB_REPO}/releases"

        self._is_checking: bool = False
        self._is_downloading: bool = False
        self._download_progress: float = 0.0
        self._download_status: str = ""
        self._downloaded_file: Path | None = None
        self._update_ready: bool = False
        self._status_message: str = ""

        # Auto-check for updates in background on startup (delayed 3s)
        threading.Timer(3.0, lambda: self.checkForUpdates(silent=True)).start()

    # ── Properties ──

    @Property(str, notify=updateStateChanged)
    def currentVersion(self) -> str:
        return self._current_version

    @Property(str, notify=updateStateChanged)
    def latestVersion(self) -> str:
        return self._latest_version

    @Property(bool, notify=updateStateChanged)
    def updateAvailable(self) -> bool:
        return self._update_available

    @Property(str, notify=updateStateChanged)
    def releaseName(self) -> str:
        return self._release_name

    @Property(str, notify=updateStateChanged)
    def changelog(self) -> str:
        return self._changelog

    @Property(str, notify=updateStateChanged)
    def publishedAt(self) -> str:
        return self._published_at

    @Property(str, notify=updateStateChanged)
    def downloadUrl(self) -> str:
        return self._download_url

    @Property(str, notify=updateStateChanged)
    def assetName(self) -> str:
        return self._asset_name

    @Property(float, notify=updateStateChanged)
    def assetSizeMb(self) -> float:
        return self._asset_size_mb

    @Property(bool, notify=updateStateChanged)
    def isChecking(self) -> bool:
        return self._is_checking

    @Property(bool, notify=updateStateChanged)
    def isDownloading(self) -> bool:
        return self._is_downloading

    @Property(float, notify=downloadProgressChanged)
    def downloadProgress(self) -> float:
        return self._download_progress

    @Property(str, notify=downloadProgressChanged)
    def downloadStatus(self) -> str:
        return self._download_status

    @Property(bool, notify=updateStateChanged)
    def updateReady(self) -> bool:
        return self._update_ready

    @Property(str, notify=updateStateChanged)
    def statusMessage(self) -> str:
        return self._status_message

    # ── Slots ──

    @Slot()
    @Slot(bool)
    def checkForUpdates(self, silent: bool = False) -> None:
        """Asynchronously checks GitHub for updates."""
        if self._is_checking or self._is_downloading:
            return

        self._is_checking = True
        self._status_message = "Prüfe auf Updates…"
        self.updateStateChanged.emit()

        def worker():
            res = check_for_updates(self._current_version)
            self._is_checking = False

            if res and res.get("update_available"):
                self._update_available = True
                self._latest_version = res.get("latest_version", "")
                self._release_name = res.get("release_name", "")
                self._changelog = res.get("changelog", "")
                self._published_at = res.get("published_at", "")
                self._download_url = res.get("download_url", "")
                self._asset_name = res.get("asset_name", "")
                self._asset_size_mb = res.get("asset_size_mb", 0.0)
                self._html_url = res.get("html_url", "")
                self._status_message = f"Update verfügbar: v{self._latest_version}"
                self.updateStateChanged.emit()
                self.checkFinished.emit(True, f"Neues Update v{self._latest_version} verfügbar!")
            else:
                self._update_available = False
                self._status_message = "EzClient ist auf dem neuesten Stand."
                self.updateStateChanged.emit()
                if not silent:
                    self.checkFinished.emit(False, "EzClient ist bereits auf dem neuesten Stand.")

        threading.Thread(target=worker, daemon=True).start()

    @Slot()
    def startDownload(self) -> None:
        """Downloads the update binary."""
        if not self._download_url or self._is_downloading:
            return

        self._is_downloading = True
        self._download_progress = 0.0
        self._download_status = "Starte Download…"
        self._update_ready = False
        self.updateStateChanged.emit()
        self.downloadProgressChanged.emit(0.0, self._download_status)

        def progress_cb(pct: float, text: str):
            self._download_progress = pct
            self._download_status = text
            self.downloadProgressChanged.emit(pct, text)

        def worker():
            target_name = self._asset_name or f"EzClient-v{self._latest_version}.exe"
            file_path = download_update_file(self._download_url, target_name, progress_cb)
            self._is_downloading = False

            if file_path and file_path.exists():
                self._downloaded_file = file_path
                self._update_ready = True
                self._status_message = "Update bereit zur Installation."
                self.updateStateChanged.emit()
                self.updateReadyToInstall.emit(str(file_path))
            else:
                self._status_message = "Download fehlgeschlagen. Bitte manuell herunterladen."
                self.updateStateChanged.emit()

        threading.Thread(target=worker, daemon=True).start()

    @Slot()
    def installAndRestart(self) -> None:
        """Executes the downloaded installer/exe and quits."""
        if self._downloaded_file and self._downloaded_file.exists():
            if run_installer_and_exit(self._downloaded_file):
                self._status_message = "Installer wurde gestartet. EzClient wird geschlossen…"
                self.updateStateChanged.emit()
                # Let Qt dispatch the status update before releasing the EXE.
                QTimer.singleShot(150, QCoreApplication.quit)
            else:
                self._status_message = "Installer konnte nicht gestartet werden. Bitte erneut herunterladen."
                self._update_ready = False
                self.updateStateChanged.emit()
        elif self._html_url:
            webbrowser.open(self._html_url)

    @Slot()
    def openReleasePage(self) -> None:
        """Opens GitHub releases page in default web browser."""
        webbrowser.open(self._html_url or f"https://github.com/{GITHUB_REPO}/releases")
