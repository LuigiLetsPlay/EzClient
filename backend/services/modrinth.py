import json
import urllib.parse
import urllib.request
import re
from typing import Any
from backend.models.types import ProfileData, ModData

MODRINTH_API = "https://api.modrinth.com/v2"
USER_AGENT = "EzClient/1.5.3 (desktop launcher)"

FALLBACK_VERSIONS = [
    "26.2", "26.1", "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5",
    "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21"
]

def get_json(url: str, timeout: int = 8) -> Any:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))

class ModrinthService:
    def get_game_versions(self) -> list[str]:
        try:
            data = get_json(f"{MODRINTH_API}/tag/game_version")
            values = [
                item["version"] for item in data
                if item.get("version_type") == "release" and
                (item.get("version", "").startswith("1.21") or
                 item.get("version", "").startswith("1.20") or
                 item.get("version", "").startswith("1.19") or
                 item.get("version", "").startswith("1.18") or
                 re.fullmatch(r"2[2-9]\.\d+(\.\d+)?", item.get("version", "")))
            ]
            return list(dict.fromkeys(values + FALLBACK_VERSIONS))
        except Exception:
            return FALLBACK_VERSIONS

    def search_mods(self, query: str, mc_version: str | None = None, category: str = "All",
                    sort: str = "relevance", loader: str = "fabric", offset: int = 0, limit: int = 20,
                    project_type: str = "mod") -> dict[str, Any]:
        ptype = project_type.lower() if project_type else "mod"
        facets = [[f"project_type:{ptype}"]]
        if ptype == "mod" and loader and loader.lower() != "all":
            facets.append([f"categories:{loader.lower()}"])
        if mc_version and mc_version.lower() != "all":
            facets.append([f"versions:{mc_version}"])
        if category not in ("Featured", "All", "all"):
            facets.append([f"categories:{category.lower()}"])

        params = urllib.parse.urlencode({
            "query": query,
            "facets": json.dumps(facets),
            "index": sort,
            "offset": offset,
            "limit": limit
        })
        data = get_json(f"{MODRINTH_API}/search?{params}")
        return {
            "hits": data.get("hits", []),
            "total_hits": data.get("total_hits", len(data.get("hits", []))),
            "offset": offset,
            "limit": limit
        }

    def get_project(self, project_id: str) -> dict[str, Any]:
        return get_json(f"{MODRINTH_API}/project/{urllib.parse.quote(project_id)}")

    def get_projects(self, project_ids: list[str]) -> list[dict[str, Any]]:
        if not project_ids:
            return []
        ids_param = json.dumps(project_ids)
        return get_json(f"{MODRINTH_API}/projects?ids={urllib.parse.quote(ids_param)}")

    def get_project_versions(self, project_id: str, mc_version: str | None = None,
                             loader: str | None = "fabric") -> list[dict[str, Any]]:
        query_params: dict[str, Any] = {}
        if loader and loader.lower() != "all":
            query_params["loaders"] = json.dumps([loader.lower()])
        if mc_version and mc_version.lower() != "all":
            query_params["game_versions"] = json.dumps([mc_version])
        qs = f"?{urllib.parse.urlencode(query_params)}" if query_params else ""
        return get_json(f"{MODRINTH_API}/project/{urllib.parse.quote(project_id)}/version{qs}")

    def get_dependencies(self, project_id_or_slug: str, mc_version: str | None = None, loader: str = "fabric") -> list[dict[str, Any]]:
        """Resolves required dependency projects for a given mod version."""
        try:
            versions = self.get_project_versions(project_id_or_slug, mc_version=mc_version, loader=loader)
            if not versions:
                versions = self.get_project_versions(project_id_or_slug, loader=loader)
            if not versions:
                versions = self.get_project_versions(project_id_or_slug)
            if not versions:
                return []

            best_ver = next((v for v in versions if v.get("version_type") == "release"), versions[0])
            deps = best_ver.get("dependencies", [])
            required_pids = [d.get("project_id") for d in deps if d.get("dependency_type") == "required" and d.get("project_id")]
            if not required_pids:
                return []

            try:
                projs = self.get_projects(required_pids)
                return [{
                    "project_id": p.get("id"),
                    "slug": p.get("slug"),
                    "name": p.get("title", p.get("slug")),
                    "icon_url": p.get("icon_url", ""),
                    "description": p.get("description", ""),
                    "author": p.get("team", "Modrinth")
                } for p in projs]
            except Exception:
                return [{"project_id": pid, "slug": pid, "name": pid, "icon_url": ""} for pid in required_pids]
        except Exception as e:
            print(f"[Modrinth] Error resolving dependencies for {project_id_or_slug}: {e}")
            return []
