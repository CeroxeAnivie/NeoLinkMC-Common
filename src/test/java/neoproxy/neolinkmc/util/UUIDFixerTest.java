package neoproxy.neolinkmc.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UUIDFixerTest {
    @AfterEach
    void tearDown() {
        UUIDFixer.resetStateForTest();
    }

    @Test
    void hookEntrySkipsLookupWhenOnlineFirstIsDisabled() {
        AtomicInteger invocations = new AtomicInteger();
        UUIDFixer.setOfficialUuidResolverForTest(name -> {
            invocations.incrementAndGet();
            return UUID.randomUUID();
        });

        assertNull(UUIDFixer.hookEntry("PlayerOne"));
        assertEquals(0, invocations.get());
    }

    @Test
    void hookEntryHonorsAlwaysOfflineListWithNormalizedNames() {
        AtomicInteger invocations = new AtomicInteger();
        UUIDFixer.setTryOnlineFirst(true);
        UUIDFixer.setAlwaysOfflinePlayers(List.of("  PlayerOne  "));
        UUIDFixer.setOfficialUuidResolverForTest(name -> {
            invocations.incrementAndGet();
            return UUID.randomUUID();
        });

        assertNull(UUIDFixer.hookEntry("playerone"));
        assertEquals(0, invocations.get());
    }

    @Test
    void hookEntryCachesResolverResultsByNormalizedName() {
        AtomicInteger invocations = new AtomicInteger();
        UUID expected = UUID.randomUUID();
        UUIDFixer.setTryOnlineFirst(true);
        UUIDFixer.setOfficialUuidResolverForTest(name -> {
            invocations.incrementAndGet();
            return expected;
        });

        assertEquals(expected, UUIDFixer.hookEntry("PlayerOne"));
        assertEquals(expected, UUIDFixer.hookEntry(" playerone "));
        assertEquals(1, invocations.get());
    }
}
