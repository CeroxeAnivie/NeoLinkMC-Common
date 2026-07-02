package neoproxy.neolinkmc.util;

import neoproxy.neolinkmc.NeoLinkCore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Opens the mod configuration directory without blocking Minecraft's render thread.
 *
 * <p>AWT Desktop can initialize native UI toolkits inside LWJGL clients. Launching the
 * platform file manager from a daemon worker keeps the GUI responsive even when the
 * operating system shell is slow or unavailable.</p>
 */
public final class ConfigFolderOpener {
    private static final ExecutorService OPEN_EXECUTOR = Executors.newSingleThreadExecutor(new DaemonThreadFactory());

    private ConfigFolderOpener() {
    }

    public static void openAsync(Path directory) {
        Path target = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        OPEN_EXECUTOR.execute(() -> openBlocking(target));
    }

    private static void openBlocking(Path directory) {
        try {
            Files.createDirectories(directory);
            Process process = new ProcessBuilder(openCommand(directory))
                    .redirectErrorStream(true)
                    .start();
            process.getInputStream().close();
            process.getOutputStream().close();
        } catch (IOException | RuntimeException e) {
            NeoLinkCore.LOGGER.error("Failed to open config folder: {}", directory, e);
        }
    }

    private static String[] openCommand(Path directory) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String path = directory.toString();
        if (os.contains("win")) {
            return new String[]{"explorer.exe", path};
        }
        if (os.contains("mac")) {
            return new String[]{"open", path};
        }
        return new String[]{"xdg-open", path};
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "NeoLinkMC-ConfigFolderOpener");
            thread.setDaemon(true);
            return thread;
        }
    }
}
