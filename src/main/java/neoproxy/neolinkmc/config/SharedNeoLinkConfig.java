package neoproxy.neolinkmc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import neoproxy.neolinkmc.NeoLinkCore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 所有 loader 共用的 JSON 配置存储。
 *
 * <p>文件格式使用 primitive value，这样 Fabric、Forge、NeoForge 以及特定版本模块
 * 都可以在边界处映射自己的 Minecraft enum 类型。这样能保证每个 jar 的默认值、
 * 校验和持久化行为完全一致。</p>
 */
public final class SharedNeoLinkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CONFIG_FILE_NAME = "config.json";
    private static final String CONFIG_DIR_NAME = "neolinkmc";
    private static final String LEGACY_CONFIG_FILE_NAME = "neolinkmc.json";
    private static final String TEMPLATE_PATH = "/templates/config.json";
    private static final ConfigData TEMPLATE_DEFAULTS = loadTemplateDefaults();

    public static final String DEFAULT_REMOTE_DOMAIN = TEMPLATE_DEFAULTS.remote_domain;
    public static final String DEFAULT_LOCAL_DOMAIN = TEMPLATE_DEFAULTS.local_domain;
    public static final int DEFAULT_LOCAL_PORT = requireTemplatePort(TEMPLATE_DEFAULTS.local_port, "local_port");
    public static final int DEFAULT_HOOK_PORT = requireTemplatePort(TEMPLATE_DEFAULTS.host_hook_port, "host_hook_port");
    public static final int DEFAULT_HOST_CONNECT_PORT = requireTemplatePort(TEMPLATE_DEFAULTS.host_connect_port, "host_connect_port");
    public static final boolean DEFAULT_PVP_ALLOWED = requireTemplateBoolean(TEMPLATE_DEFAULTS.pvp_allowed, "pvp_allowed");
    public static final boolean DEFAULT_ALLOW_CHEATS = requireTemplateBoolean(TEMPLATE_DEFAULTS.allow_cheats, "allow_cheats");
    public static final int DEFAULT_MAX_PLAYERS = requireTemplateMaxPlayers(TEMPLATE_DEFAULTS.max_players, "max_players");
    public static final String DEFAULT_GAME_TYPE = TEMPLATE_DEFAULTS.gamemode;
    public static final OnlineMode DEFAULT_ONLINE_MODE = OnlineMode.parse(TEMPLATE_DEFAULTS.onlinemode);

    private static ConfigData configData = new ConfigData();
    private static Path configDir;
    private static Path configFile;

    private SharedNeoLinkConfig() {
    }

    public static void init(Path loaderConfigDir) {
        configDir = loaderConfigDir;
        configFile = loaderConfigDir.resolve(CONFIG_DIR_NAME).resolve(CONFIG_FILE_NAME);
        load();
    }

    public static void load() {
        ensureInitialized();

        migrateLegacyConfigIfNeeded();

        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                configData = GSON.fromJson(json, ConfigData.class);
                if (configData == null) {
                    configData = new ConfigData();
                }
                migrateLegacyDefaultEndpointIfNeeded();
            } catch (IOException e) {
                NeoLinkCore.LOGGER.error("加载配置文件失败", e);
                configData = new ConfigData();
            }
            return;
        }

        if (copyTemplateConfig()) {
            load();
            return;
        }

        throw new IllegalStateException("Unable to initialize NeoLink config from bundled template: " + TEMPLATE_PATH);
    }

    private static boolean copyTemplateConfig() {
        try (InputStream templateStream = SharedNeoLinkConfig.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream == null) {
                NeoLinkCore.LOGGER.warn("找不到配置文件模板: {}", TEMPLATE_PATH);
                return false;
            }

            Files.createDirectories(configFile.getParent());
            Files.copy(templateStream, configFile, StandardCopyOption.REPLACE_EXISTING);
            NeoLinkCore.LOGGER.info("已从模板复制默认配置文件");
            return true;
        } catch (IOException e) {
            NeoLinkCore.LOGGER.error("复制配置文件模板失败", e);
            return false;
        }
    }

    private static ConfigData loadTemplateDefaults() {
        try (InputStream templateStream = SharedNeoLinkConfig.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (templateStream == null) {
                throw new IllegalStateException("找不到配置文件模板: " + TEMPLATE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(templateStream, StandardCharsets.UTF_8)) {
                ConfigData defaults = GSON.fromJson(reader, ConfigData.class);
                if (defaults == null) {
                    throw new IllegalStateException("配置文件模板为空: " + TEMPLATE_PATH);
                }
                return defaults;
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取配置文件模板失败: " + TEMPLATE_PATH, e);
        }
    }

    private static int requireTemplatePort(String value, String fieldName) {
        int parsed = ConfigRules.parsePortOrDefault(value, -1);
        if (parsed == -1) {
            throw new IllegalStateException("配置文件模板字段 " + fieldName + " 必须是 1-65535 的端口。");
        }
        return parsed;
    }

    private static int requireTemplateMaxPlayers(String value, String fieldName) {
        int parsed = ConfigRules.parseMaxPlayersOrDefault(value, -1);
        if (parsed == -1) {
            throw new IllegalStateException("配置文件模板字段 " + fieldName + " 必须是有效玩家数。");
        }
        return parsed;
    }

    private static boolean requireTemplateBoolean(Boolean value, String fieldName) {
        if (value == null) {
            throw new IllegalStateException("配置文件模板字段 " + fieldName + " 必须是布尔值。");
        }
        return value;
    }

    private static void migrateLegacyDefaultEndpointIfNeeded() {
        if (!"mc.p.ceroxe.fun".equals(textOrDefault(configData.remote_domain, ""))
                || !"9100".equals(textOrDefault(configData.host_hook_port, ""))
                || !"9101".equals(textOrDefault(configData.host_connect_port, ""))) {
            return;
        }

        configData.remote_domain = DEFAULT_REMOTE_DOMAIN;
        configData.host_hook_port = String.valueOf(DEFAULT_HOOK_PORT);
        configData.host_connect_port = String.valueOf(DEFAULT_HOST_CONNECT_PORT);
        save();
        NeoLinkCore.LOGGER.info(
                "已将旧默认 NeoLink 端点迁移到模板默认值: {}:{} / {}",
                DEFAULT_REMOTE_DOMAIN,
                DEFAULT_HOOK_PORT,
                DEFAULT_HOST_CONNECT_PORT
        );
    }

    public static void save() {
        ensureInitialized();

        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(configData));
        } catch (IOException e) {
            NeoLinkCore.LOGGER.error("保存配置文件失败", e);
        }
    }

    public static Path getConfigDir() {
        ensureInitialized();
        return configDir;
    }

    public static Path getModConfigDir() {
        ensureInitialized();
        Path dir = configDir.resolve(CONFIG_DIR_NAME);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            NeoLinkCore.LOGGER.error("创建模组配置目录失败", e);
        }
        return dir;
    }

    public static String getRemoteDomain() {
        return textOrDefault(configData.remote_domain, TEMPLATE_DEFAULTS.remote_domain);
    }

    public static int getLocalPort() {
        return ConfigRules.parsePortOrDefault(configData.local_port, DEFAULT_LOCAL_PORT);
    }

    public static int getHookPort() {
        return ConfigRules.parsePortOrDefault(configData.host_hook_port, DEFAULT_HOOK_PORT);
    }

    public static int getHostConnectPort() {
        return ConfigRules.parsePortOrDefault(configData.host_connect_port, DEFAULT_HOST_CONNECT_PORT);
    }

    public static boolean isPvpAllowed() {
        return configData.pvp_allowed == null ? DEFAULT_PVP_ALLOWED : configData.pvp_allowed;
    }

    public static boolean isAllowCheats() {
        return configData.allow_cheats == null ? DEFAULT_ALLOW_CHEATS : configData.allow_cheats;
    }

    public static int getMaxPlayers() {
        return ConfigRules.parseMaxPlayersOrDefault(configData.max_players, DEFAULT_MAX_PLAYERS);
    }

    public static String getGameTypeName() {
        return textOrDefault(configData.gamemode, TEMPLATE_DEFAULTS.gamemode);
    }

    public static OnlineMode getOnlineMode() {
        return OnlineMode.parse(configData.onlinemode);
    }

    public static void setRemoteDomain(String domain) {
        configData.remote_domain = domain;
    }

    public static void setLocalPort(int port) {
        configData.local_port = String.valueOf(port);
    }

    public static void setHookPort(int port) {
        configData.host_hook_port = String.valueOf(port);
    }

    public static void setHostConnectPort(int port) {
        configData.host_connect_port = String.valueOf(port);
    }

    public static void setPvpAllowed(boolean allowed) {
        configData.pvp_allowed = allowed;
    }

    public static void setAllowCheats(boolean allow) {
        configData.allow_cheats = allow;
    }

    public static void setMaxPlayers(int players) {
        configData.max_players = String.valueOf(players);
    }

    public static void setGameTypeName(String gameTypeName) {
        configData.gamemode = textOrDefault(gameTypeName, TEMPLATE_DEFAULTS.gamemode);
    }

    public static void setOnlineMode(OnlineMode mode) {
        configData.onlinemode = mode == null ? DEFAULT_ONLINE_MODE.name() : mode.name();
    }

    public static void setLocalDomain(String localDomain) {
        configData.local_domain = textOrDefault(localDomain, TEMPLATE_DEFAULTS.local_domain);
    }

    private static void ensureInitialized() {
        if (configFile == null || configDir == null) {
            throw new IllegalStateException("SharedNeoLinkConfig.init(Path) must be called before using config values.");
        }
    }

    private static void migrateLegacyConfigIfNeeded() {
        Path legacyFile = configDir.resolve(LEGACY_CONFIG_FILE_NAME);
        if (Files.exists(configFile) || !Files.exists(legacyFile)) {
            return;
        }

        try {
            Files.createDirectories(configFile.getParent());
            Files.move(legacyFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            NeoLinkCore.LOGGER.info("已迁移旧配置文件 {} 到 {}", legacyFile, configFile);
        } catch (IOException e) {
            NeoLinkCore.LOGGER.error("迁移旧配置文件失败", e);
        }
    }

    private static String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static final class ConfigData {
        String remote_domain;
        String local_domain;
        String local_port;
        String host_hook_port;
        String host_connect_port;
        String gamemode;
        String onlinemode;
        Boolean pvp_allowed;
        Boolean allow_cheats;
        String max_players;
    }
}
