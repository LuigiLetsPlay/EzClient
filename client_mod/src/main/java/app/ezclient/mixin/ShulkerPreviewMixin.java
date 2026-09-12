package app.ezclient.mixin;

import app.ezclient.gui.FeatureModule;
import app.ezclient.gui.ShulkerPreviewModule;
import app.ezclient.gui.EzUi;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class ShulkerPreviewMixin<T extends AbstractContainerMenu> {
    @Shadow protected Slot hoveredSlot;

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void ezclient$suppressVanillaTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
        ShulkerPreviewModule module = FeatureModule.get(ShulkerPreviewModule.class);
        if (!module.isEnabled() || hoveredSlot == null || !hoveredSlot.hasItem()) return;
        ItemStack stack = hoveredSlot.getItem();
        boolean isShulker = module.flag("shulker") && stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock;
        boolean isMap = module.flag("map") && stack.has(DataComponents.MAP_ID);
        if (isShulker || isMap) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void ezclient$containerPreview(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ShulkerPreviewModule module = FeatureModule.get(ShulkerPreviewModule.class);
        if (!module.isEnabled() || hoveredSlot == null || !hoveredSlot.hasItem()) return;
        ItemStack stack = hoveredSlot.getItem();
        if (module.flag("shulker") && stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock) {
            renderShulker(g, mouseX, mouseY, stack, module); return;
        }
        if (module.flag("map") && stack.has(DataComponents.MAP_ID)) renderMap(g, mouseX, mouseY, stack, module);
    }

    private static void renderShulker(GuiGraphicsExtractor g, int mouseX, int mouseY, ItemStack stack, ShulkerPreviewModule module) {
        var contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return;
        List<ItemStack> items = contents.allItemsCopyStream().toList();
        boolean emptySlots = module.flag("emptySlots");
        if (!emptySlots && items.stream().allMatch(ItemStack::isEmpty)) return;
        Minecraft mc = Minecraft.getInstance(); float scale = (float)module.number("scale");
        int width = 9 * 18 + 8, height = 3 * 18 + 19;
        int x = mouseX + 14, y = mouseY + 12;
        if (x + width * scale > g.guiWidth()) x = Math.max(2, mouseX - 14 - Math.round(width * scale));
        if (y + height * scale > g.guiHeight()) y = Math.max(2, g.guiHeight() - Math.round(height * scale) - 2);
        g.nextStratum(); g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale, scale);
        EzUi.roundedRect(g, 0, 0, width, height, 1, module.tint("background", false));
        g.outline(0, 0, width, height, module.tint("border", false));
        g.text(mc.font, stack.getHoverName(), 5, 4, module.tint("title", false), true);
        for (int slot = 0; slot < 27; slot++) {
            int sx = 4 + slot % 9 * 18, sy = 16 + slot / 9 * 18;
            if (emptySlots) g.fill(sx, sy, sx + 16, sy + 16, 0x33000000);
            if (slot < items.size() && !items.get(slot).isEmpty()) {
                ItemStack item = items.get(slot); g.item(item, sx, sy);
                if (module.flag("counts")) g.itemDecorations(mc.font, item, sx, sy);
            }
        }
        g.pose().popMatrix();
    }

    private static void renderMap(GuiGraphicsExtractor g, int mouseX, int mouseY, ItemStack stack, ShulkerPreviewModule module) {
        Minecraft mc = Minecraft.getInstance(); if (mc.level == null) return;
        var mapId = stack.get(DataComponents.MAP_ID); var data = MapItem.getSavedData(stack, mc.level);
        if (mapId == null || data == null) return;
        MapRenderState state = new MapRenderState(); mc.getMapRenderer().extractRenderState(mapId, data, state);
        if (!module.flag("decorations")) state.decorations.clear();
        int size = (int)module.number("mapSize"), x = mouseX + 14, y = mouseY + 12;
        if (x + size + 4 > g.guiWidth()) x = Math.max(2, mouseX - size - 18);
        if (y + size + 18 > g.guiHeight()) y = Math.max(2, g.guiHeight() - size - 20);
        g.nextStratum(); EzUi.roundedRect(g, x, y, size + 4, size + 18, 1, module.tint("background", false));
        g.outline(x, y, size + 4, size + 18, module.tint("border", false));
        g.text(mc.font, stack.getHoverName(), x + 4, y + 4, module.tint("title", false), true);
        g.pose().pushMatrix(); g.pose().translate(x + 2, y + 16); g.pose().scale(size / 128.0f, size / 128.0f); g.map(state); g.pose().popMatrix();
    }
}
