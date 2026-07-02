package neoproxy.neolinkmc;

import neoproxy.neolinkmc.config.ConnectionConfig;
import neoproxy.neolinkmc.config.SharedNeoLinkConfig;
import neoproxy.neolinkmc.service.ConnectionService;
import neoproxy.neolinkmc.service.MessageHandler;
import neoproxy.neolinkmc.service.TunnelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Loader-neutral lifecycle coordinator for NeoLinkMC.
 */
public final class NeoLinkCore {
    public static final String MOD_ID = "neolinkmc";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final long STOP_WAIT_TIMEOUT_MILLIS = 3_000L;

    private static final Object SERVICE_LOCK = new Object();

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

        SharedNeoLinkConfig.init(configDir);
        LOGGER.info("NeoLinkMC initialized. version={}", version);
        LOGGER.debug("Config directory: {}", SharedNeoLinkConfig.getConfigDir());
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

        ConnectionService nextService;
        synchronized (SERVICE_LOCK) {
            if (connectionService != null && connectionService.isRunning()) {
                LOGGER.warn("NeoLink service is already starting or running; duplicate start request ignored.");
                return;
            }

            nextService = new ConnectionService(resolveMessageHandler());
            connectionService = nextService;
        }

        try {
            nextService.start(new ConnectionConfig(
                    SharedNeoLinkConfig.getRemoteDomain(),
                    ConnectionConfig.DEFAULT_LOCAL_DOMAIN,
                    SharedNeoLinkConfig.getHookPort(),
                    SharedNeoLinkConfig.getHostConnectPort(),
                    key,
                    port > 0 ? port : SharedNeoLinkConfig.getLocalPort()
            ));

            if (nextService.getState() == TunnelState.FAILED) {
                clearConnectionService(nextService);
                return;
            }

            LOGGER.info("NeoLink core service started.");
        } catch (Exception e) {
            clearConnectionService(nextService);
            LOGGER.error("Failed to start NeoLink core service.", e);
        }
    }

    public static void stopService() {
        LOGGER.debug("stopService() called");

        ConnectionService activeService;
        synchronized (SERVICE_LOCK) {
            activeService = connectionService;
            connectionService = null;
        }

        try {
            if (activeService != null) {
                activeService.stop();
                if (!activeService.awaitStopped(STOP_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    LOGGER.warn(
                            "NeoLink worker did not stop within {} ms; continuing shutdown without blocking Minecraft.",
                            STOP_WAIT_TIMEOUT_MILLIS
                    );
                }
                LOGGER.info("NeoLink core service stopped.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to stop NeoLink core service.", e);
        }
    }

    public static ConnectionService getConnectionService() {
        return connectionService;
    }

    public static boolean isRunning() {
        ConnectionService active = connectionService;
        return active != null && active.isRunning();
    }

    public static void updateLocalPort(int port) {
        LOGGER.debug("updateLocalPort() called with port: {}", port);
        LOGGER.warn("Local port hot-reload is not supported after startup. Restart the tunnel with the new LAN port.");
    }

    public static void updateConnectionService(ConnectionService service) {
        LOGGER.debug("updateConnectionService() called");
        Objects.requireNonNull(service, "service");

        ConnectionService previousService;
        synchronized (SERVICE_LOCK) {
            previousService = connectionService;
            connectionService = service;
        }

        if (previousService != null && previousService != service && previousService.isRunning()) {
            LOGGER.warn("Replacing a running NeoLink service; stopping the previous instance first.");
            previousService.stop();
        }
        LOGGER.info("NeoLinkMC connection service updated.");
    }

    public static void onClientStarted() {
        LOGGER.info("Minecraft client started.");
    }

    public static void onClientStopping() {
        LOGGER.info("Minecraft client is stopping; shutting down NeoLink service.");
        stopService();
    }

    public static void onServerStarted(int port) {
        LOGGER.info("Minecraft server started on port {}", port);
    }

    public static void onServerStopping(boolean integratedServer) {
        LOGGER.info("Minecraft server is stopping.");
        if (integratedServer) {
            LOGGER.info("Integrated server is stopping; shutting down NeoLink service.");
            stopService();
        }
    }

    public static void onLocalPlayDisconnect() {
        LOGGER.info("Local play disconnected; shutting down NeoLink service.");
        stopService();
    }

    public static void onTitleScreenTick() {
        if (isRunning()) {
            LOGGER.info("Detected title screen while tunnel is active; shutting down NeoLink service.");
            stopService();
        }
    }

    public static void clearConnectionService(ConnectionService service) {
        if (service == null) {
            return;
        }

        synchronized (SERVICE_LOCK) {
            if (connectionService == service) {
                connectionService = null;
            }
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
