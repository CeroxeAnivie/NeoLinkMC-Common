package neoproxy.neolinkmc;

import neoproxy.neolinkmc.config.ConnectionConfig;
import neoproxy.neolinkmc.config.SharedNeoLinkConfig;
import neoproxy.neolinkmc.service.ConnectionService;
import neoproxy.neolinkmc.service.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Loader-neutral 的 NeoLinkMC 生命周期。
 *
 * <p>Fabric、Forge 与 NeoForge 的 event bus 和 metadata 格式各不相同，但 tunnel
 * 生命周期必须完全一致。将这部分行为收敛到这里，可以避免细微的平台漂移，
 * 例如某个 loader 使用不同端口校验规则，或在单人 server 关闭时忘记停止 tunnel。</p>
 */
public final class NeoLinkCore {
    public static final String MOD_ID = "neolinkmc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile ConnectionService connectionService;
    private static volatile MessageHandler messageHandler;
    private static volatile Path configDir;
    private static volatile String version = "unknown";

    private NeoLinkCore() {
    }

    public static void initialize(Path loaderConfigDir, String loaderVersion, MessageHandler handler) {
        configDir = Objects.requireNonNull(loaderConfigDir, "loaderConfigDir");
        version = loaderVersion == null || loaderVersion.isBlank() ? "unknown" : loaderVersion.trim();
        messageHandler = Objects.requireNonNull(handler, "handler");

        LOGGER.info("╔════════════════════════════════════════════════════════╗");
        LOGGER.info("║         NeoLinkMC Mod 正在初始化...                    ║");
        LOGGER.info("╚════════════════════════════════════════════════════════╝");
        LOGGER.info("Mod 版本: {}", version);

        SharedNeoLinkConfig.init(configDir);
        LOGGER.info("配置系统初始化完成");
        LOGGER.debug("配置目录路径: {}", SharedNeoLinkConfig.getConfigDir());
        LOGGER.info("NeoLinkMC Mod 初始化完成");
    }

    public static Path getConfigDir() {
        Path current = configDir;
        if (current == null) {
            throw new IllegalStateException("NeoLinkMC core has not been initialized.");
        }
        return current;
    }

    public static String getVersion() {
        return version;
    }

    public static void startService() {
        startService(null, -1);
    }

    public static void startService(String key) {
        startService(key, -1);
    }

    public static void startService(String key, int port) {
        LOGGER.debug("startService() called with port: {}", port);
        try {
            if (connectionService == null) {
                connectionService = new ConnectionService(resolveMessageHandler());
                connectionService.start(new ConnectionConfig(
                        SharedNeoLinkConfig.getRemoteDomain(),
                        ConnectionConfig.DEFAULT_LOCAL_DOMAIN,
                        SharedNeoLinkConfig.getHookPort(),
                        SharedNeoLinkConfig.getHostConnectPort(),
                        key,
                        port > 0 ? port : SharedNeoLinkConfig.getLocalPort()
                ));
                LOGGER.info("NeoLink 核心服务启动成功");
            }
        } catch (Exception e) {
            LOGGER.error("NeoLink 核心服务启动失败", e);
        }
    }

    public static void stopService() {
        LOGGER.debug("stopService() called");
        try {
            if (connectionService != null) {
                connectionService.stop();
                connectionService = null;
                LOGGER.info("NeoLink 核心服务已停止");
            }
        } catch (Exception e) {
            LOGGER.error("NeoLink 核心服务停止时出错", e);
        }
    }

    public static ConnectionService getConnectionService() {
        return connectionService;
    }

    public static boolean isRunning() {
        return connectionService != null && connectionService.isRunning();
    }

    public static void updateLocalPort(int port) {
        LOGGER.debug("updateLocalPort() called with port: {}", port);
        LOGGER.warn("服务启动后不再支持热更新本地端口，请停止后用新 LAN 端口重新启动。");
    }

    public static void updateConnectionService(ConnectionService service) {
        LOGGER.debug("updateConnectionService() called");
        if (connectionService != null && connectionService.isRunning()) {
            LOGGER.warn("已有运行中的服务，先停止旧服务");
            connectionService.stop();
        }
        connectionService = service;
        LOGGER.info("NeoLinkMC 连接服务已更新");
    }

    public static void onClientStarted() {
        LOGGER.info("Minecraft 客户端启动完成");
    }

    public static void onClientStopping() {
        LOGGER.info("Minecraft 客户端正在停止，关闭 NeoLink 服务");
        stopService();
    }

    public static void onServerStarted(int port) {
        LOGGER.info("Minecraft 服务器启动完成，端口: {}", port);
    }

    public static void onServerStopping(boolean integratedServer) {
        LOGGER.info("Minecraft 服务器正在停止");
        if (integratedServer) {
            LOGGER.info("[NeoLinkMC] 单人游戏服务器停止，停止服务...");
            stopService();
        }
    }

    public static void onLocalPlayDisconnect() {
        LOGGER.info("[NeoLinkMC] 房主断开连接，停止服务...");
        stopService();
    }

    public static void onTitleScreenTick() {
        if (isRunning()) {
            LOGGER.info("[NeoLinkMC] 检测到回到主界面，停止服务...");
            stopService();
        }
    }

    private static MessageHandler resolveMessageHandler() {
        MessageHandler current = messageHandler;
        if (current == null) {
            throw new IllegalStateException("NeoLinkMC message handler has not been initialized.");
        }
        return current;
    }
}
