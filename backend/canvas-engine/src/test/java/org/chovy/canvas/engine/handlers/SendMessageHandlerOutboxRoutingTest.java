package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SendMessageHandlerOutboxRoutingTest {

    @ParameterizedTest
    @ValueSource(strings = {"SMS", "EMAIL", "PUSH", "WECHAT", "IN_APP"})
    void routesEveryChannelThroughDeliveryBoundaryAsSubmitted(String channel) {
        CapturingDeliveryService deliveryService = new CapturingDeliveryService();
        SendMessageHandler handler = new SendMessageHandler(deliveryService);

        NodeResult result = handler.executeAsync(Map.of(
                "channel", channel.toLowerCase(),
                "templateId", "tpl-1",
                "body", "hello",
                "successNodeId", "next-ok"
        ), context()).block();

        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("success", "next-ok");
        assertThat(result.output()).containsEntry("sendStatus", "SUBMITTED");
        assertThat(deliveryService.lastRequest()).isNotNull();
        assertThat(deliveryService.lastRequest().tenantId()).isEqualTo(1L);
        assertThat(deliveryService.lastRequest().channel()).isEqualTo(channel);
        assertThat(deliveryService.lastRequest().payload()).containsEntry("channel", channel);
    }

    private static ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(1L);
        ctx.setExecutionId("exec-send-message");
        ctx.setCanvasId(20L);
        ctx.setUserId("user-1");
        return ctx;
    }

    private static class CapturingDeliveryService extends ReachDeliveryService {
        private final AtomicReference<DeliveryRequest> lastRequest = new AtomicReference<>();

        CapturingDeliveryService() {
            super(null, new ObjectMapper(), noopHttpClient());
        }

        @Override
        public Mono<DeliveryResult> send(DeliveryRequest request) {
            lastRequest.set(request);
            return Mono.just(new DeliveryResult(true, false, 100L, null, null));
        }

        DeliveryRequest lastRequest() {
            return lastRequest.get();
        }

        private static ExternalHttpClient noopHttpClient() {
            return (integrationName, path, payload) -> Mono.just(Map.of());
        }
    }
}
