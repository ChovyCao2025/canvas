package org.chovy.canvas.cdp.application;

import org.chovy.canvas.cdp.api.CdpBatchTrackCommand;
import org.chovy.canvas.cdp.api.CdpEventIngestionFacade;
import org.chovy.canvas.cdp.api.CdpIngestionError;
import org.chovy.canvas.cdp.api.CdpIngestionResult;
import org.chovy.canvas.cdp.api.CdpTrackEventCommand;
import org.chovy.canvas.cdp.api.CdpWriteKeyView;
import org.chovy.canvas.cdp.domain.CdpAcceptedEventPublisher;
import org.chovy.canvas.cdp.domain.CdpEventAttributeDiscoveryPort;
import org.chovy.canvas.cdp.domain.CdpEventDefinition;
import org.chovy.canvas.cdp.domain.CdpEventDefinitionRepository;
import org.chovy.canvas.cdp.domain.CdpEventLog;
import org.chovy.canvas.cdp.domain.CdpEventRepository;
import org.chovy.canvas.cdp.domain.CdpPrivacyTombstonePort;
import org.chovy.canvas.cdp.domain.CdpWarehouseEventSinkPort;
import org.chovy.canvas.cdp.domain.CustomerProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CdpEventIngestionApplicationService implements CdpEventIngestionFacade {

    private final CdpEventRepository eventRepository;
    private final CdpEventDefinitionRepository eventDefinitionRepository;
    private final CustomerProfileLookupApplicationService profileLookup;
    private final CdpEventAttributeDiscoveryPort discoveryPort;
    private final CdpAcceptedEventPublisher publisher;
    private final CdpWarehouseEventSinkPort warehouseSink;
    private final CdpPrivacyTombstonePort privacyTombstonePort;
    private final Clock clock;
    private final int maxBatchSize;

    @Autowired
    public CdpEventIngestionApplicationService(CdpEventRepository eventRepository,
                                               CdpEventDefinitionRepository eventDefinitionRepository,
                                               CustomerProfileRepository profileRepository,
                                               CdpEventAttributeDiscoveryPort discoveryPort,
                                               CdpAcceptedEventPublisher publisher,
                                               CdpWarehouseEventSinkPort warehouseSink,
                                               CdpPrivacyTombstonePort privacyTombstonePort,
                                               @Value("${canvas.cdp.ingestion.max-batch-size:100}") int maxBatchSize) {
        this(eventRepository, eventDefinitionRepository, profileRepository, discoveryPort, publisher, warehouseSink,
                privacyTombstonePort, Clock.systemDefaultZone(), maxBatchSize);
    }

    CdpEventIngestionApplicationService(CdpEventRepository eventRepository,
                                        CdpEventDefinitionRepository eventDefinitionRepository,
                                        CustomerProfileRepository profileRepository,
                                        CdpEventAttributeDiscoveryPort discoveryPort,
                                        CdpAcceptedEventPublisher publisher,
                                        CdpWarehouseEventSinkPort warehouseSink,
                                        CdpPrivacyTombstonePort privacyTombstonePort,
                                        Clock clock,
                                        int maxBatchSize) {
        this.eventRepository = eventRepository;
        this.eventDefinitionRepository = eventDefinitionRepository;
        this.profileLookup = new CustomerProfileLookupApplicationService(profileRepository, clock);
        this.discoveryPort = discoveryPort == null ? CdpEventAttributeDiscoveryPort.noop() : discoveryPort;
        this.publisher = publisher == null ? CdpAcceptedEventPublisher.noop() : publisher;
        this.warehouseSink = warehouseSink == null ? CdpWarehouseEventSinkPort.noop() : warehouseSink;
        this.privacyTombstonePort = privacyTombstonePort == null ? CdpPrivacyTombstonePort.allowAll() : privacyTombstonePort;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.maxBatchSize = maxBatchSize <= 0 ? 100 : maxBatchSize;
    }

    @Override
    public CdpIngestionResult ingestBatch(CdpWriteKeyView writeKey, CdpBatchTrackCommand command) {
        List<CdpTrackEventCommand> batch = command == null || command.batch() == null
                ? List.of()
                : command.batch();
        if (batch.size() > maxBatchSize) {
            return new CdpIngestionResult(0, batch.size(), List.of(
                    new CdpIngestionError(null, "BATCH_TOO_LARGE", "batch size exceeds " + maxBatchSize)));
        }
        int accepted = 0;
        List<CdpIngestionError> errors = new ArrayList<>();
        OffsetDateTime batchSentAt = command == null ? null : command.sentAt();
        for (CdpTrackEventCommand event : batch) {
            try {
                if (ingestOne(writeKey, event, batchSentAt)) {
                    accepted++;
                }
            } catch (IllegalArgumentException ex) {
                errors.add(new CdpIngestionError(event == null ? null : event.messageId(),
                        "INVALID_EVENT",
                        ex.getMessage()));
            }
        }
        return new CdpIngestionResult(accepted, errors.size(), errors);
    }

    private boolean ingestOne(CdpWriteKeyView writeKey, CdpTrackEventCommand event, OffsetDateTime batchSentAt) {
        if (writeKey == null) {
            throw new IllegalArgumentException("writeKey is required");
        }
        if (event == null) {
            throw new IllegalArgumentException("event is required");
        }
        Long tenantId = safeTenantId(writeKey.tenantId());
        String messageId = requireText(event.messageId(), "messageId");
        String eventType = requireText(event.type(), "type").toLowerCase(Locale.ROOT);
        if (!"track".equals(eventType)) {
            throw new IllegalArgumentException("only track events are supported");
        }
        String eventCode = requireText(event.event(), "event");
        String idempotencyKey = blankToNull(event.idempotencyKey());
        if (eventRepository.existsByMessageId(tenantId, messageId)
                || eventRepository.existsByIdempotencyKey(tenantId, idempotencyKey)) {
            return false;
        }
        CdpEventDefinition definition = eventDefinitionRepository.findPublishedByCode(eventCode);
        if (definition == null) {
            throw new IllegalArgumentException("unknown event code: " + eventCode);
        }
        String userId = blankToNull(event.userId());
        if (userId != null) {
            privacyTombstonePort.enforceNotBlocked(tenantId, "USER_ID", userId, "CDP_EVENT_INGESTION");
            profileLookup.ensureUser(tenantId, userId, "CDP_EVENT", eventCode);
        }
        CdpEventLog row = toEventLog(writeKey, tenantId, event, messageId, eventType, eventCode, userId,
                idempotencyKey, batchSentAt);
        if (!eventRepository.save(row)) {
            return false;
        }
        if (definition.autoDiscover()) {
            discoveryPort.discover(tenantId, eventCode, safeMap(event.properties()));
        }
        try {
            publisher.publishAccepted(row);
        } catch (RuntimeException ignored) {
            // Internal publish failures must not reject already accepted CDP events.
        }
        try {
            warehouseSink.mirrorAccepted(row);
        } catch (RuntimeException ignored) {
            // Warehouse retry adapters own their own failure handling.
        }
        return true;
    }

    private CdpEventLog toEventLog(CdpWriteKeyView writeKey,
                                   Long tenantId,
                                   CdpTrackEventCommand event,
                                   String messageId,
                                   String eventType,
                                   String eventCode,
                                   String userId,
                                   String idempotencyKey,
                                   OffsetDateTime batchSentAt) {
        OffsetDateTime eventTime = event.timestamp() == null ? OffsetDateTime.now(clock) : event.timestamp();
        OffsetDateTime sentAt = event.sentAt() == null ? batchSentAt : event.sentAt();
        return new CdpEventLog(
                null,
                tenantId,
                writeKey.writeKeyId(),
                messageId,
                eventType,
                eventCode,
                userId,
                blankToNull(event.anonymousId()),
                readContextString(event.context(), "sessionId", "session_id", "session"),
                readContextString(event.context(), "deviceId", "device_id", "device"),
                readPlatform(writeKey, event.context()),
                safeMap(event.context()),
                safeMap(event.properties()),
                idempotencyKey,
                toLocal(eventTime),
                sentAt == null ? null : toLocal(sentAt),
                LocalDateTime.now(clock),
                CdpEventLog.ACCEPTED,
                null,
                null);
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
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private String readPlatform(CdpWriteKeyView writeKey, Map<String, Object> context) {
        Object platform = context == null ? null : context.get("platform");
        return platform == null || String.valueOf(platform).isBlank()
                ? writeKey.platform()
                : String.valueOf(platform);
    }

    private LocalDateTime toLocal(OffsetDateTime value) {
        return LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }

    private static Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
