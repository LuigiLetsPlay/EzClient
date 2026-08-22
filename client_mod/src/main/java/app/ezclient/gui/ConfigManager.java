package app.ezclient.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getConfigFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("ezclient.json").toFile();
    }

    public static void load() {
        try {
            File f = getConfigFile();
            if (f.exists()) {
                FileReader reader = new FileReader(f);
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                reader.close();

                if (json != null) {
                    ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
                    if (json.has("zoomEnabled")) {
                        zoom.setEnabled(json.get("zoomEnabled").getAsBoolean());
                    }
                    if (json.has("zoomLevel")) {
                        zoom.setZoomLevel(json.get("zoomLevel").getAsDouble());
                    }
                    if (json.has("zoomMin")) zoom.setMinZoom(json.get("zoomMin").getAsDouble());
                    if (json.has("zoomMax")) zoom.setMaxZoom(json.get("zoomMax").getAsDouble());
                    if (json.has("zoomScrollSensitivity")) zoom.setScrollSensitivity(json.get("zoomScrollSensitivity").getAsDouble());
                    if (json.has("zoomSmooth")) zoom.setSmoothZoom(json.get("zoomSmooth").getAsBoolean());
                    if (json.has("fpsEnabled")) ModuleManager.getInstance().getFpsModule().setEnabled(json.get("fpsEnabled").getAsBoolean());
                    if (json.has("pingEnabled")) ModuleManager.getInstance().getPingModule().setEnabled(json.get("pingEnabled").getAsBoolean());
                    if (json.has("fullbrightEnabled")) ModuleManager.getInstance().getFullbrightModule().setEnabled(json.get("fullbrightEnabled").getAsBoolean());
                    for (HudModule hud : ModuleManager.getInstance().getHudModules()) {
                        String key = "hud" + hud.getName();
                        if (!json.has(key) || !json.get(key).isJsonObject()) continue;
                        JsonObject h = json.getAsJsonObject(key);
                        if (h.has("enabled")) hud.setEnabled(h.get("enabled").getAsBoolean());
                        if (h.has("x") && h.has("y")) hud.setPosition(h.get("x").getAsInt(), h.get("y").getAsInt());
                        if (h.has("scale")) hud.setScale(h.get("scale").getAsDouble());
                        if (h.has("prefix")) {
                            String prefix = h.get("prefix").getAsString();
                            // Migrate the short-lived example default; user-defined text remains untouched.
                            if (hud.getName().equals("FPS") && prefix.equals("Frames: ")) prefix = "FPS: ";
                            hud.setPrefix(prefix);
                        }
                        if (h.has("suffix")) hud.setSuffix(h.get("suffix").getAsString());
                        if (h.has("rainbow")) hud.setRainbow(h.get("rainbow").getAsBoolean());
                        if (h.has("background")) hud.setBackground(h.get("background").getAsBoolean());
                        if (h.has("textColor")) hud.setTextColor(h.get("textColor").getAsInt());
                        if (h.has("backgroundColor")) hud.setBackgroundColor(h.get("backgroundColor").getAsInt());
                        if (h.has("borderColor")) hud.setBorderColor(h.get("borderColor").getAsInt());
                        if (h.has("border")) hud.setBorder(h.get("border").getAsBoolean());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            JsonObject json = new JsonObject();
            ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
            
            json.addProperty("zoomEnabled", zoom.isEnabled());
            json.addProperty("zoomLevel", zoom.getZoomLevel());
            json.addProperty("zoomMin", zoom.getMinZoom());
            json.addProperty("zoomMax", zoom.getMaxZoom());
            json.addProperty("zoomScrollSensitivity", zoom.getScrollSensitivity());
            json.addProperty("zoomSmooth", zoom.isSmoothZoom());
            json.addProperty("fpsEnabled", ModuleManager.getInstance().getFpsModule().isEnabled());
            json.addProperty("pingEnabled", ModuleManager.getInstance().getPingModule().isEnabled());
            json.addProperty("fullbrightEnabled", ModuleManager.getInstance().getFullbrightModule().isEnabled());
            for (HudModule hud : ModuleManager.getInstance().getHudModules()) {
                JsonObject h = new JsonObject();
                h.addProperty("enabled", hud.isEnabled());
                h.addProperty("x", hud.getX()); h.addProperty("y", hud.getY());
                h.addProperty("scale", hud.getScale());
                h.addProperty("prefix", hud.getPrefix()); h.addProperty("suffix", hud.getSuffix());
                h.addProperty("rainbow", hud.isRainbow()); h.addProperty("background", hud.hasBackground());
                h.addProperty("textColor", hud.getTextColor()); h.addProperty("backgroundColor", hud.getBackgroundColor());
                h.addProperty("borderColor", hud.getBorderColor()); h.addProperty("border", hud.hasBorder());
                json.add("hud" + hud.getName(), h);
            }

            FileWriter writer = new FileWriter(getConfigFile());
            GSON.toJson(json, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
