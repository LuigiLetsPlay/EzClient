package app.ezclient.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static int bossbarX = -1, bossbarY = -1;
    public static double bossbarScale = 1.0;
    public static int scoreboardX = -1, scoreboardY = -1;
    public static double scoreboardScale = 1.0;
    public static int chatX = -1, chatY = -1;
    public static double chatScale = 1.0;
    public static int effectsX = -1, effectsY = -1;
    public static double effectsScale = 1.0;
    public static boolean customVanillaHud = true;

    public static void resetAllLayout(int screenWidth, int screenHeight) {
        bossbarX = (screenWidth - 182) / 2; bossbarY = 12; bossbarScale = 1.0;
        scoreboardX = screenWidth - 116; scoreboardY = Math.max(0, screenHeight / 2 - 45); scoreboardScale = 1.0;
        chatX = 4; chatY = Math.max(0, screenHeight - 60); chatScale = 1.0;
        effectsX = screenWidth - 64; effectsY = 12; effectsScale = 1.0;

        int leftX = 10;
        int currentY = 10;

        FpsModule fps = ModuleManager.getInstance().getFpsModule();
        if (fps != null) {
            fps.setPosition(leftX, currentY);
            fps.setScale(1.0);
            currentY += 18;
        }

        CpsModule cps = ModuleManager.getInstance().getCpsModule();
        if (cps != null) {
            cps.setPosition(leftX, currentY);
            cps.setScale(1.0);
            currentY += 18;
        }

        PingModule ping = ModuleManager.getInstance().getPingModule();
        if (ping != null) {
            ping.setPosition(leftX, currentY);
            ping.setScale(1.0);
            currentY += 18;
        }

        CoordinatesModule coords = ModuleManager.getInstance().getCoordinatesModule();
        if (coords != null) {
            coords.setPosition(leftX, currentY);
            coords.setScale(1.0);
            currentY += 36;
        }

        ToggleSprintSneakModule ts = ModuleManager.getInstance().getToggleSprintSneakModule();
        if (ts != null) {
            ts.setPosition(leftX, currentY);
            ts.setScale(1.0);
        }

        KeystrokesModule ks = ModuleManager.getInstance().getKeystrokesModule();
        if (ks != null) {
            ks.setPosition(Math.max(0, screenWidth - 84), Math.max(0, screenHeight - 96));
            ks.setScale(1.0);
        }

        ArmorStatusModule armor = ModuleManager.getInstance().getArmorStatusModule();
        if (armor != null) {
            armor.setPosition(leftX, Math.max(0, screenHeight - 90));
            armor.setScale(1.0);
        }

        save();
    }

    private static boolean isSaving = false;
    private static boolean isLoading = false;

    public static boolean isLoading() {
        return isLoading;
    }

    private static File getConfigFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("ezclient.json").toFile();
    }

    public static void load() {
        if (isLoading) return;
        isLoading = true;
        try {
            File f = getConfigFile();
            if (f.exists()) {
                JsonObject json;
                try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
                    json = GSON.fromJson(reader, JsonObject.class);
                }

                if (json != null) {
                    ZoomModule zoom = ModuleManager.getInstance().getZoomModule();
                    if (json.has("zoomEnabled")) zoom.setEnabled(json.get("zoomEnabled").getAsBoolean());
                    if (json.has("zoomLevel")) zoom.setZoomLevel(json.get("zoomLevel").getAsDouble());
                    if (json.has("zoomMin")) zoom.setMinZoom(json.get("zoomMin").getAsDouble());
                    if (json.has("zoomMax")) zoom.setMaxZoom(json.get("zoomMax").getAsDouble());
                    if (json.has("zoomScrollSensitivity")) zoom.setScrollSensitivity(json.get("zoomScrollSensitivity").getAsDouble());
                    if (json.has("zoomSmooth")) zoom.setSmoothZoom(json.get("zoomSmooth").getAsBoolean());
                    if (json.has("zoomSensScaling")) zoom.setMouseSensitivityScaling(json.get("zoomSensScaling").getAsBoolean());
                    if (json.has("zoomCinematic")) zoom.setCinematicCamera(json.get("zoomCinematic").getAsBoolean());

                    FpsModule fps = ModuleManager.getInstance().getFpsModule();
                    if (json.has("fpsEnabled")) fps.setEnabled(json.get("fpsEnabled").getAsBoolean());
                    if (json.has("fpsFormatOption")) {
                        try { fps.setFormatOption(FpsModule.FormatOption.valueOf(json.get("fpsFormatOption").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("fpsUpdateInterval")) fps.setUpdateIntervalMs(json.get("fpsUpdateInterval").getAsInt());
                    if (json.has("fpsColorCoding")) fps.setColorCoding(json.get("fpsColorCoding").getAsBoolean());
                    if (json.has("fpsShowMinMax")) fps.setShowMinMax(json.get("fpsShowMinMax").getAsBoolean());

                    CpsModule cps = ModuleManager.getInstance().getCpsModule();
                    if (json.has("cpsEnabled")) cps.setEnabled(json.get("cpsEnabled").getAsBoolean());
                    if (json.has("cpsDisplayMode")) {
                        try { cps.setDisplayMode(CpsModule.DisplayMode.valueOf(json.get("cpsDisplayMode").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("cpsDynamicColor")) cps.setDynamicColor(json.get("cpsDynamicColor").getAsBoolean());
                    if (json.has("cpsShowHistoryGraph")) cps.setShowHistoryGraph(json.get("cpsShowHistoryGraph").getAsBoolean());

                    if (json.has("pingEnabled")) ModuleManager.getInstance().getPingModule().setEnabled(json.get("pingEnabled").getAsBoolean());

                    KeystrokesModule ks = ModuleManager.getInstance().getKeystrokesModule();
                    if (json.has("keystrokesLayoutPreset")) {
                        try { ks.setLayoutPreset(KeystrokesModule.LayoutPreset.valueOf(json.get("keystrokesLayoutPreset").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("keystrokesSpaceStyle")) {
                        try { ks.setSpaceStyle(KeystrokesModule.SpaceStyle.valueOf(json.get("keystrokesSpaceStyle").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("keystrokesFadeTimeMs")) ks.setFadeTimeMs(json.get("keystrokesFadeTimeMs").getAsInt());
                    if (json.has("keystrokesNormalBoxColor")) ks.setNormalBoxColor(json.get("keystrokesNormalBoxColor").getAsInt());
                    if (json.has("keystrokesPressedBoxColor")) ks.setPressedBoxColor(json.get("keystrokesPressedBoxColor").getAsInt());
                    if (json.has("keystrokesKeyTextColor")) ks.setKeyTextColor(json.get("keystrokesKeyTextColor").getAsInt());
                    if (json.has("keystrokesPressedTextColor")) ks.setPressedTextColor(json.get("keystrokesPressedTextColor").getAsInt());
                    if (json.has("keystrokesShowMouseCps")) ks.setShowMouseCps(json.get("keystrokesShowMouseCps").getAsBoolean());

                    CoordinatesModule coords = ModuleManager.getInstance().getCoordinatesModule();
                    if (json.has("coordinatesLayoutMode")) {
                        try { coords.setLayoutMode(CoordinatesModule.LayoutMode.valueOf(json.get("coordinatesLayoutMode").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("coordinatesDecimalPrecision")) coords.setDecimalPrecision(json.get("coordinatesDecimalPrecision").getAsInt());
                    if (json.has("coordinatesShowBiome")) coords.setShowBiome(json.get("coordinatesShowBiome").getAsBoolean());
                    if (json.has("coordinatesShowDirection")) coords.setShowDirection(json.get("coordinatesShowDirection").getAsBoolean());
                    if (json.has("coordinatesShowNether")) coords.setShowNether(json.get("coordinatesShowNether").getAsBoolean());

                    ArmorStatusModule armor = ModuleManager.getInstance().getArmorStatusModule();
                    if (json.has("armorStatusHorizontal")) armor.setHorizontal(json.get("armorStatusHorizontal").getAsBoolean());
                    if (json.has("armorStatusDurabilityMode")) {
                        try { armor.setDurabilityMode(ArmorStatusModule.DurabilityMode.valueOf(json.get("armorStatusDurabilityMode").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("armorStatusColorTiers")) armor.setColorTiers(json.get("armorStatusColorTiers").getAsBoolean());
                    if (json.has("armorStatusDamageWarning")) armor.setDamageWarning(json.get("armorStatusDamageWarning").getAsBoolean());
                    if (json.has("armorStatusShowItemCount")) armor.setShowItemCount(json.get("armorStatusShowItemCount").getAsBoolean());
                    if (json.has("armorStatusShowHands")) armor.setShowHands(json.get("armorStatusShowHands").getAsBoolean());

                    PotionEffectModule potion = ModuleManager.getInstance().getPotionEffectModule();
                    if (json.has("potionDisplayStyle")) {
                        try { potion.setDisplayStyle(PotionEffectModule.DisplayStyle.valueOf(json.get("potionDisplayStyle").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("potionSortOrder")) {
                        try { potion.setSortOrder(PotionEffectModule.SortOrder.valueOf(json.get("potionSortOrder").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("potionVertical")) potion.setVertical(json.get("potionVertical").getAsBoolean());
                    if (json.has("potionShowTime")) potion.setShowTime(json.get("potionShowTime").getAsBoolean());
                    if (json.has("potionBlinkWarning")) potion.setBlinkWarningSeconds(json.get("potionBlinkWarning").getAsInt());
                    if (json.has("potionUseCustomColors")) potion.setUseCustomColors(json.get("potionUseCustomColors").getAsBoolean());

                    CrosshairModule crosshair = ModuleManager.getInstance().getCrosshairModule();
                    if (json.has("crosshairEnabled")) crosshair.setEnabled(json.get("crosshairEnabled").getAsBoolean());
                    if (json.has("crosshairType")) {
                        try { crosshair.setCrosshairType(CrosshairModule.CrosshairType.valueOf(json.get("crosshairType").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("crosshairGap")) crosshair.setGap(json.get("crosshairGap").getAsInt());
                    if (json.has("crosshairSize")) crosshair.setSize(json.get("crosshairSize").getAsInt());
                    if (json.has("crosshairThickness")) crosshair.setThickness(json.get("crosshairThickness").getAsInt());
                    if (json.has("crosshairVerticalSize")) crosshair.setVerticalSize(json.get("crosshairVerticalSize").getAsInt());
                    if (json.has("crosshairDotSize")) crosshair.setDotSize(json.get("crosshairDotSize").getAsInt());
                    if (json.has("crosshairOpacity")) crosshair.setOpacity(json.get("crosshairOpacity").getAsInt());
                    if (json.has("crosshairShowDot")) crosshair.setShowDot(json.get("crosshairShowDot").getAsBoolean());
                    if (json.has("crosshairShowOutline")) crosshair.setShowOutline(json.get("crosshairShowOutline").getAsBoolean());
                    if (json.has("crosshairOutlineColor")) crosshair.setOutlineColor(json.get("crosshairOutlineColor").getAsInt());
                    if (json.has("crosshairDynamicSpread")) crosshair.setDynamicSpread(json.get("crosshairDynamicSpread").getAsBoolean());
                    if (json.has("crosshairTargetMode")) {
                        try { crosshair.setTargetMode(CrosshairModule.TargetMode.valueOf(json.get("crosshairTargetMode").getAsString())); } catch (Exception ignored) {}
                    } else if (json.has("crosshairTargetHighlight")) {
                        crosshair.setTargetHighlight(json.get("crosshairTargetHighlight").getAsBoolean());
                    }
                    if (json.has("crosshairTargetColor")) crosshair.setTargetEntityColor(json.get("crosshairTargetColor").getAsInt());
                    if (json.has("crosshairTargetEntityColor")) crosshair.setTargetEntityColor(json.get("crosshairTargetEntityColor").getAsInt());
                    if (json.has("crosshairTargetPlayerColor")) crosshair.setTargetPlayerColor(json.get("crosshairTargetPlayerColor").getAsInt());
                    if (json.has("crosshairTargetHostileColor")) crosshair.setTargetHostileColor(json.get("crosshairTargetHostileColor").getAsInt());
                    if (json.has("crosshairTargetNeutralColor")) crosshair.setTargetNeutralColor(json.get("crosshairTargetNeutralColor").getAsInt());
                    if (json.has("crosshairTargetBlockColor")) crosshair.setTargetBlockColor(json.get("crosshairTargetBlockColor").getAsInt());
                    if (json.has("crosshairTargetEntityScale")) crosshair.setTargetEntityScale(json.get("crosshairTargetEntityScale").getAsFloat());
                    if (json.has("crosshairTargetPlayerScale")) crosshair.setTargetPlayerScale(json.get("crosshairTargetPlayerScale").getAsFloat());
                    if (json.has("crosshairTargetHostileScale")) crosshair.setTargetHostileScale(json.get("crosshairTargetHostileScale").getAsFloat());
                    if (json.has("crosshairTargetNeutralScale")) crosshair.setTargetNeutralScale(json.get("crosshairTargetNeutralScale").getAsFloat());
                    if (json.has("crosshairMovementSpread")) crosshair.setMovementSpread(json.get("crosshairMovementSpread").getAsBoolean());
                    if (json.has("crosshairJumpSpread")) crosshair.setJumpSpread(json.get("crosshairJumpSpread").getAsBoolean());
                    if (json.has("crosshairCooldownSpread")) crosshair.setCooldownSpread(json.get("crosshairCooldownSpread").getAsBoolean());
                    if (json.has("crosshairHideBow")) crosshair.setHideOnBowZoom(json.get("crosshairHideBow").getAsBoolean());
                    if (json.has("crosshairHideF3")) crosshair.setHideInF3(json.get("crosshairHideF3").getAsBoolean());
                    if (json.has("crosshairHideThirdPerson")) crosshair.setHideInThirdPerson(json.get("crosshairHideThirdPerson").getAsBoolean());

                    ToggleSprintSneakModule toggleSprint = ModuleManager.getInstance().getToggleSprintSneakModule();
                    if (json.has("toggleSprintEnabled")) toggleSprint.setEnabled(json.get("toggleSprintEnabled").getAsBoolean());
                    if (json.has("toggleSprintMode")) {
                        try { toggleSprint.setSprintMode(ToggleSprintSneakModule.SprintMode.valueOf(json.get("toggleSprintMode").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("toggleSneakMode")) {
                        try { toggleSprint.setSneakMode(ToggleSprintSneakModule.SneakMode.valueOf(json.get("toggleSneakMode").getAsString())); } catch (Exception ignored) {}
                    }
                    if (json.has("toggleSprintHideInactive")) toggleSprint.setHideWhenInactive(json.get("toggleSprintHideInactive").getAsBoolean());
                    if (json.has("toggleSprintFlyBoost")) toggleSprint.setFlyBoostMultiplier(json.get("toggleSprintFlyBoost").getAsFloat());

                    FullbrightModule fullbright = ModuleManager.getInstance().getFullbrightModule();
                    if (json.has("fullbrightEnabled")) fullbright.setEnabled(json.get("fullbrightEnabled").getAsBoolean());
                    if (json.has("fullbrightBrightness")) fullbright.setBrightnessLevel(json.get("fullbrightBrightness").getAsInt());
                    if (json.has("fullbrightSmoothFade")) fullbright.setSmoothFade(json.get("fullbrightSmoothFade").getAsBoolean());
                    if (json.has("fullbrightDisableNether")) fullbright.setDisableInNether(json.get("fullbrightDisableNether").getAsBoolean());
                    if (json.has("fullbrightDisableEnd")) fullbright.setDisableInEnd(json.get("fullbrightDisableEnd").getAsBoolean());

                    ClearGlassModule clearGlass = ModuleManager.getInstance().getClearGlassModule();
                    if (json.has("clearGlassEnabled")) clearGlass.setEnabled(json.get("clearGlassEnabled").getAsBoolean());
                    if (json.has("clearGlassConnected")) clearGlass.setConnectedGlass(json.get("clearGlassConnected").getAsBoolean());

                    DayCounterModule dayCounter = ModuleManager.getInstance().getDayCounterModule();
                    if (json.has("dayCounterShowDay")) dayCounter.setShowDay(json.get("dayCounterShowDay").getAsBoolean());
                    if (json.has("dayCounterShowPlaytime")) dayCounter.setShowPlaytime(json.get("dayCounterShowPlaytime").getAsBoolean());

                    if (json.has("bossbarX")) bossbarX = json.get("bossbarX").getAsInt();
                    if (json.has("bossbarY")) bossbarY = json.get("bossbarY").getAsInt();
                    if (json.has("bossbarScale")) bossbarScale = json.get("bossbarScale").getAsDouble();
                    if (json.has("scoreboardX")) scoreboardX = json.get("scoreboardX").getAsInt();
                    if (json.has("scoreboardY")) scoreboardY = json.get("scoreboardY").getAsInt();
                    if (json.has("scoreboardScale")) scoreboardScale = json.get("scoreboardScale").getAsDouble();
                    if (json.has("chatX")) chatX = json.get("chatX").getAsInt();
                    if (json.has("chatY")) chatY = json.get("chatY").getAsInt();
                    if (json.has("chatScale")) chatScale = json.get("chatScale").getAsDouble();

                    if (json.has("effectsX")) effectsX = json.get("effectsX").getAsInt();
                    if (json.has("effectsY")) effectsY = json.get("effectsY").getAsInt();
                    if (json.has("effectsScale")) effectsScale = json.get("effectsScale").getAsDouble();
                    if (json.has("customVanillaHud")) customVanillaHud = json.get("customVanillaHud").getAsBoolean();

                    for (HudModule hud : ModuleManager.getInstance().getHudModules()) {
                        String key = "hud" + hud.getName();
                        if (!json.has(key) || !json.get(key).isJsonObject()) continue;
                        JsonObject h = json.getAsJsonObject(key);
                        if (h.has("enabled")) hud.setEnabled(h.get("enabled").getAsBoolean());
                        if (h.has("x") && h.has("y")) hud.setPosition(h.get("x").getAsInt(), h.get("y").getAsInt());
                        if (h.has("scale")) hud.setScale(h.get("scale").getAsDouble());
                        if (h.has("prefix")) hud.setPrefix(h.get("prefix").getAsString());
                        if (h.has("suffix")) hud.setSuffix(h.get("suffix").getAsString());
                        if (h.has("colorMode")) {
                            try { hud.setColorMode(HudModule.ColorMode.valueOf(h.get("colorMode").getAsString())); } catch (Exception ignored) {}
                        } else if (h.has("rainbow") && h.get("rainbow").getAsBoolean()) {
                            hud.setColorMode(HudModule.ColorMode.RAINBOW);
                        }
                        if (h.has("background")) hud.setBackground(h.get("background").getAsBoolean());
                        if (h.has("textColor")) hud.setTextColor(h.get("textColor").getAsInt());
                        if (h.has("waveColor2")) hud.setWaveColor2(h.get("waveColor2").getAsInt());
                        if (h.has("backgroundColor")) hud.setBackgroundColor(h.get("backgroundColor").getAsInt());
                        if (h.has("borderColor")) hud.setBorderColor(h.get("borderColor").getAsInt());
                        if (h.has("border")) hud.setBorder(h.get("border").getAsBoolean());
                        if (h.has("textShadow")) hud.setTextShadow(h.get("textShadow").getAsBoolean());
                        if (h.has("customFont")) hud.setCustomFont(h.get("customFont").getAsBoolean());
                        if (h.has("cornerRadius")) hud.setCornerRadius(h.get("cornerRadius").getAsInt());
                        if (h.has("borderWidth")) hud.setBorderWidth(h.get("borderWidth").getAsInt());
                        if (h.has("rainbowSpeed")) hud.setRainbowSpeed(h.get("rainbowSpeed").getAsFloat());
                        if (h.has("rainbowSaturation")) hud.setRainbowSaturation(h.get("rainbowSaturation").getAsFloat());
                        if (h.has("rainbowBorder")) hud.setRainbowBorder(h.get("rainbowBorder").getAsBoolean());
                    }

                    for (Module m : ModuleManager.getInstance().getModules()) {
                        String key = "keybind_" + m.getName();
                        if (json.has(key)) {
                            m.setKeyBind(json.get(key).getAsInt());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isLoading = false;
        }
    }

    public static void save() {
        if (isSaving || isLoading) return;
        isSaving = true;
        try {
            JsonObject json = new JsonObject();
            ModuleManager mm = ModuleManager.getInstance();
            if (mm == null) return;

            ZoomModule zoom = mm.getZoomModule();
            json.addProperty("zoomEnabled", zoom.isEnabled());
            json.addProperty("zoomLevel", zoom.getZoomLevel());
            json.addProperty("zoomMin", zoom.getMinZoom());
            json.addProperty("zoomMax", zoom.getMaxZoom());
            json.addProperty("zoomScrollSensitivity", zoom.getScrollSensitivity());
            json.addProperty("zoomSmooth", zoom.isSmoothZoom());
            json.addProperty("zoomSensScaling", zoom.isMouseSensitivityScaling());
            json.addProperty("zoomCinematic", zoom.isCinematicCamera());

            FpsModule fps = ModuleManager.getInstance().getFpsModule();
            json.addProperty("fpsEnabled", fps.isEnabled());
            json.addProperty("fpsFormatOption", fps.getFormatOption().name());
            json.addProperty("fpsUpdateInterval", fps.getUpdateIntervalMs());
            json.addProperty("fpsColorCoding", fps.isColorCoding());
            json.addProperty("fpsShowMinMax", fps.isShowMinMax());

            CpsModule cps = ModuleManager.getInstance().getCpsModule();
            json.addProperty("cpsEnabled", cps.isEnabled());
            json.addProperty("cpsDisplayMode", cps.getDisplayMode().name());
            json.addProperty("cpsDynamicColor", cps.isDynamicColor());
            json.addProperty("cpsShowHistoryGraph", cps.isShowHistoryGraph());

            json.addProperty("pingEnabled", ModuleManager.getInstance().getPingModule().isEnabled());

            KeystrokesModule ks = ModuleManager.getInstance().getKeystrokesModule();
            json.addProperty("keystrokesLayoutPreset", ks.getLayoutPreset().name());
            json.addProperty("keystrokesSpaceStyle", ks.getSpaceStyle().name());
            json.addProperty("keystrokesFadeTimeMs", ks.getFadeTimeMs());
            json.addProperty("keystrokesNormalBoxColor", ks.getNormalBoxColor());
            json.addProperty("keystrokesPressedBoxColor", ks.getPressedBoxColor());
            json.addProperty("keystrokesKeyTextColor", ks.getKeyTextColor());
            json.addProperty("keystrokesPressedTextColor", ks.getPressedTextColor());
            json.addProperty("keystrokesShowMouseCps", ks.isShowMouseCps());

            CoordinatesModule coords = ModuleManager.getInstance().getCoordinatesModule();
            json.addProperty("coordinatesLayoutMode", coords.getLayoutMode().name());
            json.addProperty("coordinatesDecimalPrecision", coords.getDecimalPrecision());
            json.addProperty("coordinatesShowBiome", coords.isShowBiome());
            json.addProperty("coordinatesShowDirection", coords.isShowDirection());
            json.addProperty("coordinatesShowNether", coords.isShowNether());

            ArmorStatusModule armor = ModuleManager.getInstance().getArmorStatusModule();
            json.addProperty("armorStatusHorizontal", armor.isHorizontal());
            json.addProperty("armorStatusDurabilityMode", armor.getDurabilityMode().name());
            json.addProperty("armorStatusColorTiers", armor.isColorTiers());
            json.addProperty("armorStatusDamageWarning", armor.isDamageWarning());
            json.addProperty("armorStatusShowItemCount", armor.isShowItemCount());
            json.addProperty("armorStatusShowHands", armor.isShowHands());

            PotionEffectModule potion = ModuleManager.getInstance().getPotionEffectModule();
            json.addProperty("potionDisplayStyle", potion.getDisplayStyle().name());
            json.addProperty("potionSortOrder", potion.getSortOrder().name());
            json.addProperty("potionVertical", potion.isVertical());
            json.addProperty("potionShowTime", potion.isShowTime());
            json.addProperty("potionBlinkWarning", potion.getBlinkWarningSeconds());
            json.addProperty("potionUseCustomColors", potion.isUseCustomColors());

            CrosshairModule crosshair = ModuleManager.getInstance().getCrosshairModule();
            json.addProperty("crosshairEnabled", crosshair.isEnabled());
            json.addProperty("crosshairType", crosshair.getCrosshairType().name());
            json.addProperty("crosshairGap", crosshair.getGap());
            json.addProperty("crosshairSize", crosshair.getSize());
            json.addProperty("crosshairThickness", crosshair.getThickness());
            json.addProperty("crosshairVerticalSize", crosshair.getVerticalSize());
            json.addProperty("crosshairDotSize", crosshair.getDotSize());
            json.addProperty("crosshairOpacity", crosshair.getOpacity());
            json.addProperty("crosshairShowDot", crosshair.isShowDot());
            json.addProperty("crosshairShowOutline", crosshair.isShowOutline());
            json.addProperty("crosshairOutlineColor", crosshair.getOutlineColor());
            json.addProperty("crosshairDynamicSpread", crosshair.isDynamicSpread());
            json.addProperty("crosshairTargetMode", crosshair.getTargetMode().name());
            json.addProperty("crosshairTargetHighlight", crosshair.isTargetHighlight());
            json.addProperty("crosshairTargetEntityColor", crosshair.getTargetEntityColor());
            json.addProperty("crosshairTargetPlayerColor", crosshair.getTargetPlayerColor());
            json.addProperty("crosshairTargetHostileColor", crosshair.getTargetHostileColor());
            json.addProperty("crosshairTargetNeutralColor", crosshair.getTargetNeutralColor());
            json.addProperty("crosshairTargetBlockColor", crosshair.getTargetBlockColor());
            json.addProperty("crosshairTargetEntityScale", crosshair.getTargetEntityScale());
            json.addProperty("crosshairTargetPlayerScale", crosshair.getTargetPlayerScale());
            json.addProperty("crosshairTargetHostileScale", crosshair.getTargetHostileScale());
            json.addProperty("crosshairTargetNeutralScale", crosshair.getTargetNeutralScale());
            json.addProperty("crosshairMovementSpread", crosshair.isMovementSpread());
            json.addProperty("crosshairJumpSpread", crosshair.isJumpSpread());
            json.addProperty("crosshairCooldownSpread", crosshair.isCooldownSpread());
            json.addProperty("crosshairHideBow", crosshair.isHideOnBowZoom());
            json.addProperty("crosshairHideF3", crosshair.isHideInF3());
            json.addProperty("crosshairHideThirdPerson", crosshair.isHideInThirdPerson());

            ToggleSprintSneakModule toggleSprint = ModuleManager.getInstance().getToggleSprintSneakModule();
            json.addProperty("toggleSprintEnabled", toggleSprint.isEnabled());
            json.addProperty("toggleSprintMode", toggleSprint.getSprintMode().name());
            json.addProperty("toggleSneakMode", toggleSprint.getSneakMode().name());
            json.addProperty("toggleSprintHideInactive", toggleSprint.isHideWhenInactive());
            json.addProperty("toggleSprintFlyBoost", toggleSprint.getFlyBoostMultiplier());

            FullbrightModule fullbright = ModuleManager.getInstance().getFullbrightModule();
            json.addProperty("fullbrightEnabled", fullbright.isEnabled());
            json.addProperty("fullbrightBrightness", fullbright.getBrightnessLevel());
            json.addProperty("fullbrightSmoothFade", fullbright.isSmoothFade());
            json.addProperty("fullbrightDisableNether", fullbright.isDisableInNether());
            json.addProperty("fullbrightDisableEnd", fullbright.isDisableInEnd());

            ClearGlassModule clearGlass = ModuleManager.getInstance().getClearGlassModule();
            json.addProperty("clearGlassEnabled", clearGlass.isEnabled());
            json.addProperty("clearGlassConnected", clearGlass.isConnectedGlass());

            DayCounterModule dayCounter = ModuleManager.getInstance().getDayCounterModule();
            json.addProperty("dayCounterShowDay", dayCounter.isShowDay());
            json.addProperty("dayCounterShowPlaytime", dayCounter.isShowPlaytime());

            json.addProperty("bossbarX", bossbarX);
            json.addProperty("bossbarY", bossbarY);
            json.addProperty("bossbarScale", bossbarScale);
            json.addProperty("scoreboardX", scoreboardX);
            json.addProperty("scoreboardY", scoreboardY);
            json.addProperty("scoreboardScale", scoreboardScale);
            json.addProperty("chatX", chatX);
            json.addProperty("chatY", chatY);
            json.addProperty("chatScale", chatScale);
            json.addProperty("effectsX", effectsX);
            json.addProperty("effectsY", effectsY);
            json.addProperty("effectsScale", effectsScale);
            json.addProperty("customVanillaHud", customVanillaHud);

            for (HudModule hud : ModuleManager.getInstance().getHudModules()) {
                JsonObject h = new JsonObject();
                h.addProperty("enabled", hud.isEnabled());
                h.addProperty("x", hud.getX());
                h.addProperty("y", hud.getY());
                h.addProperty("scale", hud.getScale());
                h.addProperty("prefix", hud.getPrefix());
                h.addProperty("suffix", hud.getSuffix());
                h.addProperty("colorMode", hud.getColorMode().name());
                h.addProperty("background", hud.hasBackground());
                h.addProperty("textColor", hud.getTextColor());
                h.addProperty("waveColor2", hud.getWaveColor2());
                h.addProperty("backgroundColor", hud.getBackgroundColor());
                h.addProperty("borderColor", hud.getBorderColor());
                h.addProperty("border", hud.hasBorder());
                h.addProperty("textShadow", hud.isTextShadow());
                h.addProperty("customFont", hud.isCustomFont());
                h.addProperty("cornerRadius", hud.getCornerRadius());
                h.addProperty("borderWidth", hud.getBorderWidth());
                h.addProperty("rainbowSpeed", hud.getRainbowSpeed());
                h.addProperty("rainbowSaturation", hud.getRainbowSaturation());
                h.addProperty("rainbowBorder", hud.isRainbowBorder());
                json.add("hud" + hud.getName(), h);
            }

            for (Module m : ModuleManager.getInstance().getModules()) {
                json.addProperty("keybind_" + m.getName(), m.getKeyBind());
            }

            Path target = getConfigFile().toPath();
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "ezclient-config-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(json), StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isSaving = false;
        }
    }
}
