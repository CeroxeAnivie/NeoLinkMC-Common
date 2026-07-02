package neoproxy.neolinkmc.config;

/**
 * UI 兼容层使用的可变 loader-neutral 配置状态。
 *
 * <p>本类有意存储面向 Minecraft 的 primitive value，例如 game mode name，
 * 而不是导入 Minecraft class。Loader 模块可以在自身边界处转换这些字段，
 * 但加载、保存、默认值以及非密钥持久化规则仍统一归 common 管理。</p>
 */
public final class NeoLinkConfigState {
    public String remoteServer;
    public int localPort;
    public int hookPort;
    public int hostConnectPort;
    public boolean pvpAllowed;
    public OnlineMode onlineMode;
    public boolean allowCheats;
    public int maxPlayers;
    public String gameTypeName;

    public void loadPersisted() {
        SharedNeoLinkConfig.load();
        this.remoteServer = SharedNeoLinkConfig.getRemoteDomain();
        this.hookPort = SharedNeoLinkConfig.getHookPort();
        this.hostConnectPort = SharedNeoLinkConfig.getHostConnectPort();
        this.localPort = SharedNeoLinkConfig.getLocalPort();
        this.gameTypeName = SharedNeoLinkConfig.getGameTypeName();
        this.onlineMode = SharedNeoLinkConfig.getOnlineMode();
        this.pvpAllowed = SharedNeoLinkConfig.isPvpAllowed();
        this.allowCheats = SharedNeoLinkConfig.isAllowCheats();
        this.maxPlayers = SharedNeoLinkConfig.getMaxPlayers();
    }

    public void savePersisted() {
        SharedNeoLinkConfig.setRemoteDomain(this.remoteServer);
        SharedNeoLinkConfig.setHookPort(this.hookPort);
        SharedNeoLinkConfig.setHostConnectPort(this.hostConnectPort);
        SharedNeoLinkConfig.setLocalPort(this.localPort);
        SharedNeoLinkConfig.setLocalDomain(SharedNeoLinkConfig.DEFAULT_LOCAL_DOMAIN);
        SharedNeoLinkConfig.setGameTypeName(this.gameTypeName);
        SharedNeoLinkConfig.setOnlineMode(this.onlineMode);
        SharedNeoLinkConfig.setPvpAllowed(this.pvpAllowed);
        SharedNeoLinkConfig.setAllowCheats(this.allowCheats);
        SharedNeoLinkConfig.setMaxPlayers(this.maxPlayers);
        SharedNeoLinkConfig.save();
    }
}
