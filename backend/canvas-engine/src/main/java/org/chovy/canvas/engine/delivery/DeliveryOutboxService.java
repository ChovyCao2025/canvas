package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.dal.dataobject.MessageSendRecordDO;
import org.chovy.canvas.dal.mapper.MessageSendRecordMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class DeliveryOutboxService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENDING = "SENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_RETRY = "RETRY";
    public static final String STATUS_DEAD = "DEAD";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final MessageSendRecordMapper recordMapper;
    private final ObjectMapper objectMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final String outboxTopic;
    private final boolean mqPublishEnabled;

    @Autowired
    public DeliveryOutboxService(JdbcTemplate jdbcTemplate,
                                 MessageSendRecordMapper recordMapper,
                                 ObjectMapper objectMapper,
                                 ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider,
                                 @Value("${canvas.delivery.outbox.topic:CANVAS_DELIVERY}") String outboxTopic,
                                 @Value("${canvas.delivery.outbox.mq-publish-enabled:false}") boolean mqPublishEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.recordMapper = recordMapper;
        this.objectMapper = objectMapper;
        this.rocketMQTemplate = rocketMQTemplateProvider == null ? null : rocketMQTemplateProvider.getIfAvailable();
        this.outboxTopic = outboxTopic;
        this.mqPublishEnabled = mqPublishEnabled;
    }

    public DeliveryOutboxService(JdbcTemplate jdbcTemplate,
                                 MessageSendRecordMapper recordMapper,
                                 ObjectMapper objectMapper) {
        this(jdbcTemplate, recordMapper, objectMapper, null, "CANVAS_DELIVERY", false);
    }

    @Transactional
    public DeliveryOutboxDO enqueue(ReachDeliveryService.DeliveryRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Optional<DeliveryOutboxDO> existing = findByIdempotencyKey(request.tenantId(), request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get().markDuplicate(true);
        }

        MessageSendRecordDO record = prepareMessageRecord(request);
        try {
            Long outboxId = insertOutbox(request, record.getId());
            DeliveryOutboxDO outbox = findById(outboxId)
                    .orElseGet(() -> findByIdempotencyKey(request.tenantId(), request.idempotencyKey())
                            .orElseThrow(() -> new IllegalStateException("delivery outbox insert was not readable")))
                    .markDuplicate(false);
            publishAfterCommit(outbox);
            return outbox;
        } catch (DuplicateKeyException duplicate) {
            return findByIdempotencyKey(request.tenantId(), request.idempotencyKey())
                    .orElseThrow(() -> duplicate)
                    .markDuplicate(true);
        }
    }

    @Transactional
    public Optional<DeliveryOutboxDO> claimNext(String workerId, LocalDateTime now) {
        LocalDateTime effectiveNow = now == null ? LocalDateTime.now() : now;
        List<DeliveryOutboxDO> candidates = findClaimCandidates(effectiveNow, 10);
        for (DeliveryOutboxDO candidate : candidates) {
            int updated = jdbcTemplate.update("""
                    UPDATE delivery_outbox
                    SET status = ?, locked_by = ?, locked_at = ?, updated_at = ?
                    WHERE id = ?
                      AND status IN (?, ?)
                      AND (next_retry_at IS NULL OR next_retry_at <= ?)
                    """,
                    STATUS_SENDING, workerId, effectiveNow, effectiveNow,
                    candidate.getId(), STATUS_PENDING, STATUS_RETRY, effectiveNow);
            if (updated == 1) {
                return findById(candidate.getId());
            }
        }
        return Optional.empty();
    }

    @Transactional
    public void markSent(Long outboxId, String providerMessageId, Map<String, Object> response) {
        LocalDateTime now = LocalDateTime.now();
        String responseJson = toJson(response == null ? Map.of() : response);
        jdbcTemplate.update("""
                UPDATE delivery_outbox
                SET status = ?, provider_message_id = ?, provider_response_json = ?,
                    last_error = NULL, next_retry_at = NULL, locked_by = NULL, locked_at = NULL, updated_at = ?
                WHERE id = ?
                """, STATUS_SENT, providerMessageId, responseJson, now, outboxId);

        findById(outboxId).ifPresent(outbox -> {
            MessageSendRecordDO record = new MessageSendRecordDO();
            record.setId(outbox.getMessageSendRecordId());
            record.setStatus(MessageSendRecordDO.STATUS_SENT);
            record.setExternalMessageId(providerMessageId);
            record.setUpdatedAt(now);
            recordMapper.updateById(record);
        });
    }

    @Transactional
    public void markRetry(Long outboxId, String errorMessage, LocalDateTime nextRetryAt) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                UPDATE delivery_outbox
                SET status = ?, attempt_count = attempt_count + 1, next_retry_at = ?,
                    locked_by = NULL, locked_at = NULL, last_error = ?, updated_at = ?
                WHERE id = ?
                """, STATUS_RETRY, nextRetryAt, truncate(errorMessage, 500), now, outboxId);
    }

    @Transactional
    public void markDead(Long outboxId, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                UPDATE delivery_outbox
                SET status = ?, attempt_count = attempt_count + 1, next_retry_at = NULL,
                    locked_by = NULL, locked_at = NULL, last_error = ?, updated_at = ?
                WHERE id = ?
                """, STATUS_DEAD, truncate(errorMessage, 500), now, outboxId);

        findById(outboxId).ifPresent(outbox -> {
            MessageSendRecordDO record = new MessageSendRecordDO();
            record.setId(outbox.getMessageSendRecordId());
            record.setStatus(MessageSendRecordDO.STATUS_FAILED);
            record.setErrorMessage(truncate(errorMessage, 500));
            record.setUpdatedAt(now);
            recordMapper.updateById(record);
        });
    }

    public List<DeliveryOutboxDO> findStalePending(LocalDateTime before, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE (status = ? AND updated_at < ?)
                   OR (status = ? AND locked_at < ?)
                ORDER BY updated_at ASC, id ASC
                LIMIT ?
                """, rowMapper(), STATUS_PENDING, before, STATUS_SENDING, before, normalizeLimit(limit));
    }

    @Transactional
    public int requeueStalePending(LocalDateTime before, int limit) {
        int count = 0;
        for (DeliveryOutboxDO row : findStalePending(before, limit)) {
            int updated = jdbcTemplate.update("""
                    UPDATE delivery_outbox
                    SET status = ?, locked_by = NULL, locked_at = NULL, updated_at = ?
                    WHERE id = ? AND status IN (?, ?)
                    """, STATUS_RETRY, LocalDateTime.now(), row.getId(), STATUS_PENDING, STATUS_SENDING);
            count += updated;
        }
        return count;
    }

    @Transactional
    public boolean replayDead(Long outboxId) {
        int updated = jdbcTemplate.update("""
                UPDATE delivery_outbox
                SET status = ?, attempt_count = 0, next_retry_at = NULL,
                    locked_by = NULL, locked_at = NULL, last_error = NULL, updated_at = ?
                WHERE id = ? AND status = ?
                """, STATUS_PENDING, LocalDateTime.now(), outboxId, STATUS_DEAD);
        return updated == 1;
    }

    @Transactional
    public DeliveryReceiptLog recordReceipt(DeliveryReceiptRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        DeliveryOutboxDO outbox = findByProviderMessageId(request.provider(), request.providerMessageId())
                .orElseThrow(() -> new IllegalArgumentException("unknown provider message id"));
        LocalDateTime receivedAt = request.receivedAt() == null ? LocalDateTime.now() : request.receivedAt();
        String idempotencyKey = receiptIdempotencyKey(request);
        String rawJson = toJson(request.rawPayload() == null ? Map.of() : request.rawPayload());

        try {
            insertReceipt(outbox, request, idempotencyKey, rawJson, receivedAt);
        } catch (DuplicateKeyException ignored) {
            // Receipts are callbacks; duplicates should be harmless after the first accepted event.
        }
        updateCurrentReceiptStatus(outbox, request.receiptType(), receivedAt);
        return findReceiptByIdempotencyKey(outbox.getTenantId(), idempotencyKey)
                .orElseGet(() -> DeliveryReceiptLog.builder()
                        .tenantId(outbox.getTenantId())
                        .outboxId(outbox.getId())
                        .provider(request.provider())
                        .providerMessageId(request.providerMessageId())
                        .receiptType(normalizeStatus(request.receiptType()))
                        .rawPayloadJson(rawJson)
                        .idempotencyKey(idempotencyKey)
                        .receivedAt(receivedAt)
                        .createdAt(receivedAt)
                        .build());
    }

    public Optional<DeliveryOutboxDO> findById(Long id) {
        List<DeliveryOutboxDO> rows = jdbcTemplate.query(
                "SELECT * FROM delivery_outbox WHERE id = ? LIMIT 1",
                rowMapper(),
                id);
        return rows.stream().findFirst();
    }

    public Optional<DeliveryOutboxDO> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        List<DeliveryOutboxDO> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE tenant_id = ? AND idempotency_key = ?
                LIMIT 1
                """, rowMapper(), normalizeTenantId(tenantId), idempotencyKey);
        return rows.stream().findFirst();
    }

    public Optional<DeliveryOutboxDO> findByProviderMessageId(String provider, String providerMessageId) {
        List<DeliveryOutboxDO> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE provider = ? AND provider_message_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rowMapper(), normalizeProvider(provider), providerMessageId);
        return rows.stream().findFirst();
    }

    public PageResult<DeliveryOutboxDO> search(DeliverySearchCriteria criteria) {
        DeliverySearchCriteria c = criteria == null ? DeliverySearchCriteria.defaults() : criteria.normalized();
        List<Object> args = new ArrayList<>();
        String where = buildWhere(c, args);
        long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery_outbox " + where,
                Long.class,
                args.toArray());

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(c.size());
        pageArgs.add((c.page() - 1) * c.size());
        List<DeliveryOutboxDO> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                """ + where + """
                ORDER BY updated_at DESC, id DESC
                LIMIT ? OFFSET ?
                """, rowMapper(), pageArgs.toArray());
        return PageResult.of(total, rows);
    }

    public List<DeliveryReceiptLog> receiptHistory(Long outboxId) {
        return jdbcTemplate.query("""
                SELECT * FROM delivery_receipt_log
                WHERE outbox_id = ?
                ORDER BY received_at DESC, id DESC
                """, receiptRowMapper(), outboxId);
    }

    protected List<DeliveryOutboxDO> findClaimCandidates(LocalDateTime now, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE status IN (?, ?)
                  AND (next_retry_at IS NULL OR next_retry_at <= ?)
                ORDER BY COALESCE(next_retry_at, created_at) ASC, id ASC
                LIMIT ?
                """, rowMapper(), STATUS_PENDING, STATUS_RETRY, now, normalizeLimit(limit));
    }

    protected MessageSendRecordDO prepareMessageRecord(ReachDeliveryService.DeliveryRequest request) {
        MessageSendRecordDO existing = recordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getIdempotencyKey, request.idempotencyKey())
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }

        MessageSendRecordDO record = new MessageSendRecordDO();
        record.setTenantId(normalizeTenantId(request.tenantId()));
        record.setExecutionId(request.executionId());
        record.setCanvasId(request.canvasId());
        record.setUserId(request.userId());
        record.setNodeId(request.nodeId());
        record.setChannel(request.channel());
        record.setTemplateId(request.templateId());
        record.setIdempotencyKey(request.idempotencyKey());
        record.setRequestPayload(toJson(request.payload()));
        record.setStatus(MessageSendRecordDO.STATUS_PENDING);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(record.getCreatedAt());
        try {
            recordMapper.insert(record);
            return record;
        } catch (DuplicateKeyException duplicate) {
            MessageSendRecordDO duplicateRecord = recordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecordDO>()
                    .eq(MessageSendRecordDO::getIdempotencyKey, request.idempotencyKey())
                    .last("LIMIT 1"));
            if (duplicateRecord != null) {
                return duplicateRecord;
            }
            throw duplicate;
        }
    }

    protected Long insertOutbox(ReachDeliveryService.DeliveryRequest request, Long messageSendRecordId) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO delivery_outbox
                    (tenant_id, message_send_record_id, execution_id, canvas_id, user_id, node_id,
                     channel, provider, payload_json, idempotency_key, status, attempt_count,
                     created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, normalizeTenantId(request.tenantId()));
            ps.setLong(2, messageSendRecordId);
            ps.setString(3, request.executionId());
            ps.setLong(4, request.canvasId());
            ps.setString(5, request.userId());
            ps.setString(6, request.nodeId());
            ps.setString(7, request.channel());
            ps.setString(8, normalizeProvider(request.provider()));
            ps.setString(9, toJson(request.payload()));
            ps.setString(10, request.idempotencyKey());
            ps.setString(11, STATUS_PENDING);
            ps.setTimestamp(12, Timestamp.valueOf(now));
            ps.setTimestamp(13, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        return findByIdempotencyKey(request.tenantId(), request.idempotencyKey())
                .map(DeliveryOutboxDO::getId)
                .orElseThrow(() -> new IllegalStateException("delivery outbox insert did not return a key"));
    }

    private void insertReceipt(DeliveryOutboxDO outbox,
                               DeliveryReceiptRequest request,
                               String idempotencyKey,
                               String rawJson,
                               LocalDateTime receivedAt) {
        jdbcTemplate.update("""
                INSERT INTO delivery_receipt_log
                (tenant_id, outbox_id, provider, provider_message_id, receipt_type,
                 raw_payload_json, idempotency_key, received_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                outbox.getTenantId(), outbox.getId(), normalizeProvider(request.provider()),
                request.providerMessageId(), normalizeStatus(request.receiptType()),
                rawJson, idempotencyKey, receivedAt, LocalDateTime.now());
    }

    private Optional<DeliveryReceiptLog> findReceiptByIdempotencyKey(Long tenantId, String idempotencyKey) {
        List<DeliveryReceiptLog> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_receipt_log
                WHERE tenant_id = ? AND idempotency_key = ?
                LIMIT 1
                """, receiptRowMapper(), tenantId, idempotencyKey);
        return rows.stream().findFirst();
    }

    private void updateCurrentReceiptStatus(DeliveryOutboxDO outbox, String receiptType, LocalDateTime receivedAt) {
        String status = normalizeStatus(receiptType);
        MessageSendRecordDO record = new MessageSendRecordDO();
        record.setId(outbox.getMessageSendRecordId());
        record.setStatus(status);
        record.setExternalMessageId(outbox.getProviderMessageId());
        record.setUpdatedAt(receivedAt);
        recordMapper.updateById(record);
        jdbcTemplate.update("UPDATE delivery_outbox SET status = ?, updated_at = ? WHERE id = ?",
                status, receivedAt, outbox.getId());
    }

    private String buildWhere(DeliverySearchCriteria criteria, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (criteria.tenantId() != null) {
            clauses.add("tenant_id = ?");
            args.add(criteria.tenantId());
        }
        if (criteria.canvasId() != null) {
            clauses.add("canvas_id = ?");
            args.add(criteria.canvasId());
        }
        addStringFilter(clauses, args, "execution_id", criteria.executionId());
        addStringFilter(clauses, args, "user_id", criteria.userId());
        addStringFilter(clauses, args, "channel", criteria.channel());
        addStringFilter(clauses, args, "provider", criteria.provider());
        addStringFilter(clauses, args, "status", criteria.status());
        addStringFilter(clauses, args, "provider_message_id", criteria.providerMessageId());
        return clauses.isEmpty() ? "" : "WHERE " + String.join(" AND ", clauses) + " ";
    }

    private void addStringFilter(List<String> clauses, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) {
            clauses.add(column + " = ?");
            args.add(value);
        }
    }

    public Map<String, Object> payloadAsMap(DeliveryOutboxDO outbox) {
        if (outbox == null || outbox.getPayloadJson() == null || outbox.getPayloadJson().isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(outbox.getPayloadJson(), MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("delivery payload JSON is invalid", e);
        }
    }

    private RowMapper<DeliveryOutboxDO> rowMapper() {
        return (rs, rowNum) -> DeliveryOutboxDO.builder()
                .id(rs.getLong("id"))
                .tenantId(rs.getLong("tenant_id"))
                .messageSendRecordId(rs.getLong("message_send_record_id"))
                .executionId(rs.getString("execution_id"))
                .canvasId(rs.getLong("canvas_id"))
                .userId(rs.getString("user_id"))
                .nodeId(rs.getString("node_id"))
                .channel(rs.getString("channel"))
                .provider(rs.getString("provider"))
                .payloadJson(rs.getString("payload_json"))
                .idempotencyKey(rs.getString("idempotency_key"))
                .status(rs.getString("status"))
                .attemptCount(rs.getInt("attempt_count"))
                .nextRetryAt(toLocalDateTime(rs.getTimestamp("next_retry_at")))
                .lockedBy(rs.getString("locked_by"))
                .lockedAt(toLocalDateTime(rs.getTimestamp("locked_at")))
                .providerMessageId(rs.getString("provider_message_id"))
                .providerResponseJson(rs.getString("provider_response_json"))
                .lastError(rs.getString("last_error"))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                .build();
    }

    private RowMapper<DeliveryReceiptLog> receiptRowMapper() {
        return (rs, rowNum) -> DeliveryReceiptLog.builder()
                .id(rs.getLong("id"))
                .tenantId(rs.getLong("tenant_id"))
                .outboxId(rs.getLong("outbox_id"))
                .provider(rs.getString("provider"))
                .providerMessageId(rs.getString("provider_message_id"))
                .receiptType(rs.getString("receipt_type"))
                .rawPayloadJson(rs.getString("raw_payload_json"))
                .idempotencyKey(rs.getString("idempotency_key"))
                .receivedAt(toLocalDateTime(rs.getTimestamp("received_at")))
                .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("delivery JSON serialization failed", e);
        }
    }

    private String receiptIdempotencyKey(DeliveryReceiptRequest request) {
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            return request.idempotencyKey();
        }
        Object eventId = eventId(request.rawPayload());
        if (eventId != null) {
            return normalizeProvider(request.provider()) + ":" + request.providerMessageId()
                    + ":" + normalizeStatus(request.receiptType()) + ":" + eventId;
        }
        return normalizeProvider(request.provider()) + ":" + request.providerMessageId()
                + ":" + normalizeStatus(request.receiptType()) + ":" + Integer.toHexString(toJson(request.rawPayload()).hashCode());
    }

    private Object eventId(Map<String, Object> rawPayload) {
        if (rawPayload == null || rawPayload.isEmpty()) {
            return null;
        }
        Object eventId = rawPayload.get("eventId");
        if (eventId == null) eventId = rawPayload.get("event_id");
        if (eventId == null) eventId = rawPayload.get("id");
        return eventId;
    }

    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "REACH" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "UNKNOWN" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }

    private void publishAfterCommit(DeliveryOutboxDO outbox) {
        if (!mqPublishEnabled || rocketMQTemplate == null || outbox == null || outbox.getId() == null) {
            return;
        }
        Runnable publisher = () -> {
            try {
                rocketMQTemplate.convertAndSend(outboxTopic, Map.of(
                        "outboxId", outbox.getId(),
                        "tenantId", normalizeTenantId(outbox.getTenantId()),
                        "channel", outbox.getChannel(),
                        "provider", outbox.getProvider()));
            } catch (RuntimeException e) {
                log.warn("[DELIVERY_OUTBOX] publish wakeup failed outboxId={} topic={} reason={}",
                        outbox.getId(), outboxTopic, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    public record DeliverySearchCriteria(
            Long tenantId,
            Long canvasId,
            String executionId,
            String userId,
            String channel,
            String provider,
            String status,
            String providerMessageId,
            int page,
            int size
    ) {
        static DeliverySearchCriteria defaults() {
            return new DeliverySearchCriteria(null, null, null, null, null, null, null, null, 1, 20);
        }

        DeliverySearchCriteria normalized() {
            return new DeliverySearchCriteria(
                    tenantId,
                    canvasId,
                    blankToNull(executionId),
                    blankToNull(userId),
                    normalizeUpper(channel),
                    normalizeUpper(provider),
                    normalizeUpper(status),
                    blankToNull(providerMessageId),
                    Math.max(1, page),
                    Math.max(1, Math.min(size, 100))
            );
        }

        private static String normalizeUpper(String value) {
            String normalized = blankToNull(value);
            return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
