package app.ezclient.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws EzClient branding only on the actual in-game Escape pause screen. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    private static final Identifier EZCLIENT_ICON = Identifier.fromNamespaceAndPath("ezclient", "title/icon");

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezclient$drawPauseLogo(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        PauseScreen screen = (PauseScreen) (Object) this;
        int x = screen.width - 25;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EZCLIENT_ICON, x, 7, 16, 16);
    }
}
