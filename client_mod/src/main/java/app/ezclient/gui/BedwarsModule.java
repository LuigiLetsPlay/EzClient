package app.ezclient.gui;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

/** Reads only the scoreboard and inventory already supplied by the server. */
public final class BedwarsModule extends FeatureModule {
    private List<String> rows = List.of();
    private int ticks;
    public BedwarsModule() {
        super("Bedwars Hypixel Overlay", true, 150);
        flag("Overlay", "generators", "Generator upgrade timers", "Zeigt Generator- und Upgradeinformationen.", true); flag("Overlay", "beds", "Team beds", "Zeigt Status der Team-Betten.", true);
        flag("Overlay", "resources", "Iron / gold inventory", "Zeigt Eisen und Gold im Inventar.", true); flag("Warnungen", "height", "Build height warning", "Warnt nahe der Bauhöhenbegrenzung.", true);
        option("Warnungen", "limit", "Map build limit Y", "Y-Höhe der Kartengrenze.", 100.0, -64, 512); option("Warnungen", "warning", "Warn blocks below limit", "Abstand zur Grenze für die Warnung.", 5.0, 1, 20);
        String[] teams = {"Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"};
        String[] colors = {"FFFF5555", "FF5555FF", "FF55FF55", "FFFFFF55", "FF55FFFF", "FFFFFFFF", "FFFF55FF", "FFAAAAAA"};
        for (int i = 0; i < teams.length; i++) colorOption("Teamfarben", teams[i], teams[i] + " team", "Farbe für " + teams[i] + " im Overlay.", colors[i]);
    }
    @Override public void onTick() {
        if (!isEnabled() || ++ticks % 10 != 0) return;
        Minecraft mc = Minecraft.getInstance(); rows = List.of();
        if (mc.level == null || mc.player == null) return;
        var board = mc.level.getScoreboard(); var objective = board.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;
        String title = objective.getDisplayName().getString().toLowerCase(Locale.ROOT);
        if (!title.contains("bed wars") && !title.contains("bedwars") && !title.contains("skywars") && !title.contains("sky wars")) return;

        boolean inLobby = false;
        boolean inActiveGame = false;
        List<String> gameLines = new ArrayList<>();

        for (var entry : board.listPlayerScores(objective)) {
            if (entry.isHidden() || gameLines.size() >= 15) continue;
            String line = (entry.display() != null ? entry.display() : PlayerTeam.formatNameForTeam(board.getPlayersTeam(entry.owner()), entry.ownerName())).getString();
            String lower = line.toLowerCase(Locale.ROOT);

            if (lower.contains("coins:") || lower.contains("loot chest") || lower.contains("mystery")
                    || lower.contains("tokens:") || lower.contains("hypixel level")
                    || lower.contains("waiting...") || lower.contains("starting in")) {
                inLobby = true;
            }

            boolean isGen = lower.contains("diamond") || lower.contains("emerald") || lower.contains("diamant") || lower.contains("smaragd")
                    || lower.contains("bed destruction") || lower.contains("sudden death");
            boolean isBed = line.contains("✔") || line.contains("✘") || line.contains("✓") || line.contains("✗")
                    || lower.matches(".*\\b(red|blue|green|yellow|aqua|white|pink|gray)\\b.*:.*");

            if (isGen || isBed) {
                inActiveGame = true;
            }

            if (flag("generators") && isGen) {
                gameLines.add(line);
            } else if (flag("beds") && isBed) {
                gameLines.add(line);
            }
        }

        // Never show anything if we are in the lobby or not in an active Bedwars game
        if (inLobby || !inActiveGame) {
            rows = List.of();
            return;
        }

        List<String> output = new ArrayList<>(gameLines);
        if (flag("resources")) output.add("Iron: " + mc.player.getInventory().countItem(Items.IRON_INGOT) + "  Gold: " + mc.player.getInventory().countItem(Items.GOLD_INGOT));
        if (flag("height") && mc.player.getY() >= number("limit") - number("warning")) output.add("Build limit: " + (int)number("limit") + " Y");
        rows = List.copyOf(output);
    }
    @Override public List<String> lines(Minecraft mc, boolean editor) {
        if (!editor || !rows.isEmpty()) return rows;
        List<String> preview = new ArrayList<>();
        if (flag("generators")) {
            preview.add("Diamond II: 0:45");
            preview.add("Emerald II: 0:20");
        }
        if (flag("beds")) {
            preview.add("Red: ✔");
            preview.add("Blue: ✔");
            preview.add("Green: ✔");
            preview.add("Yellow: ✔");
            preview.add("Aqua: ✔");
            preview.add("White: ✘");
            preview.add("Pink: ✘");
            preview.add("Gray: ✘");
        }
        if (flag("resources")) {
            preview.add("Iron: 32  Gold: 8");
        }
        if (flag("height")) {
            preview.add("Build limit: " + (int) number("limit") + " Y");
        }
        if (preview.isEmpty()) {
            preview.add("Bedwars Hypixel Overlay");
        }
        return preview;
    }
    @Override public net.minecraft.network.chat.Component styledText(String text) {
        var component = super.styledText(text);
        for (String team : List.of("Red", "Blue", "Green", "Yellow", "Aqua", "White", "Pink", "Gray"))
            if (text.contains(team)) return component.copy().withStyle(style -> style.withColor(tint(team, false) & 0xffffff));
        return component;
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/bedwars.png");
    }
}
