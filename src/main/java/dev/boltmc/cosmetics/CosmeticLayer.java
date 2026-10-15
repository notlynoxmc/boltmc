package dev.boltmc.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.boltmc.BoltMcClient;
import dev.boltmc.BoltMcConfig;
import dev.boltmc.Cosmetic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/** Draws the 3D cosmetics (wings, helmet, hats, halo) on your own player. Look at yourself with F5. */
public class CosmeticLayer<M extends EntityModel<AvatarRenderState>> extends RenderLayer<AvatarRenderState, M> {

    private static final Identifier WHITE = id("textures/entity/white.png");
    private static final Identifier WINGS = id("textures/entity/wings.png");
    private static final Identifier HELMET = id("textures/entity/helmet.png");
    private static final float DEG = (float) (Math.PI / 180.0);
    private static boolean logged = false;

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("boltmc", path);
    }

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, M> parent) {
        super(parent);
        BoltMcClient.LOGGER.info("Bolt Mc: cosmetic layer attached to a player renderer.");
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.id != mc.player.getId() || state.isInvisible) return;
        if (!logged) { logged = true; BoltMcClient.LOGGER.info("Bolt Mc: cosmetic layer is rendering your player."); }

        BoltMcConfig cfg = BoltMcConfig.get();
        if (!cfg.enabled) return;

        boolean wings = cfg.on(Cosmetic.WINGS);
        boolean helmet = cfg.on(Cosmetic.CROWN);
        boolean plain = cfg.on(Cosmetic.HALO) || cfg.on(Cosmetic.CAP) || cfg.on(Cosmetic.PROPELLER)
                || cfg.on(Cosmetic.TOP_HAT) || cfg.on(Cosmetic.CAT_EARS);
        if (!wings && !helmet && !plain) return;

        long ms = System.currentTimeMillis();
        Xf body = new Xf();
        Xf head = new Xf().rotZYX(0, yRot * DEG, xRot * DEG);

        if (wings) {
            var type = this.getParentModel().renderType(WINGS);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                    CosmeticModels.wings(new Mesh(buffer, pose, light), body, cfg.rgb(Cosmetic.WINGS), ms));
        }
        if (helmet) {
            var type = this.getParentModel().renderType(HELMET);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                    CosmeticModels.helmet(new Mesh(buffer, pose, light), head, cfg.rgb(Cosmetic.CROWN)));
        }
        if (plain) {
            var type = this.getParentModel().renderType(WHITE);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                Mesh m = new Mesh(buffer, pose, light);
                if (cfg.on(Cosmetic.CAP)) CosmeticModels.cap(m, head, cfg.rgb(Cosmetic.CAP));
                if (cfg.on(Cosmetic.PROPELLER)) CosmeticModels.propeller(m, head, cfg.rgb(Cosmetic.PROPELLER), ms);
                if (cfg.on(Cosmetic.TOP_HAT)) CosmeticModels.topHat(m, head, cfg.rgb(Cosmetic.TOP_HAT));
                if (cfg.on(Cosmetic.CAT_EARS)) CosmeticModels.catEars(m, head, cfg.rgb(Cosmetic.CAT_EARS));
                if (cfg.on(Cosmetic.HALO)) {
                    float h = cfg.on(Cosmetic.TOP_HAT) ? -17.5f
                            : cfg.on(Cosmetic.PROPELLER) ? -15.5f
                            : cfg.on(Cosmetic.CROWN) ? -16.0f : -12.5f;
                    CosmeticModels.halo(m, head, cfg.rgb(Cosmetic.HALO), ms, h);
                }
            });
        }
    }
}
