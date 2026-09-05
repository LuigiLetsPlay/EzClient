# EzClient multi-version build

The mod has eight real Stonecutter build targets:

- `1.8.9` and `1.12.2`: Legacy Fabric, Java 8, legacy dashboard/HUD/modules/capes.
- `1.16.5`, `1.20.1`, and `1.21.1`: mapped Fabric adapters with the modern dashboard/HUD/modules/capes.
- `26.1`, `26.1.1`, and `26.2`: Java 25 full builds using their respective Minecraft/Fabric APIs.

The 1.21.1 adapter declares and supplies the complete supported 1.21–1.21.11 range.
Never copy or rename the 26.2 JAR to another Minecraft line: its bytecode, mixins,
Minecraft constraint, and bundled Fabric API are specific to 26.2.

## Build

```powershell
python build_mod.py
```

The jars are written to `versions/<minecraft-version>/build/libs/`.

Use the Gradle tasks named `Set active project to ...` to switch the source tree
used by the IDE. `stonecutter.gradle` keeps 26.2 as the version-control target.

## Source layout

- `src/main/java`: complete 26.x implementation and platform-specific adapter packages.
- `src/main/java/app/ezclient/shared`: Java-8-compatible, Minecraft-independent core used by every build family. `ZoomState` owns zoom limits/reset behavior; `ClickRateTracker` owns the CPS time window.
- `src/main/java/app/ezclient/v1_8`: 1.8.9/1.12.2 implementation.
- `src/main/java/app/ezclient/v1_16_v1_20`: 1.16.5–1.21.x implementation.
- `src/v1_8/resources`, `src/v1_16_v1_20/resources`, and `src/v26_1/resources`: version-specific metadata and mixin sets.

`CrossVersionHudExample.java` shows the Stonecutter conditional-comment syntax for
modern `GuiGraphicsExtractor` rendering and the 1.8.9 fixed-function OpenGL path.
Large platform-specific systems should stay in their platform package instead of
placing hundreds of conditional lines into a single class.

## Update rule

Implement new feature work in the active `26.x` implementation. Do not change
`app.ezclient.shared` during normal development because a later reproduction of
the frozen builds would inherit that change. Rendering, mixins, mappings, key
APIs, and screen APIs remain version-specific adapter code. After a 26.x change,
run `python build_mod.py`; it rebuilds and publishes only the
actively maintained 26.x targets. Frozen 2.0.0 targets require the explicit
`--frozen` or `--all` maintenance flag and must not be part of normal releases.

`tests/test_shared_mod_state.py` is the cross-version behavior gate for shared
state. Add a regression there whenever shared behavior changes.

## Runtime rule

Compilation is only the first gate. Test one representative target from Legacy,
mapped Fabric, and 26.x. In particular, 1.8.9 must run on Java 8 and must not place
Mojang's nightly LWJGL beside Legacy Fabric's patched LWJGL on the classpath.
