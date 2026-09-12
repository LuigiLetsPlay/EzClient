package app.ezclient.gui;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;

public final class BossBarModule extends FeatureModule {
    public BossBarModule() {
        super("Boss Bar Customizer", true, 12);
        setBackground(false);
        flag("Allgemein", "hide", "Hide all boss bars", "Blendet alle Bossleisten aus.", false);
        option("Allgemein", "filter", "Hide names containing", "Blendet nur Bossleisten mit diesem Text aus.", "", 0, 0);
        option("Darstellung", "style", "Style", "Wählt das Layout der Bossleiste.", "Vanilla", 0, 0, "Vanilla", "Minimal", "Text");
        option("Darstellung", "health", "Health display", "Legt die Gesundheitsanzeige fest.", "Percent", 0, 0, "Percent", "HP", "Hidden");
        option("Darstellung", "maxHp", "Known max HP (0 = unknown)", "Referenzwert für die HP-Anzeige.", 0.0, 0, 10000);
        flag("Farbe", "override", "Override bar color", "Verwendet eine eigene Leistenfarbe.", false);
        colorOption("Farbe", "bar", "Bar color", "Farbe der überschriebenen Bossleiste.", "FFAA55FF");
        flag("Farbe", "chroma", "Rainbow bar", "Animiert die Leistenfarbe.", false);
    }

    public String health(float progress) {
        if (text("health").equals("Hidden")) return "";
        if (text("health").equals("HP")) return number("maxHp") > 0
            ? String.format(Locale.ROOT, " %.0f / %.0f HP", progress * number("maxHp"), number("maxHp")) : " HP: unavailable";
        return " " + Math.round(progress * 100) + "%";
    }

    private static final Identifier[] BAR_BACKGROUND_SPRITES = new Identifier[] {
        Identifier.withDefaultNamespace("boss_bar/pink_background"),
        Identifier.withDefaultNamespace("boss_bar/blue_background"),
        Identifier.withDefaultNamespace("boss_bar/red_background"),
        Identifier.withDefaultNamespace("boss_bar/green_background"),
        Identifier.withDefaultNamespace("boss_bar/yellow_background"),
        Identifier.withDefaultNamespace("boss_bar/purple_background"),
        Identifier.withDefaultNamespace("boss_bar/white_background")
    };
    private static final Identifier[] BAR_PROGRESS_SPRITES = new Identifier[] {
        Identifier.withDefaultNamespace("boss_bar/pink_progress"),
        Identifier.withDefaultNamespace("boss_bar/blue_progress"),
        Identifier.withDefaultNamespace("boss_bar/red_progress"),
        Identifier.withDefaultNamespace("boss_bar/green_progress"),
        Identifier.withDefaultNamespace("boss_bar/yellow_progress"),
        Identifier.withDefaultNamespace("boss_bar/purple_progress"),
        Identifier.withDefaultNamespace("boss_bar/white_progress")
    };

    public static void renderBossBar(GuiGraphicsExtractor g, Minecraft mc, BossBarModule module, Component name, float progress, BossEvent.BossBarColor color, int x, int y, boolean editor) {
        if (!editor && module.flag("hide")) return;
        if (name == null) name = Component.translatable("entity.minecraft.ender_dragon");
        String nameStr = name.getString();
        if (!editor && !module.text("filter").isBlank() && nameStr.toLowerCase(Locale.ROOT).contains(module.text("filter").toLowerCase(Locale.ROOT))) return;

        String healthSuffix = module.health(progress);
        Component fullTitle = healthSuffix.isEmpty() ? name : Component.empty().append(name).append(healthSuffix);
        String style = module.text("style");

        int titleWidth = mc.font.width(fullTitle);
        int innerW = Math.max(182, titleWidth);
        int innerH = style.equals("Text") ? 9 : 15;

        boolean boxed = module.hasBackground() || module.hasBorder();
        int padX = boxed ? 5 : 0;
        int padY = boxed ? 3 : 0;
        int totalW = innerW + padX * 2;
        int totalH = innerH + padY * 2;

        if (boxed) {
            module.renderBackgroundAndBorder(g, x, y, totalW, totalH);
        }

        // Vanilla boss bar: text is centered directly 9 pixels above the bar
        int textX = x + padX + (innerW - titleWidth) / 2;
        int textY = y + padY;
        g.text(mc.font, fullTitle, textX, textY, 0xFFFFFFFF);

        if (!style.equals("Text")) {
            int barX = x + padX + (innerW - 182) / 2;
            int barY = y + padY + 10;

            if (style.equals("Vanilla")) {
                if (module.flag("override") || module.flag("chroma")) {
                    g.fill(barX, barY, barX + 182, barY + 5, 0xFF202020);
                    int barCol = module.tint("bar", module.flag("chroma"));
                    int pw = Math.max(0, Math.min(182, Math.round(182 * progress)));
                    if (pw > 0) g.fill(barX, barY, barX + pw, barY + 5, barCol);
                } else {
                    int colorIdx = color != null ? color.ordinal() : BossEvent.BossBarColor.PURPLE.ordinal();
                    Identifier bgSprite = BAR_BACKGROUND_SPRITES[colorIdx];
                    Identifier progSprite = BAR_PROGRESS_SPRITES[colorIdx];
                    g.blitSprite(RenderPipelines.GUI_TEXTURED, bgSprite, 182, 5, 0, 0, barX, barY, 182, 5);
                    int progressWidth = net.minecraft.util.Mth.lerpDiscrete(progress, 0, 182);
                    if (progressWidth > 0) {
                        g.blitSprite(RenderPipelines.GUI_TEXTURED, progSprite, 182, 5, 0, 0, barX, barY, progressWidth, 5);
                    }
                }
            } else if (style.equals("Minimal")) {
                g.fill(barX, barY + 1, barX + 182, barY + 4, 0x80000000);
                int barCol;
                if (module.flag("override") || module.flag("chroma")) {
                    barCol = module.tint("bar", module.flag("chroma"));
                } else if (color != null) {
                    barCol = switch (color) {
                        case PINK -> 0xffff55ff;
                        case BLUE -> 0xff5555ff;
                        case RED -> 0xffff5555;
                        case GREEN -> 0xff55ff55;
                        case YELLOW -> 0xffffff55;
                        case PURPLE -> 0xffaa00aa;
                        default -> 0xffffffff;
                    };
                } else {
                    barCol = 0xffaa00aa;
                }
                int pw = Math.max(0, Math.min(182, Math.round(182 * progress)));
                if (pw > 0) g.fill(barX, barY + 1, barX + pw, barY + 4, barCol);
            }
        }
    }

    @Override
    public int getWidth(Minecraft mc, boolean editor) {
        Component sample = Component.translatable("entity.minecraft.ender_dragon");
        int titleWidth = mc != null && mc.font != null ? mc.font.width(sample) + mc.font.width(health(0.75f)) : 182;
        int innerW = Math.max(182, titleWidth);
        return (hasBackground() || hasBorder()) ? innerW + 10 : innerW;
    }

    @Override
    public int getWidth(Minecraft mc) {
        return getWidth(mc, false);
    }

    @Override
    public int getHeight(Minecraft mc, boolean editor) {
        int innerH = text("style").equals("Text") ? 9 : 15;
        return (hasBackground() || hasBorder()) ? innerH + 6 : innerH;
    }

    @Override
    public int getHeight(Minecraft mc) {
        return getHeight(mc, false);
    }

    @Override
    public void renderFeature(GuiGraphicsExtractor g, Minecraft mc, boolean editor) {
        if (editor) {
            g.pose().pushMatrix();
            g.pose().translate(getX(), getY());
            g.pose().scale((float) getScale(), (float) getScale());
            renderBossBar(g, mc, this, Component.translatable("entity.minecraft.ender_dragon"), 0.75f, BossEvent.BossBarColor.PURPLE, 0, 0, true);
            g.pose().popMatrix();
        }
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/boss_bar.png");
    }
}
