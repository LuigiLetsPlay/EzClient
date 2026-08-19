from typing import Any
from PySide6.QtCore import QAbstractListModel, Qt, QModelIndex, QByteArray
from backend.models.types import ModData

class ModModel(QAbstractListModel):
    ProjectIdRole = Qt.UserRole + 1
    SlugRole = Qt.UserRole + 2
    NameRole = Qt.UserRole + 3
    VersionRole = Qt.UserRole + 4
    FilenameRole = Qt.UserRole + 5
    EnabledRole = Qt.UserRole + 6
    RecommendedRole = Qt.UserRole + 7
    AuthorRole = Qt.UserRole + 8
    DescriptionRole = Qt.UserRole + 9
    IconUrlRole = Qt.UserRole + 10
    SourceRole = Qt.UserRole + 11

    def __init__(self, parent=None):
        super().__init__(parent)
        self._mods: list[ModData] = []

    def roleNames(self) -> dict[int, QByteArray]:
        return {
            self.ProjectIdRole: QByteArray(b"projectId"),
            self.SlugRole: QByteArray(b"slug"),
            self.NameRole: QByteArray(b"name"),
            self.VersionRole: QByteArray(b"version"),
            self.FilenameRole: QByteArray(b"filename"),
            self.EnabledRole: QByteArray(b"enabled"),
            self.RecommendedRole: QByteArray(b"recommended"),
            self.AuthorRole: QByteArray(b"author"),
            self.DescriptionRole: QByteArray(b"description"),
            self.IconUrlRole: QByteArray(b"iconUrl"),
            self.SourceRole: QByteArray(b"source"),
        }

    def rowCount(self, parent=QModelIndex()) -> int:
        return len(self._mods)

    def data(self, index: QModelIndex, role: int = Qt.DisplayRole) -> Any:
        if not index.isValid() or not (0 <= index.row() < len(self._mods)):
            return None
        m = self._mods[index.row()]
        if role == self.ProjectIdRole:
            return m.project_id
        elif role == self.SlugRole:
            return m.slug
        elif role == self.NameRole:
            return m.name
        elif role == self.VersionRole:
            return m.version
        elif role == self.FilenameRole:
            return m.filename
        elif role == self.EnabledRole:
            return m.enabled
        elif role == self.RecommendedRole:
            return m.recommended
        elif role == self.AuthorRole:
            return m.author or "Modrinth"
        elif role == self.DescriptionRole:
            return m.description
        elif role == self.IconUrlRole:
            return m.icon_url
        elif role == self.SourceRole:
            return getattr(m, "source", "modrinth")
        return None

    def set_mods(self, mods: list[ModData]) -> None:
        self.beginResetModel()
        self._mods = list(mods)
        self.endResetModel()

    def get_by_index(self, index: int) -> ModData | None:
        if 0 <= index < len(self._mods):
            return self._mods[index]
        return None
