package org.chovy.canvas.infrastructure.mq;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.engine.delivery.DeliveryOutboxDO;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.chovy.canvas.engine.delivery.ReachDeliveryService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryOutboxConsumerTest {

    @Test
    void dispatchNextMarksClaimedOutboxSentWhenProviderSucceeds() {
        DeliveryOutboxService outboxService = mock(DeliveryOutboxService.class);
        ReachDeliveryService deliveryService = mock(ReachDeliveryService.class);
        DeliveryOutboxDO outbox = outbox(11L, 0);
        when(outboxService.claimNext(eq("worker-1"), any(LocalDateTime.class))).thenReturn(Optional.of(outbox));
        when(deliveryService.dispatchToProvider(outbox)).thenReturn(Mono.just(Map.of(MapFieldKeys.MESSAGE_ID, "msg-1")));
        DeliveryOutboxConsumer consumer = new DeliveryOutboxConsumer(outboxService, deliveryService, 3, 1, 1000);

        DeliveryOutboxConsumer.DispatchResult result = consumer.dispatchNext("worker-1").block(Duration.ofSeconds(2));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(DeliveryOutboxService.STATUS_SENT);
        assertThat(result.providerMessageId()).isEqualTo("msg-1");
        verify(outboxService).markSent(11L, "msg-1", Map.of(MapFieldKeys.MESSAGE_ID, "msg-1"));
        verify(outboxService, never()).markRetry(anyLong(), any(), any());
        verify(outboxService, never()).markDead(anyLong(), any());
    }

    @Test
    void dispatchNextMarksRetryBeforeMaxAttempts() {
        DeliveryOutboxService outboxService = mock(DeliveryOutboxService.class);
        ReachDeliveryService deliveryService = mock(ReachDeliveryService.class);
        DeliveryOutboxDO outbox = outbox(12L, 0);
        when(outboxService.claimNext(eq("worker-1"), any(LocalDateTime.class))).thenReturn(Optional.of(outbox));
        when(deliveryService.dispatchToProvider(outbox)).thenReturn(Mono.error(new IllegalStateException("timeout")));
        DeliveryOutboxConsumer consumer = new DeliveryOutboxConsumer(outboxService, deliveryService, 3, 1, 1000);

        DeliveryOutboxConsumer.DispatchResult result = consumer.dispatchNext("worker-1").block(Duration.ofSeconds(2));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(DeliveryOutboxService.STATUS_RETRY);
        assertThat(result.errorMessage()).contains("timeout");
        verify(outboxService).markRetry(eq(12L), eq("timeout"), any(LocalDateTime.class));
        verify(outboxService, never()).markDead(anyLong(), any());
    }

    @Test
    void dispatchNextMarksDeadAtMaxAttempts() {
        DeliveryOutboxService outboxService = mock(DeliveryOutboxService.class);
        ReachDeliveryService deliveryService = mock(ReachDeliveryService.class);
        DeliveryOutboxDO outbox = outbox(13L, 2);
        when(outboxService.claimNext(eq("worker-1"), any(LocalDateTime.class))).thenReturn(Optional.of(outbox));
        when(deliveryService.dispatchToProvider(outbox)).thenReturn(Mono.error(new IllegalStateException("provider down")));
        DeliveryOutboxConsumer consumer = new DeliveryOutboxConsumer(outboxService, deliveryService, 3, 1, 1000);

        DeliveryOutboxConsumer.DispatchResult result = consumer.dispatchNext("worker-1").block(Duration.ofSeconds(2));

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(DeliveryOutboxService.STATUS_DEAD);
        assertThat(result.errorMessage()).contains("provider down");
        verify(outboxService).markDead(13L, "provider down");
        verify(outboxService, never()).markRetry(anyLong(), any(), any());
    }

    private DeliveryOutboxDO outbox(Long id, int attemptCount) {
        return DeliveryOutboxDO.builder()
                .id(id)
                .tenantId(1L)
                .messageSendRecordId(100L + id)
                .executionId("exec-" + id)
                .canvasId(42L)
                .userId("user-1")
                .nodeId("node-send")
                .channel("SMS")
                .provider("REACH")
                .idempotencyKey("idem-" + id)
                .status(DeliveryOutboxService.STATUS_SENDING)
                .attemptCount(attemptCount)
                .build();
    }
}
