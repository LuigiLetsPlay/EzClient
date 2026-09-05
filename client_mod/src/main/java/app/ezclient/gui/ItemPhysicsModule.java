package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemPhysicsModule extends FeatureModule {
    private int nearbyItems;
    private int ticks;
    public ItemPhysicsModule() {
        super("Item Physics", false, 10);
        option("mode", "Ground rotation", "Flat", 0, 0, "Flat", "Rotating");
        flag("physics", "Trajectory rotation", true);
        option("speed", "Rotation speed", 1.0, 0, 5); option("cap", "Vanilla fallback above items", 100.0, 10, 500);
    }
    @Override public void onTick() {
        if (!isEnabled() || ++ticks % 10 != 0) return;
        Minecraft mc = Minecraft.getInstance(); nearbyItems = 0;
        if (mc.level == null || mc.player == null) return;
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ItemEntity && entity.distanceToSqr(mc.player) < 96 * 96 && ++nearbyItems > number("cap")) break;
        }
    }
    public boolean active() { return isEnabled() && nearbyItems <= number("cap"); }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/item_physics.png");
    }
}
