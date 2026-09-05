package app.ezclient.mixin;

import app.ezclient.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.BossEvent;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class BossBarCustomizerMixin {
    @Shadow @Final private Map<UUID, LerpingBossEvent> events;
    @Shadow protected abstract void extractBar(GuiGraphicsExtractor graphics, int x, int y, BossEvent event);
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ezclient$bossBars(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        var module = FeatureModule.get(BossBarModule.class);
        if (!module.isEnabled()) return;
        ci.cancel();
        var mc = Minecraft.getInstance();
        if (module.flag("hide") || EzScreenBridge.hudHidden(mc) || mc.getDebugOverlay().showDebugScreen()) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(module.getX(), module.getY()); graphics.pose().scale((float)module.getScale(), (float)module.getScale());
            int y = 0;
            for (var event : events.values()) {
                String name = event.getName().getString();
                if (!module.text("filter").isBlank() && name.toLowerCase(Locale.ROOT).contains(module.text("filter").toLowerCase(Locale.ROOT))) continue;
                String label = name + module.health(event.getProgress());
                int w = Math.max(190, mc.font.width(module.styledText(label)) + 8);
                module.renderBackgroundAndBorder(graphics, 0, y, w, module.text("style").equals("Text") ? 16 : 25);
                graphics.text(mc.font, module.styledText(label), 4, y + 3, module.color(), module.isTextShadow());
                if (!module.text("style").equals("Text")) {
                    if (module.text("style").equals("Vanilla") && !module.flag("override") && !module.flag("chroma")) extractBar(graphics, 4, y + 15, event);
                    else {
                        graphics.fill(4, y + 15, 186, y + 20, 0xff303030);
                        int color = module.flag("override") || module.flag("chroma") ? module.tint("bar", module.flag("chroma")) : switch(event.getColor()) {
                            case PINK -> 0xffff55ff; case BLUE -> 0xff5555ff; case RED -> 0xffff5555; case GREEN -> 0xff55ff55;
                            case YELLOW -> 0xffffff55; case PURPLE -> 0xffaa00aa; default -> 0xffffffff;
                        };
                        graphics.fill(4, y + 15, 4 + Math.round(182 * event.getProgress()), y + 20, color);
                    }
                }
                y += module.text("style").equals("Text") ? 18 : 28;
                if (y * module.getScale() > mc.getWindow().getGuiScaledHeight() / 2.0) break;
            }
        } finally { graphics.pose().popMatrix(); }
    }
}
