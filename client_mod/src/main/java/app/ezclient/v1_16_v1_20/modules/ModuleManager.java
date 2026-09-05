package app.ezclient.v1_16_v1_20.modules;

import app.ezclient.shared.EzClientPaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<Module>();

    private final FpsModule fpsModule = new FpsModule();
    private final CpsModule cpsModule = new CpsModule();
    private final KeystrokesModule keystrokesModule = new KeystrokesModule();
    private final CoordinatesModule coordinatesModule = new CoordinatesModule();
    private final ArmorStatusModule armorStatusModule = new ArmorStatusModule();
    private final PingModule pingModule = new PingModule();
    private final PotionEffectModule potionEffectModule = new PotionEffectModule();
    private final ComboModule comboModule = new ComboModule();
    private final ReachModule reachModule = new ReachModule();
    private final DayCounterModule dayCounterModule = new DayCounterModule();
    private final ToggleSprintModule toggleSprintModule = new ToggleSprintModule();
    private final FullbrightModule fullbrightModule = new FullbrightModule();
    private final ZoomModule zoomModule = new ZoomModule();
    private final CrosshairModule crosshairModule = new CrosshairModule();
    private final ClearGlassModule clearGlassModule = new ClearGlassModule();
    private final ClockModule clockModule = new ClockModule();
    private final MemoryModule memoryModule = new MemoryModule();

    private ModuleManager() {
        modules.add(fpsModule);
        modules.add(cpsModule);
        modules.add(keystrokesModule);
        modules.add(coordinatesModule);
        modules.add(armorStatusModule);
        modules.add(pingModule);
        modules.add(potionEffectModule);
        modules.add(comboModule);
        modules.add(reachModule);
        modules.add(dayCounterModule);
        modules.add(toggleSprintModule);
        modules.add(fullbrightModule);
        modules.add(zoomModule);
        modules.add(crosshairModule);
        modules.add(clearGlassModule);
        modules.add(clockModule);
        modules.add(memoryModule);
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<Module> getHudModules() {
        List<Module> hud = new ArrayList<Module>();
        for (Module m : modules) {
            if (m.hasHudElement()) {
                hud.add(m);
            }
        }
        return hud;
    }

    public ZoomModule getZoomModule() {
        return zoomModule;
    }

    public CrosshairModule getCrosshairModule() {
        return crosshairModule;
    }

    public FullbrightModule getFullbrightModule() {
        return fullbrightModule;
    }

    public void onTick() {
        for (Module m : modules) {
            m.onTick();
        }
    }

    public void renderHud(float tickDelta) {
        for (Module m : modules) {
            if (m.isEnabled() && m.isShowHud()) {
                m.renderHud(tickDelta);
            }
        }
    }

    public void saveConfig() {
        try {
            File dir = new File(EzClientPaths.dataDirectory().toFile(), "config");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "compat_modules.json");

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                sb.append("  \"").append(m.getId()).append("\": {\n");
                sb.append("    \"enabled\": ").append(m.isEnabled()).append(",\n");
                sb.append("    \"showHud\": ").append(m.isShowHud()).append(",\n");
                sb.append("    \"colorMode\": ").append(m.getColorMode()).append(",\n");
                sb.append("    \"showBackground\": ").append(m.isShowBackground()).append(",\n");
                sb.append("    \"posX\": ").append(m.getPosX()).append(",\n");
                sb.append("    \"posY\": ").append(m.getPosY()).append("\n");
                sb.append("  }").append(i < modules.size() - 1 ? "," : "").append("\n");
            }
            sb.append("}\n");

            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(sb.toString());
            writer.close();
        } catch (Throwable t) {
            System.err.println("[EzClient] Failed to save compat modules config: " + t.getMessage());
        }
    }

    public void loadConfig() {
        try {
            File dir = new File(EzClientPaths.dataDirectory().toFile(), "config");
            File file = new File(dir, "compat_modules.json");
            if (!file.exists()) return;

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();

            String json = sb.toString();
            for (Module m : modules) {
                String key = "\"" + m.getId() + "\"";
                int idx = json.indexOf(key);
                if (idx != -1) {
                    int blockEnd = json.indexOf("}", idx);
                    if (blockEnd != -1) {
                        String block = json.substring(idx, blockEnd);
                        if (block.contains("\"enabled\": false") || block.contains("\"enabled\":false")) {
                            m.setEnabled(false);
                        } else if (block.contains("\"enabled\": true") || block.contains("\"enabled\":true")) {
                            m.setEnabled(true);
                        }

                        if (block.contains("\"showHud\": false") || block.contains("\"showHud\":false")) {
                            m.setShowHud(false);
                        } else if (block.contains("\"showHud\": true") || block.contains("\"showHud\":true")) {
                            m.setShowHud(true);
                        }

                        if (block.contains("\"showBackground\": false") || block.contains("\"showBackground\":false")) {
                            m.setShowBackground(false);
                        } else if (block.contains("\"showBackground\": true") || block.contains("\"showBackground\":true")) {
                            m.setShowBackground(true);
                        }

                        if (block.contains("\"colorMode\":")) {
                            try {
                                int cIdx = block.indexOf("\"colorMode\":");
                                int comma = block.indexOf(",", cIdx);
                                if (comma == -1) comma = block.indexOf("\n", cIdx);
                                String num = block.substring(cIdx + 12, comma).trim();
                                m.setColorMode(Integer.parseInt(num));
                            } catch (Throwable ignored) {}
                        }

                        if (block.contains("\"posX\":")) {
                            try {
                                int pIdx = block.indexOf("\"posX\":");
                                int comma = block.indexOf(",", pIdx);
                                if (comma != -1) {
                                    String num = block.substring(pIdx + 7, comma).trim();
                                    m.setPosX(Integer.parseInt(num));
                                }
                            } catch (Throwable ignored) {}
                        }

                        if (block.contains("\"posY\":")) {
                            try {
                                int pIdx = block.indexOf("\"posY\":");
                                int comma = block.indexOf("\n", pIdx);
                                if (comma == -1) comma = block.length();
                                String num = block.substring(pIdx + 7, comma).replace("}", "").trim();
                                m.setPosY(Integer.parseInt(num));
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[EzClient] Failed to load compat modules config: " + t.getMessage());
        }
    }
}
