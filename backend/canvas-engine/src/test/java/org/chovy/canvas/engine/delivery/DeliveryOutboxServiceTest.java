package org.chovy.canvas.engine.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveryOutboxServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final MessageSendRecordMapper recordMapper = mock(MessageSendRecordMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeliveryOutboxService service = new DeliveryOutboxService(jdbcTemplate, recordMapper, objectMapper);

    @Test
    void prepareMessageRecordStoresTenantAndPendingPayload() {
        when(recordMapper.insert(any(MessageSendRecordDO.class))).thenAnswer(invocation -> {
            MessageSendRecordDO record = invocation.getArgument(0);
            record.setId(100L);
            return 1;
        });

        MessageSendRecordDO record = service.prepareMessageRecord(request());

        assertThat(record.getId()).isEqualTo(100L);
        assertThat(record.getTenantId()).isEqualTo(9L);
        assertThat(record.getStatus()).isEqualTo(MessageSendRecordDO.STATUS_PENDING);
        assertThat(record.getRequestPayload()).contains("\"channel\":\"SMS\"");
        verify(recordMapper).insert(org.mockito.ArgumentMatchers.<MessageSendRecordDO>argThat(inserted ->
                inserted.getTenantId().equals(9L)
                        && MessageSendRecordDO.STATUS_PENDING.equals(inserted.getStatus())));
    }

    @Test
    void claimNextClaimsOnlyPendingOrRetryRows() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 4, 12, 0);
        DeliveryOutboxDO pending = outbox().toBuilder()
                .id(10L)
                .status(DeliveryOutboxService.STATUS_PENDING)
                .build();
        DeliveryOutboxDO sending = pending.toBuilder()
                .status(DeliveryOutboxService.STATUS_SENDING)
                .lockedBy("worker-1")
                .lockedAt(now)
                .build();
        when(jdbcTemplate.query(contains("WHERE status IN"), org.mockito.ArgumentMatchers.<RowMapper<DeliveryOutboxDO>>any(), eq(DeliveryOutboxService.STATUS_PENDING),
                eq(DeliveryOutboxService.STATUS_RETRY), eq(now), eq(10))).thenReturn(List.of(pending));
        when(jdbcTemplate.update(contains("UPDATE delivery_outbox"), eq(DeliveryOutboxService.STATUS_SENDING),
                eq("worker-1"), eq(now), eq(now), eq(10L), eq(DeliveryOutboxService.STATUS_PENDING),
                eq(DeliveryOutboxService.STATUS_RETRY), eq(now))).thenReturn(1);
        when(jdbcTemplate.query(contains("WHERE id = ?"), org.mockito.ArgumentMatchers.<RowMapper<DeliveryOutboxDO>>any(), eq(10L))).thenReturn(List.of(sending));

        assertThat(service.claimNext("worker-1", now)).contains(sending);
    }

    @Test
    void recordReceiptUsesStableIdempotencyAndUpdatesCurrentStatus() {
        LocalDateTime receivedAt = LocalDateTime.of(2026, 6, 4, 11, 12, 13);
        DeliveryOutboxDO outbox = outbox().toBuilder()
                .id(7L)
                .messageSendRecordId(77L)
                .providerMessageId("msg-1")
                .build();
        DeliveryReceiptLog receipt = DeliveryReceiptLog.builder()
                .tenantId(9L)
                .outboxId(7L)
                .provider("REACH")
                .providerMessageId("msg-1")
                .receiptType("DELIVERED")
                .idempotencyKey("REACH:msg-1:DELIVERED:evt-1")
                .receivedAt(receivedAt)
                .build();

        when(jdbcTemplate.query(contains("WHERE provider = ? AND provider_message_id = ?"), org.mockito.ArgumentMatchers.<RowMapper<DeliveryOutboxDO>>any(),
                eq("REACH"), eq("msg-1"))).thenReturn(List.of(outbox));
        when(jdbcTemplate.query(contains("WHERE tenant_id = ? AND idempotency_key = ?"), org.mockito.ArgumentMatchers.<RowMapper<DeliveryReceiptLog>>any(),
                eq(9L), eq("REACH:msg-1:DELIVERED:evt-1"))).thenReturn(List.of(receipt));
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(recordMapper.updateById(any(MessageSendRecordDO.class))).thenReturn(1);

        DeliveryReceiptLog result = service.recordReceipt(new DeliveryReceiptRequest(
                "reach",
                "msg-1",
                "delivered",
                null,
                receivedAt,
                Map.of("eventId", "evt-1")));

        assertThat(result.getIdempotencyKey()).isEqualTo("REACH:msg-1:DELIVERED:evt-1");
        verify(recordMapper).updateById(org.mockito.ArgumentMatchers.<MessageSendRecordDO>argThat(record ->
                record.getId().equals(77L)
                        && "DELIVERED".equals(record.getStatus())
                        && "msg-1".equals(record.getExternalMessageId())));
        verify(jdbcTemplate).update(contains("SET status = ?"), eq("DELIVERED"), eq(receivedAt), eq(7L));
    }

    private ReachDeliveryService.DeliveryRequest request() {
        return new ReachDeliveryService.DeliveryRequest(
                9L,
                "exec-1",
                20L,
                "user-1",
                "node-1",
                "SMS",
                "REACH",
                "tpl-1",
                Map.of("channel", "SMS", "body", "hello"),
                "idem-1",
                ReachDeliveryService.PolicyOptions.defaults());
    }

    private DeliveryOutboxDO outbox() {
        return DeliveryOutboxDO.builder()
                .id(1L)
                .tenantId(9L)
                .messageSendRecordId(100L)
                .executionId("exec-1")
                .canvasId(20L)
                .userId("user-1")
                .nodeId("node-1")
                .channel("SMS")
                .provider("REACH")
                .payloadJson("{\"channel\":\"SMS\"}")
                .idempotencyKey("idem-1")
                .status(DeliveryOutboxService.STATUS_PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
