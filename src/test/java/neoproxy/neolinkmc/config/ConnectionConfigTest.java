package neoproxy.neolinkmc.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectionConfigTest {
    @Test
    void constructorNormalizesTextFieldsAndKey() {
        ConnectionConfig config = new ConnectionConfig(
                " example.com ",
                " localhost ",
                9100,
                9101,
                " ",
                25565
        );

        assertEquals("example.com", config.remoteDomain());
        assertEquals("localhost", config.localDomain());
        assertEquals(ConnectionConfig.DEFAULT_KEY, config.key());
    }

    @Test
    void constructorRejectsInvalidPortsAndBlankDomains() {
        assertThrows(IllegalArgumentException.class, () ->
                new ConnectionConfig("", "localhost", 44801, 44802, "Free", 25565));
        assertThrows(IllegalArgumentException.class, () ->
                new ConnectionConfig("example.com", "localhost", 0, 44802, "Free", 25565));
        assertThrows(IllegalArgumentException.class, () ->
                new ConnectionConfig("example.com", "localhost", 44801, 65536, "Free", 25565));
    }

    @Test
    void sharedDefaultsStayAlignedWithBundledTemplate() {
        InputStream templateStream = Objects.requireNonNull(
                ConnectionConfigTest.class.getResourceAsStream("/templates/config.json")
        );
        try (InputStreamReader reader = new InputStreamReader(templateStream, StandardCharsets.UTF_8)) {
            JsonObject template = JsonParser.parseReader(reader).getAsJsonObject();

            assertEquals(SharedNeoLinkConfig.DEFAULT_REMOTE_DOMAIN, template.get("remote_domain").getAsString());
            assertEquals(String.valueOf(SharedNeoLinkConfig.DEFAULT_HOOK_PORT), template.get("host_hook_port").getAsString());
            assertEquals(String.valueOf(SharedNeoLinkConfig.DEFAULT_HOST_CONNECT_PORT), template.get("host_connect_port").getAsString());
            assertEquals(String.valueOf(SharedNeoLinkConfig.DEFAULT_LOCAL_PORT), template.get("local_port").getAsString());
            assertEquals(SharedNeoLinkConfig.DEFAULT_LOCAL_DOMAIN, template.get("local_domain").getAsString());
            assertEquals(SharedNeoLinkConfig.DEFAULT_GAME_TYPE, template.get("gamemode").getAsString());
            assertEquals(SharedNeoLinkConfig.DEFAULT_ONLINE_MODE.name(), template.get("onlinemode").getAsString());
            assertEquals(SharedNeoLinkConfig.DEFAULT_PVP_ALLOWED, template.get("pvp_allowed").getAsBoolean());
            assertEquals(SharedNeoLinkConfig.DEFAULT_ALLOW_CHEATS, template.get("allow_cheats").getAsBoolean());
            assertEquals(String.valueOf(SharedNeoLinkConfig.DEFAULT_MAX_PLAYERS), template.get("max_players").getAsString());
        } catch (Exception e) {
            throw new AssertionError("Bundled default config template must be readable.", e);
        }
    }
}
