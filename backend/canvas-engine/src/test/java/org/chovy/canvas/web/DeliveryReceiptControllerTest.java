package org.chovy.canvas.web;

import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.chovy.canvas.engine.delivery.DeliveryReceiptLog;
import org.chovy.canvas.engine.delivery.DeliveryReceiptRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryReceiptControllerTest {

    @Test
    void validSecretRecordsReceiptCallback() {
        DeliveryOutboxService outboxService = mock(DeliveryOutboxService.class);
        DeliveryReceiptLog log = DeliveryReceiptLog.builder()
                .outboxId(7L)
                .provider("REACH")
                .providerMessageId("msg-1")
                .receiptType("DELIVERED")
                .idempotencyKey("receipt-1")
                .receivedAt(LocalDateTime.of(2026, 6, 4, 11, 12, 13))
                .build();
        when(outboxService.recordReceipt(any(DeliveryReceiptRequest.class))).thenReturn(log);
        DeliveryReceiptController controller = new DeliveryReceiptController(outboxService, "secret");

        var response = controller.receive("secret", request()).block();

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isZero();
        assertThat(response.getData().getReceiptType()).isEqualTo("DELIVERED");
        verify(outboxService).recordReceipt(any(DeliveryReceiptRequest.class));
    }

    @Test
    void invalidSecretIsRejectedBeforeRecording() {
        DeliveryOutboxService outboxService = mock(DeliveryOutboxService.class);
        DeliveryReceiptController controller = new DeliveryReceiptController(outboxService, "secret");

        assertThatThrownBy(() -> controller.receive("bad", request()).block())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("invalid receipt signature");
    }

    private DeliveryReceiptController.ReceiptCallbackReq request() {
        DeliveryReceiptController.ReceiptCallbackReq req = new DeliveryReceiptController.ReceiptCallbackReq();
        req.setProvider("REACH");
        req.setProviderMessageId("msg-1");
        req.setReceiptType("DELIVERED");
        req.setIdempotencyKey("receipt-1");
        req.setReceivedAt(LocalDateTime.of(2026, 6, 4, 11, 12, 13));
        req.setRawPayload(Map.of("eventId", "evt-1"));
        return req;
    }
}
