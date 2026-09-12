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

/** The vanilla cape and eight articulated strips share the same texture and pose. */
public final class WaveyCapeModel extends PlayerCapeModel {
    private static final int STRIPS = 8;
    private final ModelPart cape;
    private final ModelPart[] strips = new ModelPart[STRIPS];

    public WaveyCapeModel() {
        super(createLayer().bakeRoot());
        cape = root().getChild("body").getChild("cape");
        ModelPart strip = cape;
        for (int i = 0; i < STRIPS; i++) {
            strip = strip.getChild("wave_" + i);
            strips[i] = strip;
        }
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
        PartDefinition body = mesh.getRoot().clearRecursively().getChild("body");
        PartDefinition cape = body.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, CubeDeformation.NONE, 1.0F, 0.5F),
                PartPose.offsetAndRotation(0, 0, 2, 0, (float)Math.PI, 0));
        PartDefinition strip = cape;
        for (int i = 0; i < STRIPS; i++) {
            strip = strip.addOrReplaceChild("wave_" + i,
                    CubeListBuilder.create().texOffs(0, i * 2).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 2.0F, 1.0F, CubeDeformation.NONE, 1.0F, 0.5F),
                    i == 0 ? PartPose.ZERO : PartPose.offset(0, 2, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        WaveyCapesModule settings = FeatureModule.get(WaveyCapesModule.class);
        boolean enabled = settings.isEnabled()
                && (settings.flag("all_players") || isLocalPlayer(state.id))
                && !(settings.flag("elytra") && state.chestEquipment.is(net.minecraft.world.item.Items.ELYTRA));
        cape.skipDraw = enabled;
        strips[0].visible = enabled;
        if (!enabled) return;

        float strength = (float)settings.number("strength") * settings.styleMultiplier();
        float damping = (float)settings.number("damping");
        float amplitude = (float)settings.number("amplitude");
        float speed = (float)settings.number("speed");
        float movement = Math.min(16f, Math.abs(state.capeFlap) * .3f) * strength;
        float phase = state.ageInTicks * .16f * speed + state.id * .37f;
        for (int i = 0; i < STRIPS; i++) {
            float depth = (i + 1f) / STRIPS;
            float wind = settings.flag("wind") ? amplitude : 0f;
            float wave = (wind + movement) * depth *
                    (float)Math.sin(phase - i * (.55f + damping * .25f));
            strips[i].xRot = (float)Math.toRadians(wave * .5f);
            strips[i].zRot = (float)Math.toRadians(wave * .07f);
        }
    }

    private static boolean isLocalPlayer(int entityId) {
        var player = Minecraft.getInstance().player;
        return player != null && player.getId() == entityId;
    }
}
