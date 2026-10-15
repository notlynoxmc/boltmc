package dev.boltmc.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.boltmc.BoltMcConfig;
import dev.boltmc.Cosmetic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/** Draws the 3D cosmetics (wings, halo, hats) on your own player. */
public class CosmeticLayer<M extends EntityModel<AvatarRenderState>> extends RenderLayer<AvatarRenderState, M> {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("boltmc", "textures/entity/white.png");
    private static final float DEG = (float) (Math.PI / 180.0);

    public CosmeticLayer(RenderLayerParent<AvatarRenderState, M> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || state.id != mc.player.getId() || state.isInvisible) return;

        BoltMcConfig cfg = BoltMcConfig.get();
        if (!cfg.enabled) return;
        boolean any = false;
        for (Cosmetic c : Cosmetic.values()) {
            if (c != Cosmetic.AURA && cfg.on(c)) { any = true; break; }
        }
        if (!any) return;

        var renderType = this.getParentModel().renderType(WHITE);
        long ms = System.currentTimeMillis();

        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            Mesh m = new Mesh(buffer, pose, light);
            Xf body = new Xf();
            Xf head = new Xf().rotZYX(0, yRot * DEG, xRot * DEG);

            if (cfg.on(Cosmetic.WINGS)) CosmeticModels.wings(m, body, cfg.rgb(Cosmetic.WINGS), ms);
            if (cfg.on(Cosmetic.CROWN)) CosmeticModels.crown(m, head, cfg.rgb(Cosmetic.CROWN));
            if (cfg.on(Cosmetic.CAP)) CosmeticModels.cap(m, head, cfg.rgb(Cosmetic.CAP));
            if (cfg.on(Cosmetic.PROPELLER)) CosmeticModels.propeller(m, head, cfg.rgb(Cosmetic.PROPELLER), ms);
            if (cfg.on(Cosmetic.TOP_HAT)) CosmeticModels.topHat(m, head, cfg.rgb(Cosmetic.TOP_HAT));
            if (cfg.on(Cosmetic.CAT_EARS)) CosmeticModels.catEars(m, head, cfg.rgb(Cosmetic.CAT_EARS));
            if (cfg.on(Cosmetic.HALO)) {
                float h = cfg.on(Cosmetic.TOP_HAT) ? -17.5f
                        : cfg.on(Cosmetic.PROPELLER) ? -15.5f
                        : cfg.on(Cosmetic.CROWN) ? -14.5f : -12.5f;
                CosmeticModels.halo(m, head, cfg.rgb(Cosmetic.HALO), ms, h);
            }
        });
    }
}
