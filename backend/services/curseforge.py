import json
import urllib.request
import urllib.parse
from typing import Any
import ssl

CURSEFORGE_API_KEY = "$2a$10$bL4bIL5pUWqfcO7KQtnMReakwtfHbNKh6v1uTpKlzhwoueEJQnPnm"
BASE_URL = "https://api.curseforge.com/v1"
USER_AGENT = "EzClient/1.1.9 (github.com/LuigiLetsPlay/EzClient)"

# Loader mapping for CurseForge API
LOADER_MAP = {
    "fabric": 4,
    "forge": 1,
    "neoforge": 6,
    "quilt": 5,
}

# Project Class ID mapping
CLASS_MAP = {
    "mod": 6,
    "mods": 6,
    "shader": 6552,
    "shaders": 6552,
    "resourcepack": 12,
    "resourcepacks": 12,
    "datapack": 6945,
}

def _make_request(endpoint: str, query_params: dict[str, Any] | None = None, post_data: dict[str, Any] | None = None) -> Any:
    url = f"{BASE_URL}{endpoint}"
    if query_params:
        clean_params = {k: v for k, v in query_params.items() if v is not None and v != ""}
        if clean_params:
            url += "?" + urllib.parse.urlencode(clean_params)

    headers = {
        "x-api-key": CURSEFORGE_API_KEY,
        "Accept": "application/json",
        "User-Agent": USER_AGENT
    }

    data_bytes = None
    if post_data is not None:
        data_bytes = json.dumps(post_data).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=data_bytes, headers=headers)
    ctx = ssl.create_default_context()

    try:
        with urllib.request.urlopen(req, timeout=10, context=ctx) as resp:
            if resp.getcode() in (200, 201):
                raw = resp.read().decode("utf-8", errors="replace")
                return json.loads(raw)
    except Exception as e:
        print(f"[CurseForgeAPI] Error calling {endpoint}: {e}")
        return None
    return None


class CurseForgeService:
    """Service to search, inspect, and retrieve mod files from CurseForge."""

    def search_mods(
        self,
        query: str = "",
        mc_version: str | None = None,
        loader: str | None = "fabric",
        sort: str = "relevance",
        offset: int = 0,
        limit: int = 25,
        project_type: str = "mod"
    ) -> dict[str, Any]:
        """Search mods on CurseForge with filtering."""
        class_id = CLASS_MAP.get(str(project_type).lower(), 6)

        # Sort mapping: 1=Featured, 2=Popularity, 3=LastUpdated, 4=Name, 5=TotalDownloads
        sort_field = 2
        if sort == "downloads" or sort == "follows":
            sort_field = 5
        elif sort == "newest" or sort == "updated":
            sort_field = 3
        elif sort == "relevance" and not query:
            sort_field = 2

        params: dict[str, Any] = {
            "gameId": 432,
            "classId": class_id,
            "sortField": sort_field,
            "sortOrder": "desc",
            "pageSize": min(50, max(1, limit)),
            "index": offset
        }

        if query and query.strip():
            params["searchFilter"] = query.strip()

        if mc_version and mc_version != "All":
            params["gameVersion"] = mc_version.strip()

        if loader and str(loader).lower() in LOADER_MAP and class_id == 6:
            params["modLoaderType"] = LOADER_MAP[str(loader).lower()]

        resp = _make_request("/mods/search", query_params=params)
        if not resp or "data" not in resp:
            return {"hits": [], "total_hits": 0}

        hits = []
        for item in resp.get("data", []):
            authors_list = item.get("authors", [])
            author_name = authors_list[0].get("name", "CurseForge") if authors_list else "CurseForge"
            logo = item.get("logo", {}) or {}
            icon_url = logo.get("thumbnailUrl") or logo.get("url") or ""

            hits.append({
                "project_id": str(item.get("id")),
                "id": str(item.get("id")),
                "slug": item.get("slug", ""),
                "title": item.get("name", ""),
                "name": item.get("name", ""),
                "author": author_name,
                "description": item.get("summary", ""),
                "icon_url": icon_url,
                "downloads": item.get("downloadCount", 0),
                "follows": item.get("thumbsUpCount", 0),
                "project_type": project_type,
                "source": "curseforge",
                "categories": [c.get("name", "") for c in item.get("categories", [])],
                "date_modified": item.get("dateModified", ""),
                "client_side": "required",
                "server_side": "optional"
            })

        pagination = resp.get("pagination", {})
        total_hits = pagination.get("totalCount", len(hits))

        return {"hits": hits, "total_hits": total_hits}

    def get_project(self, mod_id_or_slug: str | int) -> dict[str, Any]:
        """Gets full details for a CurseForge project."""
        if str(mod_id_or_slug).isdigit():
            resp = _make_request(f"/mods/{mod_id_or_slug}")
            if resp and "data" in resp:
                item = resp["data"]
                authors_list = item.get("authors", [])
                author_name = authors_list[0].get("name", "CurseForge") if authors_list else "CurseForge"
                logo = item.get("logo", {}) or {}
                icon_url = logo.get("thumbnailUrl") or logo.get("url") or ""
                return {
                    "project_id": str(item.get("id")),
                    "id": str(item.get("id")),
                    "slug": item.get("slug", ""),
                    "title": item.get("name", ""),
                    "name": item.get("name", ""),
                    "author": author_name,
                    "description": item.get("summary", ""),
                    "icon_url": icon_url,
                    "downloads": item.get("downloadCount", 0),
                    "source": "curseforge",
                    "categories": [c.get("name", "") for c in item.get("categories", [])]
                }

        # If slug given, search by slug
        res = self.search_mods(query=str(mod_id_or_slug), limit=5)
        if res.get("hits"):
            for hit in res["hits"]:
                if hit.get("slug", "").lower() == str(mod_id_or_slug).lower() or hit.get("title", "").lower() == str(mod_id_or_slug).lower():
                    return hit
            return res["hits"][0]
        return {}

    def get_project_versions(self, mod_id: str | int, mc_version: str | None = None, loader: str | None = "fabric") -> list[dict[str, Any]]:
        """Gets list of version files for a CurseForge mod."""
        if not str(mod_id).isdigit():
            proj = self.get_project(str(mod_id))
            if proj and proj.get("project_id"):
                mod_id = proj["project_id"]
            else:
                return []

        params: dict[str, Any] = {"pageSize": 35}
        if mc_version and mc_version != "All":
            params["gameVersion"] = mc_version
        if loader and str(loader).lower() in LOADER_MAP:
            params["modLoaderType"] = LOADER_MAP[str(loader).lower()]

        resp = _make_request(f"/mods/{mod_id}/files", query_params=params)
        if not resp or "data" not in resp:
            return []

        versions = []
        for f in resp.get("data", []):
            rel_type = f.get("releaseType", 1)
            v_type = "release" if rel_type == 1 else ("beta" if rel_type == 2 else "alpha")
            file_id = f.get("id")
            filename = f.get("fileName", f"{mod_id}.jar")
            
            # Construct download URL if downloadUrl is null (CurseForge CDN format)
            dl_url = f.get("downloadUrl")
            if not dl_url and file_id:
                s_id = str(file_id)
                if len(s_id) > 4:
                    dl_url = f"https://edge.forgecdn.net/files/{s_id[:4]}/{s_id[4:]}/{urllib.parse.quote(filename)}"
                else:
                    dl_url = f"https://edge.forgecdn.net/files/{s_id}/{urllib.parse.quote(filename)}"

            versions.append({
                "id": str(file_id),
                "version_number": f.get("displayName", filename),
                "name": f.get("displayName", filename),
                "version_type": v_type,
                "date_published": f.get("fileDate", ""),
                "downloads": f.get("downloadCount", 0),
                "files": [{
                    "url": dl_url,
                    "filename": filename,
                    "primary": True,
                    "size": f.get("fileLength", 0)
                }],
                "game_versions": f.get("gameVersions", []),
                "loaders": [loader] if loader else ["fabric"],
                "dependencies": []
            })
        return versions

    def get_fingerprint_matches(self, fingerprints: list[int]) -> list[dict[str, Any]]:
        """Matches local Murmur2 fingerprints against CurseForge database."""
        if not fingerprints:
            return []
        resp = _make_request("/fingerprints", post_data={"fingerprints": fingerprints})
        if resp and "data" in resp:
            return resp["data"].get("exactMatches", [])
        return []
