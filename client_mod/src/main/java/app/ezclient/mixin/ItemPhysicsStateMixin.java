package app.ezclient.mixin;
import app.ezclient.gui.ItemPhysicsState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(ItemEntityRenderState.class)
public class ItemPhysicsStateMixin implements ItemPhysicsState {
    @Unique private boolean ezclient$enabled;
    @Unique private float ezclient$pitch, ezclient$yaw, ezclient$lift;
    public void ezclient$physics(boolean active, float pitch, float yaw, float lift) { ezclient$enabled = active; ezclient$pitch = pitch; ezclient$yaw = yaw; ezclient$lift = lift; }
    public boolean ezclient$active() { return ezclient$enabled; }
    public float ezclient$pitch() { return ezclient$pitch; }
    public float ezclient$yaw() { return ezclient$yaw; }
    public float ezclient$lift() { return ezclient$lift; }
}
