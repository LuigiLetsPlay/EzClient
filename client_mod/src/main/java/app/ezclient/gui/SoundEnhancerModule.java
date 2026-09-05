package app.ezclient.gui;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public final class SoundEnhancerModule extends FeatureModule implements SoundEventListener {
    private record Cue(String name, Vec3 position, long expires, boolean important) {}
    private final Deque<Cue> cues = new ArrayDeque<>();
    private Object lastLevel;
    @Override public synchronized void onTick() {
        Object level = Minecraft.getInstance().level;
        if (level != lastLevel || !isEnabled()) { cues.clear(); lastLevel = level; }
    }
    public SoundEnhancerModule() {
        super("Sound Subtitles Enhancer", true, 180);
        flag("arrows", "Directional arrows", true);
        option("highlight", "Highlight sound IDs containing", "tnt,anvil,ender_pearl", 0, 0);
        option("whitelist", "Only sound IDs containing (empty=all)", "", 0, 0);
        colorOption("highlightColor", "Highlight color", "FFFFAA33");
        option("rainVolume", "Rain volume", 1.0, 0, 2); option("stepVolume", "Footstep volume", 1.0, 0, 2);
        option("customSound", "Custom sound ID contains", "", 0, 0); option("customVolume", "Custom sound volume", 1.0, 0, 2);
        option("duration", "Subtitle seconds", 3.0, 1, 10);
    }
    private boolean matches(String id, String list) {
        for (String part : list.split(",")) if (!part.isBlank() && id.contains(part.trim().toLowerCase(Locale.ROOT))) return true;
        return false;
    }
    @Override public synchronized void onPlaySound(SoundInstance sound, WeighedSoundEvents event, float range) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || sound.isRelative() || event.getSubtitle() == null) return;
        String id = sound.getIdentifier().toString();
        if (!text("whitelist").isBlank() && !matches(id, text("whitelist"))) return;
        Vec3 pos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        if (pos.distanceToSqr(mc.player.position()) > range * range) return;
        String name = event.getSubtitle().getString();
        cues.removeIf(cue -> cue.name().equals(name) && cue.position().distanceToSqr(pos) < 4);
        while (cues.size() >= 16) cues.removeFirst();
        cues.addLast(new Cue(name, pos, System.currentTimeMillis() + (long)(number("duration") * 1000), matches(id, text("highlight"))));
    }
    public float volume(String id) {
        if (!isEnabled()) return 1;
        if (!text("customSound").isBlank() && matches(id, text("customSound"))) return (float)number("customVolume");
        if (id.contains("rain")) return (float)number("rainVolume");
        return id.contains("step") ? (float)number("stepVolume") : 1;
    }
    @Override public synchronized List<String> lines(Minecraft mc, boolean editor) {
        if (editor) return List.of("< Footsteps", "! > TNT primed");
        if (mc.level == null) { cues.clear(); return List.of(); }
        cues.removeIf(cue -> cue.expires() < System.currentTimeMillis());
        return cues.stream().map(c -> (c.important() ? "! " : "") + (flag("arrows") ? WaypointsModule.direction(mc, c.position()) + " " : "") + c.name()).toList();
    }
    @Override public net.minecraft.network.chat.Component styledText(String text) {
        var component = super.styledText(text);
        return text.startsWith("!") ? component.copy().withStyle(style -> style.withColor(tint("highlightColor", false) & 0xffffff)) : component;
    }

    @Override
    public Identifier getIcon() {
        return Identifier.fromNamespaceAndPath("ezclient", "textures/icons/sound.png");
    }
}
