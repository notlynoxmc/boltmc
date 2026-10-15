package dev.boltmc.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets Bolt Mc call the (protected) addLayer of the vanilla living-entity renderer. */
@Mixin(LivingEntityRenderer.class)
@SuppressWarnings("rawtypes")
public interface LivingEntityRendererAccessor {
    @Invoker("addLayer")
    boolean boltmc$addLayer(RenderLayer layer);
}
