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
/**
 * CdpEventIngestionService 承载对应领域的业务规则、流程编排和结果转换。
 */
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

    /**
     * 初始化 CdpEventIngestionService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventDefinitionCacheService 依赖组件，用于完成数据访问或外部能力调用。
     * @param userService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param discoveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param publisher publisher 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseEventSink warehouse event sink 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseRetryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param warehouseCheckpointService 依赖组件，用于完成数据访问或外部能力调用。
     */
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
    /**
     * 初始化 CdpEventIngestionService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventDefinitionCacheService 依赖组件，用于完成数据访问或外部能力调用。
     * @param userService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param discoveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param publisher publisher 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseEventSink warehouse event sink 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseRetryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param warehouseCheckpointService 依赖组件，用于完成数据访问或外部能力调用。
     * @param privacyTombstoneService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 初始化 CdpEventIngestionService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventDefinitionCacheService 依赖组件，用于完成数据访问或外部能力调用。
     * @param userService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param discoveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param publisher publisher 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseEventSink warehouse event sink 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 初始化 CdpEventIngestionService 实例。
     *
     * @param eventLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param eventDefinitionCacheService 依赖组件，用于完成数据访问或外部能力调用。
     * @param userService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param discoveryService 依赖组件，用于完成数据访问或外部能力调用。
     * @param publisher publisher 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseEventSink warehouse event sink 参数，用于 CdpEventIngestionService 流程中的校验、计算或对象转换。
     * @param warehouseRetryService 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param req 请求对象，承载本次操作的输入参数。
     * @return 返回 ingestBatch 流程生成的业务结果。
     */
    public IngestionResult ingestBatch(CdpWriteKeyAuthService.AuthenticatedWriteKey key, BatchTrackReq req) {
        List<TrackEventReq> batch = req == null || req.batch() == null ? List.of() : req.batch();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (batch.size() > maxBatchSize) {
            return new IngestionResult(0, batch.size(),
                    List.of(new IngestionError(null, "BATCH_TOO_LARGE", "batch size exceeds " + maxBatchSize)));
        }

        int accepted = 0;
        List<IngestionError> errors = new ArrayList<>();
        OffsetDateTime batchSentAt = req == null ? null : req.sentAt();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (TrackEventReq event : batch) {
            try {
                if (ingestOne(key, event, batchSentAt)) {
                    accepted++;
                }
            } catch (IllegalArgumentException ex) {
                errors.add(new IngestionError(event == null ? null : event.messageId(), "INVALID_EVENT", ex.getMessage()));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new IngestionResult(accepted, errors.size(), errors);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param event event 参数，用于 ingestOne 流程中的校验、计算或对象转换。
     * @param batchSentAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 ingest one 的布尔判断结果。
     */
    protected boolean ingestOne(CdpWriteKeyAuthService.AuthenticatedWriteKey key, TrackEventReq event, OffsetDateTime batchSentAt) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return true;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param userId 业务对象 ID，用于定位具体记录。
     * @param source source 参数，用于 enforcePrivacyTombstone 流程中的校验、计算或对象转换。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param failure failure 参数，用于 enqueueWarehouseRetry 流程中的校验、计算或对象转换。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param deliverySource delivery source 参数，用于 recordWarehouseCheckpointDelivered 流程中的校验、计算或对象转换。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param errorMessage error message 参数，用于 recordWarehouseCheckpointFailure 流程中的校验、计算或对象转换。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param event event 参数，用于 toRow 流程中的校验、计算或对象转换。
     * @param eventCode 业务编码，用于匹配对应类型或状态。
     * @param batchSentAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private CdpEventLogDO toRow(CdpWriteKeyAuthService.AuthenticatedWriteKey key, TrackEventReq event,
                                String eventCode, OffsetDateTime batchSentAt) {
        // 准备本次处理所需的上下文和中间变量。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param messageId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    private boolean isDuplicateMessage(Long tenantId, String messageId) {
        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
                .eq(CdpEventLogDO::getTenantId, tenantId)
                .eq(CdpEventLogDO::getMessageId, messageId));
        return count != null && count > 0;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
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
    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param MapString map string 参数，用于 readContextString 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @param directKey 业务键，用于在同一租户下定位资源。
     * @param snakeKey 业务键，用于在同一租户下定位资源。
     * @param nestedKey 业务键，用于在同一租户下定位资源。
     * @return 返回 read context string 生成的文本或业务键。
     */
    private String readContextString(Map<String, Object> context, String directKey, String snakeKey, String nestedKey) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @param MapString map string 参数，用于 readPlatform 流程中的校验、计算或对象转换。
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 read platform 生成的文本或业务键。
     */
    private String readPlatform(CdpWriteKeyAuthService.AuthenticatedWriteKey key, Map<String, Object> context) {
        Object platform = context == null ? null : context.get("platform");
        return platform == null || String.valueOf(platform).isBlank() ? key.platform() : String.valueOf(platform);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param MapString map string 参数，用于 writeJson 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 write json 生成的文本或业务键。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private LocalDateTime toLocal(OffsetDateTime value) {
        return LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require text 生成的文本或业务键。
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
