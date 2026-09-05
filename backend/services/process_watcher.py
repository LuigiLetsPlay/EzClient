import os
import sys
import time
import threading
from typing import Any, Callable

def kill_official_launcher() -> None:
    """Terminates official Minecraft launcher processes."""
    launcher_names = {
        "minecraftlauncher.exe",
        "minecraft.exe",
        "minecraftinstaller.exe"
    }
    try:
        import psutil
        for proc in psutil.process_iter(['name', 'pid']):
            try:
                name = (proc.info.get('name') or "").lower()
                if name in launcher_names:
                    proc.kill()
                    print(f"[ProcessWatcher] Terminated launcher process {name} (PID: {proc.pid})")
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass
    except Exception:
        # Fallback using taskkill on Windows
        if sys.platform.startswith("win"):
            for name in ["MinecraftLauncher.exe", "Minecraft.exe"]:
                os.system(f"taskkill /f /im {name} >nul 2>&1")

def find_minecraft_process() -> Any:
    """Finds running Minecraft java process."""
    try:
        import psutil
        for proc in psutil.process_iter(['name', 'pid', 'cmdline']):
            try:
                name = (proc.info.get('name') or "").lower()
                if "java" in name:
                    cmdline = " ".join(proc.info.get('cmdline') or []).lower()
                    if "minecraft" in cmdline or "fabricloader" in cmdline or "net.fabricmc" in cmdline or "mojang" in cmdline or "net.minecraft.client" in cmdline:
                        return proc
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                pass
    except Exception:
        pass
    return None

class MinecraftWatcher:
    def __init__(self, on_started: Callable[[], None], on_exited: Callable[[], None], kill_launcher: bool = True):
        self._on_started = on_started
        self._on_exited = on_exited
        self._kill_launcher = kill_launcher
        self._running = False
        self._thread: threading.Thread | None = None

    def start(self, timeout_seconds: int = 120) -> None:
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(target=self._watch_loop, args=(timeout_seconds,), daemon=True)
        self._thread.start()

    def stop(self) -> None:
        self._running = False

    def _watch_loop(self, timeout_seconds: int) -> None:
        start_time = time.time()
        game_proc = None

        print("[ProcessWatcher] Waiting for Minecraft game process to start…")
        # Step 1: Wait for Minecraft game (javaw.exe) to start
        while self._running and (time.time() - start_time < timeout_seconds):
            proc = find_minecraft_process()
            if proc:
                game_proc = proc
                if self._kill_launcher:
                    print(f"[ProcessWatcher] Minecraft started (PID: {proc.pid})! Killing official launcher…")
                    kill_official_launcher()
                else:
                    print(f"[ProcessWatcher] Minecraft started (PID: {proc.pid})! Launcher kill disabled.")
                if self._on_started:
                    self._on_started()
                break
            time.sleep(1.0)

        if not game_proc:
            print("[ProcessWatcher] Minecraft launch wait timed out or was cancelled.")
            if self._on_exited:
                self._on_exited()
            self._running = False
            return

        # Step 2: Wait for Minecraft game to exit
        print(f"[ProcessWatcher] Monitoring Minecraft PID {game_proc.pid}…")
        while self._running:
            try:
                if not game_proc.is_running() or game_proc.status() == "zombie":
                    break
            except Exception:
                break
            time.sleep(1.2)

        print("[ProcessWatcher] Minecraft closed! Restoring EzClient window…")
        self._running = False
        if self._on_exited:
            self._on_exited()
