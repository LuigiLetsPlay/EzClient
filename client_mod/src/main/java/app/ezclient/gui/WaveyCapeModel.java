package app.ezclient.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** A single solid cape cuboid using vanilla UVs, with optional gentle motion. */
public final class WaveyCapeModel extends PlayerCapeModel {
    private final ModelPart cape;

    public WaveyCapeModel() {
        super(createLayer().bakeRoot());
        cape = root().getChild("body").getChild("cape");
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition body = mesh.getRoot().clearRecursively().getChild("body");
        body.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, CubeDeformation.NONE, 1.0F, 0.5F),
                PartPose.offsetAndRotation(0, 0, 2, 0, (float) Math.PI, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        WaveyCapesModule settings = FeatureModule.get(WaveyCapesModule.class);
        if (!settings.isEnabled()
                || (!settings.flag("all_players") && !isLocalPlayer(state.id))
                || (settings.flag("elytra") && state.chestEquipment.is(net.minecraft.world.item.Items.ELYTRA))) {
            return;
        }
        float strength = (float) settings.number("strength") * settings.styleMultiplier();
        float speed = (float) settings.number("speed");
        float wind = settings.flag("wind") ? (float) settings.number("amplitude") : 0f;
        float movement = Math.min(16f, Math.abs(state.capeFlap) * .3f);
        float phase = state.ageInTicks * .16f * speed + state.id * .37f;
        float sway = (wind + movement) * strength * (float) Math.sin(phase);
        cape.xRot += (float) Math.toRadians(sway * .5f);
        cape.zRot += (float) Math.toRadians(sway * .07f);
    }

    private static boolean isLocalPlayer(int entityId) {
        var player = Minecraft.getInstance().player;
        return player != null && player.getId() == entityId;
    }
}
