package dev.boltmc.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.boltmc.BoltMcAuthScreen;
import dev.boltmc.BoltMcConfig;
import dev.boltmc.friends.ChatRelay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Bolt Mc account: your Minecraft name (original or cracked) plus a password you choose.
 * The first time a name is used on the relay it gets registered; after that the password is needed to log in,
 * so nobody else can take your name.
 */
public final class AccountManager {
    private AccountManager() {}

    public enum Phase { IDLE, CHECKING, NEW_USER, EXISTING_USER, NO_SERVER, OFFLINE }
    public enum Exists { YES, NO, NOT_SIGNED_IN, ERROR }

    private static volatile Phase phase = Phase.IDLE;
    private static volatile boolean prompted = false;

    public static Phase phase() { return phase; }

    public static String name() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static boolean loggedIn() {
        BoltMcConfig cfg = BoltMcConfig.get();
        return cfg.secret != null && cfg.secret.length() >= 32
                && cfg.accountName != null && cfg.accountName.equalsIgnoreCase(name());
    }

    public static boolean hasServer() { return ChatRelay.baseUrl() != null; }

    public static boolean needsPrompt() {
        return !prompted && hasServer() && !loggedIn();
    }

    /** Opens the register / login screen shortly after the title screen appears. */
    public static void prompt(Screen title) {
        CompletableFuture.delayedExecutor(300, TimeUnit.MILLISECONDS).execute(() ->
                Minecraft.getInstance().execute(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (prompted || mc.screen != title) return;
                    prompted = true;
                    mc.setScreenAndShow(new BoltMcAuthScreen(title));
                }));
    }

    /** Asks the relay whether this name already has an account. */
    public static void check() {
        String base = ChatRelay.baseUrl();
        if (base == null) { phase = Phase.NO_SERVER; return; }
        phase = Phase.CHECKING;
        JsonObject body = new JsonObject();
        body.addProperty("name", name().toLowerCase(Locale.ROOT));
        ChatRelay.post(base + "/status", body).whenComplete((resp, err) -> {
            if (err != null || resp.statusCode() / 100 != 2) { phase = Phase.OFFLINE; return; }
            try {
                boolean reg = JsonParser.parseString(resp.body()).getAsJsonObject().get("registered").getAsBoolean();
                phase = reg ? Phase.EXISTING_USER : Phase.NEW_USER;
            } catch (Exception e) {
                phase = Phase.OFFLINE;
            }
        });
    }

    /** Registers (new name) or logs in (existing name). done gets null on success, otherwise an error text. */
    public static void submit(String password, Consumer<String> done) {
        String base = ChatRelay.baseUrl();
        if (base == null) { done.accept("No relay server is set."); return; }
        if (password.length() < 4) { done.accept("Use at least 4 characters."); return; }
        boolean register = phase == Phase.NEW_USER;
        String me = name().toLowerCase(Locale.ROOT);
        JsonObject body = new JsonObject();
        body.addProperty("name", me);
        body.addProperty("password", password);
        ChatRelay.post(base + (register ? "/register" : "/login"), body).whenComplete((resp, err) -> {
            String result;
            if (err != null) {
                result = "Can't reach the relay.";
            } else if (resp.statusCode() / 100 == 2) {
                try {
                    String token = JsonParser.parseString(resp.body()).getAsJsonObject().get("token").getAsString();
                    BoltMcConfig cfg = BoltMcConfig.get();
                    cfg.secret = token;
                    cfg.accountName = name();
                    BoltMcConfig.save();
                    result = null;
                } catch (Exception e) {
                    result = "Bad reply from the relay.";
                }
            } else if (resp.statusCode() == 409) {
                phase = Phase.EXISTING_USER;
                result = "This name already has an account. Enter its password.";
            } else if (resp.statusCode() == 401) {
                result = "Wrong password.";
            } else if (resp.statusCode() == 429) {
                result = "Too many tries. Wait a minute.";
            } else if (resp.statusCode() == 400) {
                result = "Password must be 4-64 characters.";
            } else {
                result = "Relay error " + resp.statusCode();
            }
            final String r = result;
            Minecraft.getInstance().execute(() -> done.accept(r));
        });
    }

    /** Does this player have a Bolt Mc account? (Friends must have the mod.) */
    public static void exists(String target, Consumer<Exists> done) {
        String base = ChatRelay.baseUrl();
        if (base == null || !loggedIn()) { done.accept(Exists.NOT_SIGNED_IN); return; }
        JsonObject body = new JsonObject();
        body.addProperty("name", name().toLowerCase(Locale.ROOT));
        body.addProperty("token", BoltMcConfig.get().secret);
        body.addProperty("target", target.toLowerCase(Locale.ROOT));
        ChatRelay.post(base + "/exists", body).whenComplete((resp, err) -> {
            Exists r;
            if (err != null) {
                r = Exists.ERROR;
            } else if (resp.statusCode() == 401) {
                invalidate();
                r = Exists.NOT_SIGNED_IN;
            } else if (resp.statusCode() / 100 != 2) {
                r = Exists.ERROR;
            } else {
                try {
                    boolean yes = JsonParser.parseString(resp.body()).getAsJsonObject().get("exists").getAsBoolean();
                    r = yes ? Exists.YES : Exists.NO;
                } catch (Exception e) {
                    r = Exists.ERROR;
                }
            }
            final Exists fr = r;
            Minecraft.getInstance().execute(() -> done.accept(fr));
        });
    }

    /** Forget the session (token rejected, or the player signed out). */
    public static void invalidate() {
        BoltMcConfig cfg = BoltMcConfig.get();
        cfg.secret = "";
        cfg.accountName = "";
        BoltMcConfig.save();
    }
}
