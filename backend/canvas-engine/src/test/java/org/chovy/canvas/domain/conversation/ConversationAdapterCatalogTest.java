package org.chovy.canvas.domain.conversation;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationAdapterCatalogTest {

    @Test
    void findsAdaptersByNormalizedKey() {
        ConversationAdapterCatalog catalog = new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter()));

        ConversationReplyAdapter<Object> adapter = catalog.require(" sandbox ");

        assertThat(adapter).isInstanceOf(SandboxConversationReplyAdapter.class);
        assertThat(catalog.keys()).containsExactly("SANDBOX");
    }

    @Test
    void rejectsDuplicateAdapterKeys() {
        assertThatThrownBy(() -> new ConversationAdapterCatalog(List.of(
                        new SandboxConversationReplyAdapter(),
                        new SandboxConversationReplyAdapter())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate conversation adapter key: SANDBOX");
    }

    @Test
    void harnessRoutesPayloadByAdapterKey() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterCatalog catalog = new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter()));
        ConversationAdapterHarness harness = new ConversationAdapterHarness(ingressService, catalog);
        when(ingressService.ingest(eq(8L), any(ConversationIngressReq.class))).thenReturn(new ConversationIngressResp(
                100L,
                200L,
                "RECORDED",
                false,
                1));

        ConversationIngressResp resp = harness.ingest(
                8L,
                "sandbox",
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
        ArgumentCaptor<ConversationIngressReq> requestCaptor = ArgumentCaptor.forClass(ConversationIngressReq.class);
        verify(ingressService).ingest(eq(8L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().channel()).isEqualTo("SANDBOX");
    }

    @Test
    void harnessConvertsRawPayloadUsingAdapterPayloadType() {
        ConversationIngressService ingressService = mock(ConversationIngressService.class);
        ConversationAdapterCatalog catalog = new ConversationAdapterCatalog(List.of(new SandboxConversationReplyAdapter()));
        ConversationAdapterHarness harness = new ConversationAdapterHarness(ingressService, catalog, new ObjectMapper());
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
        assertThat(requestCaptor.getValue().attributes()).containsEntry("sandboxOperator", "seller-1")
                .containsEntry("buttonId", "product-a");
    }

}
