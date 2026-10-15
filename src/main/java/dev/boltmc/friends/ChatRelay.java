package dev.boltmc.friends;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.boltmc.BoltMcConfig;
import dev.boltmc.account.AccountManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Text chat between friends, through a small Bolt Mc relay server (see server/README.md).
 * Everyone is identified by player name, so original and cracked accounts both work.
 * Messages wait in the relay until the recipient's game asks for them, then they are deleted.
 *
 * The game checks for new messages every 20 seconds, and every 3 seconds while a chat window is open.
 */
public final class ChatRelay {
    private ChatRelay() {}

    private static final long SLOW_MS = 20_000;
    private static final long FAST_MS = 3_000;

    private static final Map<String, List<ChatMessage>> HISTORY = new ConcurrentHashMap<>();
    private static volatile String status = "Not set up";
    private static volatile long fastUntil = 0;
    private static long lastPoll = 0;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    public static void start() {
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BoltMc-chat");
            t.setDaemon(true);
            return t;
        });
        ex.scheduleWithFixedDelay(() -> {
            try { Minecraft.getInstance().execute(ChatRelay::tick); } catch (Throwable ignored) { }
        }, 3, 1, TimeUnit.SECONDS);
    }

    /** Called by the chat window while it is open: check for messages more often. */
    public static void touch() {
        fastUntil = System.currentTimeMillis() + 6000;
    }

    public static String status() { return status; }

    public static List<ChatMessage> historyWith(String friendId) {
        return HISTORY.computeIfAbsent(friendId.toLowerCase(Locale.ROOT), k -> new CopyOnWriteArrayList<>());
    }

    private static String myName() {
        return Minecraft.getInstance().getUser().getName().toLowerCase(Locale.ROOT);
    }

    private static void tick() {
        long now = System.currentTimeMillis();
        long interval = now < fastUntil ? FAST_MS : SLOW_MS;
        if (now - lastPoll < interval) return;
        lastPoll = now;
        poll();
    }

    /** Adds the message locally and sends it to the relay, so the friend picks it up on their next check. */
    public static void send(String friendId, String text) {
        if (text == null || text.isBlank()) return;
        text = text.length() > 240 ? text.substring(0, 240) : text;

        String me = myName();
        String to = friendId.toLowerCase(Locale.ROOT);
        historyWith(to).add(new ChatMessage(me, text, System.currentTimeMillis(), true));

        String base = baseUrl();
        if (base == null) { status = "Not set up"; return; }
        if (!AccountManager.loggedIn()) { status = "Not signed in"; return; }

        JsonObject body = new JsonObject();
        body.addProperty("from", me);
        body.addProperty("to", to);
        body.addProperty("token", BoltMcConfig.get().secret);
        body.addProperty("text", text);
        post(base + "/send", body).whenComplete((resp, err) -> {
            if (err != null) {
                status = "Relay unreachable";
            } else if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                AccountManager.invalidate();
                status = "Not signed in";
            } else if (resp.statusCode() == 404) {
                status = "That player doesn't have Bolt Mc";
            } else if (resp.statusCode() / 100 != 2) {
                status = "Relay error " + resp.statusCode();
            } else {
                status = "Connected";
            }
        });
    }

    private static void poll() {
        String base = baseUrl();
        if (base == null) { status = "Not set up"; return; }
        if (!AccountManager.loggedIn()) { status = "Not signed in"; return; }

        JsonObject body = new JsonObject();
        body.addProperty("name", myName());
        body.addProperty("token", BoltMcConfig.get().secret);

        post(base + "/inbox", body).whenComplete((resp, err) -> {
            if (err != null) { status = "Relay unreachable"; return; }
            if (resp.statusCode() == 401 || resp.statusCode() == 403) { AccountManager.invalidate(); status = "Not signed in"; return; }
            if (resp.statusCode() / 100 != 2) { status = "Relay error " + resp.statusCode(); return; }
            try {
                JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                JsonArray arr = root.has("messages") ? root.getAsJsonArray("messages") : new JsonArray();
                status = "Connected";
                if (arr.isEmpty()) return;

                // handled on the game thread (friends list, chat message)
                Minecraft.getInstance().execute(() -> {
                    for (JsonElement el : arr) {
                        JsonObject m = el.getAsJsonObject();
                        String from = m.get("from").getAsString();
                        String text = m.get("text").getAsString();
                        long at = m.has("at") ? m.get("at").getAsLong() : System.currentTimeMillis();
                        FriendsManager.addIncoming(from);
                        historyWith(from).add(new ChatMessage(from, text, at, false));
                        notifyChat("[Bolt Mc] " + from + ": " + text);
                    }
                });
            } catch (Exception e) {
                status = "Bad relay reply";
            }
        });
    }

    /** Shows a line in the normal game chat. Looked up by name at runtime so it can never break the build. */
    private static void notifyChat(String line) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        try {
            mc.player.getClass().getMethod("sendSystemMessage", Component.class)
                    .invoke(mc.player, Component.literal(line));
        } catch (Throwable ignored) {
            // no notification, the message is still in the friend's chat window
        }
    }

    public static String baseUrl() {
        BoltMcConfig cfg = BoltMcConfig.get();
        String u = (cfg.serverUrl == null || cfg.serverUrl.isBlank()
                ? BoltMcConfig.DEFAULT_SERVER_URL : cfg.serverUrl).trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (!(u.startsWith("http://") || u.startsWith("https://")) || u.length() < 10) return null;
        return u;
    }

    public static CompletableFuture<HttpResponse<String>> post(String url, JsonObject body) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString());
    }
}
