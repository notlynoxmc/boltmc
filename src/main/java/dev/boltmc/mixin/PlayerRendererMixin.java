package dev.boltmc.mixin;

import dev.boltmc.cosmetics.CosmeticLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Bolt Mc cosmetic layer to the player renderer (normal and slim). */
@Mixin(AvatarRenderer.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class PlayerRendererMixin {

    @Shadow
    protected abstract boolean addLayer(RenderLayer layer);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void boltmc$addCosmeticLayer(CallbackInfo ci) {
        this.addLayer(new CosmeticLayer((RenderLayerParent) (Object) this));
    }
}
