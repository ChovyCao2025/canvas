package org.chovy.canvas.engine.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.chovy.canvas.engine.channel.ChannelConnector;
import org.chovy.canvas.engine.channel.ChannelConnectorRegistry;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReachDeliveryServiceConnectorDispatchTest {

    @Test
    void dispatchToProviderUsesResolvedRealConnectorAndPreservesOutboxPayload() {
        RecordingConnector connector = new RecordingConnector(new ChannelConnector.ConnectorSendResult(
                true, "wamid-1", "ACCEPTED", null));
        ChannelConnectorRegistry registry = mock(ChannelConnectorRegistry.class);
        when(registry.resolve(7L, "WHATSAPP", "CLOUD_API")).thenReturn(connector);
        RecordingExternalHttpClient httpClient = new RecordingExternalHttpClient();
        ReachDeliveryService service = new ReachDeliveryService(
                mock(MessageSendRecordMapper.class),
                new ObjectMapper(),
                httpClient,
                registry);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(MapFieldKeys.CHANNEL, "WHATSAPP");
        payload.put(MapFieldKeys.TEMPLATE_ID, "welcome_template");
        payload.put(MapFieldKeys.CONTENT, Map.of("language", "en_US"));
        payload.put(MapFieldKeys.VARIABLES, Map.of("firstName", "Mia"));
        payload.put(MapFieldKeys.IDEMPOTENCY_KEY, "idem-1");
        DeliveryOutboxDO outbox = DeliveryOutboxDO.builder()
                .id(100L)
                .tenantId(7L)
                .messageSendRecordId(200L)
                .executionId("exec-1")
                .canvasId(300L)
                .userId("whatsapp:+15551234567")
                .nodeId("send-1")
                .channel("WHATSAPP")
                .provider("CLOUD_API")
                .payloadJson(json(payload))
                .idempotencyKey("idem-1")
                .build();

        Map<String, Object> response = service.dispatchToProvider(outbox).block();

        assertThat(response).containsEntry(MapFieldKeys.MESSAGE_ID, "wamid-1")
                .containsEntry("connectorStatus", "ACCEPTED");
        assertThat(connector.request.get()).isNotNull();
        assertThat(connector.request.get().tenantId()).isEqualTo(7L);
        assertThat(connector.request.get().channel()).isEqualTo("WHATSAPP");
        assertThat(connector.request.get().provider()).isEqualTo("CLOUD_API");
        assertThat(connector.request.get().userId()).isEqualTo("whatsapp:+15551234567");
        assertThat(connector.request.get().payload()).containsEntry(MapFieldKeys.TEMPLATE_ID, "welcome_template");
        assertThat(httpClient.calls).hasValue(0);
    }

    private String json(Map<String, Object> payload) {
        try {
            return new ObjectMapper().writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static class RecordingConnector implements ChannelConnector {
        private final ConnectorSendResult result;
        private final AtomicReference<ConnectorSendRequest> request = new AtomicReference<>();

        private RecordingConnector(ConnectorSendResult result) {
            this.result = result;
        }

        @Override
        public ConnectorMode mode() {
            return ConnectorMode.REAL;
        }

        @Override
        public ConnectorHealth health() {
            return new ConnectorHealth("UP", "ready");
        }

        @Override
        public ConnectorCapabilities capabilities() {
            return new ConnectorCapabilities(true, true, Map.of());
        }

        @Override
        public ConnectorSendResult send(ConnectorSendRequest request) {
            this.request.set(request);
            return result;
        }

        @Override
        public ConnectorReceiptResult parseReceipt(Map<String, Object> rawPayload) {
            return new ConnectorReceiptResult(null, "UNSUPPORTED", Map.of());
        }
    }

    private static class RecordingExternalHttpClient implements ExternalHttpClient {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Mono<Map<String, Object>> postJson(String integrationName, String path, Map<String, Object> payload) {
            calls.incrementAndGet();
            return Mono.just(Map.of(MapFieldKeys.MESSAGE_ID, "legacy-reach"));
        }
    }
}
