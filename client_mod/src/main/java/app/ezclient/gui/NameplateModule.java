package app.ezclient.gui;

public final class NameplateModule extends FeatureModule {
    public NameplateModule() {
        super("Nameplate Levelhead", false, 10);
        flag("own", "Own name in third person", true);
        option("health", "Health display", "HP", 0, 0, "HP", "Hearts", "Hidden");
        flag("nameBackground", "Name background", true); flag("nameShadow", "Name shadow", true);
        option("prefix", "Local clan / level prefix", "", 0, 0);
        option("friends", "Friends (comma-separated names)", "", 0, 0);
        colorOption("friendColor", "Friend accent", "FF22C96E");
    }
    public boolean friend(String name) {
        for (String value : text("friends").split(",")) if (value.trim().equalsIgnoreCase(name)) return true;
        return false;
    }
}
