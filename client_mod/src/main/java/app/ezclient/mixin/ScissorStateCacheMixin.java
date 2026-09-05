package app.ezclient.mixin;

import com.mojang.blaze3d.systems.ScissorState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Supplies value semantics so equal scissor states share Minecraft's render batches. */
@Mixin(ScissorState.class)
public abstract class ScissorStateCacheMixin {
    @Shadow private boolean enabled;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        ScissorStateCacheMixin state = (ScissorStateCacheMixin) other;
        return enabled == state.enabled
                && x == state.x
                && y == state.y
                && width == state.width
                && height == state.height;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(enabled);
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + width;
        return 31 * result + height;
    }
}
