package dev.boltmc.mixin;

import dev.boltmc.MenuButton;
import dev.boltmc.account.AccountManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Bolt Mc button to the main menu. */
@Mixin(value = TitleScreen.class, priority = 1500)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void boltmc$addButton(CallbackInfo ci) {
        Button b = MenuButton.create(this);
        if (b != null) this.addRenderableWidget(b);
        if (AccountManager.needsPrompt()) AccountManager.prompt(this); // first launch: ask for register / login
    }
}
