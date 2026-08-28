package app.ezclient.gui;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules;
    private final List<HudModule> hudModules;
    private final ZoomModule zoomModule;
    private final FpsModule fpsModule;
    private final CpsModule cpsModule;
    private final PingModule pingModule;
    private final CoordinatesModule coordinatesModule;
    private final KeystrokesModule keystrokesModule;
    private final ArmorStatusModule armorStatusModule;
    private final ToggleSprintSneakModule toggleSprintSneakModule;
    private final FullbrightModule fullbrightModule;
    private final PotionEffectModule potionEffectModule;
    private final CrosshairModule crosshairModule;
    private final DayCounterModule dayCounterModule;
    private final ClearGlassModule clearGlassModule;

    private ModuleManager() {
        instance = this;
        this.modules = new ArrayList<>();
        
        // 10 Core Badlion-Style Modules + Utilities
        this.fpsModule = new FpsModule();
        this.cpsModule = new CpsModule();
        this.pingModule = new PingModule();
        this.coordinatesModule = new CoordinatesModule();
        this.keystrokesModule = new KeystrokesModule();
        this.armorStatusModule = new ArmorStatusModule();
        this.dayCounterModule = new DayCounterModule();
        this.zoomModule = new ZoomModule();
        this.toggleSprintSneakModule = new ToggleSprintSneakModule();
        this.fullbrightModule = new FullbrightModule();
        this.potionEffectModule = new PotionEffectModule();
        this.crosshairModule = new CrosshairModule();
        this.clearGlassModule = new ClearGlassModule();

        this.modules.add(this.fpsModule);
        this.modules.add(this.cpsModule);
        this.modules.add(this.pingModule);
        this.modules.add(this.coordinatesModule);
        this.modules.add(this.keystrokesModule);
        this.modules.add(this.armorStatusModule);
        this.modules.add(this.dayCounterModule);
        this.modules.add(this.zoomModule);
        this.modules.add(this.toggleSprintSneakModule);
        this.modules.add(this.fullbrightModule);
        this.modules.add(this.potionEffectModule);
        this.modules.add(this.crosshairModule);
        this.modules.add(this.clearGlassModule);

        // This collection is read every rendered frame. Build it once instead of
        // allocating a stream pipeline and a new list for every frame.
        this.hudModules = this.modules.stream()
                .filter(m -> m instanceof HudModule && !(m instanceof CrosshairModule))
                .map(HudModule.class::cast)
                .toList();
    }

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    public List<Module> getModules() {
        return modules;
    }

    public ZoomModule getZoomModule() { return zoomModule; }
    public FpsModule getFpsModule() { return fpsModule; }
    public CpsModule getCpsModule() { return cpsModule; }
    public PingModule getPingModule() { return pingModule; }
    public CoordinatesModule getCoordinatesModule() { return coordinatesModule; }
    public KeystrokesModule getKeystrokesModule() { return keystrokesModule; }
    public ArmorStatusModule getArmorStatusModule() { return armorStatusModule; }
    public DayCounterModule getDayCounterModule() { return dayCounterModule; }
    public ToggleSprintSneakModule getToggleSprintSneakModule() { return toggleSprintSneakModule; }
    public ToggleSprintSneakModule getAutoSprintModule() { return toggleSprintSneakModule; } // Alias
    public FullbrightModule getFullbrightModule() { return fullbrightModule; }
    public PotionEffectModule getPotionEffectModule() { return potionEffectModule; }
    public CrosshairModule getCrosshairModule() { return crosshairModule; }
    public ClearGlassModule getClearGlassModule() { return clearGlassModule; }

    public List<HudModule> getHudModules() {
        return hudModules;
    }
}
