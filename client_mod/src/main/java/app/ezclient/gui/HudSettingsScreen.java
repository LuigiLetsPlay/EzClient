package app.ezclient.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudSettingsScreen extends Screen {
    private final Screen parent; private final HudModule module; private int x, y;
    public HudSettingsScreen(Screen parent, HudModule module) { super(Component.literal(module.getName())); this.parent=parent; this.module=module; }
    @Override protected void init() {
        x=width/2-145; y=height/2-100;
        EditBox prefix = new EditBox(font, x+95, y+38, 178, 20, Component.literal("Prefix"));
        prefix.setValue(module.getPrefix()); prefix.setResponder(module::setPrefix); addRenderableWidget(prefix);
        EditBox suffix = new EditBox(font, x+95, y+66, 178, 20, Component.literal("Suffix"));
        suffix.setValue(module.getSuffix()); suffix.setResponder(module::setSuffix); addRenderableWidget(suffix);
        addRenderableWidget(new EzSlider(x+95,y+94,178,22,(module.getScale()-0.5)/2.5,v->module.setScale(0.5+v*2.5),
                v->Component.literal(String.format(java.util.Locale.ROOT,"Size %.1fx",0.5+v*2.5))));
        addRenderableWidget(new EzButton(x+16,y+128,78,20,Component.literal(module.isEnabled()?"Enabled":"Disabled"),module.isEnabled(),b->{module.toggle();rebuildWidgets();}));
        addRenderableWidget(new EzButton(x+104,y+128,78,20,Component.literal(module.isRainbow()?"Rainbow ON":"Rainbow OFF"),module.isRainbow(),b->{module.setRainbow(!module.isRainbow());rebuildWidgets();}));
        addRenderableWidget(new EzButton(x+192,y+128,82,20,Component.literal(module.hasBackground()?"Background":"No background"),module.hasBackground(),b->{module.setBackground(!module.hasBackground());rebuildWidgets();}));
        addRenderableWidget(new EzButton(x+16,y+152,78,20,Component.literal(module.hasBorder()?"Border ON":"Border OFF"),module.hasBorder(),b->{module.setBorder(!module.hasBorder());rebuildWidgets();}));
        addRenderableWidget(new EzButton(x+104,y+152,78,20,Component.literal("Purple"),true,b->{module.setTextColor(0xFFC4B5FD);module.setBorderColor(0xFFA78BFA);rebuildWidgets();}));
        addRenderableWidget(new EzButton(x+192,y+152,82,20,Component.literal("White"),false,b->{module.setTextColor(0xFFFFFFFF);module.setBorderColor(0xFF35414D);rebuildWidgets();}));
        addRenderableWidget(new EzButton(x+196,y+190,78,19,Component.literal("Back"),false,b->onClose()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        EzUi.panel(g,x,y,290,224);
        EzUi.roundedRect(g,x+10,y+10,270,24,9,0xFF252B37);
        g.text(font,module.getName()+" HUD",x+16,y+18,0xFFC4B5FD);
        g.text(font,"Text before",x+16,y+44,0xFF89939E); g.text(font,"Text after",x+16,y+72,0xFF89939E);
        g.text(font,"Preview: "+module.displayText(minecraft),x+16,y+204,0xFFE8EDF1);
        super.extractRenderState(g,mx,my,d);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){ConfigManager.save();minecraft.gui.setScreen(parent);}
}
