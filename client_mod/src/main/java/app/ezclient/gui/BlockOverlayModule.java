package app.ezclient.gui;

import net.minecraft.resources.Identifier;

public final class BlockOverlayModule extends FeatureModule {
    public BlockOverlayModule() {
        super("Block Overlay", false, 10);
        option("style", "Render style", "Outline", 0, 0, "Outline", "Fill", "Both");
        option("width", "Line width", 2.0, 1, 5); colorOption("outline", "Outline", "FFFFFFFF");
        flag("chroma", "Chroma outline", false); colorOption("fill", "Fill", "FFFFFFFF");
        option("fillOpacity", "Fill opacity %", 15.0, 0, 100);
        option("break", "Breaking cracks", "Vanilla", 0, 0, "Vanilla", "Hidden", "Tint overlay");
        colorOption("breakColor", "Breaking overlay", "40FFAA00");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/block_overlay.png");
    }
}
