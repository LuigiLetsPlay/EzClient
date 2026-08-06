"""EzClient Setup — Downloader der EzClient.exe vom GitHub-Release."""

import sys
import os
import tkinter as tk
from tkinter import ttk, messagebox
from pathlib import Path
import requests
import subprocess


GITHUB_REPO = "LuigiLetsPlay/EzClient"
INSTALL_DIR = Path.home() / "AppData" / "Local" / "EzClient"


def download_launcher():
    r"""
    Lade EzClient.exe vom letzten GitHub-Release herunter
    und installiere sie in %LOCALAPPDATA%\EzClient.
    """
    try:
        # Hole Release-Informationen
        resp = requests.get(f"https://api.github.com/repos/{GITHUB_REPO}/releases/latest", timeout=10)
        resp.raise_for_status()
        release = resp.json()

        # Finde EzClient.exe-Asset
        download_url = None
        for asset in release.get("assets", []):
            if asset["name"] == "EzClient.exe":
                download_url = asset["browser_download_url"]
                break

        if not download_url:
            raise ValueError("EzClient.exe nicht im Release gefunden.")

        # Erstelle Installationsverzeichnis
        INSTALL_DIR.mkdir(parents=True, exist_ok=True)

        exe_path = INSTALL_DIR / "EzClient.exe"

        # Download mit Progressbar
        resp = requests.get(download_url, stream=True, timeout=30)
        resp.raise_for_status()

        total_size = int(resp.headers.get("content-length", 0)) or 1
        downloaded = 0

        with open(exe_path, "wb") as f:
            for chunk in resp.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)
                    downloaded += len(chunk)
                    progress = min(100, int((downloaded / total_size) * 100))
                    yield progress

        # Erstelle Desktop-Verknuepfung (optional)
        try:
            desktop = Path.home() / "Desktop"
            vbs_script = f'''
Set oWS = WScript.CreateObject("WScript.Shell")
sLinkFile = "{desktop}\\EzClient.lnk"
Set oLink = oWS.CreateShortcut(sLinkFile)
oLink.TargetPath = "{exe_path}"
oLink.Description = "EzClient Launcher"
oLink.Save
'''
            vbs_file = INSTALL_DIR / "create_shortcut.vbs"
            with open(vbs_file, "w") as f:
                f.write(vbs_script)

            subprocess.run(
                ["cscript.exe", str(vbs_file)],
                capture_output=True,
                timeout=5,
            )

            vbs_file.unlink()
        except Exception:
            pass  # Shortcut erstellen ist optional

        # Starte den Launcher
        subprocess.Popen(str(exe_path))
        yield 100

    except Exception as e:
        yield -1  # Fehler-Signal
        raise


class SetupUI:
    def __init__(self, root):
        self.root = root
        self.root.title("EzClient Setup")
        self.root.geometry("400x150")
        self.root.resizable(False, False)

        # Dunkles Theme
        bg_color = "#2b2b2b"
        fg_color = "#ffffff"
        self.root.configure(bg=bg_color)

        frame = tk.Frame(root, bg=bg_color)
        frame.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)

        title = tk.Label(frame, text="EzClient wird installiert...", bg=bg_color, fg=fg_color, font=("Arial", 12, "bold"))
        title.pack(pady=10)

        self.progress_var = tk.IntVar()
        self.progress_bar = ttk.Progressbar(frame, maximum=100, variable=self.progress_var, length=350)
        self.progress_bar.pack(pady=10)

        self.status_label = tk.Label(frame, text="Lade herunter...", bg=bg_color, fg=fg_color)
        self.status_label.pack()

        self.start_download()

    def start_download(self):
        def download_thread():
            try:
                gen = download_launcher()
                for progress in gen:
                    self.root.after(0, lambda p=progress: self.progress_var.set(p))

                self.root.after(500, self.root.quit)
            except Exception as e:
                self.root.after(0, lambda e=e: self._show_error(str(e)))

        import threading

        thread = threading.Thread(target=download_thread, daemon=True)
        thread.start()

    def _show_error(self, error_msg):
        messagebox.showerror("Fehler", f"Installation fehlgeschlagen:\n{error_msg}")
        self.root.quit()


def main():
    root = tk.Tk()
    app = SetupUI(root)
    root.mainloop()


if __name__ == "__main__":
    main()
