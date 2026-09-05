package app.ezclient.gui;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

/** Reads only the scoreboard and inventory already supplied by the server. */
public final class BedwarsModule extends FeatureModule {
    private List<String> rows = List.of();
    private int ticks;
    public BedwarsModule() {
        super("Bedwars Hypixel Overlay", true, 150);
        flag("generators", "Generator upgrade timers", true); flag("beds", "Team beds", true);
        flag("resources", "Iron / gold inventory", true); flag("height", "Build height warning", true);
        option("limit", "Map build limit Y", 100.0, -64, 512); option("warning", "Warn blocks below limit", 5.0, 1, 20);
        String[] teams = {"Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"};
        String[] colors = {"FFFF5555", "FF5555FF", "FF55FF55", "FFFFFF55", "FF55FFFF", "FFFFFFFF", "FFFF55FF", "FFAAAAAA"};
        for (int i = 0; i < teams.length; i++) colorOption(teams[i], teams[i] + " team", colors[i]);
    }
    @Override public void onTick() {
        if (!isEnabled() || ++ticks % 10 != 0) return;
        Minecraft mc = Minecraft.getInstance(); rows = List.of();
        if (mc.level == null || mc.player == null) return;
        var board = mc.level.getScoreboard(); var objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;
        String title = objective.getDisplayName().getString().toLowerCase(Locale.ROOT);
        if (!title.contains("bed wars") && !title.contains("bedwars") && !title.contains("skywars") && !title.contains("sky wars")) return;
        List<String> output = new ArrayList<>();
        for (var entry : board.listPlayerScores(objective)) {
            if (entry.isHidden() || output.size() >= 15) continue;
            String line = (entry.display() != null ? entry.display() : PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()), entry.ownerName())).getString();
            String lower = line.toLowerCase(Locale.ROOT);
            if (flag("generators") && (lower.contains("diamond") || lower.contains("emerald") || lower.contains("diamant") || lower.contains("smaragd"))) output.add(line);
            else if (flag("beds") && (line.contains("✔") || line.contains("✘") || line.contains("✓") || line.contains("✗")
                || lower.matches(".*\\b(red|blue|green|yellow|aqua|white|pink|gray)\\b.*:.*"))) output.add(line);
        }
        if (flag("resources")) output.add("Iron: " + mc.player.getInventory().countItem(Items.IRON_INGOT) + "  Gold: " + mc.player.getInventory().countItem(Items.GOLD_INGOT));
        if (flag("height") && mc.player.getY() >= number("limit") - number("warning")) output.add("Build limit: " + (int)number("limit") + " Y");
        rows = List.copyOf(output);
    }
    @Override public List<String> lines(Minecraft mc, boolean editor) {
        return editor ? List.of("Diamond II: 12s", "Red ✔  Blue ✘", "Iron: 32  Gold: 8") : rows;
    }
    @Override public net.minecraft.network.chat.Component styledText(String text) {
        var component = super.styledText(text);
        for (String team : List.of("Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"))
            if (text.contains(team)) return component.copy().withStyle(style -> style.withColor(tint(team, false) & 0xffffff));
        return component;
    }
}
