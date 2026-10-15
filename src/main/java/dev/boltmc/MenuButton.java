package dev.boltmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Builds the "Bolt Mc" button that the title screen and pause menu mixins add under the main buttons. */
public final class MenuButton {
    private MenuButton() {}

    /** Returns the button, or null if the screen has no centered button column to attach to. */
    public static Button create(Screen screen) {
        int cx = screen.width / 2;
        int bottom = -1;
        for (GuiEventListener l : screen.children()) {
            if (l instanceof AbstractWidget w
                    && w.getWidth() >= 90 && w.getWidth() <= 210
                    && Math.abs(w.getX() + w.getWidth() / 2 - cx) <= 60) {
                bottom = Math.max(bottom, w.getY() + w.getHeight());
            }
        }
        if (bottom < 0) return null;
        int y = Math.min(bottom + 4, screen.height - 24);
        return Button.builder(Component.literal("Bolt Mc"),
                        b -> Minecraft.getInstance().setScreenAndShow(new BoltMcScreen(screen)))
                .bounds(cx - 100, y, 200, 20).build();
    }
}
