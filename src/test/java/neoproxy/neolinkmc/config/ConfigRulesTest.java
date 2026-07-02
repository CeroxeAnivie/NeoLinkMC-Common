package neoproxy.neolinkmc.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRulesTest {
    @Test
    void portInputAcceptsEmptyAndValidPortRangeOnly() {
        assertTrue(ConfigRules.isValidPortInput(""));
        assertTrue(ConfigRules.isValidPortInput("1"));
        assertTrue(ConfigRules.isValidPortInput("65535"));

        assertFalse(ConfigRules.isValidPortInput("0"));
        assertFalse(ConfigRules.isValidPortInput("65536"));
        assertFalse(ConfigRules.isValidPortInput("abc"));
    }

    @Test
    void maxPlayersUsesPlayerRangeInsteadOfPortRange() {
        assertTrue(ConfigRules.isValidMaxPlayersInput(""));
        assertTrue(ConfigRules.isValidMaxPlayersInput("1"));
        assertTrue(ConfigRules.isValidMaxPlayersInput("1000"));

        assertFalse(ConfigRules.isValidMaxPlayersInput("0"));
        assertFalse(ConfigRules.isValidMaxPlayersInput("1001"));
        assertFalse(ConfigRules.isValidMaxPlayersInput("65535"));
    }

    @Test
    void parsingFallsBackToDefaultsOnInvalidInput() {
        assertEquals(25565, ConfigRules.parsePortOrDefault("65536", 25565));
        assertEquals(25565, ConfigRules.parsePortOrDefault("abc", 25565));
        assertEquals(24454, ConfigRules.parsePortOrDefault("24454", 25565));

        assertEquals(8, ConfigRules.parseMaxPlayersOrDefault("1001", 8));
        assertEquals(8, ConfigRules.parseMaxPlayersOrDefault("abc", 8));
        assertEquals(32, ConfigRules.parseMaxPlayersOrDefault("32", 8));
    }

    @Test
    void cyclicOptionsHaveStableOrder() {
        assertEquals(
                OnlineMode.OFFLINE_OFFLINE_UUID_ONLY,
                ConfigRules.nextOnlineMode(OnlineMode.OFFLINE_TRY_ONLINE_UUID_FIRST)
        );
        assertEquals("CREATIVE", ConfigRules.nextGameTypeName("SURVIVAL"));
        assertEquals("SURVIVAL", ConfigRules.nextGameTypeName("invalid"));
    }
}
