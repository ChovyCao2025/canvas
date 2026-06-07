package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationAdapterHarnessTest {

    @Test
    void sandboxAdapterMapsOperatorReplyToNormalizedIngressRequest() {
        SandboxConversationReplyAdapter adapter = new SandboxConversationReplyAdapter();

        ConversationIngressReq req = adapter.toIngress(new SandboxConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "user-1",
                        "reply-1",
                        "event-1",
                        "yes please",
                        "PRODUCT_A",
                        Map.of("buttonId", "product-a")),
                new ConversationAdapterContext(8L, "seller-1"));

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
                .containsEntry("sandboxOperator", "seller-1")
                .containsEntry("adapter", "SANDBOX");
        assertThat(req.occurredAt()).isNull();
    }

    @Test
    void harnessDelegatesAdapterOutputToConversationIngress() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterHarness harness = new ConversationAdapterHarness(ingressService);
        when(ingressService.ingest(eq(8L), any(ConversationIngressReq.class))).thenReturn(new ConversationIngressResp(
                100L,
                200L,
                "RECORDED",
                false,
                1));

        ConversationIngressResp resp = harness.ingest(
                8L,
                new SandboxConversationReplyAdapter(),
                new SandboxConversationReplyPayload(
                        10L,
                        20L,
                        "exec-1",
                        "user-1",
                        "reply-1",
                        "event-1",
                        "yes please",
                        "PRODUCT_A",
                        Map.of()),
                "seller-1");

        assertThat(resp.sessionId()).isEqualTo(100L);
        assertThat(resp.messageId()).isEqualTo(200L);
        ArgumentCaptor<ConversationIngressReq> requestCaptor = ArgumentCaptor.forClass(ConversationIngressReq.class);
        verify(ingressService).ingest(org.mockito.ArgumentMatchers.eq(8L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().channel()).isEqualTo("SANDBOX");
        assertThat(requestCaptor.getValue().attributes()).containsEntry("sandboxOperator", "seller-1");
    }

    @Test
    void harnessConvertsRawPayloadUsingDeclaredPayloadType() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterHarness harness = new ConversationAdapterHarness(
                ingressService,
                new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter())));
        when(ingressService.ingest(eq(8L), any(ConversationIngressReq.class))).thenReturn(new ConversationIngressResp(
                100L,
                200L,
                "RECORDED",
                false,
                1));

        ConversationIngressResp resp = harness.ingestRaw(
                8L,
                "sandbox",
                Map.of(
                        "canvasId", 10L,
                        "versionId", 20L,
                        "executionId", "exec-1",
                        "userId", "user-1",
                        "externalMessageId", "reply-1",
                        "eventId", "event-1",
                        "text", "yes please",
                        "intent", "PRODUCT_A",
                        "attributes", Map.of("buttonId", "product-a")),
                "seller-1");

        assertThat(resp.messageId()).isEqualTo(200L);
        ArgumentCaptor<ConversationIngressReq> requestCaptor = ArgumentCaptor.forClass(ConversationIngressReq.class);
        verify(ingressService).ingest(eq(8L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().channel()).isEqualTo("SANDBOX");
        assertThat(requestCaptor.getValue().userId()).isEqualTo("user-1");
        assertThat(requestCaptor.getValue().text()).isEqualTo("yes please");
        assertThat(requestCaptor.getValue().attributes()).containsEntry("sandboxOperator", "seller-1")
                .containsEntry("buttonId", "product-a");
    }

    @Test
    void harnessRejectsAdaptersThatDoNotProduceIngressRequests() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterHarness harness = new ConversationAdapterHarness(ingressService);

        assertThatThrownBy(() -> harness.ingest(8L, (payload, context) -> null, new Object(), "seller-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversation adapter must produce an ingress request");

        verify(ingressService, never()).ingest(any(), any());
    }

    @Test
    void harnessRejectsAdaptersThatOmitRequiredRoutingFields() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterHarness harness = new ConversationAdapterHarness(ingressService);
        ConversationReplyAdapter<Object> adapter = (payload, context) -> new ConversationIngressReq(
                10L,
                20L,
                "exec-1",
                " ",
                null,
                "DEFAULT",
                "reply-1",
                "event-1",
                "TEXT",
                "yes please",
                "PRODUCT_A",
                Map.of(),
                null);

        assertThatThrownBy(() -> harness.ingest(8L, adapter, new Object(), "seller-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversation adapter ingress must include userId and channel");

        verify(ingressService, never()).ingest(any(), any());
    }

    @Test
    void harnessRejectsAdaptersThatOmitProviderAndIdempotencyFields() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterHarness harness = new ConversationAdapterHarness(ingressService);
        ConversationReplyAdapter<Object> adapter = (payload, context) -> new ConversationIngressReq(
                10L,
                20L,
                "exec-1",
                "user-1",
                "WHATSAPP",
                " ",
                null,
                " ",
                "TEXT",
                "yes please",
                "PRODUCT_A",
                Map.of(),
                null);

        assertThatThrownBy(() -> harness.ingest(8L, adapter, new Object(), "seller-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversation adapter ingress must include provider, externalMessageId, and eventId");

        verify(ingressService, never()).ingest(any(), any());
    }

}
