package neoproxy.neolinkmc.config;

import java.util.Objects;

/**
 * 从 Minecraft loader 模块传入共享 NeoLinkAPI adapter 的 platform-neutral tunnel 配置。
 *
 * <p>common 模块有意只持有 primitive tunnel 字段。Fabric、Forge、NeoForge 以及
 * 未来的特定版本模块，可以分别决定如何加载 UI/config 文件，而不会把 loader API
 * 泄漏进可复用的 tunnel 生命周期。</p>
 */
public record ConnectionConfig(
        String remoteDomain,
        String localDomain,
        int hookPort,
        int hostConnectPort,
        String key,
        int localPort
) {
    public static final String DEFAULT_KEY = "Free";
    public static final String DEFAULT_LOCAL_DOMAIN = "localhost";

    public ConnectionConfig {
        remoteDomain = requireText(remoteDomain, "remoteDomain");
        localDomain = requireText(localDomain, "localDomain");
        hookPort = requirePort(hookPort, "hookPort");
        hostConnectPort = requirePort(hostConnectPort, "hostConnectPort");
        key = normalizeKey(key);
        localPort = requirePort(localPort, "localPort");
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }

    private static int requirePort(int value, String fieldName) {
        if (value < 1 || value > 65535) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 65535.");
        }
        return value;
    }

    public static String normalizeKey(String value) {
        return value == null || value.trim().isEmpty() ? DEFAULT_KEY : value.trim();
    }
}
