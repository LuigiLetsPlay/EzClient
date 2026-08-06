"""EzClient Launcher — tkinter-GUI."""

import sys
import threading
import tkinter as tk
from tkinter import ttk, messagebox
from pathlib import Path

from . import auth, game, updater, version


class EzClientLauncher:
    def __init__(self, root):
        self.root = root
        self.root.title(version.APP_NAME)
        self.root.geometry("520x420")
        self.root.resizable(False, False)

        # Dunkles Theme
        style = ttk.Style()
        style.theme_use("clam")
        bg_color = "#2b2b2b"
        fg_color = "#ffffff"
        style.configure("TLabel", background=bg_color, foreground=fg_color)
        style.configure("TButton", background=bg_color, foreground=fg_color)
        style.configure("TFrame", background=bg_color)
        style.configure("TEntry", fieldbackground="#3c3c3c", foreground=fg_color)
        style.configure("TCombobox", fieldbackground="#3c3c3c", foreground=fg_color)
        style.map("TButton", background=[("active", "#404040")])

        self.root.configure(bg=bg_color)

        self.profile = None
        self.versions = []

        # Main-Container
        self.main_frame = ttk.Frame(root)
        self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # Screens
        self.screens = {}
        self._create_screens()

        # Check fuer Updates beim Start
        self.root.after(500, self._check_updates)

        # Login-Status laden
        self.profile = auth.load_cached_profile()
        if self.profile:
            self._show_version_screen()
        else:
            self._show_login_screen()

    def _create_screens(self):
        self._create_login_screen()
        self._create_version_screen()
        self._create_install_screen()

    def _clear_main_frame(self):
        for w in self.main_frame.winfo_children():
            w.destroy()

    def _show_login_screen(self):
        self._clear_main_frame()
        self.screens["login"]()

    def _show_version_screen(self):
        self._clear_main_frame()
        self.screens["version"]()

    def _show_install_screen(self):
        self._clear_main_frame()
        self.screens["install"]()

    def _create_login_screen(self):
        def screen():
            title = ttk.Label(self.main_frame, text="EzClient Launcher", font=("Arial", 18, "bold"))
            title.pack(pady=20)

            info = ttk.Label(
                self.main_frame,
                text="Um Minecraft zu starten, melde dich\nmit deinem Microsoft-Konto an.",
                justify=tk.CENTER,
            )
            info.pack(pady=10)

            btn_login = ttk.Button(self.main_frame, text="Mit Microsoft anmelden", command=self._do_login)
            btn_login.pack(pady=20)

            self.login_status = ttk.Label(self.main_frame, text="", foreground="#ffaa00")
            self.login_status.pack(pady=10)

        self.screens["login"] = screen

    def _do_login(self):
        def login_thread():
            try:
                self._update_login_status("Verbinde mit Microsoft...")

                def on_code(user_code, uri):
                    self._update_login_status(f"Code: {user_code}\nKlick hier: {uri}")

                self.profile = auth.login(on_code)
                self.root.after(0, self._show_version_screen)
            except Exception as e:
                self._update_login_status(f"Fehler: {str(e)}", error=True)

        thread = threading.Thread(target=login_thread, daemon=True)
        thread.start()

    def _update_login_status(self, text, error=False):
        def update():
            self.login_status.config(text=text, foreground="#ff3333" if error else "#ffaa00")

        self.root.after(0, update)

    def _create_version_screen(self):
        def screen():
            # Header mit Spielername
            if self.profile:
                header = ttk.Label(
                    self.main_frame,
                    text=f"Willkommen, {self.profile['name']}!",
                    font=("Arial", 14, "bold"),
                )
                header.pack(pady=10)

            # Versionen laden
            try:
                self.versions = game.list_versions()
                version_names = [v["id"] for v in self.versions]
            except Exception as e:
                messagebox.showerror("Fehler", f"Versionen konnten nicht geladen werden:\n{e}")
                version_names = []

            label = ttk.Label(self.main_frame, text="Waehle eine Minecraft-Version:")
            label.pack(pady=10)

            self.version_combo = ttk.Combobox(
                self.main_frame,
                values=version_names,
                state="readonly",
                width=40,
            )
            if version_names:
                self.version_combo.current(0)
            self.version_combo.pack(pady=5)

            btn_play = ttk.Button(self.main_frame, text="Spielen", command=self._do_play)
            btn_play.pack(pady=20)

            btn_logout = ttk.Button(self.main_frame, text="Abmelden", command=self._do_logout)
            btn_logout.pack(pady=10)

        self.screens["version"] = screen

    def _do_play(self):
        selected = self.version_combo.get()
        if not selected:
            messagebox.showwarning("Warnung", "Bitte waehle eine Version aus.")
            return

        self._show_install_screen()

        def install_thread():
            try:
                self._update_install_status("Ueberpruefen...", 0, 100)
                game.install(selected, self._update_progress)
                self._update_install_status("Starte Minecraft...", 100, 100)

                game.launch(selected, self.profile)

                self.root.after(1000, self.root.quit)
            except Exception as e:
                self._update_install_status(f"Fehler: {str(e)}", error=True)

        thread = threading.Thread(target=install_thread, daemon=True)
        thread.start()

    def _do_logout(self):
        auth.logout()
        self.profile = None
        self._show_login_screen()

    def _create_install_screen(self):
        def screen():
            title = ttk.Label(self.main_frame, text="Installiere Minecraft...", font=("Arial", 14, "bold"))
            title.pack(pady=20)

            self.progress_var = tk.IntVar()
            self.progress_bar = ttk.Progressbar(
                self.main_frame,
                maximum=100,
                variable=self.progress_var,
                length=400,
            )
            self.progress_bar.pack(pady=20)

            self.status_label = ttk.Label(self.main_frame, text="Vorbereitung...", justify=tk.CENTER)
            self.status_label.pack(pady=10)

        self.screens["install"] = screen

    def _update_progress(self, value):
        def update():
            self.progress_var.set(int(value))

        self.root.after(0, update)

    def _update_install_status(self, text, progress=None, max_val=None, error=False):
        def update():
            self.status_label.config(text=text, foreground="#ff3333" if error else "#ffffff")
            if progress is not None and max_val is not None:
                self.progress_var.set(int(progress))
                self.progress_bar.config(maximum=max_val)

        self.root.after(0, update)

    def _check_updates(self):
        def check():
            newer_version, download_url = updater.check_for_update()
            if newer_version:
                if messagebox.askyesno(
                    "Update verfuegbar",
                    f"Version {newer_version} ist verfuegbar.\nJetzt aktualisieren?",
                ):
                    updater.download_and_apply_update(download_url)
                    self.root.quit()

        thread = threading.Thread(target=check, daemon=True)
        thread.start()


def main():
    root = tk.Tk()
    app = EzClientLauncher(root)
    root.mainloop()


if __name__ == "__main__":
    main()
