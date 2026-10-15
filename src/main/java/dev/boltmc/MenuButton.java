package dev.boltmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the "Bolt Mc" button for the title screen and pause menu. It matches the width of the vanilla
 * button column, sits right under it and moves down if another mod already put buttons there.
 */
public final class MenuButton {
    private MenuButton() {}

    private static final int H = 20, GAP = 4;

    /** Returns the button, or null if the screen has no centered button column to attach to. */
    public static Button create(Screen screen) {
        int cx = screen.width / 2;
        int colTop = Integer.MAX_VALUE, colBottom = -1, colW = 0;
        List<int[]> rects = new ArrayList<>();
        for (GuiEventListener l : screen.children()) {
            if (!(l instanceof AbstractWidget w) || !w.visible) continue;
            rects.add(new int[]{w.getX(), w.getY(), w.getX() + w.getWidth(), w.getY() + w.getHeight()});
            if (w.getWidth() >= 90 && w.getWidth() <= 210 && Math.abs(w.getX() + w.getWidth() / 2 - cx) <= 60) {
                colTop = Math.min(colTop, w.getY());
                colBottom = Math.max(colBottom, w.getY() + w.getHeight());
                colW = Math.max(colW, w.getWidth());
            }
        }
        if (colBottom < 0) return null;

        int x = cx - colW / 2;
        int y = colBottom + GAP;
        for (int i = 0; i < 8; i++) { // step below anything already sitting there
            boolean moved = false;
            for (int[] r : rects) {
                if (x < r[2] && x + colW > r[0] && y < r[3] && y + H > r[1]) { y = r[3] + GAP; moved = true; }
            }
            if (!moved) break;
        }
        if (y + H > screen.height - 4) { // no room below: put it above the column instead
            y = colTop - H - GAP;
            if (y < 4) return null;
        }
        return Button.builder(Component.literal("Bolt Mc"),
                        b -> Minecraft.getInstance().setScreenAndShow(new BoltMcScreen(screen)))
                .bounds(x, y, colW, H).build();
    }
}
