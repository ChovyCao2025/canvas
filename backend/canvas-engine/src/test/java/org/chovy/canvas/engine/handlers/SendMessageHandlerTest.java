package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SendMessageHandlerTest {

    @Test
    void sendsUsingChannelFromConfig() {
        CapturingDeliveryService deliveryService = new CapturingDeliveryService();
        SendMessageHandler handler = new SendMessageHandler(deliveryService);
        ExecutionContext ctx = context();

        NodeResult result = handler.executeAsync(Map.of(
                "channel", "SMS",
                "templateId", "tpl-1",
                "content", "hello",
                "successNodeId", "next-ok"
        ), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("success", "next-ok");
        assertThat(deliveryService.lastRequest().channel()).isEqualTo("SMS");
        assertThat(deliveryService.lastRequest().templateId()).isEqualTo("tpl-1");
        assertThat(deliveryService.lastRequest().policyOptions().requireExplicitConsent()).isTrue();
        assertThat(deliveryService.lastRequest().policyOptions().frequencyMax()).isEqualTo(1);
    }

    private static ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
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
            return Mono.just(new DeliveryResult(true, false, 100L, "external-1", null));
        }

        DeliveryRequest lastRequest() {
            return lastRequest.get();
        }

        private static ExternalHttpClient noopHttpClient() {
            return (integrationName, path, payload) -> Mono.just(Map.of());
        }
    }
}
