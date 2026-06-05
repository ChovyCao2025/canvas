package org.chovy.canvas.domain.bi.subscription;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAlertRuleDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiDeliveryLogDO;
import org.chovy.canvas.dal.dataobject.BiSubscriptionDO;
import org.chovy.canvas.dal.mapper.BiAlertRuleMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiDeliveryLogMapper;
import org.chovy.canvas.dal.mapper.BiSubscriptionMapper;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.notification.NotificationCreateCommand;
import org.chovy.canvas.domain.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BiDeliveryRuntimeService {

    private static final String JOB_SUBSCRIPTION = "SUBSCRIPTION";
    private static final String JOB_ALERT = "ALERT";
    private static final String CHANNEL_EVALUATION = "EVALUATION";
    private static final String STATUS_DELIVERED = "DELIVERED";
    private static final String STATUS_PENDING_ADAPTER = "PENDING_ADAPTER";
    private static final String STATUS_TRIGGERED = "TRIGGERED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_FAILED = "FAILED";

    private final BiSubscriptionMapper subscriptionMapper;
    private final BiAlertRuleMapper alertRuleMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDeliveryLogMapper deliveryLogMapper;
    private final BiQueryExecutionService queryExecutionService;
    private final NotificationService notificationService;
    private final BiDeliveryAdapterService deliveryAdapterService;
    private final BiDeliveryAttachmentService attachmentService;
    private final ObjectMapper objectMapper;
    private final int maxRetryCount;
    private final int retryInitialDelayMinutes;
    private final double retryBackoffMultiplier;
    private final int retryMaxDelayMinutes;

    @Autowired
    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    ObjectProvider<NotificationService> notificationServiceProvider,
                                    ObjectProvider<BiDeliveryAdapterService> deliveryAdapterServiceProvider,
                                    ObjectProvider<BiDeliveryAttachmentService> attachmentServiceProvider,
                                    ObjectMapper objectMapper,
                                    @Value("${canvas.bi.delivery.retry.max-attempts:4}") int maxRetryCount,
                                    @Value("${canvas.bi.delivery.retry.initial-delay-minutes:30}") int retryInitialDelayMinutes,
                                    @Value("${canvas.bi.delivery.retry.backoff-multiplier:2.0}") double retryBackoffMultiplier,
                                    @Value("${canvas.bi.delivery.retry.max-delay-minutes:1440}") int retryMaxDelayMinutes) {
        this(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                notificationServiceProvider.getIfAvailable(),
                deliveryAdapterServiceProvider.getIfAvailable(),
                attachmentServiceProvider.getIfAvailable(),
                objectMapper,
                maxRetryCount,
                retryInitialDelayMinutes,
                retryBackoffMultiplier,
                retryMaxDelayMinutes);
    }

    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    NotificationService notificationService,
                                    ObjectMapper objectMapper) {
        this(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                notificationService,
                null,
                null,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    NotificationService notificationService,
                                    BiDeliveryAdapterService deliveryAdapterService,
                                    ObjectMapper objectMapper) {
        this(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                notificationService,
                deliveryAdapterService,
                null,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    NotificationService notificationService,
                                    BiDeliveryAdapterService deliveryAdapterService,
                                    BiDeliveryAttachmentService attachmentService,
                                    ObjectMapper objectMapper) {
        this(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                notificationService,
                deliveryAdapterService,
                attachmentService,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    NotificationService notificationService,
                                    BiDeliveryAdapterService deliveryAdapterService,
                                    BiDeliveryAttachmentService attachmentService,
                                    ObjectMapper objectMapper,
                                    int maxRetryCount,
                                    int retryInitialDelayMinutes,
                                    double retryBackoffMultiplier,
                                    int retryMaxDelayMinutes) {
        this.subscriptionMapper = subscriptionMapper;
        this.alertRuleMapper = alertRuleMapper;
        this.datasetMapper = datasetMapper;
        this.deliveryLogMapper = deliveryLogMapper;
        this.queryExecutionService = queryExecutionService;
        this.notificationService = notificationService;
        this.deliveryAdapterService = deliveryAdapterService;
        this.attachmentService = attachmentService;
        this.objectMapper = objectMapper;
        this.maxRetryCount = Math.max(0, maxRetryCount);
        this.retryInitialDelayMinutes = Math.max(1, retryInitialDelayMinutes);
        this.retryBackoffMultiplier = retryBackoffMultiplier < 1.0 ? 1.0 : retryBackoffMultiplier;
        this.retryMaxDelayMinutes = Math.max(this.retryInitialDelayMinutes, retryMaxDelayMinutes);
    }

    public BiDeliveryRunResult runSubscription(Long tenantId, Long subscriptionId, String username) {
        BiSubscriptionDO subscription = subscriptionMapper.selectById(subscriptionId);
        if (subscription == null || !normalizeTenant(subscription.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI subscription not found: " + subscriptionId);
        }
        if (!Boolean.TRUE.equals(subscription.getEnabled())) {
            BiDeliveryLogView log = toView(insertLog(
                    tenantId,
                    subscription.getWorkspaceId(),
                    JOB_SUBSCRIPTION,
                    subscription.getId(),
                    subscription.getSubscriptionKey(),
                    subscription.getResourceType(),
                    subscription.getResourceId(),
                    CHANNEL_EVALUATION,
                    Map.of(),
                    payload("Subscription is disabled", subscription.getName(), null),
                    null,
                    STATUS_SKIPPED,
                    "Subscription is disabled",
                    null,
                    username));
            return new BiDeliveryRunResult(JOB_SUBSCRIPTION, subscription.getId(), subscription.getSubscriptionKey(), STATUS_SKIPPED, List.of(log));
        }

        Map<String, Object> receivers = map(subscription.getReceiverJson());
        Map<String, Object> delivery = map(subscription.getDeliveryJson());
        Map<String, Object> schedule = map(subscription.getScheduleJson());
        List<BiDeliveryAttachmentView> attachments = attachmentService == null
                ? List.of()
                : attachmentService.createSubscriptionAttachments(
                normalizeTenant(tenantId),
                subscription,
                schedule,
                delivery,
                username);
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("resourceType", subscription.getResourceType());
        extra.put("resourceId", subscription.getResourceId());
        extra.put("schedule", schedule);
        extra.put("delivery", delivery);
        extra.put("url", resourceUrl(subscription.getResourceType(), subscription.getResourceId()));
        extra.put("attachments", attachmentPayload(attachments));
        Map<String, Object> payload = payload(
                "BI subscription delivery is ready",
                subscription.getName(),
                extra);
        List<BiDeliveryLogView> logs = new ArrayList<>();
        for (String channel : channels(receivers)) {
            logs.add(toView(deliverChannel(
                    normalizeTenant(tenantId),
                    subscription.getWorkspaceId(),
                    JOB_SUBSCRIPTION,
                    subscription.getId(),
                    subscription.getSubscriptionKey(),
                    subscription.getResourceType(),
                    subscription.getResourceId(),
                    channel,
                    receivers,
                    payload,
                    attachments,
                    null,
                    username,
                    "BI_SUBSCRIPTION")));
        }
        return new BiDeliveryRunResult(JOB_SUBSCRIPTION, subscription.getId(), subscription.getSubscriptionKey(), STATUS_TRIGGERED, logs);
    }

    public BiDeliveryRunResult runAlert(Long tenantId, String username, String role, Long alertId) {
        BiAlertRuleDO alert = alertRuleMapper.selectById(alertId);
        if (alert == null || !normalizeTenant(alert.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI alert rule not found: " + alertId);
        }
        if (!Boolean.TRUE.equals(alert.getEnabled())) {
            BiDeliveryLogView log = toView(insertLog(
                    tenantId,
                    alert.getWorkspaceId(),
                    JOB_ALERT,
                    alert.getId(),
                    alert.getAlertKey(),
                    "DATASET",
                    alert.getDatasetId(),
                    CHANNEL_EVALUATION,
                    Map.of(),
                    payload("Alert rule is disabled", alert.getName(), null),
                    null,
                    STATUS_SKIPPED,
                    "Alert rule is disabled",
                    null,
                    username));
            return new BiDeliveryRunResult(JOB_ALERT, alert.getId(), alert.getAlertKey(), STATUS_SKIPPED, List.of(log));
        }

        Map<String, Object> condition = map(alert.getConditionJson());
        BigDecimal value = queryMetric(normalizeTenant(tenantId), username, role, alert);
        AlertEvaluation evaluation = evaluateAlert(normalizeTenant(tenantId), alert, value, condition);
        Map<String, Object> receivers = map(alert.getReceiverJson());
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("datasetId", alert.getDatasetId());
        extra.put("metricKey", alert.getMetricKey());
        extra.put("condition", condition);
        extra.put("metricValue", value);
        if (evaluation.anomaly() != null) {
            extra.put("anomaly", evaluation.anomaly().payload());
        }
        AlertSilence silence = evaluateSilence(condition, LocalDateTime.now());
        if (evaluation.matched() && silence.silenced()) {
            extra.put("silence", silence.payload());
            Map<String, Object> payload = payload(
                    "BI alert is silenced",
                    alert.getName(),
                    extra);
            BiDeliveryLogView log = toView(insertLog(
                    tenantId,
                    alert.getWorkspaceId(),
                    JOB_ALERT,
                    alert.getId(),
                    alert.getAlertKey(),
                    "DATASET",
                    alert.getDatasetId(),
                    CHANNEL_EVALUATION,
                    receivers,
                    payload,
                    value,
                    STATUS_SKIPPED,
                    "Alert is silenced",
                    null,
                    username));
            return new BiDeliveryRunResult(JOB_ALERT, alert.getId(), alert.getAlertKey(), STATUS_SKIPPED, List.of(log));
        }
        Map<String, Object> payload = payload(
                evaluation.payloadMessage(),
                alert.getName(),
                extra);
        List<BiDeliveryLogView> logs = new ArrayList<>();
        logs.add(toView(insertLog(
                tenantId,
                alert.getWorkspaceId(),
                JOB_ALERT,
                alert.getId(),
                alert.getAlertKey(),
                "DATASET",
                alert.getDatasetId(),
                CHANNEL_EVALUATION,
                receivers,
                payload,
                value,
                evaluation.status(),
                evaluation.logMessage(),
                null,
                username)));
        if (evaluation.matched()) {
            for (String channel : channels(receivers)) {
                logs.add(toView(deliverChannel(
                        normalizeTenant(tenantId),
                        alert.getWorkspaceId(),
                        JOB_ALERT,
                        alert.getId(),
                        alert.getAlertKey(),
                        "DATASET",
                        alert.getDatasetId(),
                        channel,
                        receivers,
                        payload,
                        value,
                        username,
                        "BI_ALERT")));
            }
        }
        return new BiDeliveryRunResult(JOB_ALERT, alert.getId(), alert.getAlertKey(),
                evaluation.status(), logs);
    }

    public List<BiDeliveryLogView> listLogs(Long tenantId, String jobType, Long jobId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        LambdaQueryWrapper<BiDeliveryLogDO> query = new LambdaQueryWrapper<BiDeliveryLogDO>()
                .eq(BiDeliveryLogDO::getTenantId, scopedTenantId)
                .orderByDesc(BiDeliveryLogDO::getCreatedAt)
                .orderByDesc(BiDeliveryLogDO::getId)
                .last("LIMIT " + capped);
        if (hasText(jobType)) {
            query.eq(BiDeliveryLogDO::getJobType, jobType.trim().toUpperCase(Locale.ROOT));
        }
        if (jobId != null) {
            query.eq(BiDeliveryLogDO::getJobId, jobId);
        }
        return safeList(deliveryLogMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    public BiDeliveryAuditSummary auditDeliveries(Long tenantId,
                                                  String jobType,
                                                  String status,
                                                  String channel,
                                                  Long jobId,
                                                  int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 200));
        LambdaQueryWrapper<BiDeliveryLogDO> query = new LambdaQueryWrapper<BiDeliveryLogDO>()
                .eq(BiDeliveryLogDO::getTenantId, scopedTenantId)
                .orderByDesc(BiDeliveryLogDO::getCreatedAt)
                .orderByDesc(BiDeliveryLogDO::getId)
                .last("LIMIT " + capped);
        if (hasText(jobType)) {
            query.eq(BiDeliveryLogDO::getJobType, jobType.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(status)) {
            query.eq(BiDeliveryLogDO::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (hasText(channel)) {
            query.eq(BiDeliveryLogDO::getChannel, channel.trim().toUpperCase(Locale.ROOT));
        }
        if (jobId != null) {
            query.eq(BiDeliveryLogDO::getJobId, jobId);
        }
        List<BiDeliveryLogDO> rows = safeList(deliveryLogMapper.selectList(query));
        int delivered = 0;
        int triggered = 0;
        int skipped = 0;
        int pending = 0;
        int failed = 0;
        int retryable = 0;
        int retryExhausted = 0;
        for (BiDeliveryLogDO row : rows) {
            String rowStatus = row.getStatus();
            if (STATUS_DELIVERED.equals(rowStatus)) {
                delivered++;
            } else if (STATUS_TRIGGERED.equals(rowStatus)) {
                triggered++;
            } else if (STATUS_SKIPPED.equals(rowStatus)) {
                skipped++;
            } else if (STATUS_PENDING_ADAPTER.equals(rowStatus)) {
                pending++;
            } else if (STATUS_FAILED.equals(rowStatus)) {
                failed++;
            }
            if (row.getRetryExhaustedAt() != null) {
                retryExhausted++;
            } else if (retryableStatus(rowStatus) && retryAttemptAvailable(row)) {
                retryable++;
            }
        }
        return new BiDeliveryAuditSummary(
                rows.size(),
                delivered,
                triggered,
                skipped,
                pending,
                failed,
                retryable,
                retryExhausted,
                rows.stream().map(this::toView).toList());
    }

    public BiDeliveryRetryResult retryPendingDeliveries(Long tenantId, String username, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 50));
        List<BiDeliveryLogDO> retryable = safeList(deliveryLogMapper.selectList(new LambdaQueryWrapper<BiDeliveryLogDO>()
                .eq(BiDeliveryLogDO::getTenantId, scopedTenantId)
                .in(BiDeliveryLogDO::getStatus, STATUS_PENDING_ADAPTER, STATUS_FAILED)
                .ne(BiDeliveryLogDO::getChannel, CHANNEL_EVALUATION)
                .lt(BiDeliveryLogDO::getRetryCount, maxRetryCount)
                .isNull(BiDeliveryLogDO::getRetryExhaustedAt)
                .and(query -> query.isNull(BiDeliveryLogDO::getNextRetryAt)
                        .or()
                        .le(BiDeliveryLogDO::getNextRetryAt, LocalDateTime.now()))
                .orderByAsc(BiDeliveryLogDO::getNextRetryAt)
                .orderByAsc(BiDeliveryLogDO::getCreatedAt)
                .orderByAsc(BiDeliveryLogDO::getId)
                .last("LIMIT " + capped)));
        List<BiDeliveryLogView> logs = new ArrayList<>();
        int delivered = 0;
        int pending = 0;
        int failed = 0;
        for (BiDeliveryLogDO log : retryable) {
            BiDeliveryLogDO retryLog = deliverChannel(
                    scopedTenantId,
                    log.getWorkspaceId(),
                    log.getJobType(),
                    log.getJobId(),
                    log.getJobKey(),
                    log.getResourceType(),
                    log.getResourceId(),
                    log.getChannel(),
                    map(log.getReceiverJson()),
                    map(log.getPayloadJson()),
                    log.getMetricValue(),
                    username,
                    notificationType(log.getJobType()),
                    retryAttempt(log));
            logs.add(toView(retryLog));
            if (STATUS_DELIVERED.equals(retryLog.getStatus())) {
                delivered++;
            } else if (STATUS_FAILED.equals(retryLog.getStatus())) {
                failed++;
            } else {
                pending++;
            }
        }
        return new BiDeliveryRetryResult(retryable.size(), logs.size(), delivered, pending, failed, logs);
    }

    private BiDeliveryLogDO deliverChannel(Long tenantId,
                                           Long workspaceId,
                                           String jobType,
                                           Long jobId,
                                           String jobKey,
                                           String resourceType,
                                           Long resourceId,
                                           String channel,
                                           Map<String, Object> receivers,
                                           Map<String, Object> payload,
                                           BigDecimal metricValue,
                                           String username,
                                           String notificationType) {
        return deliverChannel(tenantId,
                workspaceId,
                jobType,
                jobId,
                jobKey,
                resourceType,
                resourceId,
                channel,
                receivers,
                payload,
                metricValue,
                username,
                notificationType,
                0);
    }

    private BiDeliveryLogDO deliverChannel(Long tenantId,
                                           Long workspaceId,
                                           String jobType,
                                           Long jobId,
                                           String jobKey,
                                           String resourceType,
                                           Long resourceId,
                                           String channel,
                                           Map<String, Object> receivers,
                                           Map<String, Object> payload,
                                           BigDecimal metricValue,
                                           String username,
                                           String notificationType,
                                           int retryCount) {
        return deliverChannel(tenantId,
                workspaceId,
                jobType,
                jobId,
                jobKey,
                resourceType,
                resourceId,
                channel,
                receivers,
                payload,
                List.of(),
                metricValue,
                username,
                notificationType,
                retryCount);
    }

    private BiDeliveryLogDO deliverChannel(Long tenantId,
                                           Long workspaceId,
                                           String jobType,
                                           Long jobId,
                                           String jobKey,
                                           String resourceType,
                                           Long resourceId,
                                           String channel,
                                           Map<String, Object> receivers,
                                           Map<String, Object> payload,
                                           List<BiDeliveryAttachmentView> attachments,
                                           BigDecimal metricValue,
                                           String username,
                                           String notificationType) {
        return deliverChannel(tenantId,
                workspaceId,
                jobType,
                jobId,
                jobKey,
                resourceType,
                resourceId,
                channel,
                receivers,
                payload,
                attachments,
                metricValue,
                username,
                notificationType,
                0);
    }

    private BiDeliveryLogDO deliverChannel(Long tenantId,
                                           Long workspaceId,
                                           String jobType,
                                           Long jobId,
                                           String jobKey,
                                           String resourceType,
                                           Long resourceId,
                                           String channel,
                                           Map<String, Object> receivers,
                                           Map<String, Object> payload,
                                           List<BiDeliveryAttachmentView> attachments,
                                           BigDecimal metricValue,
                                           String username,
                                           String notificationType,
                                           int retryCount) {
        String normalizedChannel = channel.trim().toUpperCase(Locale.ROOT);
        if (SetLike.inApp(normalizedChannel) && notificationService != null) {
            for (String user : receiverUsers(receivers, username)) {
                notificationService.create(NotificationCreateCommand.builder()
                        .tenantId(tenantId)
                        .userId(user)
                        .category("BI")
                        .severity(JOB_ALERT.equals(jobType) ? "WARNING" : "INFO")
                        .type(notificationType)
                        .title(String.valueOf(payload.getOrDefault("title", "BI 通知")))
                        .content(String.valueOf(payload.getOrDefault("message", "")))
                        .targetUrl(String.valueOf(payload.getOrDefault("url", "/bi")))
                        .actionLabel("查看分析")
                        .actionUrl(String.valueOf(payload.getOrDefault("url", "/bi")))
                        .bizType(jobType)
                        .bizId(String.valueOf(jobId))
                        .dedupKey("bi:" + jobType + ":" + jobId + ":" + user + ":" + payload.getOrDefault("message", "delivery"))
                        .payloadJson(json(payload))
                        .build());
            }
            return insertLog(tenantId, workspaceId, jobType, jobId, jobKey, resourceType, resourceId,
                    normalizedChannel, receivers, payload, metricValue, STATUS_DELIVERED,
                    "Delivered to notification center", null, username, retryCount);
        }
        if (deliveryAdapterService != null) {
            List<BiEmailAttachment> emailAttachments;
            try {
                emailAttachments = emailAttachments(normalizedChannel, tenantId, attachments, payload);
            } catch (RuntimeException e) {
                return insertLog(tenantId, workspaceId, jobType, jobId, jobKey, resourceType, resourceId,
                        normalizedChannel, receivers, payload, metricValue, STATUS_FAILED,
                        "Email attachment materialization failed", e.getMessage(), username, retryCount);
            }
            BiDeliveryAdapterResult result = deliveryAdapterService.deliver(new BiDeliveryAdapterRequest(
                    tenantId,
                    workspaceId,
                    jobType,
                    jobId,
                    jobKey,
                    resourceType,
                    resourceId,
                    normalizedChannel,
                    receivers,
                    payload,
                    metricValue,
                    username,
                    emailAttachments));
            return insertLog(tenantId, workspaceId, jobType, jobId, jobKey, resourceType, resourceId,
                    normalizedChannel, receivers, payload, metricValue, result.status(),
                    result.message(), result.errorMessage(), username, retryCount);
        }
        return insertLog(tenantId, workspaceId, jobType, jobId, jobKey, resourceType, resourceId,
                normalizedChannel, receivers, payload, metricValue, STATUS_PENDING_ADAPTER,
                "Delivery adapter is not configured yet", null, username, retryCount);
    }

    private BigDecimal queryMetric(Long tenantId, String username, String role, BiAlertRuleDO alert) {
        try {
            BiQueryResult result = queryExecutionService.execute(new BiQueryRequest(
                    datasetKey(alert.getDatasetId()),
                    List.of(),
                    List.of(alert.getMetricKey()),
                    List.of(),
                    List.of(),
                    1), new BiQueryContext(tenantId, username, role));
            if (result.rows().isEmpty()) {
                return BigDecimal.ZERO;
            }
            Object value = result.rows().get(0).get(alert.getMetricKey());
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(String.valueOf(value));
        } catch (RuntimeException e) {
            insertLog(tenantId, alert.getWorkspaceId(), JOB_ALERT, alert.getId(), alert.getAlertKey(),
                    "DATASET", alert.getDatasetId(), CHANNEL_EVALUATION, map(alert.getReceiverJson()),
                    payload("BI alert metric query failed", alert.getName(), Map.of("metricKey", alert.getMetricKey())),
                    null, STATUS_FAILED, null, e.getMessage(), username);
            throw e;
        }
    }

    private String datasetKey(Long datasetId) {
        BiDatasetDO dataset = datasetMapper.selectById(datasetId);
        if (dataset == null || dataset.getDatasetKey() == null || dataset.getDatasetKey().isBlank()) {
            throw new IllegalArgumentException("BI alert dataset not found: " + datasetId);
        }
        return dataset.getDatasetKey();
    }

    private AlertEvaluation evaluateAlert(Long tenantId,
                                          BiAlertRuleDO alert,
                                          BigDecimal value,
                                          Map<String, Object> condition) {
        if (isAnomalyCondition(condition)) {
            AnomalyEvaluation anomaly = evaluateAnomaly(tenantId, alert, value, condition);
            if (anomaly.insufficientBaseline()) {
                return new AlertEvaluation(
                        false,
                        STATUS_SKIPPED,
                        "BI alert anomaly baseline is insufficient",
                        "Alert anomaly baseline is insufficient",
                        anomaly);
            }
            return new AlertEvaluation(
                    anomaly.matched(),
                    anomaly.matched() ? STATUS_TRIGGERED : STATUS_SKIPPED,
                    anomaly.matched() ? "BI alert anomaly detected" : "BI alert anomaly not detected",
                    anomaly.matched() ? "Alert anomaly detected" : "Alert anomaly not detected",
                    anomaly);
        }
        boolean matched = conditionMatches(value, condition);
        return new AlertEvaluation(
                matched,
                matched ? STATUS_TRIGGERED : STATUS_SKIPPED,
                matched ? "BI alert threshold matched" : "BI alert threshold not matched",
                matched ? "Alert condition matched" : "Alert condition not matched",
                null);
    }

    private AlertSilence evaluateSilence(Map<String, Object> condition, LocalDateTime now) {
        Map<String, Object> silence = silenceConfig(condition);
        if (silence.isEmpty()) {
            return new AlertSilence(false, Map.of());
        }
        if (Boolean.FALSE.equals(silence.get("enabled"))) {
            return new AlertSilence(false, silencePayload(false, silence, now, null));
        }

        LocalDateTime until = parseDateTime(firstValue(silence, "until", "muteUntil", "silenceUntil", "suppressedUntil"));
        if (until != null) {
            boolean active = !now.isAfter(until);
            return new AlertSilence(active, silencePayload(active, silence, now, until));
        }

        LocalTime from = parseTime(firstValue(silence, "from", "start", "startTime", "begin"));
        LocalTime to = parseTime(firstValue(silence, "to", "end", "endTime", "finish"));
        if (from != null && to != null) {
            boolean active = dayMatches(silence, now.getDayOfWeek()) && timeWindowMatches(now.toLocalTime(), from, to);
            return new AlertSilence(active, silencePayload(active, silence, now, null));
        }

        boolean enabled = Boolean.TRUE.equals(silence.getOrDefault("enabled", Boolean.TRUE));
        return new AlertSilence(enabled, silencePayload(enabled, silence, now, null));
    }

    private Map<String, Object> silenceConfig(Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            return Map.of();
        }
        Object nested = firstValue(condition, "silence", "mute", "quietHours", "silenceWindow");
        Map<String, Object> silence = objectMap(nested);
        if (Boolean.TRUE.equals(nested)) {
            silence = new LinkedHashMap<>();
            silence.put("enabled", true);
        }
        Map<String, Object> merged = new LinkedHashMap<>(silence);
        copyIfPresent(condition, merged, "enabled", "silenceEnabled", "muteEnabled");
        copyIfPresent(condition, merged, "until", "muteUntil", "silenceUntil", "suppressedUntil");
        copyIfPresent(condition, merged, "from", "start", "startTime", "begin");
        copyIfPresent(condition, merged, "to", "end", "endTime", "finish");
        copyIfPresent(condition, merged, "daysOfWeek", "days", "weekdays");
        copyIfPresent(condition, merged, "reason", "silenceReason", "muteReason");
        return merged;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String targetKey, String... sourceKeys) {
        Object value = source.get(targetKey);
        if (value == null) {
            value = firstValue(source, sourceKeys);
        }
        if (value != null && !target.containsKey(targetKey)) {
            target.put(targetKey, value);
        }
    }

    private Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, nestedValue) -> {
            if (key != null) {
                result.put(String.valueOf(key), nestedValue);
            }
        });
        return result;
    }

    private Map<String, Object> silencePayload(boolean silenced,
                                               Map<String, Object> silence,
                                               LocalDateTime now,
                                               LocalDateTime until) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("silenced", silenced);
        payload.put("reason", String.valueOf(silence.getOrDefault("reason", "")));
        payload.put("until", until == null ? firstValue(silence, "until", "muteUntil", "silenceUntil", "suppressedUntil") : until.toString());
        payload.put("from", firstValue(silence, "from", "start", "startTime", "begin"));
        payload.put("to", firstValue(silence, "to", "end", "endTime", "finish"));
        payload.put("daysOfWeek", firstValue(silence, "daysOfWeek", "days", "weekdays"));
        payload.put("evaluatedAt", now.toString());
        return payload;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        String text = String.valueOf(value).trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(text).atStartOfDay();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    private LocalTime parseTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(String.valueOf(value).trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private boolean timeWindowMatches(LocalTime current, LocalTime from, LocalTime to) {
        if (from.equals(to)) {
            return true;
        }
        if (from.isBefore(to)) {
            return !current.isBefore(from) && !current.isAfter(to);
        }
        return !current.isBefore(from) || !current.isAfter(to);
    }

    private boolean dayMatches(Map<String, Object> silence, DayOfWeek current) {
        Object days = firstValue(silence, "daysOfWeek", "days", "weekdays");
        if (!(days instanceof List<?> values) || values.isEmpty()) {
            return true;
        }
        return values.stream()
                .map(value -> String.valueOf(value).trim().toUpperCase(Locale.ROOT))
                .anyMatch(value -> value.equals(current.name()) || value.equals(String.valueOf(current.getValue())));
    }

    private boolean conditionMatches(BigDecimal value, Map<String, Object> condition) {
        BigDecimal threshold = new BigDecimal(String.valueOf(condition.getOrDefault("threshold", "0")));
        String operator = String.valueOf(condition.getOrDefault("operator", "GT")).toUpperCase(Locale.ROOT);
        int comparison = value.compareTo(threshold);
        return switch (operator) {
            case "GT" -> comparison > 0;
            case "GTE" -> comparison >= 0;
            case "LT" -> comparison < 0;
            case "LTE" -> comparison <= 0;
            case "EQ" -> comparison == 0;
            case "NEQ" -> comparison != 0;
            default -> throw new IllegalArgumentException("unsupported alert operator: " + operator);
        };
    }

    private AnomalyEvaluation evaluateAnomaly(Long tenantId,
                                              BiAlertRuleDO alert,
                                              BigDecimal value,
                                              Map<String, Object> condition) {
        int baselineWindow = intConfig(condition, 7, "baselineWindow", "windowSize", "sampleWindow");
        baselineWindow = Math.max(1, Math.min(baselineWindow, 100));
        int minSamples = intConfig(condition, 3, "minSamples", "minimumSamples");
        minSamples = Math.max(1, Math.min(minSamples, baselineWindow));
        double sensitivity = doubleConfig(condition, 2.0, "sensitivity", "stddevMultiplier");
        sensitivity = sensitivity <= 0 ? 2.0 : sensitivity;
        double minDelta = doubleConfig(condition, 0.0, "minDelta", "absoluteDelta");
        minDelta = Math.max(0.0, minDelta);
        double minDeltaPercent = doubleConfig(condition, 0.0, "minDeltaPercent", "deltaPercent");
        minDeltaPercent = Math.max(0.0, minDeltaPercent);
        String direction = anomalyDirection(condition);

        List<Double> samples = safeList(deliveryLogMapper.selectList(new LambdaQueryWrapper<BiDeliveryLogDO>()
                        .eq(BiDeliveryLogDO::getTenantId, tenantId)
                        .eq(BiDeliveryLogDO::getJobType, JOB_ALERT)
                        .eq(BiDeliveryLogDO::getJobId, alert.getId())
                        .eq(BiDeliveryLogDO::getChannel, CHANNEL_EVALUATION)
                        .isNotNull(BiDeliveryLogDO::getMetricValue)
                        .orderByDesc(BiDeliveryLogDO::getCreatedAt)
                        .orderByDesc(BiDeliveryLogDO::getId)
                        .last("LIMIT " + baselineWindow)))
                .stream()
                .map(BiDeliveryLogDO::getMetricValue)
                .filter(metricValue -> metricValue != null)
                .map(BigDecimal::doubleValue)
                .toList();
        if (samples.size() < minSamples) {
            return new AnomalyEvaluation(false, true, anomalyPayload(
                    value,
                    direction,
                    samples.size(),
                    baselineWindow,
                    minSamples,
                    sensitivity,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true));
        }

        double average = samples.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = samples.stream()
                .mapToDouble(sample -> Math.pow(sample - average, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        double current = value == null ? 0.0 : value.doubleValue();
        double delta = current - average;
        double absoluteDelta = Math.abs(delta);
        double deltaPercent = average == 0.0 ? absoluteDelta : absoluteDelta / Math.abs(average);
        double threshold = Math.max(minDelta, standardDeviation * sensitivity);
        boolean directionMatches = switch (direction) {
            case "DROP" -> delta < 0;
            case "RISE" -> delta > 0;
            default -> delta != 0;
        };
        boolean matched = directionMatches
                && absoluteDelta >= threshold
                && deltaPercent >= minDeltaPercent;
        return new AnomalyEvaluation(matched, false, anomalyPayload(
                value,
                direction,
                samples.size(),
                baselineWindow,
                minSamples,
                sensitivity,
                average,
                standardDeviation,
                delta,
                deltaPercent,
                threshold,
                matched,
                false));
    }

    private Map<String, Object> anomalyPayload(BigDecimal value,
                                               String direction,
                                               int sampleCount,
                                               int baselineWindow,
                                               int minSamples,
                                               double sensitivity,
                                               Double baselineAverage,
                                               Double standardDeviation,
                                               Double delta,
                                               Double deltaPercent,
                                               Double threshold,
                                               boolean matched,
                                               boolean insufficientBaseline) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ANOMALY");
        payload.put("direction", direction);
        payload.put("sampleCount", sampleCount);
        payload.put("baselineWindow", baselineWindow);
        payload.put("minSamples", minSamples);
        payload.put("sensitivity", sensitivity);
        payload.put("currentValue", value);
        payload.put("baselineAverage", baselineAverage);
        payload.put("standardDeviation", standardDeviation);
        payload.put("delta", delta);
        payload.put("deltaPercent", deltaPercent);
        payload.put("threshold", threshold);
        payload.put("matched", matched);
        payload.put("insufficientBaseline", insufficientBaseline);
        return payload;
    }

    private boolean isAnomalyCondition(Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }
        String operator = String.valueOf(condition.getOrDefault("operator", "")).trim().toUpperCase(Locale.ROOT);
        String mode = String.valueOf(condition.getOrDefault("mode", condition.getOrDefault("type", "")))
                .trim()
                .toUpperCase(Locale.ROOT);
        return operator.startsWith("ANOMALY") || "ANOMALY".equals(mode);
    }

    private String anomalyDirection(Map<String, Object> condition) {
        String direction = String.valueOf(condition.getOrDefault("direction", "")).trim().toUpperCase(Locale.ROOT);
        if (!direction.isBlank()) {
            return normalizeAnomalyDirection(direction);
        }
        String operator = String.valueOf(condition.getOrDefault("operator", "")).trim().toUpperCase(Locale.ROOT);
        if (operator.contains("DROP") || operator.contains("DECREASE")) {
            return "DROP";
        }
        if (operator.contains("RISE") || operator.contains("INCREASE") || operator.contains("SPIKE")) {
            return "RISE";
        }
        return "BOTH";
    }

    private String normalizeAnomalyDirection(String direction) {
        return switch (direction) {
            case "DROP", "DECREASE", "DOWN" -> "DROP";
            case "RISE", "INCREASE", "SPIKE", "UP" -> "RISE";
            default -> "BOTH";
        };
    }

    private int intConfig(Map<String, Object> condition, int defaultValue, String... keys) {
        Object value = firstValue(condition, keys);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double doubleConfig(Map<String, Object> condition, double defaultValue, String... keys) {
        Object value = firstValue(condition, keys);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Object firstValue(Map<String, Object> condition, String... keys) {
        if (condition == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (condition.containsKey(key)) {
                return condition.get(key);
            }
        }
        return null;
    }

    private BiDeliveryLogDO insertLog(Long tenantId,
                                      Long workspaceId,
                                      String jobType,
                                      Long jobId,
                                      String jobKey,
                                      String resourceType,
                                      Long resourceId,
                                      String channel,
                                      Map<String, Object> receiver,
                                      Map<String, Object> payload,
                                      BigDecimal metricValue,
                                      String status,
                                      String message,
                                      String errorMessage,
                                      String triggeredBy) {
        return insertLog(
                tenantId,
                workspaceId,
                jobType,
                jobId,
                jobKey,
                resourceType,
                resourceId,
                channel,
                receiver,
                payload,
                metricValue,
                status,
                message,
                errorMessage,
                triggeredBy,
                0);
    }

    private BiDeliveryLogDO insertLog(Long tenantId,
                                      Long workspaceId,
                                      String jobType,
                                      Long jobId,
                                      String jobKey,
                                      String resourceType,
                                      Long resourceId,
                                      String channel,
                                      Map<String, Object> receiver,
                                      Map<String, Object> payload,
                                      BigDecimal metricValue,
                                      String status,
                                      String message,
                                      String errorMessage,
                                      String triggeredBy,
                                      int retryCount) {
        BiDeliveryLogDO row = new BiDeliveryLogDO();
        LocalDateTime now = LocalDateTime.now();
        row.setTenantId(normalizeTenant(tenantId));
        row.setWorkspaceId(workspaceId);
        row.setJobType(jobType);
        row.setJobId(jobId);
        row.setJobKey(jobKey);
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setChannel(channel);
        row.setReceiverJson(json(receiver));
        row.setPayloadJson(json(payload));
        row.setMetricValue(metricValue);
        row.setStatus(status);
        row.setMessage(truncate(message));
        row.setErrorMessage(truncate(errorMessage));
        row.setRetryCount(Math.max(0, retryCount));
        row.setMaxRetryCount(maxRetryCount);
        row.setLastRetryAt(retryCount > 0 ? now : null);
        row.setNextRetryAt(nextRetryAt(status, retryCount, now));
        row.setRetryExhaustedAt(retryExhaustedAt(status, retryCount, now));
        row.setTriggeredBy(defaultUser(triggeredBy));
        deliveryLogMapper.insert(row);
        return row;
    }

    private BiDeliveryLogView toView(BiDeliveryLogDO row) {
        return new BiDeliveryLogView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getJobType(),
                row.getJobId(),
                row.getJobKey(),
                row.getResourceType(),
                row.getResourceId(),
                row.getChannel(),
                map(row.getReceiverJson()),
                map(row.getPayloadJson()),
                row.getMetricValue(),
                row.getStatus(),
                row.getMessage(),
                row.getErrorMessage(),
                row.getRetryCount(),
                row.getMaxRetryCount(),
                row.getNextRetryAt(),
                row.getLastRetryAt(),
                row.getRetryExhaustedAt(),
                row.getTriggeredBy(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private List<String> channels(Map<String, Object> receivers) {
        Object configured = receivers.get("channels");
        if (configured instanceof List<?> values && !values.isEmpty()) {
            return values.stream().map(String::valueOf).filter(this::hasText).toList();
        }
        return List.of("IN_APP");
    }

    private List<String> receiverUsers(Map<String, Object> receivers, String fallbackUser) {
        Object configured = receivers.get("users");
        if (configured instanceof List<?> values && !values.isEmpty()) {
            return values.stream()
                    .map(String::valueOf)
                    .filter(this::hasText)
                    .map(value -> "CURRENT_USER".equalsIgnoreCase(value) ? defaultUser(fallbackUser) : value)
                    .distinct()
                    .toList();
        }
        return List.of(defaultUser(fallbackUser));
    }

    private Map<String, Object> payload(String message, String title, Map<String, Object> extra) {
        return extra == null
                ? Map.of("title", title, "message", message, "url", "/bi")
                : Map.of("title", title, "message", message, "url", "/bi", "extra", extra);
    }

    private List<Map<String, Object>> attachmentPayload(List<BiDeliveryAttachmentView> attachments) {
        return safeList(attachments).stream()
                .map(attachment -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", attachment.id());
                    row.put("attachmentType", attachment.attachmentType());
                    row.put("fileName", attachment.fileName());
                    row.put("contentType", attachment.contentType());
                    row.put("fileUrl", attachment.fileUrl());
                    row.put("sizeBytes", attachment.sizeBytes());
                    row.put("retentionDays", attachment.retentionDays());
                    row.put("expiresAt", attachment.expiresAt());
                    row.put("downloadCount", attachment.downloadCount());
                    row.put("lastDownloadedAt", attachment.lastDownloadedAt());
                    row.put("status", attachment.status());
                    return row;
                })
                .toList();
    }

    private List<BiEmailAttachment> emailAttachments(String channel,
                                                     Long tenantId,
                                                     List<BiDeliveryAttachmentView> attachments,
                                                     Map<String, Object> payload) {
        if (!"EMAIL".equals(channel) || attachmentService == null) {
            return List.of();
        }
        List<BiEmailAttachment> emailAttachments = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (BiDeliveryAttachmentView attachment : safeList(attachments)) {
            if ("COMPLETED".equals(attachment.status()) && attachment.id() != null && seen.add(attachment.id())) {
                BiDeliveryAttachmentDownload download = attachmentService.download(tenantId, attachment.id());
                emailAttachments.add(new BiEmailAttachment(download.filename(), download.contentType(), download.bytes()));
            }
        }
        for (Long attachmentId : payloadAttachmentIds(payload)) {
            if (seen.add(attachmentId)) {
                BiDeliveryAttachmentDownload download = attachmentService.download(tenantId, attachmentId);
                emailAttachments.add(new BiEmailAttachment(download.filename(), download.contentType(), download.bytes()));
            }
        }
        return List.copyOf(emailAttachments);
    }

    private List<Long> payloadAttachmentIds(Map<String, Object> payload) {
        Object extra = payload == null ? null : payload.get("extra");
        if (!(extra instanceof Map<?, ?> extraMap)) {
            return List.of();
        }
        Object attachments = extraMap.get("attachments");
        if (!(attachments instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value instanceof Map<?, ?>)
                .map(value -> ((Map<?, ?>) value).get("id"))
                .map(this::toLong)
                .filter(id -> id != null && id > 0)
                .toList();
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resourceUrl(String resourceType, Long resourceId) {
        return "/bi?resourceType=" + resourceType + "&resourceId=" + resourceId;
    }

    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI delivery payload", e);
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private String notificationType(String jobType) {
        return JOB_ALERT.equals(jobType) ? "BI_ALERT" : "BI_SUBSCRIPTION";
    }

    private int retryAttempt(BiDeliveryLogDO log) {
        return Math.max(0, log == null || log.getRetryCount() == null ? 0 : log.getRetryCount()) + 1;
    }

    private LocalDateTime nextRetryAt(String status, int retryCount, LocalDateTime now) {
        if (!retryableStatus(status) || maxRetryCount <= 0 || retryCount >= maxRetryCount) {
            return null;
        }
        return now.plusMinutes(retryDelayMinutes(retryCount + 1));
    }

    private LocalDateTime retryExhaustedAt(String status, int retryCount, LocalDateTime now) {
        if (!retryableStatus(status) || maxRetryCount <= 0) {
            return null;
        }
        return retryCount >= maxRetryCount ? now : null;
    }

    private boolean retryableStatus(String status) {
        return STATUS_PENDING_ADAPTER.equals(status) || STATUS_FAILED.equals(status);
    }

    private boolean retryAttemptAvailable(BiDeliveryLogDO row) {
        int retryCount = Math.max(0, row == null || row.getRetryCount() == null ? 0 : row.getRetryCount());
        int maxAttempts = Math.max(0, row == null || row.getMaxRetryCount() == null ? maxRetryCount : row.getMaxRetryCount());
        return maxAttempts > 0 && retryCount < maxAttempts;
    }

    private long retryDelayMinutes(int nextAttempt) {
        int attempt = Math.max(1, nextAttempt);
        double multiplier = Math.pow(retryBackoffMultiplier, attempt - 1);
        long delay = Math.round(retryInitialDelayMinutes * multiplier);
        return Math.max(1, Math.min(delay, retryMaxDelayMinutes));
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static final class SetLike {
        private SetLike() {
        }

        private static boolean inApp(String channel) {
            return "IN_APP".equals(channel) || "NOTIFICATION".equals(channel) || "MESSAGE_CENTER".equals(channel);
        }
    }

    private record AlertEvaluation(
            boolean matched,
            String status,
            String payloadMessage,
            String logMessage,
            AnomalyEvaluation anomaly
    ) {
    }

    private record AnomalyEvaluation(
            boolean matched,
            boolean insufficientBaseline,
            Map<String, Object> payload
    ) {
    }

    private record AlertSilence(
            boolean silenced,
            Map<String, Object> payload
    ) {
    }
}
