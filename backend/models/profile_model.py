from typing import Any

from PySide6.QtCore import QAbstractListModel, Qt, QModelIndex, QByteArray, Slot, Signal
from backend.models.types import ProfileData

class ProfileModel(QAbstractListModel):
    IdRole = Qt.UserRole + 1
    NameRole = Qt.UserRole + 2
    VersionRole = Qt.UserRole + 3
    LoaderRole = Qt.UserRole + 4
    ModsCountRole = Qt.UserRole + 5
    LastPlayedRole = Qt.UserRole + 6
    OptimizeRole = Qt.UserRole + 7
    IconRole = Qt.UserRole + 8

    def __init__(self, parent=None):
        super().__init__(parent)
        self._profiles: list[ProfileData] = []

    def roleNames(self) -> dict[int, QByteArray]:
        return {
            self.IdRole: QByteArray(b"profileId"),
            self.NameRole: QByteArray(b"profileName"),
            self.VersionRole: QByteArray(b"minecraftVersion"),
            self.LoaderRole: QByteArray(b"loader"),
            self.ModsCountRole: QByteArray(b"modsCount"),
            self.LastPlayedRole: QByteArray(b"lastPlayed"),
            self.OptimizeRole: QByteArray(b"optimize"),
            self.IconRole: QByteArray(b"icon"),
        }

    def rowCount(self, parent=QModelIndex()) -> int:
        return len(self._profiles)

    def data(self, index: QModelIndex, role: int = Qt.DisplayRole) -> Any:
        if not index.isValid() or not (0 <= index.row() < len(self._profiles)):
            return None
        p = self._profiles[index.row()]
        if role == self.IdRole:
            return p.id
        elif role == self.NameRole:
            return p.name
        elif role == self.VersionRole:
            return p.minecraft_version
        elif role == self.LoaderRole:
            return p.loader
        elif role == self.ModsCountRole:
            return len(p.mods)
        elif role == self.LastPlayedRole:
            return p.last_played or "Never"
        elif role == self.OptimizeRole:
            return p.optimize
        elif role == self.IconRole:
            return getattr(p, "icon", "") or ""
        return None

    def set_profiles(self, profiles: list[ProfileData]) -> None:
        self.beginResetModel()
        self._profiles = list(profiles)
        self.endResetModel()

    def get_by_index(self, index: int) -> ProfileData | None:
        if 0 <= index < len(self._profiles):
            return self._profiles[index]
        return None
