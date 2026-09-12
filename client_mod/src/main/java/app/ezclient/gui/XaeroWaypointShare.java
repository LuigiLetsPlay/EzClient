package app.ezclient.gui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

/** Parses Xaero's chat share format and turns it into a local-only action. */
public record XaeroWaypointShare(String raw, String name, double x, double y, double z, int color, String dimension) {
    private static final Pattern SHARE = Pattern.compile("xaero-waypoint:[^\\s]+", Pattern.CASE_INSENSITIVE);
    public static final String COMMAND_PREFIX = "/ezclient-waypoint ";
    private static final int[] COLORS = {
        0xFF202020, 0xFF3546B8, 0xFF2E9E55, 0xFF2FA7A0,
        0xFFB83A3A, 0xFF9B4DB8, 0xFFE39A32, 0xFFAAAAAA,
        0xFF555555, 0xFF4D6FFF, 0xFF45D66B, 0xFF46D9D0,
        0xFFFF5555, 0xFFFF63D8, 0xFFFFD84A, 0xFFFFFFFF
    };

    public static XaeroWaypointShare parse(String raw) {
        if (raw == null) return null;
        String[] fields = raw.trim().split(":");
        if (fields.length < 10 || !"xaero-waypoint".equalsIgnoreCase(fields[0])) return null;
        try {
            String name = WaypointsModule.cleanWaypointName(fields[1].trim());
            if (name.isEmpty() || name.length() > 80) return null;
            double x = Double.parseDouble(fields[3]);
            double y = Double.parseDouble(fields[4]);
            double z = Double.parseDouble(fields[5]);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
            int color = COLORS[Math.floorMod(Integer.parseInt(fields[6]), COLORS.length)];
            String dimension = fields[9].toLowerCase(java.util.Locale.ROOT).contains("nether")
                    ? "minecraft:the_nether"
                    : fields[9].toLowerCase(java.util.Locale.ROOT).contains("end")
                        ? "minecraft:the_end" : "minecraft:overworld";
            return new XaeroWaypointShare(raw.trim(), name, x, y, z, color, dimension);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static Component decorate(Component message) {
        if (message == null) return null;
        Matcher matcher = SHARE.matcher(message.getString());
        if (!matcher.find()) return message;
        XaeroWaypointShare share = parse(matcher.group());
        if (share == null) return message;
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(share.raw().getBytes(StandardCharsets.UTF_8));
        return message.copy().append(Component.literal(" [Hinzufügen]").withStyle(style -> style
                .withColor(0xAAAAAA)
                .withClickEvent(new ClickEvent.RunCommand(COMMAND_PREFIX + encoded))));
    }

    public static XaeroWaypointShare fromCommand(String command) {
        if (command == null || !command.startsWith(COMMAND_PREFIX)) return null;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(command.substring(COMMAND_PREFIX.length())), StandardCharsets.UTF_8);
            return parse(raw);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
