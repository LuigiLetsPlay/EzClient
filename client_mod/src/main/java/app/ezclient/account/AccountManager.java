package app.ezclient.account;

import app.ezclient.EzClientMod;
import app.ezclient.mixin.MinecraftAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages saved accounts, live session switching, session refreshing,
 * and synchronization with the EzClient launcher.
 */
public final class AccountManager {
    public static final String MICROSOFT_CLIENT_ID = "00000000402b5328";
    public static final String MICROSOFT_REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    public static final String MICROSOFT_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    public static final String MICROSOFT_AUTH_URL =
            "https://login.live.com/oauth20_authorize.srf"
                    + "?client_id=" + MICROSOFT_CLIENT_ID
                    + "&response_type=code"
                    + "&redirect_uri=" + URLEncoder.encode(MICROSOFT_REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(MICROSOFT_SCOPE, StandardCharsets.UTF_8)
                    + "&prompt=select_account";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "EzClient-AccountWorker");
        thread.setDaemon(true);
        return thread;
    });

    private static final Object LOCK = new Object();

    private AccountManager() {}

    public static Path getAccountsFile() {
        return EzClientMod.getEzClientDataDir().resolve("accounts.json");
    }

    public static Path getAuthCacheFile() {
        return EzClientMod.getEzClientDataDir().resolve("auth_cache.json");
    }

    public static Map<String, EzAccount> loadAccounts() {
        synchronized (LOCK) {
            Map<String, EzAccount> accounts = new LinkedHashMap<>();
            Path accountsFile = getAccountsFile();
            Path cacheFile = getAuthCacheFile();

            if (Files.isRegularFile(accountsFile)) {
                try {
                    String content = Files.readString(accountsFile, StandardCharsets.UTF_8);
                    JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            EzAccount acc = EzAccount.fromJson(entry.getValue().getAsJsonObject());
                            accounts.put(acc.getUuid(), acc);
                        }
                    }
                } catch (Exception e) {
                    EzClientMod.log("Failed to load accounts.json: " + e.getMessage());
                }
            }

            // If accounts.json is missing or doesn't have active account, check auth_cache.json
            if (Files.isRegularFile(cacheFile)) {
                try {
                    String content = Files.readString(cacheFile, StandardCharsets.UTF_8);
                    JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                    EzAccount cached = EzAccount.fromJson(obj);
                    if (!cached.getUuid().isEmpty() && !accounts.containsKey(cached.getUuid())) {
                        accounts.put(cached.getUuid(), cached);
                        saveAccounts(accounts);
                    }
                } catch (Exception ignored) {}
            }

            // If still empty, seed with current in-game user
            if (accounts.isEmpty() && Minecraft.getInstance() != null && Minecraft.getInstance().getUser() != null) {
                User user = Minecraft.getInstance().getUser();
                String hexUuid = user.getProfileId() != null ? user.getProfileId().toString().replace("-", "") : UUID.randomUUID().toString().replace("-", "");
                EzAccount current = new EzAccount(
                        hexUuid,
                        user.getName(),
                        user.getAccessToken() != null ? user.getAccessToken() : "0",
                        "",
                        0L,
                        user.getAccessToken() != null && !user.getAccessToken().equals("0") && user.getAccessToken().length() > 10,
                        "",
                        "",
                        "default",
                        "msa"
                );
                accounts.put(current.getUuid(), current);
                saveAccounts(accounts);
                saveActiveSession(current);
            }

            return accounts;
        }
    }

    public static List<EzAccount> getAccountList() {
        Map<String, EzAccount> map = loadAccounts();
        String activeUuid = getActiveUuid();
        List<EzAccount> list = new ArrayList<>();
        for (EzAccount acc : map.values()) {
            if (acc.isOnline()) {
                list.add(acc);
            }
        }
        list.sort((a, b) -> {
            boolean aActive = a.getUuid().equalsIgnoreCase(activeUuid);
            boolean bActive = b.getUuid().equalsIgnoreCase(activeUuid);
            if (aActive != bActive) return aActive ? -1 : 1;
            return a.getUsername().compareToIgnoreCase(b.getUsername());
        });
        return list;
    }

    public static void saveAccounts(Map<String, EzAccount> map) {
        synchronized (LOCK) {
            try {
                Path file = getAccountsFile();
                Files.createDirectories(file.getParent());
                JsonObject root = new JsonObject();
                for (Map.Entry<String, EzAccount> entry : map.entrySet()) {
                    root.add(entry.getKey(), entry.getValue().toJson());
                }
                Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                EzClientMod.log("Failed to save accounts.json: " + e.getMessage());
            }
        }
    }

    public static void saveActiveSession(EzAccount account) {
        synchronized (LOCK) {
            try {
                Path file = getAuthCacheFile();
                Files.createDirectories(file.getParent());
                Files.writeString(file, account.toJson().toString(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                EzClientMod.log("Failed to save auth_cache.json: " + e.getMessage());
            }
        }
    }

    public static String getActiveUuid() {
        Path cacheFile = getAuthCacheFile();
        if (Files.isRegularFile(cacheFile)) {
            try {
                String content = Files.readString(cacheFile, StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
                if (obj.has("uuid")) {
                    return EzAccount.normalizeUuid(obj.get("uuid").getAsString());
                }
            } catch (Exception ignored) {}
        }
        if (Minecraft.getInstance() != null && Minecraft.getInstance().getUser() != null) {
            UUID id = Minecraft.getInstance().getUser().getProfileId();
            if (id != null) {
                return EzAccount.normalizeUuid(id.toString());
            }
        }
        return "";
    }

    public static boolean isActive(EzAccount account) {
        if (account == null) return false;
        String active = getActiveUuid();
        return !active.isEmpty() && active.equalsIgnoreCase(account.getUuid());
    }

    public static void applySessionToMinecraft(Minecraft mc, EzAccount account) {
        if (mc == null || account == null) return;
        try {
            User newUser = account.toMinecraftUser();
            ((MinecraftAccessor) mc).ezclient$setUser(newUser);

            CompletableFuture<ProfileResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return mc.services().sessionService().fetchProfile(newUser.getProfileId(), true);
                } catch (Throwable t) {
                    return null;
                }
            }, net.minecraft.util.Util.nonCriticalIoPool());

            ((MinecraftAccessor) mc).ezclient$setProfileFuture(future);
            EzClientMod.log("Applied active session to Minecraft: " + account.getUsername() + " (" + account.getUuid() + ")");
        } catch (Throwable t) {
            EzClientMod.log("Error applying session to Minecraft: " + t.getMessage());
        }
    }

    public static void switchAccount(Minecraft mc, EzAccount account, Consumer<String> onDone) {
        EXECUTOR.execute(() -> {
            try {
                // If expired and has refresh token, refresh first
                long now = System.currentTimeMillis() / 1000L;
                if (account.isOnline() && !account.getRefreshToken().isEmpty() && account.getExpiresAt() <= now + 300L) {
                    refreshSessionInternal(mc, account, result -> {
                        if (result.success) {
                            saveActiveSession(account);
                            mc.execute(() -> {
                                applySessionToMinecraft(mc, account);
                                onDone.accept("Zu " + account.getUsername() + " gewechselt (Session erneuert).");
                            });
                        } else {
                            // Still switch even if refresh failed (offline fallback)
                            saveActiveSession(account);
                            mc.execute(() -> {
                                applySessionToMinecraft(mc, account);
                                onDone.accept("Zu " + account.getUsername() + " gewechselt (Offline-Modus).");
                            });
                        }
                    });
                    return;
                }

                saveActiveSession(account);
                mc.execute(() -> {
                    applySessionToMinecraft(mc, account);
                    onDone.accept("Zu " + account.getUsername() + " gewechselt.");
                });
            } catch (Exception e) {
                mc.execute(() -> onDone.accept("Fehler beim Wechseln: " + e.getMessage()));
            }
        });
    }

    public static void refreshSession(Minecraft mc, EzAccount account, Consumer<String> onDone) {
        EXECUTOR.execute(() -> {
            if (!account.isOnline()) {
                mc.execute(() -> onDone.accept("Offline-Accounts benötigen keine Session-Erneuerung."));
                return;
            }
            if (account.getRefreshToken().isEmpty()) {
                mc.execute(() -> onDone.accept("Kein Refresh-Token vorhanden. Bitte erneut anmelden."));
                return;
            }

            refreshSessionInternal(mc, account, result -> {
                if (result.success) {
                    Map<String, EzAccount> map = loadAccounts();
                    map.put(account.getUuid(), account);
                    saveAccounts(map);

                    if (isActive(account)) {
                        saveActiveSession(account);
                        mc.execute(() -> applySessionToMinecraft(mc, account));
                    }
                    mc.execute(() -> onDone.accept("Session für " + account.getUsername() + " erfolgreich erneuert!"));
                } else {
                    mc.execute(() -> onDone.accept("Fehler: " + result.message));
                }
            });
        });
    }

    public static void removeAccount(Minecraft mc, String uuid, Consumer<String> onDone) {
        EXECUTOR.execute(() -> {
            try {
                String cleanUuid = EzAccount.normalizeUuid(uuid);
                Map<String, EzAccount> map = loadAccounts();
                if (!map.containsKey(cleanUuid)) {
                    mc.execute(() -> onDone.accept("Account nicht gefunden."));
                    return;
                }

                boolean wasActive = cleanUuid.equalsIgnoreCase(getActiveUuid());
                EzAccount removed = map.remove(cleanUuid);
                saveAccounts(map);

                if (wasActive) {
                    if (!map.isEmpty()) {
                        EzAccount next = map.values().iterator().next();
                        saveActiveSession(next);
                        mc.execute(() -> applySessionToMinecraft(mc, next));
                    } else {
                        Path cache = getAuthCacheFile();
                        try { Files.deleteIfExists(cache); } catch (Exception ignored) {}
                    }
                }

                mc.execute(() -> onDone.accept("Account " + (removed != null ? removed.getUsername() : "") + " entfernt."));
            } catch (Exception e) {
                mc.execute(() -> onDone.accept("Fehler beim Entfernen: " + e.getMessage()));
            }
        });
    }

    public static void addOfflineAccount(Minecraft mc, String username, Consumer<String> onDone) {
        EXECUTOR.execute(() -> {
            String name = username != null ? username.trim() : "";
            if (name.isEmpty() || !name.matches("^[a-zA-Z0-9_]{3,16}$")) {
                mc.execute(() -> onDone.accept("Ungültiger Minecraft-Name (3-16 Zeichen, a-z, 0-9, _)."));
                return;
            }

            UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            String hexUuid = EzAccount.normalizeUuid(offlineUuid.toString());

            EzAccount account = new EzAccount(
                    hexUuid,
                    name,
                    "0",
                    "",
                    0L,
                    false,
                    "",
                    "",
                    "default",
                    "offline"
            );

            Map<String, EzAccount> map = loadAccounts();
            map.put(account.getUuid(), account);
            saveAccounts(map);
            saveActiveSession(account);

            mc.execute(() -> {
                applySessionToMinecraft(mc, account);
                onDone.accept("Offline-Account " + name + " hinzugefügt und aktiviert.");
            });
        });
    }

    public static void addMicrosoftAccount(Minecraft mc, String codeOrUrl, Consumer<String> onDone) {
        EXECUTOR.execute(() -> {
            try {
                String input = codeOrUrl != null ? codeOrUrl.trim() : "";
                String code = input;
                if (input.contains("code=")) {
                    Matcher m = Pattern.compile("code=([^&]+)").matcher(input);
                    if (m.find()) {
                        code = m.group(1);
                    }
                }

                if (code.isEmpty() || code.length() < 10) {
                    mc.execute(() -> onDone.accept("Ungültiger Autorisierungs-Code oder Link."));
                    return;
                }

                // 1. Exchange auth code for MSA token
                String tokenUrl = "https://login.live.com/oauth20_token.srf";
                String payload = "client_id=" + MICROSOFT_CLIENT_ID
                        + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                        + "&grant_type=authorization_code"
                        + "&redirect_uri=" + URLEncoder.encode(MICROSOFT_REDIRECT_URI, StandardCharsets.UTF_8)
                        + "&scope=" + URLEncoder.encode(MICROSOFT_SCOPE, StandardCharsets.UTF_8);

                JsonObject msaResp = httpPostForm(tokenUrl, payload);
                if (msaResp == null || !msaResp.has("access_token")) {
                    mc.execute(() -> onDone.accept("Microsoft OAuth-Austausch fehlgeschlagen. Code evtl. abgelaufen?"));
                    return;
                }

                String msaToken = msaResp.get("access_token").getAsString();
                String refreshToken = msaResp.has("refresh_token") ? msaResp.get("refresh_token").getAsString() : "";

                // 2. Exchange to Minecraft Session
                SessionResult sessionResult = exchangeMsaTokenToMinecraft(msaToken, refreshToken);
                if (sessionResult == null) {
                    mc.execute(() -> onDone.accept("Xbox Live / Minecraft-Anmeldung fehlgeschlagen."));
                    return;
                }

                EzAccount account = new EzAccount(
                        sessionResult.uuid,
                        sessionResult.username,
                        sessionResult.accessToken,
                        sessionResult.refreshToken,
                        sessionResult.expiresAt,
                        true,
                        sessionResult.skinUrl,
                        sessionResult.capeUrl,
                        sessionResult.skinModel,
                        "msa"
                );

                Map<String, EzAccount> map = loadAccounts();
                map.put(account.getUuid(), account);
                saveAccounts(map);
                saveActiveSession(account);

                mc.execute(() -> {
                    applySessionToMinecraft(mc, account);
                    onDone.accept("Microsoft-Account " + account.getUsername() + " erfolgreich hinzugefügt!");
                });
            } catch (Exception e) {
                mc.execute(() -> onDone.accept("Fehler beim Hinzufügen: " + e.getMessage()));
            }
        });
    }

    private static void refreshSessionInternal(Minecraft mc, EzAccount account, Consumer<RefreshResult> callback) {
        try {
            String tokenUrl = "https://login.live.com/oauth20_token.srf";
            String payload = "client_id=" + MICROSOFT_CLIENT_ID
                    + "&refresh_token=" + URLEncoder.encode(account.getRefreshToken(), StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token"
                    + "&scope=" + URLEncoder.encode(MICROSOFT_SCOPE, StandardCharsets.UTF_8);

            JsonObject msaResp = httpPostForm(tokenUrl, payload);
            if (msaResp == null || !msaResp.has("access_token")) {
                callback.accept(new RefreshResult(false, "Microsoft Refresh-Token ungültig."));
                return;
            }

            String msaToken = msaResp.get("access_token").getAsString();
            String newRefresh = msaResp.has("refresh_token") ? msaResp.get("refresh_token").getAsString() : account.getRefreshToken();

            SessionResult res = exchangeMsaTokenToMinecraft(msaToken, newRefresh);
            if (res == null) {
                callback.accept(new RefreshResult(false, "Xbox/Mojang-Session konnte nicht erneuert werden."));
                return;
            }

            account.setUsername(res.username);
            account.setAccessToken(res.accessToken);
            account.setRefreshToken(res.refreshToken);
            account.setExpiresAt(res.expiresAt);
            account.setSkinUrl(res.skinUrl);
            account.setCapeUrl(res.capeUrl);
            account.setSkinModel(res.skinModel);
            account.setOnline(true);

            callback.accept(new RefreshResult(true, "OK"));
        } catch (Exception e) {
            callback.accept(new RefreshResult(false, e.getMessage()));
        }
    }

    private static SessionResult exchangeMsaTokenToMinecraft(String msaToken, String refreshToken) {
        try {
            // 1. Xbox Live User Auth
            JsonObject xblBody = new JsonObject();
            JsonObject xblProps = new JsonObject();
            xblProps.addProperty("AuthMethod", "RPS");
            xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
            xblProps.addProperty("RpsTicket", msaToken);
            xblBody.add("Properties", xblProps);
            xblBody.addProperty("RelyingParty", "http://auth.xboxlive.com");
            xblBody.addProperty("TokenType", "JWT");

            JsonObject xblResp = httpPostJson("https://user.auth.xboxlive.com/user/authenticate", xblBody.toString());
            if (xblResp == null || !xblResp.has("Token")) return null;
            String xblToken = xblResp.get("Token").getAsString();
            String uhs = xblResp.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

            // 2. XSTS Token for Minecraft Services
            JsonObject xstsBody = new JsonObject();
            JsonObject xstsProps = new JsonObject();
            xstsProps.addProperty("SandboxId", "RETAIL");
            JsonArray userTokens = new JsonArray();
            userTokens.add(xblToken);
            xstsProps.add("UserTokens", userTokens);
            xstsBody.add("Properties", xstsProps);
            xstsBody.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            xstsBody.addProperty("TokenType", "JWT");

            JsonObject xstsResp = httpPostJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody.toString());
            if (xstsResp == null || !xstsResp.has("Token")) return null;
            String xstsToken = xstsResp.get("Token").getAsString();

            // 3. Minecraft Login with Xbox
            JsonObject mcBody = new JsonObject();
            mcBody.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
            JsonObject mcResp = httpPostJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody.toString());
            if (mcResp == null || !mcResp.has("access_token")) return null;

            String mcToken = mcResp.get("access_token").getAsString();
            long expiresIn = mcResp.has("expires_in") ? mcResp.get("expires_in").getAsLong() : 86400L;
            long expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn - 60L;

            // 4. Mojang Minecraft Profile
            HttpRequest profReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .header("Authorization", "Bearer " + mcToken)
                    .timeout(Duration.ofSeconds(12))
                    .GET()
                    .build();

            HttpResponse<String> profResp = HTTP_CLIENT.send(profReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (profResp.statusCode() != 200) return null;

            JsonObject profObj = JsonParser.parseString(profResp.body()).getAsJsonObject();
            String name = profObj.has("name") ? profObj.get("name").getAsString() : "Player";
            String uuid = profObj.has("id") ? profObj.get("id").getAsString() : "";

            String skinUrl = "";
            String skinModel = "default";
            if (profObj.has("skins") && profObj.get("skins").isJsonArray()) {
                for (JsonElement elem : profObj.getAsJsonArray("skins")) {
                    if (elem.isJsonObject()) {
                        JsonObject skin = elem.getAsJsonObject();
                        if ("ACTIVE".equalsIgnoreCase(skin.has("state") ? skin.get("state").getAsString() : "ACTIVE")) {
                            if (skin.has("url")) skinUrl = skin.get("url").getAsString();
                            if (skin.has("variant") && "slim".equalsIgnoreCase(skin.get("variant").getAsString())) {
                                skinModel = "slim";
                            }
                            break;
                        }
                    }
                }
            }

            String capeUrl = "";
            if (profObj.has("capes") && profObj.get("capes").isJsonArray()) {
                for (JsonElement elem : profObj.getAsJsonArray("capes")) {
                    if (elem.isJsonObject()) {
                        JsonObject cape = elem.getAsJsonObject();
                        if ("ACTIVE".equalsIgnoreCase(cape.has("state") ? cape.get("state").getAsString() : "ACTIVE")) {
                            if (cape.has("url")) capeUrl = cape.get("url").getAsString();
                            break;
                        }
                    }
                }
            }

            return new SessionResult(uuid, name, mcToken, refreshToken, expiresAt, skinUrl, capeUrl, skinModel);
        } catch (Exception e) {
            EzClientMod.log("Error in exchangeMsaTokenToMinecraft: " + e.getMessage());
            return null;
        }
    }

    private static JsonObject httpPostForm(String url, String formBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return JsonParser.parseString(resp.body()).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JsonObject httpPostJson(String url, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(12))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return JsonParser.parseString(resp.body()).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public record RefreshResult(boolean success, String message) {}

    private record SessionResult(
            String uuid,
            String username,
            String accessToken,
            String refreshToken,
            long expiresAt,
            String skinUrl,
            String capeUrl,
            String skinModel
    ) {}
}
