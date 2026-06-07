package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.WhatsAppConversationReplyPayload;
import org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapper;
import org.chovy.canvas.domain.conversation.WhatsAppWebhookSecurityService;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.chovy.canvas.engine.delivery.DeliveryReceiptLog;
import org.chovy.canvas.engine.delivery.DeliveryReceiptRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicConversationWebhookControllerTest {

    private static final String APP_SECRET = "whatsapp-app-secret";

    @Test
    void whatsappChallengeReturnsRawChallengeWhenVerifyTokenMatches() {
        PublicConversationWebhookController controller = controller(
                mock(ConversationAdapterHarness.class),
                mock(WhatsAppWebhookPayloadMapper.class));

        StepVerifier.create(controller.verifyWhatsApp(7L, "subscribe", "verify-token", "challenge-1"))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo("challenge-1");
                })
                .verifyComplete();
    }

    @Test
    void whatsappWebhookVerifiesSignatureBeforeDelegatingToHarness() {
        ConversationAdapterHarness harness = mock(ConversationAdapterHarness.class);
        WhatsAppWebhookPayloadMapper mapper = mock(WhatsAppWebhookPayloadMapper.class);
        PublicConversationWebhookController controller = controller(harness, mapper);
        String rawBody = "{\"entry\":[{\"id\":\"entry-1\"}]}";
        WhatsAppConversationReplyPayload payload = new WhatsAppConversationReplyPayload(
                null,
                null,
                null,
                "whatsapp:15551234567",
                "CLOUD_API",
                "wamid.1",
                "whatsapp:entry-1:wamid.1",
                "hello",
                null,
                null,
                null,
                Map.of("phoneNumberId", "phone-number-id-1"),
                null);
        when(mapper.toAdapterPayloads(Map.of("entry", List.of(Map.of("id", "entry-1")))))
                .thenReturn(List.of(payload));
        when(harness.ingest(7L, "WHATSAPP", payload, "whatsapp-webhook"))
                .thenReturn(new ConversationIngressResp(100L, 200L, "RECORDED", false, 1));

        StepVerifier.create(controller.receiveWhatsApp(7L, signature(rawBody), rawBody))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).singleElement()
                            .satisfies(resp -> assertThat(resp.messageId()).isEqualTo(200L));
                })
                .verifyComplete();

        verify(mapper).toAdapterPayloads(Map.of("entry", List.of(Map.of("id", "entry-1"))));
        verify(harness).ingest(7L, "WHATSAPP", payload, "whatsapp-webhook");
    }

    @Test
    void whatsappWebhookRecordsStatusReceiptsWithoutConversationIngress() {
        ConversationAdapterHarness harness = mock(ConversationAdapterHarness.class);
        WhatsAppWebhookPayloadMapper mapper = new WhatsAppWebhookPayloadMapper();
        DeliveryOutboxService outboxService = mock(DeliveryOutboxService.class);
        when(outboxService.recordReceipt(any(DeliveryReceiptRequest.class)))
                .thenReturn(DeliveryReceiptLog.builder()
                        .id(500L)
                        .provider("CLOUD_API")
                        .providerMessageId("wamid.1")
                        .receiptType("DELIVERED")
                        .build());
        PublicConversationWebhookController controller = new PublicConversationWebhookController(
                harness,
                mapper,
                new WhatsAppWebhookSecurityService("verify-token", APP_SECRET),
                new ObjectMapper(),
                outboxService);
        String rawBody = """
                {"entry":[{"id":"entry-1","changes":[{"field":"messages","value":{"metadata":{"phone_number_id":"phone-number-id-1"},"statuses":[{"id":"wamid.1","status":"delivered","timestamp":"1780372800","recipient_id":"15551234567"}]}}]}]}
                """;

        StepVerifier.create(controller.receiveWhatsApp(7L, signature(rawBody), rawBody))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData()).isEmpty();
                })
                .verifyComplete();

        verify(outboxService).recordReceipt(org.mockito.ArgumentMatchers.<DeliveryReceiptRequest>argThat(request ->
                "CLOUD_API".equals(request.provider())
                        && "wamid.1".equals(request.providerMessageId())
                        && "DELIVERED".equals(request.receiptType())));
        verify(harness, never()).ingest(any(), any(String.class), any(), any());
    }

    @Test
    void whatsappWebhookRejectsInvalidSignatureBeforeParsingPayload() {
        ConversationAdapterHarness harness = mock(ConversationAdapterHarness.class);
        WhatsAppWebhookPayloadMapper mapper = mock(WhatsAppWebhookPayloadMapper.class);
        PublicConversationWebhookController controller = controller(harness, mapper);

        StepVerifier.create(controller.receiveWhatsApp(7L, "sha256=bad", "{\"entry\":[]}"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();

        verify(mapper, never()).toAdapterPayloads(any());
        verify(harness, never()).ingest(any(), any(String.class), any(), any());
    }

    private PublicConversationWebhookController controller(ConversationAdapterHarness harness,
                                                          WhatsAppWebhookPayloadMapper mapper) {
        return new PublicConversationWebhookController(
                harness,
                mapper,
                new WhatsAppWebhookSecurityService("verify-token", APP_SECRET),
                new ObjectMapper());
    }

    private String signature(String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
