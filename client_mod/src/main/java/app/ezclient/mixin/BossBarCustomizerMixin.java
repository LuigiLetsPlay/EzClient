package app.ezclient.mixin;

import app.ezclient.gui.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public abstract class BossBarCustomizerMixin {
    @Shadow @Final private Map<UUID, LerpingBossEvent> events;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ezclient$bossBars(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        var module = FeatureModule.get(BossBarModule.class);
        if (!module.isEnabled()) return;
        ci.cancel();
        var mc = Minecraft.getInstance();
        if (module.flag("hide") || EzScreenBridge.hudHidden(mc) || mc.getDebugOverlay().showDebugScreen()) return;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(module.getX(), module.getY());
            graphics.pose().scale((float) module.getScale(), (float) module.getScale());
            int y = 0;
            for (var event : events.values()) {
                String name = event.getName().getString();
                if (!module.text("filter").isBlank() && name.toLowerCase(Locale.ROOT).contains(module.text("filter").toLowerCase(Locale.ROOT))) continue;
                BossBarModule.renderBossBar(graphics, mc, module, event.getName(), event.getProgress(), event.getColor(), 0, y, false);
                y += module.text("style").equals("Text") ? 16 : 24;
                if (y * module.getScale() > mc.getWindow().getGuiScaledHeight() / 2.0) break;
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }
}
