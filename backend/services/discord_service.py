import time
import threading
import psutil

try:
    from pypresence import Presence
except ImportError:
    Presence = None

# Client ID der vom Nutzer erstellten EzClient-App im Discord Developer Portal
CLIENT_ID = "1539683024382197931"

_rpc = None
_running = False
_current_state = {
    "details": "Im EzClient Launcher",
    "state": "Navigating Menus",
    "start": int(time.time()),
    "large_image": "logo",
    "large_text": "EzClient"
}

def init_rpc():
    global _rpc, _running
    if Presence is None:
        print("[DiscordRPC] pypresence not installed.")
        return
    if _running: return
    _running = True
    
    def rpc_worker():
        global _rpc
        import asyncio
        import sys
        try:
            if sys.platform == "win32":
                loop = asyncio.ProactorEventLoop()
            else:
                loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        except Exception:
            pass

        while _running:
            try:
                if _rpc is None:
                    _rpc = Presence(CLIENT_ID)
                    _rpc.connect()
                    with open("discord_debug.log", "a") as f:
                        f.write(f"Connected to Discord!\n")
                
                _rpc.update(
                    state=_current_state.get("state"),
                    details=_current_state.get("details"),
                    large_image=_current_state.get("large_image"),
                    large_text=_current_state.get("large_text"),
                    start=_current_state.get("start")
                )
                with open("discord_debug.log", "a") as f:
                    f.write(f"Updated status: {_current_state.get('details')}\n")
            except Exception as e:
                with open("discord_debug.log", "a") as f:
                    f.write(f"RPC Error: {e}\n")
                _rpc = None
            time.sleep(15)
            
    threading.Thread(target=rpc_worker, daemon=True, name="DiscordRPC").start()

def set_rpc_state(details: str, state: str, start_time: int = None):
    global _current_state
    _current_state["details"] = details
    _current_state["state"] = state
    _current_state["start"] = start_time or int(time.time())

def stop_rpc():
    global _running, _rpc
    _running = False
    if _rpc:
        try:
            _rpc.close()
        except: pass
        _rpc = None
