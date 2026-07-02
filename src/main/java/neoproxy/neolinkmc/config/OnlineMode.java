package neoproxy.neolinkmc.config;

/**
 * Loader-neutral 的 online-mode 策略。
 *
 * <p>该模式按 enum name 持久化，确保每个 loader 和 Minecraft 版本都应用相同的
 * authentication 与 UUID-repair 语义。UI label 保留在 loader-specific 代码中，
 * 因为它们依赖 Minecraft 的 Component API。</p>
 */
public enum OnlineMode {
    ONLINE_ONLINE_UUID_ONLY(true, false),
    OFFLINE_TRY_ONLINE_UUID_FIRST(false, true),
    OFFLINE_OFFLINE_UUID_ONLY(false, false);

    public final boolean onlineModeEnabled;
    public final boolean tryOnlineUUIDFirst;

    OnlineMode(boolean onlineModeEnabled, boolean tryOnlineUUIDFirst) {
        this.onlineModeEnabled = onlineModeEnabled;
        this.tryOnlineUUIDFirst = tryOnlineUUIDFirst;
    }

    public static OnlineMode of(boolean onlineModeEnabled, boolean tryOnlineUUIDFirst) {
        if (onlineModeEnabled) {
            return ONLINE_ONLINE_UUID_ONLY;
        }
        return tryOnlineUUIDFirst ? OFFLINE_TRY_ONLINE_UUID_FIRST : OFFLINE_OFFLINE_UUID_ONLY;
    }

    public static OnlineMode parse(String value) {
        if (value == null || value.isBlank()) {
            return OFFLINE_TRY_ONLINE_UUID_FIRST;
        }
        try {
            return valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return OFFLINE_TRY_ONLINE_UUID_FIRST;
        }
    }
}
