package app.ezclient.gui;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;

public final class HitboxModule extends FeatureModule {
    public HitboxModule() {
        super("Hitbox Visualizer", false, 10);
        flag("players", "Players", true); flag("hostile", "Hostile mobs", true);
        flag("animals", "Passive animals", true); flag("projectiles", "Projectiles", true); flag("items", "Dropped items", false);
        colorOption("box", "Hitbox color", "FFFFFFFF"); option("width", "Line width", 1.0, 1, 3);
        flag("chroma", "Chroma hitboxes", false); flag("eyes", "Eye height", true); flag("look", "Look vector", true);
        colorOption("eyeColor", "Eye color", "FFFF3333"); colorOption("lookColor", "Look color", "FF3377FF");
        flag("fill", "Fill", false); colorOption("fillColor", "Fill color", "26FFFFFF");
        flag("debugOnly", "Only with F3+B", false);
    }
    public boolean accepts(Entity e) {
        return e instanceof Player ? flag("players") : e instanceof Enemy ? flag("hostile") : e instanceof Animal ? flag("animals")
            : e instanceof Projectile ? flag("projectiles") : e instanceof ItemEntity && flag("items");
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/hitbox.png");
    }
}
