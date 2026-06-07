package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationIngressReq;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.ConversationIngressService;
import org.chovy.canvas.domain.demo.DemoSandboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoSandboxControllerTest {

    @Test
    void installRequiresAuthenticatedContextAndDelegatesToService() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        DemoSandboxController controller = new DemoSandboxController(service, resolver, mock(ConversationIngressService.class));
        when(service.install(8L, "Retail Demo", 14)).thenReturn(new DemoSandboxService.Sandbox(
                8L, "Retail Demo", "DEMO_TENANT_8", "ACTIVE",
                Instant.parse("2026-06-17T00:00:00Z"), null));

        StepVerifier.create(controller.install(new DemoSandboxController.InstallRequest(8L, "Retail Demo", 14)))
                .assertNext(response -> assertThat(response.getData().demoMarker()).isEqualTo("DEMO_TENANT_8"))
                .verifyComplete();
    }

    @Test
    void resetUsesAuthenticatedOperator() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        DemoSandboxController controller = new DemoSandboxController(service, resolver, mock(ConversationIngressService.class));
        when(service.reset(8L, "seller-1")).thenReturn(new DemoSandboxService.ResetResult(
                8L, "DEMO_TENANT_8", Instant.parse("2026-06-03T00:00:00Z")));

        StepVerifier.create(controller.reset(8L))
                .assertNext(response -> assertThat(response.getData().resetAt())
                        .isEqualTo(Instant.parse("2026-06-03T00:00:00Z")))
                .verifyComplete();

        verify(service).reset(8L, "seller-1");
    }

    @Test
    void expiredRequiresAuthenticatedContext() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        DemoSandboxController controller = new DemoSandboxController(service, resolver, mock(ConversationIngressService.class));
        when(service.expired()).thenReturn(List.of(new DemoSandboxService.Sandbox(
                9L, "Old Demo", "DEMO_TENANT_9", "EXPIRED",
                Instant.parse("2026-06-01T00:00:00Z"), null)));

        StepVerifier.create(controller.expired())
                .assertNext(response -> assertThat(response.getData()).hasSize(1))
                .verifyComplete();
    }

    @Test
    void rejectsMissingTenantContext() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
        DemoSandboxController controller = new DemoSandboxController(service, resolver, mock(ConversationIngressService.class));

        StepVerifier.create(controller.expired())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(SecurityException.class)
                        .hasMessageContaining("AUTH_003"))
                .verify();
    }

    @Test
    void conversationReplyUsesSandboxChannelAndTargetTenant() {
        DemoSandboxService service = mock(DemoSandboxService.class);
        ConversationIngressService conversationService = mock(ConversationIngressService.class);
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(operator()));
        when(conversationService.ingest(eq(8L), any(ConversationIngressReq.class)))
                .thenReturn(new ConversationIngressResp(100L, 200L, "RECORDED", false, 1));
        DemoSandboxController controller = new DemoSandboxController(service, resolver, conversationService);

        StepVerifier.create(controller.reply(8L, new DemoSandboxController.ConversationReplyRequest(
                        10L,
                        20L,
                        "exec-1",
                        "user-1",
                        "reply-1",
                        "event-1",
                        "yes please",
                        "PRODUCT_A",
                        Map.of("buttonId", "product-a"))))
                .assertNext(response -> {
                    assertThat(response.getData().sessionId()).isEqualTo(100L);
                    assertThat(response.getData().resumedWaitCount()).isEqualTo(1);
                })
                .verifyComplete();

        ArgumentCaptor<ConversationIngressReq> requestCaptor = ArgumentCaptor.forClass(ConversationIngressReq.class);
        verify(conversationService).ingest(eq(8L), requestCaptor.capture());
        ConversationIngressReq req = requestCaptor.getValue();
        assertThat(req.canvasId()).isEqualTo(10L);
        assertThat(req.versionId()).isEqualTo(20L);
        assertThat(req.executionId()).isEqualTo("exec-1");
        assertThat(req.userId()).isEqualTo("user-1");
        assertThat(req.channel()).isEqualTo("SANDBOX");
        assertThat(req.provider()).isEqualTo("DEFAULT");
        assertThat(req.externalMessageId()).isEqualTo("reply-1");
        assertThat(req.eventId()).isEqualTo("event-1");
        assertThat(req.messageType()).isEqualTo("TEXT");
        assertThat(req.text()).isEqualTo("yes please");
        assertThat(req.intent()).isEqualTo("PRODUCT_A");
        assertThat(req.attributes()).containsEntry("buttonId", "product-a")
                .containsEntry("sandboxOperator", "seller-1");
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "seller-1");
    }
}
