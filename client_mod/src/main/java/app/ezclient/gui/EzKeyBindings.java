package app.ezclient.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Registers all EzClient keybindings in Minecraft's Controls menu under the "EzClient" category
 * and maintains bidirectional synchronization with EzClient's module configuration.
 */
public final class EzKeyBindings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ezclient", "controls"));

    public static KeyMapping KEY_HUB;
    public static KeyMapping KEY_HUD_EDITOR;
    public static KeyMapping KEY_WAYPOINT_MANAGER;
    public static KeyMapping KEY_QUICK_WAYPOINT;
    public static KeyMapping KEY_ZOOM;
    public static KeyMapping KEY_COPY_COORDINATES;
    public static KeyMapping KEY_FREELOOK;
    public static KeyMapping KEY_TIMER_TOGGLE;
    public static KeyMapping KEY_TIMER_RESET;
    public static KeyMapping KEY_FULLBRIGHT;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        KEY_HUB = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.hub",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        KEY_HUD_EDITOR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.hud_editor",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        KEY_WAYPOINT_MANAGER = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.waypoint_manager",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                CATEGORY
        ));

        KEY_QUICK_WAYPOINT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.quick_waypoint",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                CATEGORY
        ));

        KEY_ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY
        ));

        KEY_COPY_COORDINATES = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.copy_coordinates",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                CATEGORY
        ));

        KEY_FREELOOK = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY));
        KEY_TIMER_TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.timer_toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
        KEY_TIMER_RESET = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.timer_reset", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
        KEY_FULLBRIGHT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.ezclient.fullbright", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
    }

    public static int getKeyCode(KeyMapping mapping) {
        if (mapping == null) return -1;
        try {
            InputConstants.Key key = ((app.ezclient.mixin.KeyMappingAccessor) mapping).ezclient$getKey();
            if (key != null && key != InputConstants.UNKNOWN) {
                return key.getType() == InputConstants.Type.MOUSE ? -100 - key.getValue() : key.getValue();
            }
        } catch (Throwable ignored) {}
        try {
            InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
            if (key != null && key != InputConstants.UNKNOWN) {
                return key.getType() == InputConstants.Type.MOUSE ? -100 - key.getValue() : key.getValue();
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    public static void setKeyCode(KeyMapping mapping, int keyOrMouseButton) {
        if (mapping == null) return;
        if (getKeyCode(mapping) == keyOrMouseButton) return;
        InputConstants.Key key;
        if (keyOrMouseButton <= -100) {
            int mouseButton = -keyOrMouseButton - 100;
            key = InputConstants.Type.MOUSE.getOrCreate(mouseButton);
        } else if (keyOrMouseButton > 0) {
            key = InputConstants.Type.KEYSYM.getOrCreate(keyOrMouseButton);
        } else {
            key = InputConstants.UNKNOWN;
        }
        mapping.setKey(key);
        KeyMapping.resetMapping();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.save();
        }
    }

    public static String getKeyOrMouseName(int code) {
        if (code <= -100) {
            int btn = -code - 100;
            return switch (btn) {
                case 0 -> "Linke Maustaste";
                case 1 -> "Rechte Maustaste";
                case 2 -> "Mittlere Maustaste";
                case 3 -> "Maustaste 4";
                case 4 -> "Maustaste 5";
                default -> "Maustaste " + (btn + 1);
            };
        }
        if (code <= 0) return "Nicht belegt";
        return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
    }

    public static void applyModuleKeyBind(Module module, int code) {
        if (module == null) return;
        module.setKeyBind(code);
        if (module instanceof ZoomModule) {
            setKeyCode(KEY_ZOOM, code);
        } else if (module instanceof FreelookModule) {
            setKeyCode(KEY_FREELOOK, code);
        } else if (module instanceof FullbrightModule) {
            setKeyCode(KEY_FULLBRIGHT, code);
        } else if (module instanceof WaypointsModule) {
            setKeyCode(KEY_WAYPOINT_MANAGER, code);
        } else if (module instanceof CoordinatesModule) {
            setKeyCode(KEY_COPY_COORDINATES, code);
        }
        ConfigManager.save();
    }

    /**
     * Polls key bindings for external changes (e.g. user changing controls in Minecraft options)
     * and syncs them to EzClient modules.
     */
    public static void syncFromControls(Minecraft mc) {
        if (!initialized || mc == null) return;

        syncModuleKey(ModuleManager.getInstance().getZoomModule(), KEY_ZOOM);
        syncModuleKey(FeatureModule.get(WaypointsModule.class), KEY_WAYPOINT_MANAGER);
        syncModuleKey(ModuleManager.getInstance().getCoordinatesModule(), KEY_COPY_COORDINATES);
        syncModuleKey(FeatureModule.get(FreelookModule.class), KEY_FREELOOK);
        syncModuleKey(ModuleManager.getInstance().getFullbrightModule(), KEY_FULLBRIGHT);

        var timer = FeatureModule.get(BastiTimerModule.class);
        if (timer != null) {
            if (KEY_TIMER_TOGGLE != null) {
                int controlsKey = getKeyCode(KEY_TIMER_TOGGLE);
                if (controlsKey != -1 && controlsKey != timer.getToggleKey()) {
                    timer.setToggleKey(controlsKey);
                }
            }
            if (KEY_TIMER_RESET != null) {
                int controlsKey = getKeyCode(KEY_TIMER_RESET);
                if (controlsKey != -1 && controlsKey != timer.getResetKey()) {
                    timer.setResetKey(controlsKey);
                }
            }
        }
    }

    private static void syncModuleKey(Module module, KeyMapping mapping) {
        if (module == null || mapping == null) return;
        int controlsKey = getKeyCode(mapping);
        if (controlsKey != -1 && controlsKey != module.getKeyBind()) {
            module.setKeyBind(controlsKey);
            ConfigManager.save();
        }
    }
}
