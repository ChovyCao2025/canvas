package org.chovy.canvas.domain.cdp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.CdpEventLogDO;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.dal.mapper.CdpEventLogMapper;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dto.cdp.BatchTrackReq;
import org.chovy.canvas.dto.cdp.IngestionError;
import org.chovy.canvas.dto.cdp.IngestionResult;
import org.chovy.canvas.dto.cdp.TrackEventReq;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEventSink;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCheckpointService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeRetryService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CdpEventIngestionService {
    private final CdpEventLogMapper eventLogMapper;
    private final EventDefinitionCacheService eventDefinitionCacheService;
    private final CdpUserService userService;
    private final ObjectMapper objectMapper;
    private final EventAttributeDiscoveryService discoveryService;
    private final CdpEventPublisher publisher;
    private final ObjectProvider<CdpWarehouseEventSink> warehouseEventSink;
    private final ObjectProvider<CdpWarehouseRealtimeRetryService> warehouseRetryService;
    private final ObjectProvider<CdpWarehouseRealtimeCheckpointService> warehouseCheckpointService;
    private final ObjectProvider<CdpWarehousePrivacyTombstoneService> privacyTombstoneService;

    @Value("${canvas.cdp.ingestion.max-batch-size:100}")
    private int maxBatchSize = 100;

    public CdpEventIngestionService(CdpEventLogMapper eventLogMapper,
                                    EventDefinitionCacheService eventDefinitionCacheService,
                                    CdpUserService userService,
                                    ObjectMapper objectMapper,
                                    EventAttributeDiscoveryService discoveryService,
                                    CdpEventPublisher publisher,
                                    ObjectProvider<CdpWarehouseEventSink> warehouseEventSink,
                                    ObjectProvider<CdpWarehouseRealtimeRetryService> warehouseRetryService,
                                    ObjectProvider<CdpWarehouseRealtimeCheckpointService> warehouseCheckpointService) {
        this(eventLogMapper, eventDefinitionCacheService, userService, objectMapper, discoveryService, publisher,
                warehouseEventSink, warehouseRetryService, warehouseCheckpointService, null);
    }

    @Autowired
    public CdpEventIngestionService(CdpEventLogMapper eventLogMapper,
                                    EventDefinitionCacheService eventDefinitionCacheService,
                                    CdpUserService userService,
                                    ObjectMapper objectMapper,
                                    EventAttributeDiscoveryService discoveryService,
                                    CdpEventPublisher publisher,
                                    ObjectProvider<CdpWarehouseEventSink> warehouseEventSink,
                                    ObjectProvider<CdpWarehouseRealtimeRetryService> warehouseRetryService,
                                    ObjectProvider<CdpWarehouseRealtimeCheckpointService> warehouseCheckpointService,
                                    ObjectProvider<CdpWarehousePrivacyTombstoneService> privacyTombstoneService) {
        this.eventLogMapper = eventLogMapper;
        this.eventDefinitionCacheService = eventDefinitionCacheService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.discoveryService = discoveryService;
        this.publisher = publisher;
        this.warehouseEventSink = warehouseEventSink;
        this.warehouseRetryService = warehouseRetryService;
        this.warehouseCheckpointService = warehouseCheckpointService;
        this.privacyTombstoneService = privacyTombstoneService;
    }

    public CdpEventIngestionService(CdpEventLogMapper eventLogMapper,
                                    EventDefinitionCacheService eventDefinitionCacheService,
                                    CdpUserService userService,
                                    ObjectMapper objectMapper,
                                    EventAttributeDiscoveryService discoveryService,
                                    CdpEventPublisher publisher,
                                    ObjectProvider<CdpWarehouseEventSink> warehouseEventSink) {
        this(eventLogMapper, eventDefinitionCacheService, userService, objectMapper,
                discoveryService, publisher, warehouseEventSink, null, null);
    }

    public CdpEventIngestionService(CdpEventLogMapper eventLogMapper,
                                    EventDefinitionCacheService eventDefinitionCacheService,
                                    CdpUserService userService,
                                    ObjectMapper objectMapper,
                                    EventAttributeDiscoveryService discoveryService,
                                    CdpEventPublisher publisher,
                                    ObjectProvider<CdpWarehouseEventSink> warehouseEventSink,
                                    ObjectProvider<CdpWarehouseRealtimeRetryService> warehouseRetryService) {
        this(eventLogMapper, eventDefinitionCacheService, userService, objectMapper,
                discoveryService, publisher, warehouseEventSink, warehouseRetryService, null);
    }

    public IngestionResult ingestBatch(CdpWriteKeyAuthService.AuthenticatedWriteKey key, BatchTrackReq req) {
        List<TrackEventReq> batch = req == null || req.batch() == null ? List.of() : req.batch();
        if (batch.size() > maxBatchSize) {
            return new IngestionResult(0, batch.size(),
                    List.of(new IngestionError(null, "BATCH_TOO_LARGE", "batch size exceeds " + maxBatchSize)));
        }

        int accepted = 0;
        List<IngestionError> errors = new ArrayList<>();
        OffsetDateTime batchSentAt = req == null ? null : req.sentAt();
        for (TrackEventReq event : batch) {
            try {
                if (ingestOne(key, event, batchSentAt)) {
                    accepted++;
                }
            } catch (IllegalArgumentException ex) {
                errors.add(new IngestionError(event == null ? null : event.messageId(), "INVALID_EVENT", ex.getMessage()));
            }
        }
        return new IngestionResult(accepted, errors.size(), errors);
    }

    protected boolean ingestOne(CdpWriteKeyAuthService.AuthenticatedWriteKey key, TrackEventReq event, OffsetDateTime batchSentAt) {
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        requireText(event.messageId(), "messageId");
        String eventType = requireText(event.type(), "type").toLowerCase();
        if (!"track".equals(eventType)) {
            throw new IllegalArgumentException("only track events are supported");
        }
        String eventCode = requireText(event.event(), "event");
        if (isDuplicateMessage(key.tenantId(), event.messageId())
                || isDuplicateIdempotency(key.tenantId(), event.idempotencyKey())) {
            return false;
        }

        EventDefinitionDO def = eventDefinitionCacheService.getPublishedByCode(eventCode);
        if (def == null) {
            throw new IllegalArgumentException("unknown event code: " + eventCode);
        }
        enforcePrivacyTombstone(key.tenantId(), event.userId(), "CDP_EVENT_INGESTION");
        if (event.userId() != null && !event.userId().isBlank()) {
            userService.ensureUser(key.tenantId(), event.userId(), "CDP_EVENT", eventCode);
        }

        CdpEventLogDO row = toRow(key, event, eventCode, batchSentAt);
        try {
            eventLogMapper.insert(row);
        } catch (DuplicateKeyException duplicate) {
            return false;
        }

        if (Integer.valueOf(1).equals(def.getAutoDiscover())) {
            discoveryService.discover(key.tenantId(), eventCode, event.properties());
        }
        try {
            publisher.publishAccepted(row);
        } catch (RuntimeException ex) {
            log.warn("[CDP] internal event publish failed messageId={}: {}", event.messageId(), ex.getMessage());
        }
        mirrorToWarehouse(row);
        return true;
    }

    private void mirrorToWarehouse(CdpEventLogDO row) {
        CdpWarehouseEventSink sink = warehouseEventSink == null ? null : warehouseEventSink.getIfAvailable();
        if (sink == null) {
            return;
        }
        try {
            sink.writeAccepted(row);
            recordWarehouseCheckpointDelivered(row, "INGESTION");
        } catch (RuntimeException ex) {
            log.warn("[CDP] warehouse event sink failed eventLogId={} messageId={}: {}",
                    row.getId(), row.getMessageId(), ex.getMessage());
            recordWarehouseCheckpointFailure(row, ex.getMessage());
            enqueueWarehouseRetry(row, ex);
        }
    }

    private void enforcePrivacyTombstone(Long tenantId, String userId, String source) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        CdpWarehousePrivacyTombstoneService service =
                privacyTombstoneService == null ? null : privacyTombstoneService.getIfAvailable();
        if (service == null) {
            return;
        }
        service.enforceNotBlocked(tenantId, "USER_ID", userId, source);
    }

    private void enqueueWarehouseRetry(CdpEventLogDO row, RuntimeException failure) {
        CdpWarehouseRealtimeRetryService retryService =
                warehouseRetryService == null ? null : warehouseRetryService.getIfAvailable();
        if (retryService == null) {
            return;
        }
        try {
            retryService.enqueueFailure(row, failure.getMessage());
        } catch (RuntimeException retryFailure) {
            log.warn("[CDP] warehouse retry enqueue failed eventLogId={} messageId={}: {}",
                    row.getId(), row.getMessageId(), retryFailure.getMessage());
        }
    }

    private void recordWarehouseCheckpointDelivered(CdpEventLogDO row, String deliverySource) {
        CdpWarehouseRealtimeCheckpointService checkpointService =
                warehouseCheckpointService == null ? null : warehouseCheckpointService.getIfAvailable();
        if (checkpointService == null) {
            return;
        }
        try {
            checkpointService.recordDelivered(row, deliverySource);
        } catch (RuntimeException checkpointFailure) {
            log.warn("[CDP] warehouse checkpoint delivered update failed eventLogId={} messageId={}: {}",
                    row.getId(), row.getMessageId(), checkpointFailure.getMessage());
        }
    }

    private void recordWarehouseCheckpointFailure(CdpEventLogDO row, String errorMessage) {
        CdpWarehouseRealtimeCheckpointService checkpointService =
                warehouseCheckpointService == null ? null : warehouseCheckpointService.getIfAvailable();
        if (checkpointService == null) {
            return;
        }
        try {
            checkpointService.recordFailure(row, errorMessage);
        } catch (RuntimeException checkpointFailure) {
            log.warn("[CDP] warehouse checkpoint failure update failed eventLogId={} messageId={}: {}",
                    row.getId(), row.getMessageId(), checkpointFailure.getMessage());
        }
    }

    private CdpEventLogDO toRow(CdpWriteKeyAuthService.AuthenticatedWriteKey key, TrackEventReq event,
                                String eventCode, OffsetDateTime batchSentAt) {
        CdpEventLogDO row = new CdpEventLogDO();
        row.setTenantId(key.tenantId());
        row.setWriteKeyId(key.writeKeyId());
        row.setMessageId(event.messageId().trim());
        row.setEventType(event.type().trim().toLowerCase());
        row.setEventCode(eventCode);
        row.setUserId(blankToNull(event.userId()));
        row.setAnonymousId(blankToNull(event.anonymousId()));
        row.setSessionId(readContextString(event.context(), "sessionId", "session_id", "session"));
        row.setDeviceId(readContextString(event.context(), "deviceId", "device_id", "device"));
        row.setPlatform(readPlatform(key, event.context()));
        row.setSdkContext(writeJson(event.context()));
        row.setProperties(writeJson(event.properties()));
        row.setIdempotencyKey(blankToNull(event.idempotencyKey()));
        row.setEventTime(toLocal(event.timestamp() == null ? OffsetDateTime.now(ZoneOffset.UTC) : event.timestamp()));
        OffsetDateTime sentAt = event.sentAt() != null ? event.sentAt() : batchSentAt;
        row.setSentAt(sentAt == null ? null : toLocal(sentAt));
        row.setReceivedAt(LocalDateTime.now());
        row.setStatus(CdpEventLogDO.ACCEPTED);
        return row;
    }

    private boolean isDuplicateMessage(Long tenantId, String messageId) {
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getMessageId, messageId));
        return count != null && count > 0;
    }

    private boolean isDuplicateIdempotency(Long tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return false;
        }
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getIdempotencyKey, idempotencyKey));
        return count != null && count > 0;
    }

    @SuppressWarnings("unchecked")
    private String readContextString(Map<String, Object> context, String directKey, String snakeKey, String nestedKey) {
        if (context == null) {
            return null;
        }
        Object value = context.get(directKey);
        if (value == null) {
            value = context.get(snakeKey);
        }
        if (value == null && context.get(nestedKey) instanceof Map<?, ?> nested) {
            value = ((Map<String, Object>) nested).get(directKey);
            if (value == null) {
                value = ((Map<String, Object>) nested).get("id");
            }
        }
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private String readPlatform(CdpWriteKeyAuthService.AuthenticatedWriteKey key, Map<String, Object> context) {
        Object platform = context == null ? null : context.get("platform");
        return platform == null || String.valueOf(platform).isBlank() ? key.platform() : String.valueOf(platform);
    }

    private String writeJson(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("event JSON field is invalid", e);
        }
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
