package neoproxy.neolinkmc.service;

import neoproxy.neolinkmc.NeoLinkCore;
import neoproxy.neolinkmc.config.ConnectionConfig;
import top.ceroxe.api.neolink.NeoLinkAPI;
import top.ceroxe.api.neolink.NeoLinkCfg;
import top.ceroxe.api.neolink.NeoLinkState;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loader-neutral adapter for the NeoLink tunnel lifecycle.
 */
public final class ConnectionService implements AutoCloseable {
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final double LOW_TRAFFIC_REMAINING_MIB = 100.0D;
    private static final Pattern TRAFFIC_QUOTA_PATTERN = Pattern.compile(
            "([0-9]+(?:\\.[0-9]+)?)\\s*(MiB|MIB|MB|GiB|GIB|GB)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private final MessageHandler messageHandler;
    private final AtomicBoolean startingOrRunning = new AtomicBoolean(false);

    private volatile NeoLinkAPI client;
    private volatile ConnectionConfig config;
    private volatile Thread workerThread;
    private volatile TunnelState state = TunnelState.STOPPED;
    private volatile boolean stopRequested;
    private volatile boolean trafficQuotaMessageSent;

    public ConnectionService(MessageHandler messageHandler) {
        this.messageHandler = Objects.requireNonNull(messageHandler, "messageHandler");
    }

    public void start(ConnectionConfig nextConfig) {
        Objects.requireNonNull(nextConfig, "nextConfig");
        if (!startingOrRunning.compareAndSet(false, true)) {
            messageHandler.log("NeoLink tunnel is already starting or running.", MessageHandler.LogLevel.WARN);
            return;
        }

        config = nextConfig;
        stopRequested = false;
        trafficQuotaMessageSent = false;
        state = TunnelState.STARTING;

        try {
            NeoLinkAPI nextClient = createClient(nextConfig);
            client = nextClient;

            workerThread = new Thread(() -> runTunnel(nextClient), "NeoLinkMC-Tunnel");
            workerThread.setDaemon(true);
            workerThread.start();
        } catch (RuntimeException e) {
            state = TunnelState.FAILED;
            startingOrRunning.set(false);
            client = null;
            workerThread = null;
            onError("Failed to initialize NeoLink tunnel.", e);
        }
    }

    public void stop() {
        stopRequested = true;
        if (startingOrRunning.get()) {
            state = TunnelState.STOPPING;
        }

        NeoLinkAPI activeClient = client;
        if (activeClient != null) {
            activeClient.close();
        }

        Thread activeWorker = workerThread;
        if (activeWorker != null) {
            activeWorker.interrupt();
        }
    }

    public boolean awaitStopped(long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit, "unit");
        Thread activeWorker = workerThread;
        if (activeWorker == null || Thread.currentThread() == activeWorker) {
            return true;
        }

        try {
            activeWorker.join(unit.toMillis(timeout));
            return !activeWorker.isAlive();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean isRunning() {
        TunnelState currentState = state;
        return startingOrRunning.get()
                && (currentState == TunnelState.STARTING || currentState == TunnelState.RUNNING);
    }

    public TunnelState getState() {
        return state;
    }

    private NeoLinkAPI createClient(ConnectionConfig nextConfig) {
        NeoLinkCfg apiConfig = new NeoLinkCfg(
                nextConfig.remoteDomain(),
                nextConfig.hookPort(),
                nextConfig.hostConnectPort(),
                nextConfig.key(),
                nextConfig.localPort()
        )
                .setLocalDomainName(nextConfig.localDomain())
                .setLanguage(NeoLinkCfg.ZH_CH)
                .setTCPEnabled(true)
                .setUDPEnabled(false);

        messageHandler.log(
                "NeoLink runtime config: remote=" + nextConfig.remoteDomain()
                        + ", hookPort=" + nextConfig.hookPort()
                        + ", hostConnectPort=" + nextConfig.hostConnectPort()
                        + ", localDomain=" + nextConfig.localDomain()
                        + ", localPort=" + nextConfig.localPort()
                        + ", key=" + describeKey(nextConfig.key())
                        + ", tcp=true, udp=false",
                MessageHandler.LogLevel.INFO
        );

        return new NeoLinkAPI(apiConfig)
                .setOnStateChanged(apiState -> onStateChanged(toTunnelState(apiState)))
                .setOnServerMessage(this::onServerMessage)
                .setOnError(this::onError)
                .setOnConnect((protocol, sourceAddress, targetAddress) ->
                        onConnect(
                                String.valueOf(protocol),
                                String.valueOf(sourceAddress),
                                String.valueOf(targetAddress)
                        ))
                .setOnDisconnect((protocol, sourceAddress, targetAddress) ->
                        onDisconnect(
                                String.valueOf(protocol),
                                String.valueOf(sourceAddress),
                                String.valueOf(targetAddress)
                        ))
                .setDebugSink(this::onDebug);
    }

    private static String describeKey(String key) {
        if (key == null || key.isBlank()) {
            return "blank";
        }
        if (ConnectionConfig.DEFAULT_KEY.equals(key)) {
            return "default(" + ConnectionConfig.DEFAULT_KEY.length() + " chars)";
        }
        return "custom(" + key.length() + " chars)";
    }

    private static TunnelState toTunnelState(NeoLinkState state) {
        return switch (state) {
            case STARTING -> TunnelState.STARTING;
            case RUNNING -> TunnelState.RUNNING;
            case STOPPING -> TunnelState.STOPPING;
            case FAILED -> TunnelState.FAILED;
            case STOPPED -> TunnelState.STOPPED;
        };
    }

    private void runTunnel(NeoLinkAPI tunnel) {
        try {
            messageHandler.log("NeoLinkMC client is starting.", MessageHandler.LogLevel.INFO);
            messageHandler.send("Starting NeoLinkMC client...", MessageHandler.MessageType.INFO);
            tunnel.start(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        } catch (Exception e) {
            if (startingOrRunning.get()) {
                state = stopRequested ? TunnelState.STOPPED : TunnelState.FAILED;
                onError("NeoLink tunnel failed while starting or running.", e);
            }
        } finally {
            if (state != TunnelState.FAILED || stopRequested) {
                state = TunnelState.STOPPED;
            }
            startingOrRunning.set(false);
            client = null;
            workerThread = null;
            NeoLinkCore.clearConnectionService(this);
            messageHandler.log("NeoLink tunnel has stopped.", MessageHandler.LogLevel.INFO);
            messageHandler.send("NeoLink tunnel has stopped.", MessageHandler.MessageType.INFO);
        }
    }

    public void onStateChanged(TunnelState nextState) {
        state = nextState;
        messageHandler.log("NeoLink tunnel state changed: " + nextState, MessageHandler.LogLevel.INFO);
        switch (nextState) {
            case STARTING -> {
                ConnectionConfig activeConfig = config;
                if (activeConfig != null) {
                    messageHandler.send(
                            "Connecting to " + activeConfig.remoteDomain() + "...",
                            MessageHandler.MessageType.INFO
                    );
                }
            }
            case RUNNING -> messageHandler.send("NeoLink tunnel connected.", MessageHandler.MessageType.SUCCESS);
            case STOPPING -> messageHandler.send("Stopping NeoLink tunnel...", MessageHandler.MessageType.INFO);
            case FAILED -> messageHandler.send("NeoLink tunnel disconnected unexpectedly.", MessageHandler.MessageType.ERROR);
            case STOPPED -> {
            }
        }
    }

    public void onServerMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        messageHandler.log("NeoLink server message: " + message, MessageHandler.LogLevel.INFO);
        if (isTrafficQuotaMessage(message)) {
            if (shouldShowTrafficQuotaInChat(message)) {
                messageHandler.send(message, MessageHandler.MessageType.WARNING);
            }
            return;
        }
        messageHandler.send(message, MessageHandler.MessageType.INFO);
    }

    public void onError(String message, Throwable cause) {
        String detail = cause == null || cause.getMessage() == null ? message : message + " " + cause.getMessage();
        messageHandler.send(summarizeErrorForChat(cause), MessageHandler.MessageType.ERROR);
        if (cause != null) {
            messageHandler.log(detail, MessageHandler.LogLevel.ERROR, cause);
        }
    }

    private boolean shouldShowTrafficQuotaInChat(String message) {
        Double remainingMiB = parseRemainingTrafficMiB(message);
        if (!trafficQuotaMessageSent) {
            trafficQuotaMessageSent = true;
            return true;
        }
        return remainingMiB != null && remainingMiB <= LOW_TRAFFIC_REMAINING_MIB;
    }

    private static boolean isTrafficQuotaMessage(String message) {
        return TRAFFIC_QUOTA_PATTERN.matcher(message).find();
    }

    private static Double parseRemainingTrafficMiB(String message) {
        Matcher matcher = TRAFFIC_QUOTA_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        double amount = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).toUpperCase();
        return unit.startsWith("G") ? amount * 1024.0D : amount;
    }

    private static String summarizeErrorForChat(Throwable cause) {
        String causeMessage = cause == null ? null : cause.getMessage();
        if (causeMessage == null || causeMessage.isBlank()) {
            return "NeoLink connection failed. Full details were written to the log.";
        }
        return "NeoLink connection failed: " + causeMessage + ". Full details were written to the log.";
    }

    public void onConnect(String protocol, String sourceAddress, String targetAddress) {
        messageHandler.log(
                protocol + " connection " + sourceAddress + " -> " + targetAddress + " established.",
                MessageHandler.LogLevel.INFO
        );
    }

    public void onDisconnect(String protocol, String sourceAddress, String targetAddress) {
        messageHandler.log(
                protocol + " connection " + sourceAddress + " -> " + targetAddress + " closed.",
                MessageHandler.LogLevel.INFO
        );
    }

    public void onDebug(String message, Throwable cause) {
        if (cause != null) {
            String detail = message == null || message.isBlank() ? "NeoLinkAPI debug exception" : message;
            messageHandler.log(detail, MessageHandler.LogLevel.DEBUG, cause);
            return;
        }
        if (message != null) {
            messageHandler.log(message, MessageHandler.LogLevel.DEBUG);
        }
    }

    @Override
    public void close() {
        stop();
    }
}
