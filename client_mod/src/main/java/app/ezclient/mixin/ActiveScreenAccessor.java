package app.ezclient.mixin;

import net.minecraft.client.Minecraft;
//? if >=26.2 {
import net.minecraft.client.gui.Gui;
//?}
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Writes the active screen without running screen lifecycle hooks. */
//? if >=26.2 {
@Mixin(Gui.class)
//?} else {
/*@Mixin(Minecraft.class)
*///?}
public interface ActiveScreenAccessor {
    @Accessor("screen")
    void ezclient$setActiveScreen(Screen screen);
}
