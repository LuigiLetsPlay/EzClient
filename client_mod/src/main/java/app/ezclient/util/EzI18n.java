package app.ezclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Universal, high-performance in-game localization engine for EzClient.
 * Automatically synchronizes with Minecraft's in-game language settings (DE / EN),
 * provides multi-tier fallbacks, and prevents raw variable leakage in UI.
 */
public final class EzI18n {
    private static final Map<String, String> DE_MAP = new HashMap<>();
    private static final Map<String, String> EN_MAP = new HashMap<>();
    private static boolean initialized = false;

    static {
        loadLanguage("de_de", DE_MAP);
        loadLanguage("en_us", EN_MAP);
        initialized = true;
    }

    private EzI18n() {}

    private static void loadLanguage(String langCode, Map<String, String> targetMap) {
        try (InputStream is = EzI18n.class.getResourceAsStream("/assets/ezclient/lang/" + langCode + ".json")) {
            if (is == null) return;
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Regex to match "key" : "value" handling escaped characters
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"((?:\\\\\"|[^\"])+)\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
            java.util.regex.Matcher matcher = pattern.matcher(json);
            while (matcher.find()) {
                String key = matcher.group(1).replace("\\\"", "\"");
                String val = unescapeJsonString(matcher.group(2));
                targetMap.put(key, val);
            }
        } catch (Throwable t) {
            System.err.println("[EzClient] Warning: Could not load lang/" + langCode + ".json: " + t.getMessage());
        }
    }

    private static String unescapeJsonString(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case '/' -> { sb.append('/'); i++; }
                    case 'b' -> { sb.append('\b'); i++; }
                    case 'f' -> { sb.append('\f'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'u' -> {
                        if (i + 5 < raw.length()) {
                            try {
                                int unicode = Integer.parseInt(raw.substring(i + 2, i + 6), 16);
                                sb.append((char) unicode);
                                i += 5;
                            } catch (NumberFormatException ignored) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isGerman() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                if (mc.options != null && mc.options.languageCode != null) {
                    return mc.options.languageCode.toLowerCase().startsWith("de");
                }
                if (mc.getLanguageManager() != null && mc.getLanguageManager().getSelected() != null) {
                    return mc.getLanguageManager().getSelected().toLowerCase().startsWith("de");
                }
            }
        } catch (Throwable ignored) {}
        return true;
    }

    public static boolean has(String key) {
        if (isGerman()) {
            if (DE_MAP.containsKey(key)) return true;
        }
        return EN_MAP.containsKey(key);
    }

    public static String getRaw(String key) {
        if (isGerman()) {
            String val = DE_MAP.get(key);
            if (val != null) return val;
        }
        String val = EN_MAP.get(key);
        if (val != null) return val;
        // Fallback to German if English map didn't have it
        return DE_MAP.get(key);
    }

    public static String get(String key) {
        String raw = getRaw(key);
        return raw != null ? raw : key;
    }

    public static String get(String key, Object... args) {
        String raw = getRaw(key);
        if (raw == null) {
            // If the key is not in any lang file, but a single string argument was provided
            // that doesn't look like a format specifier, treat it as the default/fallback text
            if (args != null && args.length == 1 && args[0] instanceof String s && !s.contains("%")) {
                return s;
            }
            return key;
        }
        try {
            return String.format(raw, args);
        } catch (Exception e) {
            return raw;
        }
    }

    public static String getOrDefault(String key, String fallback) {
        String raw = getRaw(key);
        return raw != null ? raw : fallback;
    }

    public static Component comp(String key) {
        return Component.literal(get(key));
    }

    public static Component comp(String key, Object... args) {
        return Component.literal(get(key, args));
    }

    public static Component compOrDefault(String key, String fallback) {
        return Component.literal(getOrDefault(key, fallback));
    }

    public static String onOrOff(boolean state) {
        if (isGerman()) {
            return state ? "AN" : "AUS";
        }
        return state ? "ON" : "OFF";
    }
}
