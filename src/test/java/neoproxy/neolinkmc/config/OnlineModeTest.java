package neoproxy.neolinkmc.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnlineModeTest {
    @Test
    void parseFallsBackToDefaultForBlankOrUnknownValues() {
        assertEquals(SharedNeoLinkConfig.DEFAULT_ONLINE_MODE, OnlineMode.parse(null));
        assertEquals(SharedNeoLinkConfig.DEFAULT_ONLINE_MODE, OnlineMode.parse(" "));
        assertEquals(SharedNeoLinkConfig.DEFAULT_ONLINE_MODE, OnlineMode.parse("unknown"));
    }

    @Test
    void flagsMapToExpectedMode() {
        assertEquals(OnlineMode.ONLINE_ONLINE_UUID_ONLY, OnlineMode.of(true, false));
        assertEquals(OnlineMode.OFFLINE_TRY_ONLINE_UUID_FIRST, OnlineMode.of(false, true));
        assertEquals(OnlineMode.OFFLINE_OFFLINE_UUID_ONLY, OnlineMode.of(false, false));
    }
}
