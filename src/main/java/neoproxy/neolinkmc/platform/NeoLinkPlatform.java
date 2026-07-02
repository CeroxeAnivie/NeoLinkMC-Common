package neoproxy.neolinkmc.platform;

import java.nio.file.Path;

/**
 * 共享 NeoLinkMC core 所需的 loader-specific 服务。
 *
 * <p>common 模块负责 tunnel 生命周期，但不能导入 Fabric、Forge 或 NeoForge class。
 * 每个 loader 模块都提供这个窄 adapter，确保所有 platform API 都停留在代码库边界。</p>
 */
public interface NeoLinkPlatform {
    Path configDir();

    String version();
}
