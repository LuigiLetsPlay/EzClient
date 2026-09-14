package app.ezclient.account;

import com.google.gson.JsonObject;
import net.minecraft.client.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a saved Minecraft account in EzClient.
 * Compatible with the launcher's accounts.json format.
 */
public final class EzAccount {
    private final String uuid;
    private String username;
    private String accessToken;
    private String refreshToken;
    private long expiresAt;
    private boolean isOnline;
    private String skinUrl;
    private String capeUrl;
    private String skinModel;
    private String userType;

    public EzAccount(
            String uuid,
            String username,
            String accessToken,
            String refreshToken,
            long expiresAt,
            boolean isOnline,
            String skinUrl,
            String capeUrl,
            String skinModel,
            String userType
    ) {
        this.uuid = normalizeUuid(uuid);
        this.username = username != null ? username : "Player";
        this.accessToken = accessToken != null ? accessToken : "0";
        this.refreshToken = refreshToken != null ? refreshToken : "";
        this.expiresAt = expiresAt;
        this.isOnline = isOnline;
        this.skinUrl = skinUrl != null ? skinUrl : "";
        this.capeUrl = capeUrl != null ? capeUrl : "";
        this.skinModel = skinModel != null ? skinModel : "default";
        this.userType = userType != null ? userType : (isOnline ? "msa" : "offline");
    }

    public static String normalizeUuid(String raw) {
        if (raw == null) return "";
        return raw.replace("-", "").trim().toLowerCase();
    }

    public static UUID parseUuid(String hex) {
        String clean = normalizeUuid(hex);
        if (clean.length() == 32) {
            long most = Long.parseUnsignedLong(clean.substring(0, 16), 16);
            long least = Long.parseUnsignedLong(clean.substring(16, 32), 16);
            return new UUID(most, least);
        }
        try {
            return UUID.fromString(hex);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }

    public static EzAccount fromJson(JsonObject json) {
        String uuid = json.has("uuid") ? json.get("uuid").getAsString() : "";
        String username = json.has("username") ? json.get("username").getAsString() : "Player";
        String accessToken = json.has("access_token") ? json.get("access_token").getAsString() : "0";
        String refreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : "";
        long expiresAt = json.has("expires_at") ? json.get("expires_at").getAsLong() : 0L;
        boolean isOnline = json.has("is_online") && json.get("is_online").getAsBoolean();
        String skinUrl = json.has("skin_url") ? json.get("skin_url").getAsString() : "";
        String capeUrl = json.has("cape_url") ? json.get("cape_url").getAsString() : "";
        String skinModel = json.has("skin_model") ? json.get("skin_model").getAsString() : "default";
        String userType = json.has("user_type") ? json.get("user_type").getAsString() : (isOnline ? "msa" : "offline");

        return new EzAccount(uuid, username, accessToken, refreshToken, expiresAt, isOnline, skinUrl, capeUrl, skinModel, userType);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("uuid", uuid);
        obj.addProperty("username", username);
        obj.addProperty("access_token", accessToken);
        obj.addProperty("refresh_token", refreshToken);
        obj.addProperty("expires_at", expiresAt);
        obj.addProperty("is_online", isOnline);
        obj.addProperty("skin_url", skinUrl);
        obj.addProperty("cape_url", capeUrl);
        obj.addProperty("skin_model", skinModel);
        obj.addProperty("user_type", userType);
        return obj;
    }

    public User toMinecraftUser() {
        UUID parsedUuid = parseUuid(this.uuid);
        return new User(
                this.username,
                parsedUuid,
                this.accessToken,
                Optional.empty(),
                Optional.empty()
        );
    }

    public String getUuid() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public void setSkinUrl(String skinUrl) {
        this.skinUrl = skinUrl;
    }

    public String getCapeUrl() {
        return capeUrl;
    }

    public void setCapeUrl(String capeUrl) {
        this.capeUrl = capeUrl;
    }

    public String getSkinModel() {
        return skinModel;
    }

    public void setSkinModel(String skinModel) {
        this.skinModel = skinModel;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
