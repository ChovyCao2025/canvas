package org.chovy.canvas.engine.channel;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppCloudApiConnectorTest {

    @Test
    void sendsTemplateMessageThroughCloudApiClient() {
        RecordingClient client = new RecordingClient(Map.of(
                "messages", List.of(Map.of("id", "wamid.template-1"))));
        WhatsAppCloudApiConnector connector = new WhatsAppCloudApiConnector(
                client,
                "phone-number-id-1",
                "access-token-1",
                "en_US");
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("plan", "Pro");
        variables.put("firstName", "Mia");

        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                7L,
                "WHATSAPP",
                "CLOUD_API",
                "whatsapp:+15551234567",
                payload("welcome_template", Map.of(), variables)));

        assertThat(result.accepted()).isTrue();
        assertThat(result.externalMessageId()).isEqualTo("wamid.template-1");
        assertThat(result.status()).isEqualTo("ACCEPTED");
        assertThat(client.phoneNumberId.get()).isEqualTo("phone-number-id-1");
        assertThat(client.accessToken.get()).isEqualTo("access-token-1");
        assertThat(client.payload.get()).containsEntry("messaging_product", "whatsapp")
                .containsEntry("to", "15551234567")
                .containsEntry("type", "template");
        assertThat(client.payload.get()).containsEntry("template", Map.of(
                "name", "welcome_template",
                "language", Map.of("code", "en_US"),
                "components", List.of(Map.of(
                        "type", "body",
                        "parameters", List.of(
                                Map.of("type", "text", "text", "Pro"),
                                Map.of("type", "text", "text", "Mia"))))));
    }

    @Test
    void sendsSessionTextMessageWhenTemplateIsMissing() {
        RecordingClient client = new RecordingClient(Map.of(
                "messages", List.of(Map.of("id", "wamid.text-1"))));
        WhatsAppCloudApiConnector connector = new WhatsAppCloudApiConnector(
                client,
                "phone-number-id-1",
                "access-token-1",
                "en_US");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("body", "Thanks for your reply");

        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                7L,
                "WHATSAPP",
                "CLOUD_API",
                "+15551234567",
                payload(null, content, Map.of())));

        assertThat(result.accepted()).isTrue();
        assertThat(result.externalMessageId()).isEqualTo("wamid.text-1");
        assertThat(client.payload.get()).containsEntry("type", "text")
                .containsEntry("text", Map.of("body", "Thanks for your reply"));
    }

    @Test
    void failsClosedWhenCredentialsAreMissing() {
        RecordingClient client = new RecordingClient(Map.of());
        WhatsAppCloudApiConnector connector = new WhatsAppCloudApiConnector(client, "", "", "en_US");

        ChannelConnector.ConnectorSendResult result = connector.send(new ChannelConnector.ConnectorSendRequest(
                7L,
                "WHATSAPP",
                "CLOUD_API",
                "+15551234567",
                payload("welcome_template", Map.of(), Map.of())));

        assertThat(result.accepted()).isFalse();
        assertThat(result.status()).isEqualTo("DISABLED");
        assertThat(result.reason()).contains("not configured");
        assertThat(client.payload.get()).isNull();
    }

    private Map<String, Object> payload(String templateId,
                                        Map<String, Object> content,
                                        Map<String, Object> variables) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("templateId", templateId);
        payload.put("content", content);
        payload.put("variables", variables);
        payload.put("idempotencyKey", "idem-1");
        return payload;
    }

    private static class RecordingClient implements WhatsAppCloudApiClient {
        private final Map<String, Object> response;
        private final AtomicReference<String> phoneNumberId = new AtomicReference<>();
        private final AtomicReference<String> accessToken = new AtomicReference<>();
        private final AtomicReference<Map<String, Object>> payload = new AtomicReference<>();

        private RecordingClient(Map<String, Object> response) {
            this.response = response;
        }

        @Override
        public Map<String, Object> sendMessage(String phoneNumberId,
                                               String accessToken,
                                               Map<String, Object> payload) {
            this.phoneNumberId.set(phoneNumberId);
            this.accessToken.set(accessToken);
            this.payload.set(payload);
            return response;
        }
    }
}
