package app.ezclient.gui;

import net.minecraft.resources.Identifier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class AutoSprintModule extends Module {

    public AutoSprintModule() {
        super("AutoSprint", "MOVEMENT", false);
    }
    @Override
    public Identifier getIcon() { return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/auto_sprint.png"); }


    @Override
    public void onTick() {
        super.onTick();
        Minecraft client = Minecraft.getInstance();
        if (isEnabled() && client.player != null) {
            if (client.options.keyUp.isDown() && !client.player.isSprinting() && !client.player.isCrouching() && !client.player.horizontalCollision && client.player.getFoodData().getFoodLevel() > 6) {
                client.player.setSprinting(true);
            }
        }
    }
}
