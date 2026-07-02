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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared UUID repair service used by loader-specific mixins.
 */
public final class UUIDFixer {
    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_CACHE_ENTRIES = 512;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(LOOKUP_TIMEOUT)
            .build();
    private static final ConcurrentMap<String, Optional<UUID>> LOOKUP_CACHE = new ConcurrentHashMap<>();

    private static volatile boolean tryOnlineFirst;
    private static volatile Set<String> alwaysOfflinePlayers = Set.of();
    private static volatile Function<String, UUID> officialUuidResolver = UUIDFixer::queryOfficialUUID;

    private UUIDFixer() {
    }

    public static void setTryOnlineFirst(boolean enabled) {
        tryOnlineFirst = enabled;
    }

    public static void setAlwaysOfflinePlayers(List<String> players) {
        if (players == null || players.isEmpty()) {
            alwaysOfflinePlayers = Set.of();
            return;
        }

        alwaysOfflinePlayers = players.stream()
                .map(UUIDFixer::normalizeName)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static UUID hookEntry(String playerName) {
        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty() || !tryOnlineFirst) {
            return null;
        }

        if (alwaysOfflinePlayers.contains(normalizedName)) {
            return null;
        }

        trimCacheIfNeeded();
        return LOOKUP_CACHE.computeIfAbsent(
                normalizedName,
                name -> Optional.ofNullable(officialUuidResolver.apply(name))
        ).orElse(null);
    }

    public static UUID getOfficialUUID(String playerName) {
        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        return officialUuidResolver.apply(normalizedName);
    }

    static void resetStateForTest() {
        tryOnlineFirst = false;
        alwaysOfflinePlayers = Set.of();
        officialUuidResolver = UUIDFixer::queryOfficialUUID;
        LOOKUP_CACHE.clear();
    }

    static void setOfficialUuidResolverForTest(Function<String, UUID> resolver) {
        officialUuidResolver = resolver == null ? UUIDFixer::queryOfficialUUID : resolver;
        LOOKUP_CACHE.clear();
    }

    private static UUID queryOfficialUUID(String normalizedName) {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + normalizedName;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(LOOKUP_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body().isEmpty()) {
                return null;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String responseName = normalizeName(root.getAsJsonPrimitive("name").getAsString());
            String uuidString = root.getAsJsonPrimitive("id").getAsString();
            UUID uuid = parseUUIDFromString(uuidString);
            return responseName.equals(normalizedName) ? uuid : null;
        } catch (IOException | InterruptedException | JsonSyntaxException | IllegalStateException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            NeoLinkCore.LOGGER.debug("获取正版玩家编号失败：{}", normalizedName, e);
            return null;
        }
    }

    private static String normalizeName(String playerName) {
        return playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
    }

    private static void trimCacheIfNeeded() {
        if (LOOKUP_CACHE.size() > MAX_CACHE_ENTRIES) {
            LOOKUP_CACHE.clear();
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
