import re
import shutil
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Optional, Tuple, List
from backend.models.types import ProfileData, ModData
from backend.services.modrinth import ModrinthService, select_preferred_version
from backend.services.curseforge import CurseForgeService
from backend.services.mod_downloader import sync_profile_mods, download_file


@dataclass
class CrashDiagnosis:
    has_solution: bool = False
    problem_title: str = "Unbekannter Absturz"
    problem_description: str = "Minecraft wurde unerwartet beendet."
    solution_title: str = "Keine automatische Lösung verfügbar"
    solution_description: str = "Bitte prüfe die Logs oder deaktiviere kürzlich hinzugefügte Mods."
    action_type: str = "NONE"  # "FIX_MOD_INCOMPATIBILITY", "INSTALL_DEPENDENCY", "REMOVE_DUPLICATES", "INCREASE_RAM", "FIX_JAVA", "DISABLE_MOD"
    action_data: dict[str, Any] = field(default_factory=dict)
    can_auto_fix: bool = False
    raw_log: str = ""
    short_error: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {
            "has_solution": self.has_solution,
            "hasSolution": self.has_solution,
            "problem_title": self.problem_title,
            "problemTitle": self.problem_title,
            "problem_description": self.problem_description,
            "problemDescription": self.problem_description,
            "description": self.problem_description,
            "solution_title": self.solution_title,
            "solutionTitle": self.solution_title,
            "solution_description": self.solution_description,
            "solutionDescription": self.solution_description,
            "proposed_solution": self.solution_description,
            "action_type": self.action_type,
            "actionType": self.action_type,
            "action_data": self.action_data,
            "actionData": self.action_data,
            "action_button_text": "⚡ Crash jetzt automatisch beheben" if self.can_auto_fix else "⚡ Crash beheben",
            "actionButtonText": "⚡ Crash jetzt automatisch beheben" if self.can_auto_fix else "⚡ Crash beheben",
            "can_auto_fix": self.can_auto_fix,
            "canAutoFix": self.can_auto_fix,
            "raw_log": self.raw_log,
            "rawLog": self.raw_log,
            "short_error": self.short_error,
            "shortError": self.short_error,
        }


class CrashDoctorService:
    """Intelligent Minecraft crash analyzer and 1-click automatic repair engine."""

    def __init__(self):
        self._modrinth = ModrinthService()
        self._curseforge = CurseForgeService()

    def analyze(self, log_text: str, short_err: str = "", profile: Optional[ProfileData] = None) -> CrashDiagnosis:
        """Parses crash log and determines the root cause + actionable fix."""
        log = (log_text or "").strip()
        combined_text = f"{short_err}\n{log}"

        if profile and "Mixin apply for mod ezclient failed" in combined_text:
            from backend.services.store import has_ezclient_asset
            if not has_ezclient_asset(profile.minecraft_version):
                return CrashDiagnosis(
                    has_solution=True,
                    problem_title="EzClient Core ist für diese Minecraft-Version inkompatibel",
                    problem_description=f"Der installierte EzClient-Build besitzt keine geprüften Mixins für Minecraft {profile.minecraft_version}.",
                    solution_title="Inkompatiblen EzClient Core sicher entfernen",
                    solution_description="Das Profil bleibt als performantes Fabric-Profil erhalten. Nur der inkompatible EzClient Core wird entfernt.",
                    action_type="REMOVE_INCOMPATIBLE_EZCLIENT",
                    action_data={},
                    can_auto_fix=True,
                    raw_log=log,
                    short_error=f"EzClient Core unterstützt Minecraft {profile.minecraft_version} noch nicht",
                )

        # 1. Check for Out Of Memory error
        if "OutOfMemoryError" in combined_text or "GC overhead limit exceeded" in combined_text:
            current_ram = getattr(profile, "ram_mb", 4096) if profile else 4096
            new_ram = min(max(current_ram + 2048, 6144), 16384)
            return CrashDiagnosis(
                has_solution=True,
                problem_title="Zu wenig Arbeitsspeicher (RAM)",
                problem_description=f"Minecraft ist abgestürzt, weil der zugewiesene Speicher ({round(current_ram/1024, 1)} GB) voll war (OutOfMemoryError).",
                solution_title="RAM automatisch erhöhen",
                solution_description=f"Den zugewiesenen Arbeitsspeicher auf {round(new_ram/1024, 1)} GB erhöhen.",
                action_type="INCREASE_RAM",
                action_data={"new_ram_mb": new_ram},
                can_auto_fix=True,
                raw_log=log,
                short_error="java.lang.OutOfMemoryError: Java heap space",
            )

        # 2. Check for Java Version Mismatch
        if "UnsupportedClassVersionError" in combined_text or "compiled by a more recent version of the Java Runtime" in combined_text:
            return CrashDiagnosis(
                has_solution=True,
                problem_title="Java-Laufzeitumgebung inkompatibel",
                problem_description="Minecraft oder eine installierte Mod benötigt eine neuere Java-Version (z. B. Java 21).",
                solution_title="Java-Version automatisch aktualisieren",
                solution_description="EzClient lädt die erforderliche Java-Laufzeitumgebung automatisch herunter und konfiguriert sie.",
                action_type="FIX_JAVA",
                action_data={"required_major": 21},
                can_auto_fix=True,
                raw_log=log,
                short_error="UnsupportedClassVersionError",
            )

        # 3. Check for Fabric Incompatible Mods: Sodium vs. Iris or specific replacement recommendations
        wrong_target = re.search(
            r"Mod ['\"](?P<name>[^'\"]+)['\"]\s*\((?P<mod_id>[^)]+)\).*?requires any (?P<required>[^\s]+) version of ['\"]?(?P<target>Minecraft|OpenJDK[^'\"\r\n]*)['\"]?.*?wrong version is present:\s*(?P<present>[^!\r\n]+)",
            combined_text,
            re.IGNORECASE | re.DOTALL,
        )
        if wrong_target:
            details = wrong_target.groupdict()
            mod_id = details["mod_id"]
            compatible_versions: list[dict[str, Any]] = []
            if profile:
                try:
                    compatible_versions = self._modrinth.get_project_versions(
                        mod_id, mc_version=profile.minecraft_version, loader=profile.loader
                    )
                    if not compatible_versions:
                        compatible_versions = self._curseforge.get_project_versions(
                            mod_id, mc_version=profile.minecraft_version, loader=profile.loader
                        )
                except Exception:
                    compatible_versions = []

            if compatible_versions:
                return CrashDiagnosis(
                    has_solution=True,
                    problem_title=f"Falsche Version von {details['name']}",
                    problem_description=(
                        f"Die installierte Mod verlangt {details['target']} {details['required']}, "
                        f"das Profil verwendet aber {details['present'].strip()}."
                    ),
                    solution_title="Passende Mod-Version installieren",
                    solution_description=(
                        f"EzClient ersetzt '{details['name']}' durch einen Build, der ausdrücklich "
                        f"mit Minecraft {profile.minecraft_version} und {profile.loader} kompatibel ist."
                    ),
                    action_type="FIX_MOD_INCOMPATIBILITY",
                    action_data={"primary_mod": mod_id, "primary_name": details["name"], "conflicting_mod": ""},
                    can_auto_fix=True,
                    raw_log=log,
                    short_error=f"{details['name']} ist nicht mit diesem Profil kompatibel",
                )

            return CrashDiagnosis(
                has_solution=False,
                problem_title=f"Keine kompatible Version von {details['name']}",
                problem_description=(
                    f"Für Minecraft {getattr(profile, 'minecraft_version', details['present'].strip())} "
                    f"wurde kein kompatibler Build gefunden."
                ),
                solution_title="Keine einwandfreie automatische Lösung verfügbar",
                solution_description="Die Mod muss entfernt oder das Profil auf eine unterstützte Minecraft-Version umgestellt werden.",
                action_type="NONE",
                can_auto_fix=False,
                raw_log=log,
                short_error=f"Kein kompatibler Build für {details['name']}",
            )

        # Pattern 3A: Replace mod X with any version compatible with Y
        # Example: Replace mod 'Sodium' (sodium) 0.8.14+mc1.21.11 with any 0.8.x version that is compatible with: - iris 1.10.7+mc1.21.11
        replace_match = re.search(
            r"Replace mod ['\"](?P<mod_a_name>[^'\"]+)['\"]\s*\((?P<mod_a_id>[^)]+)\)\s*(?P<mod_a_ver>[^\s]+)\s+with any (?P<target_ver>[^\s]+) version that is compatible with:\s*-\s*(?P<mod_b_id>[^\s]+)\s+(?P<mod_b_ver>[^\s\r\n]+)",
            combined_text,
            re.IGNORECASE
        )
        if replace_match:
            d = replace_match.groupdict()
            mod_a_name = d["mod_a_name"]
            mod_a_id = d["mod_a_id"]
            mod_b_id = d["mod_b_id"]
            mod_b_ver = d["mod_b_ver"]
            mod_a_ver = d["mod_a_ver"]
            
            mod_b_display = mod_b_id.capitalize() if mod_b_id.islower() else mod_b_id
            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Mod-Inkompatibilität: {mod_a_name} & {mod_b_display}",
                problem_description=f"Mod '{mod_a_name}' ({mod_a_ver}) ist nicht kompatibel mit deiner installierten '{mod_b_display}'-Version ({mod_b_ver}).",
                solution_title="Kompatible Versionen synchronisieren",
                solution_description=f"EzClient aktualisiert '{mod_b_display}' auf die passende Version oder passt '{mod_a_name}' an, damit beide Mods harmonieren.",
                action_type="FIX_MOD_INCOMPATIBILITY",
                action_data={
                    "primary_mod": mod_a_id,
                    "primary_name": mod_a_name,
                    "conflicting_mod": mod_b_id,
                    "conflicting_name": mod_b_display,
                    "target_hint": d.get("target_ver", "")
                },
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Inkompatibilität: {mod_a_name} ({mod_a_ver}) ↔ {mod_b_display} ({mod_b_ver})",
            )

        # Pattern 3B: Mod A is incompatible with version ... of Mod B
        incompat_match = re.search(
            r"Mod ['\"](?P<mod_a_name>[^'\"]+)['\"]\s*\((?P<mod_a_id>[^)]+)\)\s*(?P<mod_a_ver>[^\s]+)?\s*is incompatible with version (?P<incompat_ver>[^\s]+) or earlier of mod ['\"](?P<mod_b_name>[^'\"]+)['\"]\s*\((?P<mod_b_id>[^)]+)\)",
            combined_text,
            re.IGNORECASE
        )
        if incompat_match:
            d = incompat_match.groupdict()
            mod_a_name = d["mod_a_name"]
            mod_a_id = d["mod_a_id"]
            mod_b_name = d["mod_b_name"]
            mod_b_id = d["mod_b_id"]

            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Inkompatible Versionen: {mod_a_name} & {mod_b_name}",
                problem_description=f"Mod '{mod_a_name}' verträgt sich nicht mit deiner aktuellen Version von '{mod_b_name}'.",
                solution_title=f"'{mod_b_name}' aktualisieren",
                solution_description=f"EzClient lädt die neueste kompatible Version von '{mod_b_name}' herunter und behebt den Konflikt.",
                action_type="FIX_MOD_INCOMPATIBILITY",
                action_data={
                    "primary_mod": mod_a_id,
                    "primary_name": mod_a_name,
                    "conflicting_mod": mod_b_id,
                    "conflicting_name": mod_b_name,
                },
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Inkompatibilität: {mod_a_name} ↔ {mod_b_name}",
            )

        # Pattern 3C: General Incompatibility: Mod A is incompatible with mod B
        gen_incompat = re.search(
            r"Mod ['\"](?P<mod_a_name>[^'\"]+)['\"]\s*\((?P<mod_a_id>[^)]+)\)\s*(?:[^\n]+)?is incompatible with mod ['\"](?P<mod_b_name>[^'\"]+)['\"]\s*\((?P<mod_b_id>[^)]+)\)",
            combined_text,
            re.IGNORECASE
        )
        if gen_incompat:
            d = gen_incompat.groupdict()
            mod_a_name = d["mod_a_name"]
            mod_a_id = d["mod_a_id"]
            mod_b_name = d["mod_b_name"]
            mod_b_id = d["mod_b_id"]

            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Mod-Konflikt: {mod_a_name} ↔ {mod_b_name}",
                problem_description=f"Die Mods '{mod_a_name}' und '{mod_b_name}' können nicht gleichzeitig genutzt werden.",
                solution_title="Konflikt lösen (Mod aktualisieren oder deaktivieren)",
                solution_description=f"EzClient prüft verfügbare Updates für '{mod_b_name}' oder deaktiviert die inkompatible Mod.",
                action_type="FIX_MOD_INCOMPATIBILITY",
                action_data={
                    "primary_mod": mod_a_id,
                    "primary_name": mod_a_name,
                    "conflicting_mod": mod_b_id,
                    "conflicting_name": mod_b_name,
                },
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Mod-Konflikt: {mod_a_name} ↔ {mod_b_name}",
            )

        # 4. Check for Missing Dependencies
        # Pattern 4A: Mod 'X' requires mod 'Y' (which is missing)
        missing_dep_match = re.search(
            r"Mod ['\"](?P<mod_a_name>[^'\"]+)['\"]\s*\((?P<mod_a_id>[^)]+)\)\s*requires (?:version [^\s]+ of )?mod ['\"](?P<mod_b_name>[^'\"]+)['\"]\s*\((?P<mod_b_id>[^)]+)\),\s*which is missing",
            combined_text,
            re.IGNORECASE
        )
        if missing_dep_match:
            d = missing_dep_match.groupdict()
            mod_a_name = d["mod_a_name"]
            mod_b_name = d["mod_b_name"]
            mod_b_id = d["mod_b_id"]

            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Fehlende Abhängigkeit: {mod_b_name}",
                problem_description=f"Mod '{mod_a_name}' benötigt die Mod '{mod_b_name}' ({mod_b_id}), welche nicht installiert ist.",
                solution_title=f"'{mod_b_name}' automatisch installieren",
                solution_description=f"EzClient lädt die erforderliche Mod '{mod_b_name}' von Modrinth herunter und fügt sie deinem Profil hinzu.",
                action_type="INSTALL_DEPENDENCY",
                action_data={
                    "dep_id": mod_b_id,
                    "dep_name": mod_b_name,
                    "required_by": mod_a_name,
                },
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Fehlende Mod: {mod_b_name} ({mod_b_id})",
            )

        # Pattern 4B: Could not find required mod: X requires {Y @ [...]}
        missing_dep_format2 = re.search(
            r"Could not find required mod:\s*(?P<mod_a_id>[^\s]+)\s+requires\s+\{(?P<mod_b_id>[^\s@]+)\s*@\s*\[(?P<req_ver>[^\]]+)\]\}",
            combined_text,
            re.IGNORECASE
        )
        if missing_dep_format2:
            d = missing_dep_format2.groupdict()
            mod_a_id = d["mod_a_id"]
            mod_b_id = d["mod_b_id"]
            req_ver = d.get("req_ver", "*")

            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Fehlende Bibliothek: {mod_b_id}",
                problem_description=f"'{mod_a_id}' benötigt die Bibliothek '{mod_b_id}' [{req_ver}], die im Profil fehlt.",
                solution_title=f"'{mod_b_id}' herunterladen & aktivieren",
                solution_description=f"EzClient installiert die passende Version von '{mod_b_id}' automatisch.",
                action_type="INSTALL_DEPENDENCY",
                action_data={
                    "dep_id": mod_b_id,
                    "dep_name": mod_b_id,
                    "required_by": mod_a_id,
                },
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Fehlende Bibliothek: {mod_b_id}",
            )

        # 5. Check for Duplicate Mods
        duplicate_match = re.search(
            r"duplicate mod ['\"](?P<mod_id>[^'\"]+)['\"]\s*found in files ['\"](?P<file_a>[^'\"]+)['\"]\s*and\s*['\"](?P<file_b>[^'\"]+)['\"]",
            combined_text,
            re.IGNORECASE
        )
        if duplicate_match:
            d = duplicate_match.groupdict()
            mod_id = d["mod_id"]
            file_a = d["file_a"]
            file_b = d["file_b"]

            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Doppelte Mod-Dateien: {mod_id}",
                problem_description=f"Die Mod '{mod_id}' existiert doppelt im Mods-Ordner ({file_a} und {file_b}).",
                solution_title="Alte Version automatisch entfernen",
                solution_description=f"EzClient bereinigt die veraltete Datei '{file_a}' und behält nur die aktuelle Version.",
                action_type="REMOVE_DUPLICATES",
                action_data={
                    "mod_id": mod_id,
                    "file_a": file_a,
                    "file_b": file_b,
                },
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Doppelte Mod-Dateien: {file_a}, {file_b}",
            )

        # 6. Check for Mixin Crash or Specific Mod Crash in Stacktrace
        mixin_match = re.search(
            r"MixinTransformerError.*?in mod \((?P<mod_id>[^)]+)\)",
            combined_text,
            re.IGNORECASE
        )
        if mixin_match:
            mod_id = mixin_match.group("mod_id")
            return CrashDiagnosis(
                has_solution=True,
                problem_title=f"Mod-Fehler bei '{mod_id}'",
                problem_description=f"Die Mod '{mod_id}' hat einen internen Mixin-Fehler beim Laden des Spiels verursacht.",
                solution_title=f"'{mod_id}' aktualisieren oder deaktivieren",
                solution_description=f"EzClient sucht nach einem Update für '{mod_id}' oder schaltet die Mod vorübergehend ab.",
                action_type="UPDATE_OR_DISABLE_MOD",
                action_data={"mod_id": mod_id},
                can_auto_fix=True,
                raw_log=log,
                short_error=f"Mixin-Fehler in Mod '{mod_id}'",
            )

        # 7. Check if Fabric Loader generic FormattedException
        if "net.fabricmc.loader.impl.FormattedException" in combined_text:
            lines = [l.strip() for l in log.splitlines() if l.strip()]
            err_line = next((l for l in lines if "incompatible" in l.lower() or "solution" in l.lower()), "Mod-Inkompatibilität festgestellt.")
            return CrashDiagnosis(
                has_solution=True,
                problem_title="Fabric Mod-Inkompatibilität",
                problem_description="Fabric Loader hat inkompatible Mods im Profil festgestellt.",
                solution_title="Mods automatisch synchronisieren",
                solution_description="EzClient aktualisiert alle installierten Mods auf die neuesten kompatiblen Versionen für dieses Profil.",
                action_type="SYNC_ALL_MODS",
                action_data={},
                can_auto_fix=True,
                raw_log=log,
                short_error=err_line,
            )

        # 8. Fallback / Generic Crash
        short_summary = short_err or "Minecraft unerwartet beendet"
        return CrashDiagnosis(
            has_solution=False,
            problem_title="Minecraft Absturz",
            problem_description="Das Spiel wurde mit einem Fehler beendet. Genauere Details findest du im Log.",
            solution_title="Mods & Cache überprüfen",
            solution_description="Du kannst versuchen, kürzlich hinzugefügte Mods zu deaktivieren oder das vollständige Log zu prüfen.",
            action_type="GENERIC_REPAIR",
            action_data={},
            can_auto_fix=False,
            raw_log=log,
            short_error=short_summary,
        )

    def apply_fix(
        self,
        profile: ProfileData,
        diagnosis: CrashDiagnosis,
        progress_callback: Optional[Callable[[str], None]] = None
    ) -> Tuple[bool, str]:
        """Executes the automatic fix according to the diagnosed problem."""
        def notify(msg: str):
            if progress_callback:
                progress_callback(msg)
            print(f"[CrashDoctor] {msg}")

        if not profile:
            return False, "Kein aktives Profil gefunden."

        action = diagnosis.action_type
        data = diagnosis.action_data

        try:
            # A. INCREASE RAM
            if action == "INCREASE_RAM":
                new_ram = data.get("new_ram_mb", 6144)
                profile.ram_mb = new_ram
                notify(f"RAM auf {round(new_ram/1024, 1)} GB erhöht…")
                return True, f"Arbeitsspeicher erfolgreich auf {round(new_ram/1024, 1)} GB erhöht!"

            # B. FIX JAVA RUNTIME
            elif action == "FIX_JAVA":
                notify("Prüfe und installiere passende Java-Laufzeitumgebung…")
                from backend.services.java_runtime import install_required_java
                from backend.services.minecraft import minecraft_dir
                req_major = data.get("required_major", 21)
                java_bin = install_required_java(minecraft_dir(), req_major, notify)
                return True, f"Java {req_major} wurde erfolgreich eingerichtet ({Path(java_bin).name})."

            elif action == "REMOVE_INCOMPATIBLE_EZCLIENT":
                notify("Entferne inkompatiblen EzClient Core …")
                removed = False
                retained = []
                for mod in profile.mods:
                    identity = f"{mod.slug} {mod.project_id} {mod.name} {mod.filename}".lower()
                    if "ezclient" in identity:
                        removed = True
                        for candidate in (profile.mods_path / (mod.filename or ""),):
                            if candidate.is_file():
                                candidate.unlink(missing_ok=True)
                        continue
                    retained.append(mod)
                for candidate in profile.mods_path.glob("*EzClient*.jar"):
                    candidate.unlink(missing_ok=True)
                    removed = True
                profile.mods = retained
                profile.profile_type = "raw"
                profile.managed_core_mods = [value for value in profile.managed_core_mods if "ezclient" not in value.lower()]
                profile.integrated_mods = [value for value in profile.integrated_mods if "ezclient" not in value.lower()]
                if not removed:
                    return False, "Der inkompatible EzClient Core war bereits entfernt."
                return True, "Inkompatibler EzClient Core wurde entfernt. Das Fabric-Profil ist wieder startfähig."

            # C. FIX MOD INCOMPATIBILITY (e.g. Sodium & Iris version mismatch)
            elif action == "FIX_MOD_INCOMPATIBILITY":
                primary_id = (data.get("primary_mod") or "").lower()
                conflicting_id = (data.get("conflicting_mod") or "").lower()
                notify(f"Löse Inkompatibilität zwischen '{primary_id}' und '{conflicting_id}'…")

                mc_ver = profile.minecraft_version
                loader = profile.loader

                # Special case: Sodium & Iris
                if ("sodium" in primary_id and "iris" in conflicting_id) or ("iris" in primary_id and "sodium" in conflicting_id):
                    notify("Hole kompatible Versionen für Sodium und Iris von Modrinth…")
                    iris_versions = self._modrinth.get_project_versions("iris", mc_version=mc_ver, loader=loader)
                    sodium_versions = self._modrinth.get_project_versions("sodium", mc_version=mc_ver, loader=loader)

                    # Look for stable release pair
                    best_iris = select_preferred_version(iris_versions)
                    best_sodium = select_preferred_version(sodium_versions)

                    # If Iris is 1.10.7 or earlier, Sodium 0.8.13 and 0.8.14 are incompatible and must be 0.8.12 or earlier
                    if best_iris and best_sodium:
                        iris_ver_str = best_iris.get("version_number", "")
                        if "1.10.8" not in iris_ver_str:
                            # Sodium 0.8.13 and 0.8.14 break Iris <= 1.10.7; pick Sodium 0.8.12
                            compat_sodium_list = [
                                v for v in sodium_versions 
                                if not any(bad in v.get("version_number", "") for bad in ("0.8.14", "0.8.13"))
                            ]
                            if compat_sodium_list:
                                best_sodium = select_preferred_version(compat_sodium_list)

                    installed_updated = []
                    # Update Iris
                    if best_iris:
                        files = best_iris.get("files", [])
                        primary_file = next((f for f in files if f.get("primary")), files[0] if files else None)
                        if primary_file and primary_file.get("url"):
                            fn = primary_file.get("filename", "iris.jar")
                            dest = profile.mods_path / fn
                            notify(f"Lade kompatibles Iris ({best_iris.get('version_number')}) herunter…")
                            if download_file(primary_file["url"], dest):
                                for old_j in profile.mods_path.glob("*iris*.jar"):
                                    if old_j != dest:
                                        try: old_j.unlink()
                                        except Exception: pass
                                for m in profile.mods:
                                    if (m.slug and "iris" in m.slug.lower()) or (m.project_id and "iris" in m.project_id.lower()) or ("iris" in (m.name or "").lower()):
                                        m.filename = fn
                                        m.version = best_iris.get("version_number", m.version)
                                        m.enabled = True
                                installed_updated.append(f"Iris ({best_iris.get('version_number')})")

                    # Update Sodium
                    if best_sodium:
                        files = best_sodium.get("files", [])
                        primary_file = next((f for f in files if f.get("primary")), files[0] if files else None)
                        if primary_file and primary_file.get("url"):
                            fn = primary_file.get("filename", "sodium.jar")
                            dest = profile.mods_path / fn
                            notify(f"Lade kompatibles Sodium ({best_sodium.get('version_number')}) herunter…")
                            if download_file(primary_file["url"], dest):
                                for old_j in profile.mods_path.glob("*sodium*.jar"):
                                    if old_j != dest:
                                        try: old_j.unlink()
                                        except Exception: pass
                                for m in profile.mods:
                                    if (m.slug and "sodium" in m.slug.lower()) or (m.project_id and "sodium" in m.project_id.lower()) or ("sodium" in (m.name or "").lower()):
                                        m.filename = fn
                                        m.version = best_sodium.get("version_number", m.version)
                                        m.enabled = True
                                installed_updated.append(f"Sodium ({best_sodium.get('version_number')})")

                    notify("Synchronisiere Mod-Ordner…")
                    sync_profile_mods(profile, status_callback=notify)
                    return True, f"Mod-Inkompatibilität behoben! {' & '.join(installed_updated)} wurden aufeinander abgestimmt."

                # General conflicting mod resolution: Try updating the conflicting mod
                # Loader targets such as minecraft/java are constraints, not
                # downloadable mods. In that case replace the reported mod.
                target_slug = primary_id if conflicting_id in {"", "minecraft", "java"} else conflicting_id
                notify(f"Suche kompatibles Update für '{target_slug}'…")
                versions = self._modrinth.get_project_versions(target_slug, mc_version=mc_ver, loader=loader)
                if not versions:
                    versions = self._curseforge.get_project_versions(target_slug, mc_version=mc_ver, loader=loader)

                if versions:
                    best_ver = select_preferred_version(versions)
                    if best_ver:
                        files = best_ver.get("files", [])
                        primary_file = next((f for f in files if f.get("primary")), files[0] if files else None)
                        if primary_file and primary_file.get("url"):
                            fn = primary_file.get("filename", f"{target_slug}.jar")
                            dest = profile.mods_path / fn
                            notify(f"Lade '{fn}' herunter…")
                            if download_file(primary_file["url"], dest):
                                for old_j in profile.mods_path.glob(f"*{target_slug}*.jar"):
                                    if old_j != dest:
                                        try: old_j.unlink()
                                        except Exception: pass
                                for m in profile.mods:
                                    if (m.slug and target_slug in m.slug.lower()) or (m.project_id and target_slug in m.project_id.lower()):
                                        m.filename = fn
                                        m.version = best_ver.get("version_number", m.version)
                                        m.enabled = True
                                sync_profile_mods(profile, status_callback=notify)
                                return True, f"Mod '{target_slug}' wurde erfolgreich auf Version {best_ver.get('version_number')} aktualisiert."

                # If no update found, offer to disable conflicting mod
                notify(f"Kein kompatibles Update gefunden. Deaktiviere inkompatible Mod '{conflicting_id}'…")
                for m in profile.mods:
                    if (m.slug and conflicting_id in m.slug.lower()) or (m.project_id and conflicting_id in m.project_id.lower()):
                        m.enabled = False
                for old_j in profile.mods_path.glob(f"*{conflicting_id}*.jar"):
                    try:
                        disabled_p = old_j.with_suffix(".jar.disabled")
                        old_j.rename(disabled_p)
                    except Exception:
                        pass
                sync_profile_mods(profile, status_callback=notify)
                return True, f"Inkompatible Mod '{conflicting_id}' wurde vorübergehend deaktiviert."

            # D. INSTALL MISSING DEPENDENCY
            elif action == "INSTALL_DEPENDENCY":
                dep_id = data.get("dep_id", "")
                dep_name = data.get("dep_name", dep_id)
                notify(f"Suche fehlende Abhängigkeit '{dep_name}' auf Modrinth…")

                mc_ver = profile.minecraft_version
                loader = profile.loader

                versions = self._modrinth.get_project_versions(dep_id, mc_version=mc_ver, loader=loader)
                if not versions:
                    versions = self._modrinth.get_project_versions(dep_id, loader=loader)
                if not versions:
                    versions = self._curseforge.get_project_versions(dep_id, mc_version=mc_ver, loader=loader)

                if versions:
                    best_ver = select_preferred_version(versions)
                    if best_ver:
                        files = best_ver.get("files", [])
                        primary_file = next((f for f in files if f.get("primary")), files[0] if files else None)
                        if primary_file and primary_file.get("url"):
                            fn = primary_file.get("filename", f"{dep_id}.jar")
                            dest = profile.mods_path / fn
                            notify(f"Lade '{fn}' herunter…")
                            if download_file(primary_file["url"], dest):
                                # Add to profile mods list if not already present
                                existing = next((m for m in profile.mods if (m.slug or "").lower() == dep_id.lower() or (m.project_id or "").lower() == dep_id.lower()), None)
                                if existing:
                                    existing.enabled = True
                                    existing.filename = fn
                                    existing.version = best_ver.get("version_number", existing.version)
                                else:
                                    profile.mods.append(ModData(
                                        project_id=dep_id,
                                        name=dep_name,
                                        slug=dep_id,
                                        version=best_ver.get("version_number", "Latest"),
                                        filename=fn,
                                        enabled=True,
                                        essential=False
                                    ))
                                sync_profile_mods(profile, status_callback=notify)
                                return True, f"Fehlende Abhängigkeit '{dep_name}' wurde erfolgreich installiert!"

                return False, f"Abhängigkeit '{dep_name}' konnte nicht im Mod-Repository gefunden werden."

            # E. REMOVE DUPLICATES
            elif action == "REMOVE_DUPLICATES":
                file_a = data.get("file_a", "")
                file_b = data.get("file_b", "")
                path_a = profile.mods_path / file_a
                path_b = profile.mods_path / file_b

                # Remove the older or smaller duplicate
                to_delete = path_a if path_a.exists() else path_b
                if path_a.exists() and path_b.exists():
                    to_delete = path_a if path_a.stat().st_mtime < path_b.stat().st_mtime else path_b

                if to_delete.exists():
                    notify(f"Entferne doppelte Datei '{to_delete.name}'…")
                    to_delete.unlink(missing_ok=True)
                    sync_profile_mods(profile, status_callback=notify)
                    return True, f"Doppelte Mod-Datei '{to_delete.name}' wurde erfolgreich entfernt!"

                return True, "Doppelte Dateien wurden bereits bereinigt."

            # F. UPDATE OR DISABLE MOD / SYNC ALL MODS
            elif action in ("UPDATE_OR_DISABLE_MOD", "SYNC_ALL_MODS", "GENERIC_REPAIR"):
                mod_id = data.get("mod_id", "")
                if mod_id:
                    notify(f"Deaktiviere fehlerhafte Mod '{mod_id}'…")
                    for m in profile.mods:
                        if (m.slug and mod_id in m.slug.lower()) or (m.project_id and mod_id in m.project_id.lower()):
                            m.enabled = False
                    for old_j in profile.mods_path.glob(f"*{mod_id}*.jar"):
                        try:
                            disabled_p = old_j.with_suffix(".jar.disabled")
                            old_j.rename(disabled_p)
                        except Exception:
                            pass
                notify("Synchronisiere Profil-Mods…")
                sync_profile_mods(profile, status_callback=notify)
                return True, "Profil-Mods wurden erfolgreich synchronisiert und bereinigt."

            return False, "Unbekannter Reparatur-Typ."

        except Exception as exc:
            print(f"[CrashDoctor] Fix failed: {exc}")
            return False, f"Fehler bei der Reparatur: {exc}"
