package neoproxy.neolinkmc.config;

/**
 * 用户可编辑配置共用的校验与状态切换规则。
 *
 * <p>Loader UI 代码只应负责收集文本和渲染控件。边界值、默认值、循环顺序以及
 * secret 归一化逻辑都收敛在这里，避免 Fabric、Forge 与 NeoForge 出现行为漂移。</p>
 */
public final class ConfigRules {
    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65535;
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 1000;

    private ConfigRules() {
    }

    public static boolean isValidPortInput(String input) {
        return isEmptyOrIntegerInRange(input, MIN_PORT, MAX_PORT);
    }

    public static boolean isValidMaxPlayersInput(String input) {
        return isEmptyOrIntegerInRange(input, MIN_PLAYERS, MAX_PLAYERS);
    }

    public static int parsePortOrDefault(String value, int defaultValue) {
        return parseIntegerOrDefault(value, defaultValue, MIN_PORT, MAX_PORT);
    }

    public static int parseMaxPlayersOrDefault(String value, int defaultValue) {
        return parseIntegerOrDefault(value, defaultValue, MIN_PLAYERS, MAX_PLAYERS);
    }

    public static String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public static String displayKey(String key) {
        return ConnectionConfig.normalizeKey(key);
    }

    public static String transientKeyFromDisplay(String key) {
        String normalized = textOrDefault(key, "");
        return ConnectionConfig.DEFAULT_KEY.equals(normalized) ? "" : normalized;
    }

    public static OnlineMode nextOnlineMode(OnlineMode mode) {
        OnlineMode current = mode == null ? SharedNeoLinkConfig.DEFAULT_ONLINE_MODE : mode;
        return switch (current) {
            case ONLINE_ONLINE_UUID_ONLY -> OnlineMode.OFFLINE_TRY_ONLINE_UUID_FIRST;
            case OFFLINE_TRY_ONLINE_UUID_FIRST -> OnlineMode.OFFLINE_OFFLINE_UUID_ONLY;
            case OFFLINE_OFFLINE_UUID_ONLY -> OnlineMode.ONLINE_ONLINE_UUID_ONLY;
        };
    }

    public static String nextGameTypeName(String gameTypeName) {
        return switch (textOrDefault(gameTypeName, SharedNeoLinkConfig.DEFAULT_GAME_TYPE)) {
            case "SURVIVAL" -> "CREATIVE";
            case "CREATIVE" -> "ADVENTURE";
            case "ADVENTURE" -> "SPECTATOR";
            default -> "SURVIVAL";
        };
    }

    public static ConnectionConfig toConnectionConfig(
            NeoLinkConfigState state,
            String key,
            int actualLocalPort
    ) {
        return new ConnectionConfig(
                state.remoteServer,
                ConnectionConfig.DEFAULT_LOCAL_DOMAIN,
                state.hookPort,
                state.hostConnectPort,
                key,
                actualLocalPort
        );
    }

    private static boolean isEmptyOrIntegerInRange(String input, int min, int max) {
        if (input == null || input.isEmpty()) {
            return true;
        }
        try {
            int value = Integer.parseInt(input);
            return value >= min && value <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int parseIntegerOrDefault(String value, int defaultValue, int min, int max) {
        try {
            String trimmed = value == null ? "" : value.trim();
            if (trimmed.isEmpty()) {
                return defaultValue;
            }
            int parsed = Integer.parseInt(trimmed);
            return parsed >= min && parsed <= max ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
