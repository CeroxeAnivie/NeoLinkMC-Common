package neoproxy.neolinkmc.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionServiceMessageTest {
    @Test
    void trafficQuotaMessagesAreLoggedFullyButOnlyImportantOnesReachChat() {
        RecordingMessageHandler handler = new RecordingMessageHandler();
        ConnectionService service = new ConnectionService(handler);

        service.onServerMessage("这个密钥有 9999828.064498688 MB 流量可以消耗。");
        service.onServerMessage("这个密钥有 9999818.064498688 MB 流量可以消耗。");
        service.onServerMessage("这个密钥有 99.5 MB 流量可以消耗。");

        assertEquals(3, handler.logs.size());
        assertEquals(2, handler.chatMessages.size());
        assertEquals("这个密钥有 9999828.064498688 MB 流量可以消耗。", handler.chatMessages.get(0));
        assertEquals("这个密钥有 99.5 MB 流量可以消耗。", handler.chatMessages.get(1));
    }

    private static final class RecordingMessageHandler implements MessageHandler {
        private final List<String> chatMessages = new ArrayList<>();
        private final List<String> logs = new ArrayList<>();

        @Override
        public void send(String message, MessageType type) {
            chatMessages.add(message);
        }

        @Override
        public void log(String message, LogLevel level) {
            logs.add(message);
        }

        @Override
        public void log(String message, LogLevel level, Throwable throwable) {
            logs.add(message);
        }
    }
}
