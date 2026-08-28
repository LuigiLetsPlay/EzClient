from __future__ import annotations

from dataclasses import asdict, dataclass, field
from pathlib import Path
import re
from typing import TYPE_CHECKING

from backend.models.types import ModData
from backend.services.mod_scanner import extract_jar_metadata

if TYPE_CHECKING:
    from backend.models.types import ProfileData
    from backend.services.store import ProfileStore


MIGRATION_VERSION = 1801
CORE_IDS = ("ezclient", "sodium", "lithium", "iris")

# Normalized Fabric/Forge ids and historical launcher slugs. Filename matching
# is only accepted together with ownership evidence from the saved profile.
LEGACY_ALIASES: dict[str, set[str]] = {
    "entityculling": {"entityculling", "entitycullingfabric"},
    "ferritecore": {"ferritecore", "ferrite-core"},
    "immediatelyfast": {"immediatelyfast", "immediately-fast"},
    "krypton": {"krypton"},
    "memoryleakfix": {"memoryleakfix", "memory-leak-fix"},
    "fastsuite": {"fastsuite", "fast-suite"},
    "moreculling": {"moreculling", "more-culling"},
    "culllessleaves": {"culllessleaves", "cull-less-leaves", "cull_less_leaves"},
    "yacl": {"yacl", "yet-another-config-lib", "yet_another_config_lib", "yet_another_config_lib_v3", "yacl3"},
    "clothconfig": {"cloth-config", "cloth_config", "cloth-config-fabric", "clothconfig", "clothconfigv2", "cloth-config2"},
}


def _normal(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", str(value or "").lower())


NORMALIZED_TO_LEGACY = {
    _normal(alias): canonical
    for canonical, aliases in LEGACY_ALIASES.items()
    for alias in aliases | {canonical}
}


def _legacy_id(*values: str) -> str:
    for value in values:
        normalized = _normal(value)
        if normalized in NORMALIZED_TO_LEGACY:
            return NORMALIZED_TO_LEGACY[normalized]
        for alias, canonical in NORMALIZED_TO_LEGACY.items():
            if alias and alias in normalized:
                return canonical
    return ""


@dataclass
class MigrationReport:
    changed: bool = False
    removed_files: list[str] = field(default_factory=list)
    repaired_profiles: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


class ProfileMigrationService:
    """Versioned, conservative cleanup for launcher-owned profile content."""

    def __init__(self, store: "ProfileStore") -> None:
        self.store = store

    def run_if_needed(self) -> MigrationReport:
        current = int(self.store.settings.get("profile_migration_version", 0) or 0)
        if current >= MIGRATION_VERSION:
            return MigrationReport()

        report = MigrationReport()
        for profile in self.store.profiles:
            try:
                if self._migrate_profile(profile, report):
                    report.changed = True
                    report.repaired_profiles.append(profile.id)
            except Exception as exc:
                report.errors.append(f"{profile.name}: {exc}")

        # A filesystem error must be retried on the next start.
        if not report.errors:
            self.store.settings["profile_migration_version"] = MIGRATION_VERSION
            report.changed = True
        return report

    def _migrate_profile(self, profile: "ProfileData", report: MigrationReport) -> bool:
        from backend.services.store import PERFORMANCE_MODS

        changed = False
        managed_manifest = {
            _normal(value) for value in (profile.integrated_mods + profile.managed_core_mods)
            if value
        }
        owned_legacy_ids: set[str] = set()
        owned_filenames: set[str] = set()
        raw_owned_core_filenames: set[str] = set()
        failed_legacy_ids: set[str] = set()
        failed_core_filenames: set[str] = set()
        preserved_user_files: set[str] = set()

        for mod in profile.mods:
            legacy = _legacy_id(mod.slug, mod.project_id, mod.name, mod.filename)
            identifiers = {_normal(mod.slug), _normal(mod.project_id)}
            launcher_owned = bool(identifiers & managed_manifest) or bool(mod.recommended or mod.essential)
            if legacy and launcher_owned:
                owned_legacy_ids.add(legacy)
                if mod.filename:
                    owned_filenames.add(mod.filename.lower().removesuffix(".disabled"))
            if (profile.profile_type == "raw" and launcher_owned
                    and _normal(mod.slug or mod.project_id) in {_normal(value) for value in CORE_IDS}
                    and mod.filename):
                raw_owned_core_filenames.add(mod.filename.lower().removesuffix(".disabled"))

        if profile.mods_path.is_dir():
            candidates = list(profile.mods_path.glob("*.jar")) + list(profile.mods_path.glob("*.jar.disabled"))
            for jar_path in candidates:
                try:
                    metadata = extract_jar_metadata(jar_path)
                except Exception:
                    metadata = {}
                legacy = _legacy_id(
                    str(metadata.get("mod_id", "")),
                    str(metadata.get("name", "")),
                    jar_path.name,
                )
                filename = jar_path.name.lower().removesuffix(".disabled")
                remove_legacy = bool(
                    legacy and (
                        profile.profile_type == "ezclient"
                        or legacy in owned_legacy_ids
                        or filename in owned_filenames
                    )
                )
                remove_raw_core = profile.profile_type == "raw" and filename in raw_owned_core_filenames
                if not remove_legacy and not remove_raw_core:
                    preserved_user_files.add(jar_path.name.removesuffix(".disabled"))
                    continue
                try:
                    jar_path.unlink()
                    report.removed_files.append(str(jar_path))
                    changed = True
                except OSError as exc:
                    if legacy:
                        failed_legacy_ids.add(legacy)
                    if remove_raw_core:
                        failed_core_filenames.add(filename)
                    report.errors.append(f"{profile.name}: {jar_path.name} konnte nicht entfernt werden: {exc}")

        retained_mods: list[ModData] = []
        for mod in profile.mods:
            legacy = _legacy_id(mod.slug, mod.project_id, mod.name, mod.filename)
            identifiers = {_normal(mod.slug), _normal(mod.project_id)}
            launcher_owned = bool(identifiers & managed_manifest) or bool(mod.recommended or mod.essential)
            if legacy and (profile.profile_type == "ezclient" or (launcher_owned and legacy not in failed_legacy_ids)):
                changed = True
                continue
            core_id = _normal(mod.slug or mod.project_id)
            filename = (mod.filename or "").lower().removesuffix(".disabled")
            if (profile.profile_type == "raw" and launcher_owned
                    and core_id in {_normal(value) for value in CORE_IDS}
                    and filename not in failed_core_filenames):
                changed = True
                continue
            retained_mods.append(mod)
        profile.mods = retained_mods

        old_integrated = list(profile.integrated_mods)
        profile.integrated_mods = [
            value for value in profile.integrated_mods if not _legacy_id(value)
        ]
        changed = changed or old_integrated != profile.integrated_mods

        if profile.profile_type == "ezclient":
            existing = {
                _normal(mod.slug or mod.project_id): mod for mod in profile.mods
            }
            for template in PERFORMANCE_MODS:
                target = existing.get(_normal(template.slug))
                if target is None:
                    profile.mods.append(ModData(**asdict(template)))
                    changed = True
                else:
                    if not target.enabled or not target.essential:
                        target.enabled = True
                        target.essential = True
                        changed = True
                    if template.slug != "ezclient" and target.version != "Latest":
                        target.version = "Latest"
                        changed = True
            expected_core = list(CORE_IDS)
            if profile.managed_core_mods != expected_core:
                profile.managed_core_mods = expected_core
                changed = True
            profile.integrated_mods = list(CORE_IDS)
        else:
            if profile.managed_core_mods:
                profile.managed_core_mods = []
                changed = True

        managed = {_normal(value) for value in profile.managed_core_mods}
        profile.user_mods = list(dict.fromkeys(
            [
                mod.slug or mod.project_id
                for mod in profile.mods
                if _normal(mod.slug or mod.project_id) not in managed
            ] + sorted(preserved_user_files)
        ))
        return changed
