package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;

public final class ItemPhysicsModule extends FeatureModule {
    private int nearbyItems;
    private int ticks;
    public ItemPhysicsModule() {
        super("Item Physics", false, 10);
        option("Darstellung", "mode", "Ground rotation", "Legt die Drehung liegender Gegenstände fest.", "Flat", 0, 0, "Flat", "Rotating");
        flag("Darstellung", "physics", "Trajectory rotation", "Dreht Gegenstände während der Flugbahn.", true);
        option("Darstellung", "speed", "Rotation speed", "Geschwindigkeit der Gegenstandsdrehung.", 1.0, 0, 5); option("Performance", "cap", "Vanilla fallback above items", "Schützt die Leistung bei vielen Gegenständen.", 100.0, 10, 500);
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
