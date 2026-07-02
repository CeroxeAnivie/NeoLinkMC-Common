package neoproxy.neolinkmc.platform;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 共享代码使用的进程级 platform registry。
 *
 * <p>Minecraft loaders 会在很早期初始化 mods，并且 entrypoint 类型各不相同。
 * 这个极小的显式 holder 让 common 代码保持确定性：每个平台都必须在配置或生命周期
 * 代码运行前完成注册，故障也会在真正的源头暴露，而不是之后被隐藏在 null path 后面。</p>
 */
public final class NeoLinkPlatformHolder {
    private static volatile NeoLinkPlatform platform;

    private NeoLinkPlatformHolder() {
    }

    public static void register(NeoLinkPlatform nextPlatform) {
        platform = Objects.requireNonNull(nextPlatform, "nextPlatform");
    }

    public static NeoLinkPlatform get() {
        NeoLinkPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("NeoLinkMC platform has not been registered.");
        }
        return current;
    }

    public static Path configDir() {
        return get().configDir();
    }

    public static String version() {
        return get().version();
    }
}
