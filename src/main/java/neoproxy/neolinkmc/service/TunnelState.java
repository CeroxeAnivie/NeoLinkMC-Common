package neoproxy.neolinkmc.service;

/**
 * 共享 Minecraft 逻辑使用的 Java 17-safe tunnel 生命周期状态。
 *
 * <p>UI 和 loader entrypoint 只依赖这个稳定状态模型，NeoLinkAPI 的具体枚举值
 * 在 common 的 {@link ConnectionService} 内部完成映射，避免 loader 模块出现业务分支。</p>
 */
public enum TunnelState {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
    FAILED
}
