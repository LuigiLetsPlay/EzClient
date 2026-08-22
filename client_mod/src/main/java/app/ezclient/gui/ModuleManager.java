package app.ezclient.gui;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules;
    private final ZoomModule zoomModule;
    private final FpsModule fpsModule;
    private final PingModule pingModule;
    private final CoordinatesModule coordinatesModule;
    private final ClockModule clockModule;
    private final SpeedModule speedModule;
    private final FullbrightModule fullbrightModule;

    private ModuleManager() {
        this.modules = new ArrayList<>();
        
        // Initialize modules
        this.zoomModule = new ZoomModule();
        this.fpsModule = new FpsModule();
        this.pingModule = new PingModule();
        this.coordinatesModule = new CoordinatesModule();
        this.clockModule = new ClockModule();
        this.speedModule = new SpeedModule();
        this.fullbrightModule = new FullbrightModule();
        this.modules.add(this.zoomModule);
        this.modules.add(this.fpsModule);
        this.modules.add(this.pingModule);
        this.modules.add(this.coordinatesModule);
        this.modules.add(this.clockModule);
        this.modules.add(this.speedModule);
        this.modules.add(this.fullbrightModule);
        
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

    public ZoomModule getZoomModule() {
        return zoomModule;
    }

    public FpsModule getFpsModule() { return fpsModule; }
    public PingModule getPingModule() { return pingModule; }
    public FullbrightModule getFullbrightModule() { return fullbrightModule; }
    public java.util.List<HudModule> getHudModules() {
        return modules.stream().filter(HudModule.class::isInstance).map(HudModule.class::cast).toList();
    }
}
