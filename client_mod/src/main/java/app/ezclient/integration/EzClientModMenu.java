package app.ezclient.integration;

import app.ezclient.gui.EzClientScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Exposes the same module screen through Mod Menu's configure action. */
public final class EzClientModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EzClientScreen::new;
    }
}
