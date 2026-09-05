@C:\Users\Luigi\.codex\RTK.md

# EzClient maintenance policy

## Active Minecraft line

- Only the `26.x` family (`26.1`, `26.1.1`, `26.2`) is actively developed and receives
  EzClient module, rendering, performance, compatibility, or feature updates.
- Normal changes belong in the current `app/ezclient` implementation and in
  explicitly `26.x`-scoped sources/resources.
- A normal JAR build is `python client_mod/build_mod.py`; it builds only
  `26.1`, `26.1.1`, and `26.2`.

## Retired legacy EzClient lines & Base Launcher Support

- EzClient mod JARs for Minecraft versions outside of `26.x` (1.8.9, 1.12.2, 1.16.5,
  1.20.1, 1.21.x) are retired and archived in `Old/` (ignored via `.gitignore`).
- The launcher continues to fully support all Minecraft releases from `1.8.9` to `26.x`
  for **Vanilla**, **Fabric**, and **Forge**.
- No legacy/frozen stars or labels are displayed in the launcher; only active `26.x`
  versions receive the gold EzClient badge.

## 26.x artifact safety

- The `26.x` family shares one maintained source implementation but produces an
  exact JAR per Minecraft version. Never substitute one `26.x` JAR for another.
- Future product version updates apply to `26.x` only.
