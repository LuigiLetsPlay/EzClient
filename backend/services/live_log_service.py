import sys
import os
import time
import re
import threading
from pathlib import Path
from typing import Any
from PySide6.QtCore import QObject, Signal, Slot, Property

try:
    import psutil
except ImportError:
    psutil = None

TIMESTAMP_REGEX = re.compile(r"^\[(\d{2}:\d{2}:\d{2})\]")
LOG_LEVEL_REGEX = re.compile(r"\[([^\]]+)\]:\s*|\/([A-Z]+)\b|\[(ERROR|WARN|INFO|DEBUG|TRACE)\]")

class LiveLogService(QObject):
    logAppended = Signal(str, str, str, str)  # raw_line, level, time_str, message
    statsUpdated = Signal(float, float, int)   # cpu_percent, ram_mb, uptime_seconds
    isRunningChanged = Signal()
    logsCleared = Signal()

    def __init__(self, parent=None):
        super().__init__(parent)
        self._is_running: bool = False
        self._intentional_stop: bool = False
        self._current_process = None
        self._current_pid: int | None = None
        self._start_time: float = 0
        self._log_file_path: Path | None = None
        self._tail_thread: threading.Thread | None = None
        self._stats_thread: threading.Thread | None = None
        self._stop_event = threading.Event()
        self._lines_buffer: list[dict[str, str]] = []
        self._instance_name: str = "Minecraft"
        self._loader_version: str = "Fabric 26.2"

    @property
    def intentional_stop(self) -> bool:
        return self._intentional_stop

    @Property(bool, notify=isRunningChanged)
    def isRunning(self) -> bool:
        return self._is_running

    @Property(str, notify=isRunningChanged)
    def instanceName(self) -> str:
        return self._instance_name

    @Property(str, notify=isRunningChanged)
    def loaderVersion(self) -> str:
        return self._loader_version

    @Property(int, notify=isRunningChanged)
    def totalLines(self) -> int:
        return len(self._lines_buffer)

    def attach_process(self, proc, log_file: Path, instance_name: str = "Minecraft", loader_version: str = "Fabric 26.2") -> None:
        """Starts monitoring a Minecraft process and tailing its log file."""
        self._stop_event.set()
        if self._tail_thread and self._tail_thread.is_alive():
            self._tail_thread.join(timeout=1.0)
        if self._stats_thread and self._stats_thread.is_alive():
            self._stats_thread.join(timeout=1.0)

        self._stop_event.clear()
        self._intentional_stop = False
        self._current_process = proc
        self._current_pid = proc.pid if proc else None
        self._log_file_path = log_file
        self._instance_name = instance_name
        self._loader_version = loader_version
        self._start_time = time.time()
        self._is_running = True
        self.isRunningChanged.emit()

        try:
            from backend.services import discord_service
            discord_service.set_rpc_state(f"Playing {instance_name}", "In Game")
        except Exception:
            pass

        # Start log tailing thread
        self._tail_thread = threading.Thread(target=self._tail_worker, daemon=True)
        self._tail_thread.start()

        # Start stats polling thread
        self._stats_thread = threading.Thread(target=self._stats_worker, daemon=True)
        self._stats_thread.start()

    def detach_process(self) -> None:
        """Stops live monitoring when process terminates."""
        self._is_running = False
        self._stop_event.set()
        self.isRunningChanged.emit()
        
        try:
            from backend.services import discord_service
            discord_service.set_rpc_state("Im EzClient Launcher", "Navigating Menus")
        except Exception:
            pass

    def _tail_worker(self) -> None:
        last_pos = 0
        while not self._stop_event.is_set():
            if self._log_file_path and self._log_file_path.exists():
                try:
                    with open(self._log_file_path, "r", encoding="utf-8", errors="replace") as f:
                        f.seek(last_pos)
                        new_lines = f.readlines()
                        last_pos = f.tell()

                    for raw in new_lines:
                        raw = raw.rstrip("\r\n")
                        if not raw.strip():
                            continue
                        self._process_log_line(raw)
                except Exception as e:
                    pass
            time.sleep(0.2)

    def _process_log_line(self, raw_line: str) -> None:
        # Extract time
        time_match = TIMESTAMP_REGEX.search(raw_line)
        time_str = time_match.group(1) if time_match else time.strftime("%H:%M:%S")

        # Extract level
        level = "INFO"
        upper = raw_line.upper()
        if "ERROR" in upper or "FATAL" in upper or "EXCEPTION" in upper or "CRASH" in upper:
            level = "ERROR"
        elif "WARN" in upper or "WARNING" in upper:
            level = "WARN"
        elif "DEBUG" in upper:
            level = "DEBUG"
        elif "TRACE" in upper:
            level = "TRACE"

        clean_msg = raw_line
        self._lines_buffer.append({
            "raw": raw_line,
            "level": level,
            "time": time_str,
            "message": clean_msg
        })
        if len(self._lines_buffer) > 6000:
            self._lines_buffer = self._lines_buffer[-5000:]

        self.logAppended.emit(raw_line, level, time_str, clean_msg)

    def append_system_message(self, message: str, level: str = "INFO") -> None:
        """Expose launcher/bootstrap progress in the same log window."""
        timestamp = time.strftime("%H:%M:%S")
        raw = f"[{timestamp}] [EzClient/{level}]: {message}"
        self._lines_buffer.append({
            "raw": raw, "level": level, "time": timestamp, "message": message
        })
        if len(self._lines_buffer) > 6000:
            self._lines_buffer = self._lines_buffer[-5000:]
        self.logAppended.emit(raw, level, timestamp, message)

    def _stats_worker(self) -> None:
        p_obj = None
        if psutil and self._current_pid:
            try:
                p_obj = psutil.Process(self._current_pid)
            except Exception:
                p_obj = None

        while not self._stop_event.is_set():
            if not self._is_running:
                break

            cpu_val = 0.0
            ram_val = 0.0
            uptime = int(time.time() - self._start_time) if self._start_time > 0 else 0

            if p_obj:
                try:
                    if p_obj.is_running():
                        cpu_val = p_obj.cpu_percent(interval=0.1)
                        ram_bytes = p_obj.memory_info().rss
                        ram_val = ram_bytes / (1024 * 1024)
                    else:
                        break
                except Exception:
                    break

            self.statsUpdated.emit(round(cpu_val, 1), round(ram_val, 1), uptime)
            time.sleep(1.0)

    @Slot()
    def clearLogs(self) -> None:
        self._lines_buffer.clear()
        self.logsCleared.emit()

    @Slot()
    def stopInstance(self) -> None:
        """Kills the active running Minecraft process upon user request."""
        self._intentional_stop = True
        if self._current_process:
            try:
                self._current_process.terminate()
            except Exception:
                try:
                    self._current_process.kill()
                except Exception:
                    pass
        self.detach_process()

    @Slot(result=str)
    def getAllLogsText(self) -> str:
        return "\n".join(item["raw"] for item in self._lines_buffer)
