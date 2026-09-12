package app.ezclient.gui;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Preset based, live-rendered crosshair with independently configurable parts. */
public class CrosshairModule extends HudModule {
    public static final int PAINT_SIZE = 21;
    private static final double[] CIRCLE_X = {1,.8660254,.5,0,-.5,-.8660254,-1,-.8660254,-.5,0,.5,.8660254};
    private static final double[] CIRCLE_Y = {0,.5,.8660254,1,.8660254,.5,0,-.5,-.8660254,-1,-.8660254,-.5};
    public enum CrosshairType { CLASSIC_CROSS, CUSTOM_CROSS, DOT, CIRCLE, T_SHAPE, CHEVRON }
    public enum CrosshairPreset { CLASSIC("Classic"), PLUS("Plus"), DOT("Dot"), CIRCLE("Circle"), CUSTOM("Custom"); private final String label; CrosshairPreset(String label){this.label=label;} public String getLabel(){return label;} }
    public enum CrosshairElement { TOP("Oben"), BOTTOM("Unten"), LEFT("Links"), RIGHT("Rechts"), CENTER("Mittelpunkt"); private final String label; CrosshairElement(String label){this.label=label;} public String getLabel(){return label;} }
    public enum ColorTarget { MAIN("Hauptfarbe"), OUTLINE("Randfarbe"), ELEMENT("Elementfarbe"), TARGET("Trefferfarbe"); private final String label; ColorTarget(String label){this.label=label;} public String getLabel(){return label;} }
    public enum TargetMode { OFF, ENTITIES, PLAYERS, HOSTILE, NEUTRAL, BLOCKS, ALL }
    private static final CrosshairElement[] ELEMENTS = CrosshairElement.values();
    private static final CrosshairPreset[] PRESETS = CrosshairPreset.values();
    private static final ColorTarget[] COLOR_TARGETS = ColorTarget.values();
    public record CrosshairTargetRule(int color, float scale, String type, String pattern) {
        public CrosshairTargetRule(int color, float scale, String type) {
            this(color, scale, type, "");
        }
    }
    private final java.util.Map<String, CrosshairTargetRule> targetRules = new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Part { boolean enabled; int length, thickness, x, y, color, alpha; boolean main; Part(boolean e,int l,int t,int x,int y,int c,int a,boolean m){enabled=e;length=l;thickness=t;this.x=x;this.y=y;color=c;alpha=a;main=m;} }
    private final Part[] parts = { new Part(true,5,1,0,0,0xFFFFFFFF,100,true), new Part(true,5,1,0,0,0xFFFFFFFF,100,true), new Part(true,5,1,0,0,0xFFFFFFFF,100,true), new Part(true,5,1,0,0,0xFFFFFFFF,100,true), new Part(false,2,2,0,0,0xFFFFFFFF,100,true) };
    private CrosshairType crosshairType = CrosshairType.CLASSIC_CROSS;
    private CrosshairPreset preset = CrosshairPreset.CLASSIC;
    private CrosshairElement selectedElement = CrosshairElement.TOP;
    private ColorTarget colorTarget = ColorTarget.MAIN;
    private boolean applyingPreset;
    private int gap=3, size=5, thickness=1, verticalSize=5, dotSize=2, opacity=100, outlineColor=0xFF000000;
    private boolean showDot, showOutline=true, dynamicSpread;
    private TargetMode targetMode=TargetMode.ALL;
    private int targetEntityColor=0xFFFF3333, targetPlayerColor=0xFF38BDF8, targetHostileColor=0xFFFF3333, targetNeutralColor=0xFFFFB020, targetBlockColor=0xFFFACC15;
    private float targetEntityScale=1.05f, targetPlayerScale=1.15f, targetHostileScale=1.2f, targetNeutralScale=1.1f;
    private boolean movementSpread=true, jumpSpread=true, cooldownSpread=true, hideOnBowZoom=true, hideInF3=true, hideInThirdPerson=true;
    private boolean centerOnMonitor;
    private final boolean[][] paintedPixels = new boolean[PAINT_SIZE][PAINT_SIZE];

    public CrosshairModule(){ super("Custom Crosshair", "RENDER", false, 0, 0, "", ""); setBorder(false); setBackground(false); showOutline = false; crosshairType = CrosshairType.CUSTOM_CROSS; preset = CrosshairPreset.CUSTOM; resetPaintPattern(false); }
    @Override public boolean hasBorder(){ return false; }
    @Override public boolean hasBackground(){ return false; }
    @Override public Identifier getIcon(){ return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/crosshair.png"); }
    private static int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private static float clampScale(float v){return Math.max(.5f,Math.min(2f,v));}
    private Part part(CrosshairElement e){return parts[(e==null?CrosshairElement.TOP:e).ordinal()];}
    private void custom(){if(!applyingPreset)preset=CrosshairPreset.CUSTOM;}
    private void save(){ConfigManager.save();}

    public CrosshairType getCrosshairType(){return crosshairType;}
    public void setCrosshairType(CrosshairType v){crosshairType=v==null?CrosshairType.CLASSIC_CROSS:v;custom();save();}
    public String getCrosshairTypeLabel(){return switch(crosshairType){case CLASSIC_CROSS->"Kreuz";case CUSTOM_CROSS->"Custom";case DOT->"Punkt";case CIRCLE->"Kreis";case T_SHAPE->"T-Form";case CHEVRON->"Chevron";};}
    public void cycleCrosshairType(int direction){CrosshairType[] types={CrosshairType.CLASSIC_CROSS,CrosshairType.DOT,CrosshairType.CIRCLE,CrosshairType.T_SHAPE,CrosshairType.CHEVRON,CrosshairType.CUSTOM_CROSS};int index=0;for(int i=0;i<types.length;i++)if(types[i]==crosshairType)index=i;setCrosshairType(types[Math.floorMod(index+direction,types.length)]);}
    public CrosshairPreset getPreset(){return preset;}
    public String getPresetLabel(){return preset.getLabel();}
    public void cyclePreset(int direction){setPreset(PRESETS[Math.floorMod(preset.ordinal()+direction,PRESETS.length)]);}
    public void setPreset(CrosshairPreset value){
        applyingPreset=true;preset=value==null?CrosshairPreset.CLASSIC:value;
        switch(preset){
            case CLASSIC->applyLayout(CrosshairType.CLASSIC_CROSS,3,5,1,2,false,true);
            case PLUS->applyLayout(CrosshairType.CLASSIC_CROSS,0,6,2,2,false,true);
            case DOT->applyLayout(CrosshairType.DOT,0,3,1,3,true,false);
            case CIRCLE->applyLayout(CrosshairType.CIRCLE,3,8,1,2,false,false);
            case CUSTOM->crosshairType=CrosshairType.CUSTOM_CROSS;
        }
        applyingPreset=false;save();
    }
    /** Config restore intentionally does not overwrite the saved fine tuning. */
    public void restorePreset(CrosshairPreset value){preset=value==null?CrosshairPreset.CLASSIC:value;crosshairType=switch(preset){case CLASSIC,PLUS->CrosshairType.CLASSIC_CROSS;case DOT->CrosshairType.DOT;case CIRCLE->CrosshairType.CIRCLE;case CUSTOM->CrosshairType.CUSTOM_CROSS;};}
    private void applyLayout(CrosshairType type,int newGap,int newSize,int newThickness,int newDot,boolean center,boolean lines){crosshairType=type;gap=newGap;size=newSize;verticalSize=newSize;thickness=newThickness;dotSize=newDot;opacity=100;showDot=center;showOutline=true;dynamicSpread=false;setPart(CrosshairElement.TOP,lines,newSize,newThickness,0,0,100,true);setPart(CrosshairElement.BOTTOM,lines,newSize,newThickness,0,0,100,true);setPart(CrosshairElement.LEFT,lines,newSize,newThickness,0,0,100,true);setPart(CrosshairElement.RIGHT,lines,newSize,newThickness,0,0,100,true);setPart(CrosshairElement.CENTER,center,newDot,newDot,0,0,100,true);}
    private void setPart(CrosshairElement e,boolean enabled,int length,int lineThickness,int x,int y,int alpha,boolean main){Part p=part(e);p.enabled=enabled;p.length=clamp(length,1,32);p.thickness=clamp(lineThickness,1,8);p.x=clamp(x,-32,32);p.y=clamp(y,-32,32);p.alpha=clamp(alpha,0,100);p.main=main;}

    public CrosshairElement getSelectedElement(){return selectedElement;}
    public String getSelectedElementLabel(){return selectedElement.getLabel();}
    public void cycleSelectedElement(int direction){selectedElement=ELEMENTS[Math.floorMod(selectedElement.ordinal()+direction,ELEMENTS.length)];}
    public boolean isElementEnabled(CrosshairElement e){return part(e).enabled;}
    public void setElementEnabled(CrosshairElement e,boolean v){part(e).enabled=v;custom();save();}
    public int getElementLength(CrosshairElement e){return part(e).length;}
    public void setElementLength(CrosshairElement e,int v){part(e).length=clamp(v,1,32);custom();save();}
    public int getElementThickness(CrosshairElement e){return part(e).thickness;}
    public void setElementThickness(CrosshairElement e,int v){part(e).thickness=clamp(v,1,8);custom();save();}
    public int getElementOffsetX(CrosshairElement e){return part(e).x;}
    public void setElementOffsetX(CrosshairElement e,int v){part(e).x=clamp(v,-32,32);custom();save();}
    public int getElementOffsetY(CrosshairElement e){return part(e).y;}
    public void setElementOffsetY(CrosshairElement e,int v){part(e).y=clamp(v,-32,32);custom();save();}
    public int getElementColor(CrosshairElement e){return part(e).color;}
    public void setElementColor(CrosshairElement e,int v){Part p=part(e);p.color=v;p.main=false;save();}
    public int getElementAlpha(CrosshairElement e){return part(e).alpha;}
    public void setElementAlpha(CrosshairElement e,int v){part(e).alpha=clamp(v,0,100);save();}
    public boolean isElementUsingMainColor(CrosshairElement e){return part(e).main;}
    public void setElementUsingMainColor(CrosshairElement e,boolean v){part(e).main=v;save();}
    public ColorTarget getColorTarget(){return colorTarget;}
    public String getColorTargetLabel(){return colorTarget.getLabel();}
    public void cycleColorTarget(int direction){colorTarget=COLOR_TARGETS[Math.floorMod(colorTarget.ordinal()+direction,COLOR_TARGETS.length)];}
    public void selectElementColor(CrosshairElement e){selectedElement=e==null?CrosshairElement.TOP:e;colorTarget=ColorTarget.ELEMENT;}

    public int getGap(){return gap;} public void setGap(int v){gap=clamp(v,0,15);custom();save();}
    public int getSize(){return size;} public void setSize(int v){size=clamp(v,2,32);custom();save();}
    public int getThickness(){return thickness;} public void setThickness(int v){thickness=clamp(v,1,8);custom();save();}
    public int getVerticalSize(){return verticalSize;} public void setVerticalSize(int v){verticalSize=clamp(v,2,32);custom();save();}
    public int getDotSize(){return dotSize;} public void setDotSize(int v){dotSize=clamp(v,1,12);custom();save();}
    public int getOpacity(){return opacity;} public void setOpacity(int v){opacity=clamp(v,0,100);save();}
    public boolean isShowDot(){return showDot;} public void setShowDot(boolean v){showDot=v;save();}
    public boolean isShowOutline(){return showOutline;} public void setShowOutline(boolean v){showOutline=v;save();}
    public int getOutlineColor(){return outlineColor;} public void setOutlineColor(int v){outlineColor=v;save();}
    public boolean isDynamicSpread(){return dynamicSpread;} public void setDynamicSpread(boolean v){dynamicSpread=v;save();}
    public TargetMode getTargetMode(){return targetMode;} public void setTargetMode(TargetMode v){targetMode=v==null?TargetMode.OFF:v;save();}
    public boolean isTargetHighlight(){return targetMode!=TargetMode.OFF;} public void setTargetHighlight(boolean v){setTargetMode(v?TargetMode.ALL:TargetMode.OFF);}
    public int getTargetEntityColor(){return targetEntityColor;} public void setTargetEntityColor(int v){targetEntityColor=v;save();}
    public int getTargetPlayerColor(){return targetPlayerColor;} public void setTargetPlayerColor(int v){targetPlayerColor=v;save();}
    public int getTargetHostileColor(){return targetHostileColor;} public void setTargetHostileColor(int v){targetHostileColor=v;save();}
    public int getTargetNeutralColor(){return targetNeutralColor;} public void setTargetNeutralColor(int v){targetNeutralColor=v;save();}
    public int getTargetBlockColor(){return targetBlockColor;} public void setTargetBlockColor(int v){targetBlockColor=v;save();}
    public float getTargetEntityScale(){return targetEntityScale;} public void setTargetEntityScale(float v){targetEntityScale=clampScale(v);save();}
    public float getTargetPlayerScale(){return targetPlayerScale;} public void setTargetPlayerScale(float v){targetPlayerScale=clampScale(v);save();}
    public float getTargetHostileScale(){return targetHostileScale;} public void setTargetHostileScale(float v){targetHostileScale=clampScale(v);save();}
    public float getTargetNeutralScale(){return targetNeutralScale;} public void setTargetNeutralScale(float v){targetNeutralScale=clampScale(v);save();}
    public boolean isMovementSpread(){return movementSpread;} public void setMovementSpread(boolean v){movementSpread=v;save();}
    public boolean isJumpSpread(){return jumpSpread;} public void setJumpSpread(boolean v){jumpSpread=v;save();}
    public boolean isCooldownSpread(){return cooldownSpread;} public void setCooldownSpread(boolean v){cooldownSpread=v;save();}
    public boolean isHideOnBowZoom(){return hideOnBowZoom;} public void setHideOnBowZoom(boolean v){hideOnBowZoom=v;save();}
    public boolean isHideInF3(){return hideInF3;} public void setHideInF3(boolean v){hideInF3=v;save();}
    public boolean isHideInThirdPerson(){return hideInThirdPerson;} public void setHideInThirdPerson(boolean v){hideInThirdPerson=v;save();}
    public boolean isCenterOnMonitor(){return centerOnMonitor;} public void setCenterOnMonitor(boolean v){centerOnMonitor=v;save();}

    public boolean isPainted(int x,int y){return x>=0&&x<PAINT_SIZE&&y>=0&&y<PAINT_SIZE&&paintedPixels[y][x];}
    public void setPainted(int x,int y,boolean value){if(x<0||x>=PAINT_SIZE||y<0||y>=PAINT_SIZE)return;paintedPixels[y][x]=value;crosshairType=CrosshairType.CUSTOM_CROSS;preset=CrosshairPreset.CUSTOM;}
    public void clearPaint(){for(boolean[] row:paintedPixels)java.util.Arrays.fill(row,false);crosshairType=CrosshairType.CUSTOM_CROSS;preset=CrosshairPreset.CUSTOM;}
    public void resetPaintPattern(boolean persist){clearPaint();int c=PAINT_SIZE/2;paintedPixels[c][c]=true;for(int i=3;i<=5;i++){paintedPixels[c-i][c]=true;paintedPixels[c+i][c]=true;paintedPixels[c][c-i]=true;paintedPixels[c][c+i]=true;}if(persist)save();}
    public void finishPainting(){crosshairType=CrosshairType.CUSTOM_CROSS;preset=CrosshairPreset.CUSTOM;save();}
    public String getPaintPattern(){StringBuilder out=new StringBuilder(PAINT_SIZE*PAINT_SIZE);for(boolean[] row:paintedPixels)for(boolean pixel:row)out.append(pixel?'1':'0');return out.toString();}
    public void setPaintPattern(String value){if(value==null||value.length()!=PAINT_SIZE*PAINT_SIZE)return;for(int y=0;y<PAINT_SIZE;y++)for(int x=0;x<PAINT_SIZE;x++)paintedPixels[y][x]=value.charAt(y*PAINT_SIZE+x)=='1';}

    /** Shared colour picker entry point. */
    public int getSelectedRuleColor(){return switch(colorTarget){case MAIN->getTextColor();case OUTLINE->outlineColor;case ELEMENT->getElementColor(selectedElement);case TARGET->targetColor();};}
    public void setSelectedRuleColor(int color){switch(colorTarget){case MAIN->setTextColor(color);case OUTLINE->setOutlineColor(color);case ELEMENT->setElementColor(selectedElement,color);case TARGET->setTargetColor(color);}}
    private int targetColor(){return switch(targetMode){case PLAYERS->targetPlayerColor;case HOSTILE->targetHostileColor;case NEUTRAL->targetNeutralColor;case BLOCKS->targetBlockColor;case ENTITIES,ALL,OFF->targetEntityColor;};}
    private void setTargetColor(int color){switch(targetMode){case PLAYERS->setTargetPlayerColor(color);case HOSTILE->setTargetHostileColor(color);case NEUTRAL->setTargetNeutralColor(color);case BLOCKS->setTargetBlockColor(color);case ENTITIES,ALL,OFF->setTargetEntityColor(color);}}
    public float getSelectedRuleScale(){return switch(targetMode){case PLAYERS->targetPlayerScale;case HOSTILE->targetHostileScale;case NEUTRAL->targetNeutralScale;default->targetEntityScale;};}
    public void setSelectedRuleScale(float v){switch(targetMode){case PLAYERS->setTargetPlayerScale(v);case HOSTILE->setTargetHostileScale(v);case NEUTRAL->setTargetNeutralScale(v);default->setTargetEntityScale(v);}}
    public boolean isCustomColor(){return isTargetHighlight();} public void setCustomColor(boolean v){setTargetHighlight(v);} public int getDefaultColor(){return getTextColor();} public void setDefaultColor(int v){setTextColor(v);} public int getBlockColor(){return targetBlockColor;} public void setBlockColor(int v){setTargetBlockColor(v);} public int getEntityColor(){return targetEntityColor;} public void setEntityColor(int v){setTargetEntityColor(v);} public boolean isDynamicForm(){return dynamicSpread;} public void setDynamicForm(boolean v){setDynamicSpread(v);}

    public void writeElementSettings(JsonObject out){for(CrosshairElement e:ELEMENTS){Part p=part(e);JsonObject o=new JsonObject();o.addProperty("enabled",p.enabled);o.addProperty("length",p.length);o.addProperty("thickness",p.thickness);o.addProperty("offsetX",p.x);o.addProperty("offsetY",p.y);o.addProperty("color",p.color);o.addProperty("alpha",p.alpha);o.addProperty("useMainColor",p.main);out.add(e.name(),o);}}
    public void readElementSettings(JsonObject in){if(in==null)return;applyingPreset=true;try{for(CrosshairElement e:ELEMENTS){if(!in.has(e.name())||!in.get(e.name()).isJsonObject())continue;JsonObject o=in.getAsJsonObject(e.name());Part p=part(e);if(o.has("enabled"))p.enabled=o.get("enabled").getAsBoolean();if(o.has("length"))p.length=clamp(o.get("length").getAsInt(),1,32);if(o.has("thickness"))p.thickness=clamp(o.get("thickness").getAsInt(),1,8);if(o.has("offsetX"))p.x=clamp(o.get("offsetX").getAsInt(),-32,32);if(o.has("offsetY"))p.y=clamp(o.get("offsetY").getAsInt(),-32,32);if(o.has("color"))p.color=o.get("color").getAsInt();if(o.has("alpha"))p.alpha=clamp(o.get("alpha").getAsInt(),0,100);if(o.has("useMainColor"))p.main=o.get("useMainColor").getAsBoolean();}}catch(RuntimeException ignored){}finally{applyingPreset=false;}}

    public java.util.Map<String, CrosshairTargetRule> getTargetRules() { return targetRules; }
    public CrosshairTargetRule getTargetRule(String targetId) { return targetRules.get(targetId); }
    public void setTargetRule(String targetId, CrosshairTargetRule rule) { if (rule == null) targetRules.remove(targetId); else targetRules.put(targetId, rule); save(); }
    public void removeTargetRule(String targetId) { targetRules.remove(targetId); save(); }
    public void clearTargetRules() { targetRules.clear(); save(); }

    public static boolean[][] parsePattern(String patternStr) {
        boolean[][] grid = new boolean[PAINT_SIZE][PAINT_SIZE];
        if (patternStr != null && patternStr.length() == PAINT_SIZE * PAINT_SIZE) {
            for (int y = 0; y < PAINT_SIZE; y++) {
                for (int x = 0; x < PAINT_SIZE; x++) {
                    grid[y][x] = patternStr.charAt(y * PAINT_SIZE + x) == '1';
                }
            }
        }
        return grid;
    }

    public CrosshairTargetRule getRuleForTarget(String prefix, Identifier id) {
        if (id == null) return null;
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        String full = id.toString().toLowerCase(java.util.Locale.ROOT);

        CrosshairTargetRule r = targetRules.get(prefix + ":" + full);
        if (r != null) return r;
        r = targetRules.get(prefix + ":" + path);
        if (r != null) return r;
        r = targetRules.get(full);
        if (r != null) return r;
        r = targetRules.get(path);
        if (r != null) return r;
        return null;
    }

    public CrosshairTargetRule getActiveCustomRule(Minecraft client) {
        if (client == null) return null;
        Entity e = client.crosshairPickEntity;
        if (e == null && client.hitResult instanceof net.minecraft.world.phys.EntityHitResult eHit) {
            e = eHit.getEntity();
        }
        if (e != null) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
            CrosshairTargetRule rule = getRuleForTarget("entity", id);
            if (rule != null) return rule;
        }

        HitResult hit = client.hitResult;
        if ((hit == null || hit.getType() == HitResult.Type.MISS) && client.getCameraEntity() != null && client.level != null) {
            try {
                hit = client.getCameraEntity().pick(20.0, 0.0f, false);
            } catch (Throwable ignored) {}
        }
        if (hit instanceof BlockHitResult bHit && bHit.getType() == HitResult.Type.BLOCK && client.level != null) {
            var blockState = client.level.getBlockState(bHit.getBlockPos());
            if (!blockState.isAir()) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                CrosshairTargetRule rule = getRuleForTarget("block", id);
                if (rule != null) return rule;
            }
        }
        return null;
    }

    @Override public int getWidth(Minecraft client){return 64;} @Override public int getHeight(Minecraft client){return 64;} @Override protected String value(Minecraft client){return "";}
    @Override public void resetSettings(){super.resetSettings();resetPaintPattern(false);clearTargetRules();setPreset(CrosshairPreset.CLASSIC);targetMode=TargetMode.ALL;targetEntityColor=0xFFFF3333;targetPlayerColor=0xFF38BDF8;targetHostileColor=0xFFFF3333;targetNeutralColor=0xFFFFB020;targetBlockColor=0xFFFACC15;targetEntityScale=1.05f;targetPlayerScale=1.15f;targetHostileScale=1.2f;targetNeutralScale=1.1f;movementSpread=jumpSpread=cooldownSpread=true;centerOnMonitor=false;colorTarget=ColorTarget.MAIN;selectedElement=CrosshairElement.TOP;save();}
    public int getEffectiveColor(Minecraft client,boolean editor){int color=(getColorMode()==ColorMode.RAINBOW||getColorMode()==ColorMode.WAVE)?color():getTextColor();if(!editor&&targetActive(client)){CrosshairTargetRule custom=getActiveCustomRule(client);if(custom!=null&&custom.color()!=0){color=custom.color();}else if(getColorMode()==ColorMode.SOLID){Entity e=client.crosshairPickEntity;if(e instanceof Player)color=targetPlayerColor;else if(e instanceof Enemy)color=targetHostileColor;else if(e instanceof NeutralMob)color=targetNeutralColor;else if(e!=null)color=targetEntityColor;else if(client.hitResult!=null&&client.hitResult.getType()==HitResult.Type.BLOCK)color=targetBlockColor;}}return opacity(color,opacity);}
    public float getEffectiveScale(Minecraft client,boolean editor){if(editor||!targetActive(client))return 1f;CrosshairTargetRule custom=getActiveCustomRule(client);if(custom!=null&&custom.scale()>0){return custom.scale();}Entity e=client.crosshairPickEntity;if(e instanceof Player)return targetPlayerScale;if(e instanceof Enemy)return targetHostileScale;if(e instanceof NeutralMob)return targetNeutralScale;return e!=null?targetEntityScale:1f;}
    private boolean targetActive(Minecraft c){if(c==null||c.hitResult==null)return false;if(getActiveCustomRule(c)!=null)return true;if(targetMode==TargetMode.OFF)return false;if(c.hitResult.getType()==HitResult.Type.BLOCK)return targetMode==TargetMode.BLOCKS||targetMode==TargetMode.ALL;if(c.hitResult.getType()!=HitResult.Type.ENTITY)return false;Entity e=c.crosshairPickEntity;return switch(targetMode){case PLAYERS->e instanceof Player;case HOSTILE->e instanceof Enemy;case NEUTRAL->e instanceof NeutralMob;case ENTITIES,ALL->e!=null;default->false;};}
    private static int opacity(int color,int percent){int a=((color>>>24)&255)*clamp(percent,0,100)/100;return(color&0x00FFFFFF)|(a<<24);}
    public int getEffectiveGap(Minecraft c,boolean editor){int g=gap;if(!editor&&dynamicSpread&&c!=null&&c.player!=null){if(movementSpread&&c.player.getDeltaMovement().horizontalDistanceSqr()>.0025)g+=2;if(jumpSpread&&!c.player.onGround())g+=3;if(cooldownSpread&&c.player.getAttackStrengthScale(0)<.98f)g+=2;}return g;}
    public void renderCrosshair(GuiGraphicsExtractor g,Minecraft c,float cx,float cy,boolean editor){int main=getEffectiveColor(c,editor),gap=getEffectiveGap(c,editor);boolean hit=!editor&&targetActive(c);g.pose().pushMatrix();g.pose().translate(cx,cy);float scale=getEffectiveScale(c,editor);g.pose().scale(scale,scale);CrosshairType activeType=crosshairType;String customPattern=null;if(!editor&&hit){CrosshairTargetRule custom=getActiveCustomRule(c);if(custom!=null&&custom.type()!=null&&!custom.type().isBlank()){if(custom.type().equalsIgnoreCase("CUSTOM")){activeType=CrosshairType.CUSTOM_CROSS;customPattern=custom.pattern();}else if(!custom.type().equalsIgnoreCase("AUTO")&&!custom.type().equalsIgnoreCase("DEFAULT")){try{activeType=CrosshairType.valueOf(custom.type());}catch(Exception ignored){}}}}switch(activeType){case CLASSIC_CROSS->{element(g,CrosshairElement.TOP,main,gap,hit);element(g,CrosshairElement.BOTTOM,main,gap,hit);element(g,CrosshairElement.LEFT,main,gap,hit);element(g,CrosshairElement.RIGHT,main,gap,hit);if(showDot)element(g,CrosshairElement.CENTER,main,gap,hit);}case CUSTOM_CROSS->painted(g,main,customPattern,hit);case DOT->dot(g,main,hit);case CIRCLE->circle(g,main,gap);case T_SHAPE->legacyT(g,main,gap);case CHEVRON->chevron(g,main,gap);}g.pose().popMatrix();}
    public void painted(GuiGraphicsExtractor g,int color){painted(g,color,null,false);}
    public void painted(GuiGraphicsExtractor g,int color,String customPattern,boolean hit){int center=PAINT_SIZE/2;boolean[][] pattern=(customPattern!=null&&customPattern.length()==PAINT_SIZE*PAINT_SIZE)?parsePattern(customPattern):paintedPixels;if(showOutline){int outline=opacity(outlineColor,opacity);for(int y=0;y<PAINT_SIZE;y++)for(int x=0;x<PAINT_SIZE;x++)if(pattern[y][x])g.fill(x-center-1,y-center-1,x-center+2,y-center+2,outline);}for(int y=0;y<PAINT_SIZE;y++)for(int x=0;x<PAINT_SIZE;x++)if(pattern[y][x]){int pixelColor=color;if(!hit){if(getColorMode()==ColorMode.RAINBOW)pixelColor=color((x+y)*45L);else if(getColorMode()==ColorMode.WAVE)pixelColor=color((x+y)*35L);}g.fill(x-center,y-center,x-center+1,y-center+1,pixelColor);}}
    private void element(GuiGraphicsExtractor g,CrosshairElement e,int main,int gap,boolean hit){Part p=part(e);if(!p.enabled)return;int color=p.main||hit?main:opacity(p.color,opacity);float half=p.thickness/2f;switch(e){case TOP->rect(g,-half+p.x,-gap-p.length+p.y,p.thickness,p.length,color,p.alpha);case BOTTOM->rect(g,-half+p.x,gap+p.y,p.thickness,p.length,color,p.alpha);case LEFT->rect(g,-gap-p.length+p.x,-half+p.y,p.length,p.thickness,color,p.alpha);case RIGHT->rect(g,gap+p.x,-half+p.y,p.length,p.thickness,color,p.alpha);case CENTER->rect(g,-p.length/2f+p.x,-p.thickness/2f+p.y,p.length,p.thickness,color,p.alpha);}}
    private void dot(GuiGraphicsExtractor g,int main,boolean hit){Part p=part(CrosshairElement.CENTER);if(!p.enabled)return;int color=p.main||hit?main:opacity(p.color,opacity);rect(g,-dotSize/2f+p.x,-dotSize/2f+p.y,dotSize,dotSize,color,p.alpha);}
    private void circle(GuiGraphicsExtractor g,int color,int gap){int radius=gap+size/2;float half=thickness/2f;for(int i=0;i<CIRCLE_X.length;i++)rect(g,(float)(CIRCLE_X[i]*radius)-half,(float)(CIRCLE_Y[i]*radius)-half,thickness,thickness,color,100);if(showDot)element(g,CrosshairElement.CENTER,color,gap,false);}
    private void legacyT(GuiGraphicsExtractor g,int color,int gap){float half=thickness/2f;rect(g,-half,gap,thickness,size,color,100);rect(g,-gap-size,-half,size,thickness,color,100);rect(g,gap,-half,size,thickness,color,100);}
    private void chevron(GuiGraphicsExtractor g,int color,int gap){for(int i=0;i<size;i++){rect(g,-gap-i-thickness,gap+i,thickness,thickness,color,100);rect(g,gap+i,gap+i,thickness,thickness,color,100);}}
    private void rect(GuiGraphicsExtractor g,float x,float y,int w,int h,int color,int alpha){if(w<=0||h<=0||alpha<=0)return;g.pose().pushMatrix();g.pose().translate(x,y);if(showOutline)g.fill(-1,-1,w+1,h+1,opacity(opacity(outlineColor,opacity),alpha));g.fill(0,0,w,h,opacity(color,alpha));g.pose().popMatrix();}
    public void renderCustom(GuiGraphicsExtractor g,Minecraft c,boolean editor){if(editor)renderCrosshair(g,c,getX()+32f,getY()+32f,true);}
}
