package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressReq;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.ConversationIngressService;
import org.chovy.canvas.domain.conversation.ConversationMessageView;
import org.chovy.canvas.domain.conversation.ConversationSessionView;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationControllerTest {

    @Test
    void ingressPassesCurrentTenantToService() {
        ConversationIngressService service = mock(ConversationIngressService.class);
        TenantContextResolver resolver = tenantResolver();
        ConversationIngressReq req = ingress();
        when(service.ingest(7L, req)).thenReturn(new ConversationIngressResp(100L, 200L, "RECORDED", false, 1));
        ConversationController controller = new ConversationController(service, resolver);

        StepVerifier.create(controller.ingest(req))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().sessionId()).isEqualTo(100L);
                    assertThat(response.getData().resumedWaitCount()).isEqualTo(1);
                })
                .verifyComplete();

        verify(service).ingest(7L, req);
    }

    @Test
    void adapterIngressPassesCurrentTenantOperatorAndRawPayloadToHarness() {
        ConversationIngressService service = mock(ConversationIngressService.class);
        ConversationAdapterHarness harness = mock(ConversationAdapterHarness.class);
        TenantContextResolver resolver = tenantResolver();
        Map<String, Object> payload = Map.of(
                "canvasId", 10L,
                "versionId", 20L,
                "executionId", "exec-1",
                "userId", "user-1",
                "externalMessageId", "reply-1",
                "eventId", "event-1",
                "text", "yes",
                "intent", "PRODUCT_A");
        when(harness.ingestRaw(7L, "sandbox", payload, "operator-1"))
                .thenReturn(new ConversationIngressResp(100L, 200L, "RECORDED", false, 1));
        ConversationController controller = new ConversationController(service, resolver, harness);

        StepVerifier.create(controller.ingestAdapter("sandbox", payload))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().messageId()).isEqualTo(200L);
                })
                .verifyComplete();

        verify(harness).ingestRaw(7L, "sandbox", payload, "operator-1");
    }

    @Test
    void listSessionsPassesTenantFiltersAndBoundedLimit() {
        ConversationIngressService service = mock(ConversationIngressService.class);
        TenantContextResolver resolver = tenantResolver();
        ConversationSessionView view = new ConversationSessionView(
                100L, 7L, 10L, 20L, "exec-1", "user-1", "WHATSAPP", "DEFAULT",
                "ACTIVE", 2, Map.of("intent", "PRODUCT_A"), now(), null, now(), now());
        when(service.listRecentSessions(7L, "user-1", "whatsapp", 100)).thenReturn(List.of(view));
        ConversationController controller = new ConversationController(service, resolver);

        StepVerifier.create(controller.list("user-1", "whatsapp", 500))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).singleElement()
                            .satisfies(session -> assertThat(session.id()).isEqualTo(100L));
                })
                .verifyComplete();

        verify(service).listRecentSessions(7L, "user-1", "whatsapp", 100);
    }

    @Test
    void listMessagesPassesTenantSessionAndBoundedLimit() {
        ConversationIngressService service = mock(ConversationIngressService.class);
        TenantContextResolver resolver = tenantResolver();
        ConversationMessageView view = new ConversationMessageView(
                200L, 7L, 100L, "INBOUND", "TEXT", "msg-1",
                "yes", "PRODUCT_A", Map.of("text", "yes"), now());
        when(service.listMessages(7L, 100L, 1)).thenReturn(List.of(view));
        ConversationController controller = new ConversationController(service, resolver);

        StepVerifier.create(controller.messages(100L, 0))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).singleElement()
                            .satisfies(message -> assertThat(message.id()).isEqualTo(200L));
                })
                .verifyComplete();

        verify(service).listMessages(7L, 100L, 1);
    }

    private TenantContextResolver tenantResolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }

    private ConversationIngressReq ingress() {
        return new ConversationIngressReq(
                10L,
                20L,
                "exec-1",
                "user-1",
                "WHATSAPP",
                "DEFAULT",
                "msg-1",
                "evt-1",
                "TEXT",
                "yes",
                "PRODUCT_A",
                Map.of("locale", "en-US"),
                now());
    }

    private LocalDateTime now() {
        return LocalDateTime.of(2026, 6, 5, 10, 0);
    }
}
