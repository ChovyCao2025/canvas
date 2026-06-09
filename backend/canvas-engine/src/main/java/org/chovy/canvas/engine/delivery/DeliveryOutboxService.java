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
/**
 * DeliveryOutboxService 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
 */
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
    /**
     * 初始化 DeliveryOutboxService 实例。
     *
     * @param jdbcTemplate jdbc template 参数，用于 DeliveryOutboxService 流程中的校验、计算或对象转换。
     * @param recordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param rocketMQTemplateProvider rocket mqtemplate provider 参数，用于 DeliveryOutboxService 流程中的校验、计算或对象转换。
     * @param outboxTopic outbox topic 参数，用于 DeliveryOutboxService 流程中的校验、计算或对象转换。
     * @param mqPublishEnabled mq publish enabled 参数，用于 DeliveryOutboxService 流程中的校验、计算或对象转换。
     */
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

    /**
     * 初始化 DeliveryOutboxService 实例。
     *
     * @param jdbcTemplate jdbc template 参数，用于 DeliveryOutboxService 流程中的校验、计算或对象转换。
     * @param recordMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public DeliveryOutboxService(JdbcTemplate jdbcTemplate,
                                 MessageSendRecordMapper recordMapper,
                                 ObjectMapper objectMapper) {
        this(jdbcTemplate, recordMapper, objectMapper, null, "CANVAS_DELIVERY", false);
    }

    @Transactional
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 enqueue 流程生成的业务结果。
     */
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
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param workerId 业务对象 ID，用于定位具体记录。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 claimNext 流程生成的业务结果。
     */
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
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param outboxId 业务对象 ID，用于定位具体记录。
     * @param providerMessageId 业务对象 ID，用于定位具体记录。
     * @param MapString map string 参数，用于 markSent 流程中的校验、计算或对象转换。
     * @param response response 参数，用于 markSent 流程中的校验、计算或对象转换。
     */
    public void markSent(Long outboxId, String providerMessageId, Map<String, Object> response) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime now = LocalDateTime.now();
        String responseJson = toJson(response == null ? Map.of() : response);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param outboxId 业务对象 ID，用于定位具体记录。
     * @param errorMessage error message 参数，用于 markRetry 流程中的校验、计算或对象转换。
     * @param nextRetryAt 时间参数，用于计算窗口、过期或审计时间。
     */
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
    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param outboxId 业务对象 ID，用于定位具体记录。
     * @param errorMessage error message 参数，用于 markDead 流程中的校验、计算或对象转换。
     */
    public void markDead(Long outboxId, String errorMessage) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime now = LocalDateTime.now();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param before before 参数，用于 findStalePending 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
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
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param before before 参数，用于 requeueStalePending 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 requeue stale pending 计算得到的数量、金额或指标值。
     */
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
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param outboxId 业务对象 ID，用于定位具体记录。
     * @return 返回 replay dead 的布尔判断结果。
     */
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
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Optional<DeliveryOutboxDO> findById(Long id) {
        List<DeliveryOutboxDO> rows = jdbcTemplate.query(
                "SELECT * FROM delivery_outbox WHERE id = ? LIMIT 1",
                rowMapper(),
                id);
        return rows.stream().findFirst();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    public Optional<DeliveryOutboxDO> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        List<DeliveryOutboxDO> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE tenant_id = ? AND idempotency_key = ?
                LIMIT 1
                """, rowMapper(), normalizeTenantId(tenantId), idempotencyKey);
        return rows.stream().findFirst();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param provider provider 参数，用于 findByProviderMessageId 流程中的校验、计算或对象转换。
     * @param providerMessageId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Optional<DeliveryOutboxDO> findByProviderMessageId(String provider, String providerMessageId) {
        List<DeliveryOutboxDO> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE provider = ? AND provider_message_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, rowMapper(), normalizeProvider(provider), providerMessageId);
        return rows.stream().findFirst();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param criteria criteria 参数，用于 search 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param outboxId 业务对象 ID，用于定位具体记录。
     * @return 返回 receipt history 汇总后的集合、分页或映射视图。
     */
    public List<DeliveryReceiptLog> receiptHistory(Long outboxId) {
        return jdbcTemplate.query("""
                SELECT * FROM delivery_receipt_log
                WHERE outbox_id = ?
                ORDER BY received_at DESC, id DESC
                """, receiptRowMapper(), outboxId);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    protected List<DeliveryOutboxDO> findClaimCandidates(LocalDateTime now, int limit) {
        return jdbcTemplate.query("""
                SELECT * FROM delivery_outbox
                WHERE status IN (?, ?)
                  AND (next_retry_at IS NULL OR next_retry_at <= ?)
                ORDER BY COALESCE(next_retry_at, created_at) ASC, id ASC
                LIMIT ?
                """, rowMapper(), STATUS_PENDING, STATUS_RETRY, now, normalizeLimit(limit));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 prepareMessageRecord 流程生成的业务结果。
     */
    protected MessageSendRecordDO prepareMessageRecord(ReachDeliveryService.DeliveryRequest request) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        MessageSendRecordDO existing = recordMapper.selectOne(new LambdaQueryWrapper<MessageSendRecordDO>()
                .eq(MessageSendRecordDO::getIdempotencyKey, request.idempotencyKey())
                .last("LIMIT 1"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
                // 汇总前面计算出的状态和明细，返回给调用方。
                return duplicateRecord;
            }
            throw duplicate;
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @param messageSendRecordId 业务对象 ID，用于定位具体记录。
     * @return 返回 insert outbox 计算得到的数量、金额或指标值。
     */
    protected Long insertOutbox(ReachDeliveryService.DeliveryRequest request, Long messageSendRecordId) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return findByIdempotencyKey(request.tenantId(), request.idempotencyKey())
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(DeliveryOutboxDO::getId)
                .orElseThrow(() -> new IllegalStateException("delivery outbox insert did not return a key"));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param outbox outbox 参数，用于 insertReceipt 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @param rawJson JSON 字符串，承载结构化配置或明细。
     * @param receivedAt 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param idempotencyKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private Optional<DeliveryReceiptLog> findReceiptByIdempotencyKey(Long tenantId, String idempotencyKey) {
        List<DeliveryReceiptLog> rows = jdbcTemplate.query("""
                SELECT * FROM delivery_receipt_log
                WHERE tenant_id = ? AND idempotency_key = ?
                LIMIT 1
                """, receiptRowMapper(), tenantId, idempotencyKey);
        return rows.stream().findFirst();
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param outbox outbox 参数，用于 updateCurrentReceiptStatus 流程中的校验、计算或对象转换。
     * @param receiptType 类型标识，用于选择对应处理分支。
     * @param receivedAt 时间参数，用于计算窗口、过期或审计时间。
     */
    private void updateCurrentReceiptStatus(DeliveryOutboxDO outbox, String receiptType, LocalDateTime receivedAt) {
        // 准备本次处理所需的上下文和中间变量。
        String status = normalizeStatus(receiptType);
        MessageSendRecordDO record = new MessageSendRecordDO();
        record.setId(outbox.getMessageSendRecordId());
        record.setStatus(status);
        record.setExternalMessageId(outbox.getProviderMessageId());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        record.setUpdatedAt(receivedAt);
        recordMapper.updateById(record);
        jdbcTemplate.update("UPDATE delivery_outbox SET status = ?, updated_at = ? WHERE id = ?",
                status, receivedAt, outbox.getId());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param criteria criteria 参数，用于 buildWhere 流程中的校验、计算或对象转换。
     * @param args 命令行参数，用于读取运行配置。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param clauses clauses 参数，用于 addStringFilter 流程中的校验、计算或对象转换。
     * @param args 命令行参数，用于读取运行配置。
     * @param column column 参数，用于 addStringFilter 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     */
    private void addStringFilter(List<String> clauses, List<Object> args, String column, String value) {
        if (value != null && !value.isBlank()) {
            clauses.add(column + " = ?");
            args.add(value);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param outbox outbox 参数，用于 payloadAsMap 流程中的校验、计算或对象转换。
     * @return 返回 payloadAsMap 流程生成的业务结果。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @return 返回 row mapper 汇总后的集合、分页或映射视图。
     */
    private RowMapper<DeliveryOutboxDO> rowMapper() {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                .build();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 receipt row mapper 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param timestamp 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回组装或转换后的结果对象。
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("delivery JSON serialization failed", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 receipt idempotency key 生成的文本或业务键。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 eventId 流程中的校验、计算或对象转换。
     * @param rawPayload raw payload 参数，用于 eventId 流程中的校验、计算或对象转换。
     * @return 返回 eventId 流程生成的业务结果。
     */
    private Object eventId(Map<String, Object> rawPayload) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rawPayload == null || rawPayload.isEmpty()) {
            return null;
        }
        Object eventId = rawPayload.get("eventId");
        if (eventId == null) eventId = rawPayload.get("event_id");
        if (eventId == null) eventId = rawPayload.get("id");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return eventId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenantId(Long tenantId) {
        return tenantId == null ? 1L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param provider provider 参数，用于 normalizeProvider 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeProvider(String provider) {
        return provider == null || provider.isBlank() ? "REACH" : provider.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "UNKNOWN" : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 truncate 流程中的校验、计算或对象转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param outbox outbox 参数，用于 publishAfterCommit 流程中的校验、计算或对象转换。
     */
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
                /**
                 * 根据方法职责完成对应的业务处理流程。
                 */
                public void afterCommit() {
                    publisher.run();
                }
            });
            return;
        }
        publisher.run();
    }

    /**
     * DeliverySearchCriteria 参与画布执行引擎流程，封装节点、调度或运行时处理能力。
     */
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
        /**
         * 生成默认值或兜底结果，保证调用链稳定。
         *
         * @return 返回 defaults 流程生成的业务结果。
         */
        static DeliverySearchCriteria defaults() {
            return new DeliverySearchCriteria(null, null, null, null, null, null, null, null, 1, 20);
        }

        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @return 返回解析、归一化或安全处理后的值。
         */
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

        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回解析、归一化或安全处理后的值。
         */
        private static String normalizeUpper(String value) {
            String normalized = blankToNull(value);
            return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
        }

        /**
         * 解析、归一化或保护输入值，生成安全可用的中间结果。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回解析、归一化或安全处理后的值。
         */
        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
