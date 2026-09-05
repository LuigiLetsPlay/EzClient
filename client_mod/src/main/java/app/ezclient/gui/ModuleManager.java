package app.ezclient.gui;

import java.util.ArrayList;
import java.util.List;

/** Central registry and lifecycle manager for all client modules. */
public final class ModuleManager {
    private static ModuleManager instance;
    private boolean initialized;
    public static boolean isInitialized() { return instance != null && instance.initialized; }

    private final List<Module> modules;
    private final List<HudModule> hudModules;

    private final FpsModule fpsModule;
    private final CpsModule cpsModule;
    private final PingModule pingModule;
    private final CoordinatesModule coordinatesModule;
    private final ZoomModule zoomModule;
    private final KeystrokesModule keystrokesModule;
    private final ArmorStatusModule armorStatusModule;
    private final ToggleSprintSneakModule toggleSprintSneakModule;
    private final FullbrightModule fullbrightModule;
    private final PotionEffectModule potionEffectModule;
    private final CrosshairModule crosshairModule;
    private final DayCounterModule dayCounterModule;
    private final ClearGlassModule clearGlassModule;
    private final ClockModule clockModule;
    private final MemoryModule memoryModule;

    // Badlion Modules 11 to 20
    private final ReachModule reachModule;
    private final ComboCounterModule comboCounterModule;
    private final ScoreboardModule scoreboardModule;
    private final FovChangerModule fovChangerModule;
    private final DamageTintModule damageTintModule;
    private final MotionBlurModule motionBlurModule;
    private final ChatCustomizerModule chatCustomizerModule;
    private final TntTimerModule tntTimerModule;
    private final AutoGgModule autoGgModule;

    private ModuleManager() {
        instance = this;
        this.modules = new ArrayList<>();

        // Core Modules 1 to 10 + Utilities
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
        this.clockModule = new ClockModule();
        this.memoryModule = new MemoryModule();

        // Badlion Modules 11 to 20
        this.reachModule = new ReachModule();
        this.comboCounterModule = new ComboCounterModule();
        this.scoreboardModule = new ScoreboardModule();
        this.fovChangerModule = new FovChangerModule();
        this.damageTintModule = new DamageTintModule();
        this.motionBlurModule = new MotionBlurModule();
        this.chatCustomizerModule = new ChatCustomizerModule();
        this.tntTimerModule = new TntTimerModule();
        this.autoGgModule = new AutoGgModule();

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
        this.modules.add(this.clockModule);
        this.modules.add(this.memoryModule);

        this.modules.add(this.reachModule);
        this.modules.add(this.comboCounterModule);
        this.modules.add(this.scoreboardModule);
        this.modules.add(this.fovChangerModule);
        this.modules.add(this.damageTintModule);
        this.modules.add(this.motionBlurModule);
        this.modules.add(this.chatCustomizerModule);
        this.modules.add(this.tntTimerModule);
        this.modules.add(this.autoGgModule);

        // HUD modules collection cached for rendering loop
        this.modules.addAll(List.of(new HitboxModule(), new ItemPhysicsModule(), new TimeWeatherModule(),
                new ParticleCustomizerModule(), new BlockOverlayModule(), new BossBarModule(), new BedwarsModule(),
                new NameplateModule(), new WaypointsModule(), new SoundEnhancerModule()));
        this.hudModules = this.modules.stream()
                .filter(m -> m instanceof HudModule && !(m instanceof CrosshairModule))
                .filter(m -> !(m instanceof FeatureModule feature) || feature.hasHud())
                .map(HudModule.class::cast)
                .toList();
        initialized = true;
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
    public ClockModule getClockModule() { return clockModule; }
    public MemoryModule getMemoryModule() { return memoryModule; }

    public ReachModule getReachModule() { return reachModule; }
    public ComboCounterModule getComboCounterModule() { return comboCounterModule; }
    public ScoreboardModule getScoreboardModule() { return scoreboardModule; }
    public FovChangerModule getFovChangerModule() { return fovChangerModule; }
    public DamageTintModule getDamageTintModule() { return damageTintModule; }
    public MotionBlurModule getMotionBlurModule() { return motionBlurModule; }
    public ChatCustomizerModule getChatCustomizerModule() { return chatCustomizerModule; }
    public TntTimerModule getTntTimerModule() { return tntTimerModule; }
    public AutoGgModule getAutoGgModule() { return autoGgModule; }

    public List<HudModule> getHudModules() {
        return hudModules;
    }
}
