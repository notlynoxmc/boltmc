package dev.boltmc.mixin;

import dev.boltmc.MenuButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Bolt Mc button to the in-game pause menu. */
@Mixin(value = PauseScreen.class, priority = 1500)
public abstract class PauseScreenMixin extends Screen {
    protected PauseScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void boltmc$addButton(CallbackInfo ci) {
        Button b = MenuButton.create(this);
        if (b != null) this.addRenderableWidget(b);
    }
}
