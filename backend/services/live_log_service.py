import re
import threading
import time
import uuid
from pathlib import Path

from PySide6.QtCore import QObject, Property, Signal, Slot

try:
    import psutil
except ImportError:
    psutil = None

TIMESTAMP_REGEX = re.compile(r"^\[(\d{2}:\d{2}:\d{2})\]")
LOG_LEVEL_REGEX = re.compile(r"\[(?:[^/\]]+/)?(FATAL|ERROR|WARN|WARNING|INFO|DEBUG|TRACE)\]", re.IGNORECASE)


def detect_log_level(raw: str) -> str:
    m = LOG_LEVEL_REGEX.search(raw)
    if m:
        lvl = m.group(1).upper()
        if lvl == "WARNING":
            return "WARN"
        if lvl == "FATAL":
            return "ERROR"
        return lvl
    upper = raw.upper()
    if any(x in upper for x in ("EXCEPTION", "CRASH")) or "ERROR:" in upper:
        return "ERROR"
    if "WARN" in upper:
        return "WARN"
    if "DEBUG" in upper:
        return "DEBUG"
    if "TRACE" in upper:
        return "TRACE"
    return "INFO"



class LiveLogService(QObject):
    """Registry for independent Minecraft processes, logs and runtime stats."""

    logAppended = Signal(str, str, str, str)
    statsUpdated = Signal(float, float, int)
    isRunningChanged = Signal()
    logsCleared = Signal()
    instancesChanged = Signal()
    selectedInstanceChanged = Signal()
    allInstancesStopped = Signal()
    instanceExited = Signal(str, int, bool)

    def __init__(self, parent=None):
        super().__init__(parent)
        self._instances: dict[str, dict] = {}
        self._instance_order: list[str] = []
        self._selected_id = ""
        self._pending_lines: list[dict[str, str]] = []
        self._lock = threading.RLock()

    def _selected(self) -> dict | None:
        with self._lock:
            return self._instances.get(self._selected_id)

    @property
    def intentional_stop(self) -> bool:
        state = self._selected()
        return bool(state and state["intentional_stop"])

    def was_intentionally_stopped(self, instance_id: str) -> bool:
        with self._lock:
            state = self._instances.get(instance_id)
            return bool(state and state["intentional_stop"])

    @Property(bool, notify=isRunningChanged)
    def isRunning(self) -> bool:
        state = self._selected()
        return bool(state and state["running"])

    @Property(str, notify=selectedInstanceChanged)
    def instanceName(self) -> str:
        state = self._selected()
        return str(state["name"]) if state else "Minecraft"

    @Property(str, notify=selectedInstanceChanged)
    def loaderVersion(self) -> str:
        state = self._selected()
        return str(state["loader"]) if state else ""

    @Property(str, notify=selectedInstanceChanged)
    def selectedInstanceId(self) -> str:
        return self._selected_id

    @Property(int, notify=instancesChanged)
    def runningCount(self) -> int:
        with self._lock:
            return sum(1 for state in self._instances.values() if state["running"])

    @Property(int, notify=selectedInstanceChanged)
    def totalLines(self) -> int:
        state = self._selected()
        return len(state["lines"]) if state else 0

    @Property("QVariantList", notify=instancesChanged)
    def instances(self) -> list[dict]:
        now = time.time()
        with self._lock:
            return [{
                "instanceId": instance_id,
                "name": state["name"],
                "loader": state["loader"],
                "account": state["account"],
                "profilePath": state["profile_path"],
                "running": state["running"],
                "pid": state["pid"] or 0,
                "startTime": state["start_time"],
                "uptime": int((state.get("end_time") or now) - state["start_time"]),
                "lineCount": len(state["lines"]),
            } for instance_id in reversed(self._instance_order)
              if (state := self._instances.get(instance_id))]

    def attach_process(self, proc, log_file: Path, instance_name: str = "Minecraft",
                       loader_version: str = "", account_name: str = "Player",
                       profile_path: str = "", instance_id: str = "") -> str:
        instance_id = instance_id or uuid.uuid4().hex
        state = {
            "process": proc, "pid": proc.pid if proc else None,
            "log_file": Path(log_file), "name": instance_name,
            "loader": loader_version, "account": account_name,
            "profile_path": profile_path, "start_time": time.time(),
            "end_time": None, "running": True, "intentional_stop": False,
            "stop_event": threading.Event(), "lines": list(self._pending_lines),
        }
        with self._lock:
            existing = self._instances.get(instance_id)
            if existing:
                state["lines"] = existing["lines"]
                state["start_time"] = existing["start_time"]
            else:
                self._pending_lines.clear()
            self._instances[instance_id] = state
            if instance_id not in self._instance_order:
                self._instance_order.append(instance_id)
            self._selected_id = instance_id
        self.instancesChanged.emit()
        self.selectedInstanceChanged.emit()
        self.isRunningChanged.emit()
        self._emit_selected_stats()
        threading.Thread(target=self._tail_worker, args=(instance_id,), daemon=True).start()
        threading.Thread(target=self._stats_worker, args=(instance_id,), daemon=True).start()
        threading.Thread(target=self._process_watcher_worker, args=(instance_id,), daemon=True).start()
        try:
            from backend.services import discord_service
            discord_service.set_rpc_state(f"Playing {instance_name}", "In Game")
        except Exception:
            pass
        return instance_id

    def begin_instance(self, log_file: Path, instance_name: str, loader_version: str,
                       account_name: str, profile_path: str) -> str:
        """Creates the instance before downloads/bootstrap so early logs stay isolated."""
        instance_id = uuid.uuid4().hex
        with self._lock:
            self._instances[instance_id] = {
                "process": None, "pid": None, "log_file": Path(log_file),
                "name": instance_name, "loader": loader_version,
                "account": account_name, "profile_path": profile_path,
                "start_time": time.time(), "end_time": None, "running": True,
                "intentional_stop": False, "stop_event": threading.Event(),
                "lines": [],
            }
            self._instance_order.append(instance_id)
            self._selected_id = instance_id
        self.instancesChanged.emit()
        self.selectedInstanceChanged.emit()
        self.isRunningChanged.emit()
        return instance_id

    def detach_process(self, instance_id: str = "") -> None:
        target_id = instance_id or self._selected_id
        with self._lock:
            state = self._instances.get(target_id)
            if not state or not state.get("running"):
                return
            state["running"] = False
            state["end_time"] = time.time()
            state["stop_event"].set()
        self.instancesChanged.emit()
        if target_id == self._selected_id:
            self.isRunningChanged.emit()
            self._emit_selected_stats()
        if self.runningCount == 0:
            try:
                from backend.services import discord_service
                discord_service.set_rpc_state("Im EzClient Launcher", "Navigating Menus")
            except Exception:
                pass
            try:
                self.allInstancesStopped.emit()
            except RuntimeError:
                pass

    def _process_watcher_worker(self, instance_id: str) -> None:
        with self._lock:
            state = self._instances.get(instance_id)
            if not state:
                return
            proc = state.get("process")
            pid = state.get("pid")
            stop_event = state.get("stop_event")

        if proc and hasattr(proc, "wait") and callable(proc.wait):
            try:
                proc.wait()
            except Exception:
                pass
        elif proc and hasattr(proc, "poll") and callable(proc.poll):
            while True:
                if stop_event and stop_event.is_set():
                    break
                if proc.poll() is not None:
                    break
                time.sleep(0.5)
        elif psutil and pid:
            try:
                p = psutil.Process(pid)
                while p.is_running():
                    if stop_event and stop_event.is_set():
                        break
                    time.sleep(0.5)
            except Exception:
                return
        else:
            return

        time.sleep(0.5)
        with self._lock:
            state = self._instances.get(instance_id)
            if not state or not state.get("running"):
                return
            intentional = state.get("intentional_stop", False)
            rc = getattr(proc, "returncode", None) if proc else None

        if rc is not None and not intentional:
            if rc != 0:
                self.append_system_message(f"Minecraft wurde mit Exit-Code {rc} beendet.", "WARN", instance_id)
            else:
                self.append_system_message("Minecraft wurde beendet.", "INFO", instance_id)
        elif not intentional:
            self.append_system_message("Minecraft wurde beendet.", "INFO", instance_id)

        try:
            self.instanceExited.emit(instance_id, int(rc) if rc is not None else 0, intentional)
        except Exception:
            pass

        self.detach_process(instance_id)

    def _tail_worker(self, instance_id: str) -> None:
        last_pos = 0
        while True:
            with self._lock:
                state = self._instances.get(instance_id)
                if not state:
                    return
                stop_event, log_file = state["stop_event"], state["log_file"]
            if log_file.exists():
                try:
                    with open(log_file, "r", encoding="utf-8", errors="replace") as handle:
                        handle.seek(last_pos)
                        lines, last_pos = handle.readlines(), handle.tell()
                    for raw in lines:
                        raw = raw.rstrip("\r\n")
                        if raw.strip():
                            self._process_log_line(instance_id, raw)
                except OSError:
                    pass
            if stop_event.is_set():
                return
            time.sleep(0.2)

    def _process_log_line(self, instance_id: str, raw: str) -> None:
        match = TIMESTAMP_REGEX.search(raw)
        stamp = match.group(1) if match else time.strftime("%H:%M:%S")
        level = detect_log_level(raw)
        entry = {"raw": raw, "level": level, "time": stamp, "message": raw}
        with self._lock:
            state = self._instances.get(instance_id)
            if not state:
                return
            state["lines"].append(entry)
            if len(state["lines"]) > 6000:
                state["lines"] = state["lines"][-5000:]
            selected = instance_id == self._selected_id
        if selected:
            try:
                self.logAppended.emit(raw, level, stamp, raw)
            except RuntimeError:
                pass

    def append_system_message(self, message: str, level: str = "INFO", instance_id: str = "") -> None:
        stamp = time.strftime("%H:%M:%S")
        raw = f"[{stamp}] [EzClient/{level}]: {message}"
        entry = {"raw": raw, "level": level, "time": stamp, "message": message}
        target_id = instance_id or self._selected_id
        with self._lock:
            state = self._instances.get(target_id)
            if state:
                state["lines"].append(entry)
                selected = target_id == self._selected_id
            else:
                self._pending_lines.append(entry)
                selected = False
        if selected:
            try:
                self.logAppended.emit(raw, level, stamp, message)
            except RuntimeError:
                pass

    @Slot(result="QVariantList")
    def getBufferedLogs(self) -> list[dict[str, str]]:
        state = self._selected()
        if not state:
            return list(self._pending_lines)
        with self._lock:
            # Fallback: if in-memory lines are empty, load lines from log file on disk
            if not state["lines"] and state.get("log_file"):
                try:
                    p = Path(state["log_file"])
                    if p.exists():
                        with open(p, "r", encoding="utf-8", errors="replace") as f:
                            for raw in f:
                                raw = raw.rstrip("\r\n")
                                if raw.strip():
                                    match = TIMESTAMP_REGEX.search(raw)
                                    stamp = match.group(1) if match else time.strftime("%H:%M:%S")
                                    level = detect_log_level(raw)
                                    state["lines"].append({"raw": raw, "level": level, "time": stamp, "message": raw})
                except Exception:
                    pass
            return [dict(item) for item in state["lines"]]

    @Slot(str, result=str)
    def getFullLog(self, instance_id: str = "") -> str:
        target_id = instance_id or self._selected_id
        with self._lock:
            state = self._instances.get(target_id)
            if state:
                log_file = state.get("log_file")
                if log_file and Path(log_file).exists():
                    try:
                        return Path(log_file).read_text(encoding="utf-8", errors="replace")
                    except Exception:
                        pass
                if state.get("lines"):
                    return "\n".join(entry.get("raw", "") for entry in state["lines"])
        return "\n".join(entry.get("raw", "") for entry in self._pending_lines)

    def _stats_worker(self, instance_id: str) -> None:
        with self._lock:
            state = self._instances.get(instance_id)
            pid = state["pid"] if state else None
        process = None
        if psutil and pid:
            try:
                process = psutil.Process(pid)
            except Exception:
                process = None
        while True:
            with self._lock:
                state = self._instances.get(instance_id)
                if not state or not state.get("running") or state["stop_event"].is_set():
                    return
                uptime = int(time.time() - state["start_time"])
                selected = (instance_id == self._selected_id)
            cpu = 0.0
            ram = 0.0
            if process:
                try:
                    if not process.is_running():
                        break
                    cpu = process.cpu_percent()
                    ram = process.memory_info().rss / (1024 * 1024)
                except Exception:
                    pass
            if selected:
                try:
                    self.statsUpdated.emit(round(cpu, 1), round(ram, 1), uptime)
                except Exception:
                    pass
            time.sleep(1.0)

    def _emit_selected_stats(self) -> None:
        state = self._selected()
        now = time.time()
        uptime = int((state.get("end_time") or now) - state["start_time"]) if state else 0
        try:
            self.statsUpdated.emit(0.0, 0.0, uptime)
        except Exception:
            pass

    @Slot(str, result=bool)
    def selectInstance(self, instance_id: str) -> bool:
        with self._lock:
            if instance_id not in self._instances:
                return False
            self._selected_id = instance_id
        self.selectedInstanceChanged.emit()
        self.isRunningChanged.emit()
        self._emit_selected_stats()
        return True

    @Slot()
    def clearLogs(self) -> None:
        state = self._selected()
        if state:
            with self._lock:
                state["lines"].clear()
        else:
            self._pending_lines.clear()
        self.logsCleared.emit()
        self.instancesChanged.emit()

    @Slot()
    @Slot(str)
    def stopInstance(self, instance_id: str = "") -> None:
        target_id = instance_id or self._selected_id
        with self._lock:
            state = self._instances.get(target_id)
            if not state or not state["running"]:
                return
            state["intentional_stop"] = True
            proc = state["process"]
        try:
            proc.terminate()
        except Exception:
            try:
                proc.kill()
            except Exception:
                pass
        self.detach_process(target_id)

    @Slot(str)
    def removeInstance(self, instance_id: str) -> None:
        with self._lock:
            state = self._instances.get(instance_id)
            if not state or state["running"]:
                return
            self._instances.pop(instance_id, None)
            if instance_id in self._instance_order:
                self._instance_order.remove(instance_id)
            if self._selected_id == instance_id:
                self._selected_id = self._instance_order[-1] if self._instance_order else ""
        self.instancesChanged.emit()
        self.selectedInstanceChanged.emit()

    @Slot(result=str)
    def getAllLogsText(self) -> str:
        return "\n".join(item["raw"] for item in self.getBufferedLogs())
