package org.chovy.canvas.web;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.WhatsAppConversationReplyPayload;
import org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationProviderWebhookControllerTest {

    @Test
    void whatsappWebhookIngressMapsRawPayloadAndDelegatesToHarness() {
        TenantContextResolver resolver = tenantResolver();
        ConversationAdapterHarness harness = mock(ConversationAdapterHarness.class);
        WhatsAppWebhookPayloadMapper mapper = mock(WhatsAppWebhookPayloadMapper.class);
        Map<String, Object> rawPayload = Map.of("entry", List.of());
        WhatsAppConversationReplyPayload adapterPayload = new WhatsAppConversationReplyPayload(
                10L,
                20L,
                "exec-1",
                "whatsapp:15551234567",
                "cloud_api",
                "wamid.1",
                "whatsapp:entry-1:wamid.1",
                "hello",
                null,
                null,
                null,
                Map.of("phoneNumberId", "phone-number-id-1"),
                null);
        when(mapper.toAdapterPayloads(rawPayload)).thenReturn(List.of(adapterPayload));
        when(harness.ingest(7L, "WHATSAPP", adapterPayload, "operator-1"))
                .thenReturn(new ConversationIngressResp(100L, 200L, "RECORDED", false, 1));
        ConversationProviderWebhookController controller = new ConversationProviderWebhookController(
                resolver,
                harness,
                mapper);

        StepVerifier.create(controller.ingestWhatsApp(rawPayload))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).singleElement().satisfies(resp -> {
                        assertThat(resp.sessionId()).isEqualTo(100L);
                        assertThat(resp.messageId()).isEqualTo(200L);
                    });
                })
                .verifyComplete();

        verify(mapper).toAdapterPayloads(rawPayload);
        verify(harness).ingest(7L, "WHATSAPP", adapterPayload, "operator-1");
    }

    @Test
    void whatsappWebhookIngressReturnsEmptyWhenPayloadHasNoMessages() {
        TenantContextResolver resolver = tenantResolver();
        ConversationAdapterHarness harness = mock(ConversationAdapterHarness.class);
        WhatsAppWebhookPayloadMapper mapper = mock(WhatsAppWebhookPayloadMapper.class);
        Map<String, Object> rawPayload = Map.of("entry", List.of());
        when(mapper.toAdapterPayloads(rawPayload)).thenReturn(List.of());
        ConversationProviderWebhookController controller = new ConversationProviderWebhookController(
                resolver,
                harness,
                mapper);

        StepVerifier.create(controller.ingestWhatsApp(rawPayload))
                .assertNext(response -> assertThat(response.getData()).isEmpty())
                .verifyComplete();

        verify(harness, never()).ingest(any(), any(String.class), any(), any());
    }

    private TenantContextResolver tenantResolver() {
        TenantContextResolver resolver = mock(TenantContextResolver.class);
        when(resolver.currentOrError()).thenReturn(Mono.just(new TenantContext(7L, RoleNames.OPERATOR, "operator-1")));
        return resolver;
    }
}
