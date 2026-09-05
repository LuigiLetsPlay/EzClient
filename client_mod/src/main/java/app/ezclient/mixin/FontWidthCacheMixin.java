package app.ezclient.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * High-speed L1 cache for text measurement (ImmediatelyFast-style FastTextLookup).
 * Eliminates character-by-character glyph advance iteration for frequently measured HUD and UI strings.
 */
@Mixin(Font.class)
abstract class FontWidthCacheMixin {
    private static final int CACHE_SIZE = 1024;
    private static final int CACHE_MASK = CACHE_SIZE - 1;
    private static final String[] CACHED_TEXTS = new String[CACHE_SIZE];
    private static final int[] CACHED_WIDTHS = new int[CACHE_SIZE];

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true)
    private void ezclient$fastWidthLookup(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null || text.isEmpty()) {
            cir.setReturnValue(0);
            return;
        }
        if (text.length() <= 48) {
            int slot = text.hashCode() & CACHE_MASK;
            String cached = CACHED_TEXTS[slot];
            if (cached != null && cached.equals(text)) {
                cir.setReturnValue(CACHED_WIDTHS[slot]);
            }
        }
    }

    @Inject(method = "width(Ljava/lang/String;)I", at = @At("RETURN"))
    private void ezclient$storeFastWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text != null && text.length() <= 48) {
            int slot = text.hashCode() & CACHE_MASK;
            CACHED_TEXTS[slot] = text;
            CACHED_WIDTHS[slot] = cir.getReturnValueI();
        }
    }
}
