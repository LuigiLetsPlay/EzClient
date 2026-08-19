import threading
import re
from typing import Any
from PySide6.QtCore import QObject, Signal, Slot, Property
from backend.services.modrinth import ModrinthService
from backend.services.curseforge import CurseForgeService


class ModrinthController(QObject):
    projectTypeChanged = Signal()
    searchResultsChanged = Signal()
    selectedModChanged = Signal()
    loadingChanged = Signal()
    versionsChanged = Signal()
    versionTypeFilterChanged = Signal()
    mcVersionChanged = Signal()
    statusChanged = Signal(str)
    gameVersionsChanged = Signal()
    sourceChanged = Signal()
    loaderChanged = Signal()

    # Internal thread-safe queued signals
    _searchDoneSignal = Signal(dict, bool)
    _versionsDoneSignal = Signal(list)
    _projectDoneSignal = Signal(dict)
    _errorSignal = Signal(str)

    def __init__(self, profile_controller=None, parent=None):
        super().__init__(parent)
        self._profile_controller = profile_controller
        self._modrinth_svc = ModrinthService()
        self._curseforge_svc = CurseForgeService()

        self._source: str = "all"  # "all", "modrinth", "curseforge"
        self._loader: str = "fabric"  # "fabric", "forge", "neoforge", "quilt"
        self._results: list[dict] = []
        self._total_hits: int = 0
        self._selected: dict = {}
        self._versions: list[dict] = []
        self._version_type_filter: str = "release"  # Release is standard filter!
        self._loading: bool = False
        self._query: str = ""
        self._project_type: str = "mod"  # "mod", "shader", "resourcepack"
        self._mc_version: str = "26.2"  # Default to 26.2
        self._category: str = "All"
        self._sort: str = "relevance"
        self._offset: int = 0
        self._game_versions: list[str] = [
            "All", "26.2", "26.1", "1.21.8", "1.21.7", "1.21.6", "1.21.5",
            "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21", "1.20.6",
            "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"
        ]

        # Connect internal thread signals to main thread slots
        self._searchDoneSignal.connect(self._on_search_done)
        self._versionsDoneSignal.connect(self._on_versions_done)
        self._projectDoneSignal.connect(self._on_project_done)
        self._errorSignal.connect(self._on_error)

    def set_profile_controller(self, pc) -> None:
        self._profile_controller = pc

    # ---- Properties ----
    @Property(str, notify=sourceChanged)
    def source(self) -> str:
        return self._source

    @Slot(str)
    def setSource(self, s: str) -> None:
        val = str(s).lower().strip()
        if val in ("all", "modrinth", "curseforge") and self._source != val:
            self._source = val
            self.sourceChanged.emit()
            self.search()

    @Property(str, notify=loaderChanged)
    def loader(self) -> str:
        return self._loader

    @Slot(str)
    def setLoader(self, ldr: str) -> None:
        val = str(ldr).lower().strip()
        if self._loader != val:
            self._loader = val
            self.loaderChanged.emit()
            self.search()

    @Property(bool, notify=loadingChanged)
    def loading(self) -> bool:
        return self._loading

    @Property(int, notify=searchResultsChanged)
    def totalHits(self) -> int:
        return self._total_hits

    @Property("QVariantList", notify=searchResultsChanged)
    def results(self) -> list:
        # Check installed status dynamically against active profile
        enriched = []
        for r in self._results:
            item = dict(r)
            item["is_installed"] = self._check_is_installed(item)
            enriched.append(item)
        return enriched

    def _check_is_installed(self, item: dict) -> bool:
        if not self._profile_controller:
            return False
        return self._profile_controller.isModInstalled(
            item.get("project_id", ""),
            item.get("slug", ""),
            item.get("title", "") or item.get("name", ""),
            item.get("filename", "")
        )

    @Property("QVariantList", notify=versionsChanged)
    def versions(self) -> list:
        return self._versions

    @Property("QVariantList", notify=versionTypeFilterChanged)
    def filteredVersions(self) -> list:
        res = self._versions or []
        if self._version_type_filter != "all":
            filtered = [v for v in res if v.get("version_type") == self._version_type_filter]
            return filtered if filtered else res
        return res

    @Property(str, notify=versionTypeFilterChanged)
    def versionTypeFilter(self) -> str:
        return self._version_type_filter

    @Property(str, notify=projectTypeChanged)
    def projectType(self) -> str:
        return self._project_type

    @Slot(str)
    def setProjectType(self, pt: str) -> None:
        val = str(pt).lower().strip()
        if val in ("mod", "shader", "resourcepack", "datapack") and self._project_type != val:
            self._project_type = val
            self.projectTypeChanged.emit()
            self.search()

    @Property(str, notify=mcVersionChanged)
    def mcVersion(self) -> str:
        return self._mc_version

    @Property("QVariantMap", notify=selectedModChanged)
    def selectedMod(self) -> dict:
        if not self._selected:
            return {}
        sel = dict(self._selected)
        sel["is_installed"] = self._check_is_installed(sel)
        return sel

    @Property("QVariantList", notify=gameVersionsChanged)
    def gameVersions(self) -> list:
        return self._game_versions

    # ---- Slots ----
    @Slot(str)
    def setVersionTypeFilter(self, filter_type: str) -> None:
        self._version_type_filter = filter_type.lower()
        self.versionTypeFilterChanged.emit()

    @Slot(str)
    def setQuery(self, q: str) -> None:
        self._query = q

    @Slot(str)
    def setMcVersion(self, v: str) -> None:
        if self._mc_version != v:
            self._mc_version = v
            self.mcVersionChanged.emit()

    @Slot(str)
    def setCategory(self, c: str) -> None:
        self._category = c

    @Slot(str)
    def setSort(self, s: str) -> None:
        self._sort = s

    @Slot()
    def search(self) -> None:
        self._offset = 0
        self._results = []
        self._total_hits = 0
        self._set_loading(True)
        self._run_search(append=False)

    @Slot()
    def loadMore(self) -> None:
        if self._offset + 25 >= self._total_hits:
            return
        self._offset += 25
        self._set_loading(True)
        self._run_search(append=True)

    def _run_search(self, append: bool = False) -> None:
        q = self._query
        mv = self._mc_version if self._mc_version != "All" else None
        cat = self._category
        sort = self._sort
        offset = self._offset
        ptype = self._project_type
        src = self._source
        ldr = self._loader

        def worker():
            hits: list[dict] = []
            total = 0

            try:
                if src in ("all", "modrinth"):
                    try:
                        m_res = self._modrinth_svc.search_mods(
                            query=q,
                            mc_version=mv,
                            category=cat,
                            sort=sort,
                            offset=offset,
                            limit=25,
                            project_type=ptype
                        )
                        for h in m_res.get("hits", []):
                            h["source"] = "modrinth"
                            hits.append(h)
                        total += m_res.get("total_hits", len(m_res.get("hits", [])))
                    except Exception as ex_m:
                        print(f"[ModrinthController] Modrinth search error: {ex_m}")

                if src in ("all", "curseforge"):
                    try:
                        c_res = self._curseforge_svc.search_mods(
                            query=q,
                            mc_version=mv,
                            loader=ldr,
                            sort=sort,
                            offset=offset,
                            limit=25,
                            project_type=ptype
                        )
                        for h in c_res.get("hits", []):
                            h["source"] = "curseforge"
                            # If source is all, avoid visual exact duplicates with Modrinth
                            if src == "all":
                                title_clean = re.sub(r"[^a-z0-9]+", "", (h.get("title") or "").lower())
                                slug_clean = (h.get("slug") or "").lower()
                                if any(slug_clean == (mh.get("slug") or "").lower() or title_clean == re.sub(r"[^a-z0-9]+", "", (mh.get("title") or "").lower()) for mh in hits if mh.get("source") == "modrinth"):
                                    continue
                            hits.append(h)
                        total += c_res.get("total_hits", len(c_res.get("hits", [])))
                    except Exception as ex_c:
                        print(f"[ModrinthController] CurseForge search error: {ex_c}")

                # If source is all, sort combined hits by downloads or relevance
                if src == "all" and hits:
                    if sort == "downloads" or sort == "follows":
                        hits.sort(key=lambda x: x.get("downloads", 0), reverse=True)

                self._searchDoneSignal.emit({"hits": hits, "total_hits": total}, append)

            except Exception as e:
                self._errorSignal.emit(str(e))

        t = threading.Thread(target=worker, daemon=True)
        t.start()

    @Slot(dict, bool)
    def _on_search_done(self, result: dict, append: bool) -> None:
        if append:
            self._results = self._results + result.get("hits", [])
        else:
            self._results = result.get("hits", [])
        self._total_hits = result.get("total_hits", len(self._results))
        self._set_loading(False)
        self.searchResultsChanged.emit()

    @Slot("QVariantMap")
    @Slot("QVariantMap", str)
    def selectMod(self, mod_data: dict, mc_version: str = "") -> None:
        self._selected = dict(mod_data) if mod_data else {}
        self._versions = []
        self.selectedModChanged.emit()
        self.versionsChanged.emit()
        self.versionTypeFilterChanged.emit()

        proj_id = self._selected.get("project_id", "") or self._selected.get("slug", "") or self._selected.get("id", "")
        if not proj_id:
            return

        target_v = mc_version if mc_version else self._mc_version
        mv = target_v if target_v != "All" else None
        source = self._selected.get("source", "modrinth")
        ldr = self._loader

        def worker():
            try:
                if source == "curseforge":
                    versions = self._curseforge_svc.get_project_versions(proj_id, mc_version=mv, loader=ldr)
                else:
                    versions = self._modrinth_svc.get_project_versions(proj_id, mc_version=mv, loader=ldr)
                self._versionsDoneSignal.emit(versions)
            except Exception as e:
                self._errorSignal.emit(str(e))

        t = threading.Thread(target=worker, daemon=True)
        t.start()

    @Slot(str)
    @Slot(str, str)
    def inspectInstalledMod(self, slug_or_id: str, mc_version: str = "") -> None:
        self._versions = []
        self.versionsChanged.emit()
        self.versionTypeFilterChanged.emit()

        def worker():
            try:
                data = self._modrinth_svc.get_project(slug_or_id)
                if not data:
                    data = self._curseforge_svc.get_project(slug_or_id)
                self._projectDoneSignal.emit(data)
            except Exception as e:
                self._errorSignal.emit(str(e))

        t = threading.Thread(target=worker, daemon=True)
        t.start()

    @Slot(dict)
    def _on_project_done(self, data: dict) -> None:
        self.selectMod(data, self._mc_version)

    @Slot(str)
    @Slot(str, str)
    def fetchInstalledModVersions(self, slug_or_id: str, mc_version: str = "") -> None:
        self._versions = []
        self.versionsChanged.emit()
        self.versionTypeFilterChanged.emit()
        target_v = mc_version if mc_version else self._mc_version
        mv = target_v if target_v != "All" else None

        def worker():
            try:
                versions = self._modrinth_svc.get_project_versions(slug_or_id, mc_version=mv)
                if not versions:
                    versions = self._curseforge_svc.get_project_versions(slug_or_id, mc_version=mv)
                self._versionsDoneSignal.emit(versions)
            except Exception as e:
                self._errorSignal.emit(str(e))

        t = threading.Thread(target=worker, daemon=True)
        t.start()

    @Slot(list)
    def _on_versions_done(self, versions: list) -> None:
        self._versions = versions[:35]
        self.versionsChanged.emit()
        self.versionTypeFilterChanged.emit()

    @Slot(str)
    def _on_error(self, error: str) -> None:
        self._set_loading(False)
        self.statusChanged.emit(f"Error: {error}")

    def _set_loading(self, value: bool) -> None:
        self._loading = value
        self.loadingChanged.emit()
