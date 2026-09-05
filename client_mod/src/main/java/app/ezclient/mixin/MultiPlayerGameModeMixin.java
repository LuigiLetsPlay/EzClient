package app.ezclient.mixin;

import app.ezclient.gui.ComboCounterModule;
import app.ezclient.gui.ModuleManager;
import app.ezclient.gui.ReachModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void ezclient$onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        if (target == null || player == null) return;
        app.ezclient.gui.FeatureModule.get(app.ezclient.gui.ParticleCustomizerModule.class).attack(target);

        ReachModule reachModule = ModuleManager.getInstance().getReachModule();
        if (reachModule != null && reachModule.isEnabled()) {
            Vec3 eyePos = player.getEyePosition();
            AABB box = target.getBoundingBox();

            double dist;
            if (reachModule.isRaytracePrecision()) {
                Vec3 look = player.getViewVector(1.0f);
                Vec3 reachVec = eyePos.add(look.scale(6.0));
                Optional<Vec3> hit = box.clip(eyePos, reachVec);
                dist = hit.map(eyePos::distanceTo).orElseGet(() -> eyePos.distanceTo(target.position().add(0, target.getEyeHeight(), 0)));
            } else {
                dist = eyePos.distanceTo(target.position().add(0, target.getEyeHeight(), 0));
            }

            ReachModule.recordReach(dist);
        }

        ComboCounterModule comboModule = ModuleManager.getInstance().getComboCounterModule();
        if (comboModule != null && comboModule.isEnabled() && target instanceof Player) {
            ComboCounterModule.onPlayerAttack();
        }
    }
}
