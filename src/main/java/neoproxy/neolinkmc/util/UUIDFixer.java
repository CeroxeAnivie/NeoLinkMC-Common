package neoproxy.neolinkmc.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import neoproxy.neolinkmc.NeoLinkCore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * loader-specific mixin 入口共用的 UUID 修复服务。
 */
public final class UUIDFixer {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static boolean tryOnlineFirst = false;
    public static List<String> alwaysOfflinePlayers = Collections.emptyList();

    private static boolean enabled = false;

    private UUIDFixer() {
    }

    public static void enableFixer() {
        enabled = true;
    }

    public static void disableFixer() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        UUIDFixer.enabled = enabled;
    }

    public static UUID hookEntry(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        if (alwaysOfflinePlayers.contains(playerName)) {
            return null;
        }

        return tryOnlineFirst ? getOfficialUUID(playerName) : null;
    }

    public static UUID getOfficialUUID(String playerName) {
        String normalizedName = playerName == null ? "" : playerName.trim();
        if (normalizedName.isEmpty()) {
            return null;
        }

        String url = "https://api.mojang.com/users/profiles/minecraft/" + normalizedName;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body().isEmpty()) {
                return null;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String responseName = root.getAsJsonPrimitive("name").getAsString();
            String uuidString = root.getAsJsonPrimitive("id").getAsString();
            UUID uuid = parseUUIDFromString(uuidString);
            return responseName.equalsIgnoreCase(normalizedName) ? uuid : null;
        } catch (IOException | InterruptedException | JsonSyntaxException | IllegalStateException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            NeoLinkCore.LOGGER.debug("获取正版 UUID 失败: {}", normalizedName, e);
            return null;
        }
    }

    private static UUID parseUUIDFromString(String uuidString) {
        if (uuidString == null || uuidString.length() != 32) {
            return null;
        }

        try {
            long uuidMSB = Long.parseLong(uuidString.substring(0, 8), 16);
            uuidMSB <<= 32;
            uuidMSB |= Long.parseLong(uuidString.substring(8, 16), 16);
            long uuidLSB = Long.parseLong(uuidString.substring(16, 24), 16);
            uuidLSB <<= 32;
            uuidLSB |= Long.parseLong(uuidString.substring(24, 32), 16);
            return new UUID(uuidMSB, uuidLSB);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }
}
