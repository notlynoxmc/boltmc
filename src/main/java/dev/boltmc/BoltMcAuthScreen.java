package dev.boltmc;

import dev.boltmc.account.AccountManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Register (new name) or log in (existing name). Only a password is asked; the name is your in-game name. */
public class BoltMcAuthScreen extends Screen {

    private final Screen parent;
    private EditBox input;
    private Button go, skip;
    private String error = "";
    private boolean busy = false;
    private int px1, py1, px2, py2;

    public BoltMcAuthScreen(Screen parent) {
        super(Component.literal("Bolt Mc account"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AccountManager.check();
        int cx = this.width / 2, cy = this.height / 2;
        px1 = cx - 140; px2 = cx + 140; py1 = cy - 85; py2 = cy + 85;

        input = new EditBox(this.font, cx - 100, py1 + 82, 200, 16, Component.empty());
        input.setMaxLength(64);
        addWidget(input);
        setInitialFocus(input);

        go = Button.builder(Component.literal("Continue"), b -> submit()).bounds(cx - 100, py1 + 108, 200, 20).build();
        addRenderableWidget(go);
        skip = Button.builder(Component.literal("Skip for now"), b -> onClose()).bounds(cx - 100, py1 + 132, 200, 20).build();
        addRenderableWidget(skip);
    }

    private void submit() {
        if (busy) return;
        AccountManager.Phase p = AccountManager.phase();
        if (p != AccountManager.Phase.NEW_USER && p != AccountManager.Phase.EXISTING_USER) return;
        busy = true;
        error = "";
        AccountManager.submit(input.getValue(), err -> {
            busy = false;
            if (err == null) {
                this.minecraft.setScreenAndShow(parent);
            } else {
                error = err;
                input.setValue("");
            }
        });
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (k == 257 || k == 335) { submit(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return AccountManager.phase() == AccountManager.Phase.OFFLINE;
    }

    private void label(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(this.font, s, x, y, color, false);
    }

    private void centered(GuiGraphicsExtractor g, String s, int y, int color) {
        label(g, s, this.width / 2 - this.font.width(s) / 2, y, color);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.fill(0, 0, this.width, this.height, UiTheme.BG);
        UiTheme.box(g, px1, py1, px2, py2, UiTheme.PANEL, UiTheme.LINE);
        g.fill(px1 + 1, py1 + 1, px2 - 1, py1 + 26, UiTheme.HEADER);
        g.fill(px1 + 1, py1 + 25, px2 - 1, py1 + 26, UiTheme.LINE);
        centered(g, UiTheme.BOLD + "Bolt Mc account", py1 + 9, UiTheme.TEXT);

        AccountManager.Phase p = AccountManager.phase();
        centered(g, "Playing as " + AccountManager.name(), py1 + 34, UiTheme.TEXT);

        String info;
        int infoColor = UiTheme.SUB;
        switch (p) {
            case NEW_USER -> info = "New here! Choose a password to protect your name.";
            case EXISTING_USER -> info = "Welcome back! Enter your password to log in.";
            case OFFLINE -> { info = "Can't reach the relay right now."; infoColor = 0xFFCC5500; }
            case NO_SERVER -> { info = "No relay server is set (see server/README.md)."; infoColor = 0xFFCC5500; }
            default -> info = "Checking your name...";
        }
        centered(g, info, py1 + 50, infoColor);
        if (!error.isEmpty()) centered(g, error, py1 + 64, 0xFFE5484D);

        // password field, drawn by hand (masked) like the other Bolt Mc fields
        int fx1 = this.width / 2 - 100, fx2 = fx1 + 200, fy = py1 + 82;
        UiTheme.box(g, fx1, fy, fx2, fy + 16, UiTheme.FIELD, input.isFocused() ? UiTheme.BLUE : UiTheme.BORDER);
        String masked = "*".repeat(input.getValue().length());
        if (masked.isEmpty() && !input.isFocused()) {
            label(g, "Password", fx1 + 4, fy + 4, UiTheme.SUB);
        } else {
            String shown = masked;
            while (!shown.isEmpty() && this.font.width(shown) > fx2 - fx1 - 12) shown = shown.substring(1);
            label(g, shown, fx1 + 4, fy + 4, UiTheme.TEXT);
            if (input.isFocused() && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cxp = fx1 + 4 + this.font.width(shown);
                g.fill(cxp, fy + 3, cxp + 1, fy + 13, UiTheme.TEXT);
            }
        }

        boolean ready = p == AccountManager.Phase.NEW_USER || p == AccountManager.Phase.EXISTING_USER;
        go.active = ready && !busy;
        go.setMessage(Component.literal(busy ? "Please wait..." : p == AccountManager.Phase.NEW_USER ? "Register" : "Log in"));
        skip.visible = p == AccountManager.Phase.OFFLINE || p == AccountManager.Phase.NO_SERVER;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(parent);
    }
}
