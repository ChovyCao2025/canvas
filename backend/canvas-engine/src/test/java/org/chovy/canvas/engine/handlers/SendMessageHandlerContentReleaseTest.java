package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.content.MarketingContentReleaseService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.http.ExternalHttpClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SendMessageHandlerContentReleaseTest {

    @Test
    @SuppressWarnings("unchecked")
    void contentReleaseKeyResolvesImmutableSnapshotIntoDeliveryPayload() {
        CapturingDeliveryService deliveryService = new CapturingDeliveryService();
        MarketingContentReleaseService releaseService = mock(MarketingContentReleaseService.class);
        when(releaseService.resolve(
                argThat(tenant -> tenant != null && tenant.tenantId().equals(0L)),
                eq("template-welcome"),
                eq(Map.of("firstName", "Alice"))))
                .thenReturn(new MarketingContentReleaseService.ResolvedRelease(
                        "template-welcome",
                        "TEMPLATE",
                        "welcome",
                        3,
                        "ACTIVE",
                        "Hi Alice",
                        "<h1>Welcome</h1>",
                        List.of(),
                        "{\"templateKey\":\"welcome\"}",
                        List.of(new MarketingContentReleaseService.ResolvedAsset(
                                "hero-video",
                                "READY",
                                "{\"assetKey\":\"hero-video\",\"storageUrl\":\"https://cdn.example.com/hero.mp4\"}"))));
        SendMessageHandler handler = new SendMessageHandler(deliveryService, releaseService);
        ExecutionContext ctx = context();
        ctx.setContextValue("firstName", "Alice");

        NodeResult result = handler.executeAsync(Map.of(
                "channel", "EMAIL",
                "contentReleaseKey", "template-welcome",
                "variables", Map.of("firstName", "$.firstName"),
                "successNodeId", "next-ok"
        ), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(deliveryService.lastRequest().templateId()).isEqualTo("template-welcome");
        Map<String, Object> content = (Map<String, Object>) deliveryService.lastRequest().payload().get("content");
        assertThat(content)
                .containsEntry("subject", "Hi Alice")
                .containsEntry("body", "<h1>Welcome</h1>")
                .containsEntry("contentReleaseKey", "template-welcome")
                .containsEntry("contentReleaseVersion", 3);
        List<Map<String, Object>> assets = (List<Map<String, Object>>) content.get("assets");
        assertThat(assets).hasSize(1);
        assertThat(assets.get(0))
                .containsEntry("assetKey", "hero-video")
                .containsEntry("status", "READY");
        assertThat(result.output())
                .containsEntry("contentReleaseKey", "template-welcome")
                .containsEntry("contentReleaseVersion", 3);
    }

    private static ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(0L);
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
