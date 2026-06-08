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
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
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

/**
 * Coordinates BI subscription and alert delivery after a caller has selected work to run.
 *
 * <p>The scheduler decides when a job is due; this service validates access, evaluates alert state, renders optional
 * subscription attachments, dispatches channels, and records durable delivery decisions in {@code bi_delivery_log}.</p>
 */
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
    private final BiPermissionService permissionService;
    private final NotificationService notificationService;
    private final BiDeliveryAdapterService deliveryAdapterService;
    private final BiDeliveryAttachmentService attachmentService;
    private final ObjectMapper objectMapper;
    private final int maxRetryCount;
    private final int retryInitialDelayMinutes;
    private final double retryBackoffMultiplier;
    private final int retryMaxDelayMinutes;

    /**
     * 执行 BiDeliveryRuntimeService 流程，围绕 bi delivery runtime service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAdapterServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param attachmentServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param maxRetryCount max retry count 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     * @param retryInitialDelayMinutes retry initial delay minutes 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     * @param retryBackoffMultiplier retry backoff multiplier 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     * @param retryMaxDelayMinutes retry max delay minutes 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    ObjectProvider<BiPermissionService> permissionServiceProvider,
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
                permissionServiceProvider == null ? null : permissionServiceProvider.getIfAvailable(),
                notificationServiceProvider.getIfAvailable(),
                deliveryAdapterServiceProvider.getIfAvailable(),
                attachmentServiceProvider.getIfAvailable(),
                objectMapper,
                maxRetryCount,
                retryInitialDelayMinutes,
                retryBackoffMultiplier,
                retryMaxDelayMinutes);
    }

    /**
     * 执行 BiDeliveryRuntimeService 流程，围绕 bi delivery runtime service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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
                null,
                notificationService,
                null,
                null,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    /**
     * 执行 BiDeliveryRuntimeService 流程，围绕 bi delivery runtime service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAdapterService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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
                null,
                notificationService,
                deliveryAdapterService,
                null,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    /**
     * 执行 BiDeliveryRuntimeService 流程，围绕 bi delivery runtime service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAdapterService 依赖组件，用于完成数据访问或外部能力调用。
     * @param attachmentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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
                null,
                notificationService,
                deliveryAdapterService,
                attachmentService,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    /**
     * 执行 BiDeliveryRuntimeService 流程，围绕 bi delivery runtime service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAdapterService 依赖组件，用于完成数据访问或外部能力调用。
     * @param attachmentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    BiPermissionService permissionService,
                                    NotificationService notificationService,
                                    BiDeliveryAdapterService deliveryAdapterService,
                                    BiDeliveryAttachmentService attachmentService,
                                    ObjectMapper objectMapper) {
        this(subscriptionMapper,
                alertRuleMapper,
                datasetMapper,
                deliveryLogMapper,
                queryExecutionService,
                permissionService,
                notificationService,
                deliveryAdapterService,
                attachmentService,
                objectMapper,
                4,
                30,
                2.0,
                1440);
    }

    /**
     * 执行 BiDeliveryRuntimeService 流程，围绕 bi delivery runtime service 完成校验、计算或结果组装。
     *
     * @param subscriptionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param alertRuleMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param queryExecutionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionService 依赖组件，用于完成数据访问或外部能力调用。
     * @param notificationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param deliveryAdapterService 依赖组件，用于完成数据访问或外部能力调用。
     * @param attachmentService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param maxRetryCount max retry count 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     * @param retryInitialDelayMinutes retry initial delay minutes 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     * @param retryBackoffMultiplier retry backoff multiplier 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     * @param retryMaxDelayMinutes retry max delay minutes 参数，用于 BiDeliveryRuntimeService 流程中的校验、计算或对象转换。
     */
    public BiDeliveryRuntimeService(BiSubscriptionMapper subscriptionMapper,
                                    BiAlertRuleMapper alertRuleMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDeliveryLogMapper deliveryLogMapper,
                                    BiQueryExecutionService queryExecutionService,
                                    BiPermissionService permissionService,
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
        this.permissionService = permissionService;
        this.notificationService = notificationService;
        this.deliveryAdapterService = deliveryAdapterService;
        this.attachmentService = attachmentService;
        this.objectMapper = objectMapper;
        this.maxRetryCount = Math.max(0, maxRetryCount);
        this.retryInitialDelayMinutes = Math.max(1, retryInitialDelayMinutes);
        this.retryBackoffMultiplier = retryBackoffMultiplier < 1.0 ? 1.0 : retryBackoffMultiplier;
        this.retryMaxDelayMinutes = Math.max(this.retryInitialDelayMinutes, retryMaxDelayMinutes);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param subscriptionId 业务对象 ID，用于定位具体记录。
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public BiDeliveryRunResult runSubscription(Long tenantId, Long subscriptionId, String username) {
        return runSubscription(tenantId, subscriptionId, username, null);
    }

    /**
     * Runs a single subscription delivery and records skipped state when a disabled subscription is invoked.
     */
    public BiDeliveryRunResult runSubscription(Long tenantId, Long subscriptionId, String username, String role) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiSubscriptionDO subscription = subscriptionMapper.selectById(subscriptionId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (subscription == null || !normalizeTenant(subscription.getTenantId()).equals(normalizeTenant(tenantId))) {
            throw new IllegalArgumentException("BI subscription not found: " + subscriptionId);
        }
        enforceDeliveryAccess(tenantId, subscription.getWorkspaceId(), subscription.getResourceType(),
                subscription.getResourceId(), username, role);
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
        // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * Evaluates one alert rule and writes an EVALUATION log before optional outbound channel logs.
     */
    public BiDeliveryRunResult runAlert(Long tenantId, String username, String role, Long alertId) {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiAlertRuleDO alert = alertRuleMapper.selectById(alertId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiDeliveryLogView> listLogs(Long tenantId, String jobType, Long jobId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        LambdaQueryWrapper<BiDeliveryLogDO> query = new LambdaQueryWrapper<BiDeliveryLogDO>()
                .eq(BiDeliveryLogDO::getTenantId, scopedTenantId)
                .orderByDesc(BiDeliveryLogDO::getCreatedAt)
                .orderByDesc(BiDeliveryLogDO::getId)
                .last("LIMIT " + capped);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(jobType)) {
            query.eq(BiDeliveryLogDO::getJobType, jobType.trim().toUpperCase(Locale.ROOT));
        }
        if (jobId != null) {
            query.eq(BiDeliveryLogDO::getJobId, jobId);
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(deliveryLogMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 记录审计、指标或状态变更信息。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param channel channel 参数，用于 auditDeliveries 流程中的校验、计算或对象转换。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 auditDeliveries 流程生成的业务结果。
     */
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
            // 根据前序判断结果进入后续条件分支。
            } else if (STATUS_TRIGGERED.equals(rowStatus)) {
                triggered++;
            // 根据前序判断结果进入后续条件分支。
            } else if (STATUS_SKIPPED.equals(rowStatus)) {
                skipped++;
            // 根据前序判断结果进入后续条件分支。
            } else if (STATUS_PENDING_ADAPTER.equals(rowStatus)) {
                pending++;
            // 根据前序判断结果进入后续条件分支。
            } else if (STATUS_FAILED.equals(rowStatus)) {
                failed++;
            }
            if (row.getRetryExhaustedAt() != null) {
                retryExhausted++;
            // 根据前序判断结果进入后续条件分支。
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public BiDeliveryRetryResult retryPendingDeliveries(Long tenantId, String username, int limit) {
        return retryPendingDeliveries(tenantId, username, null, limit);
    }

    /**
     * Replays pending adapter and failed channel logs using their original receiver and payload snapshots.
     */
    public BiDeliveryRetryResult retryPendingDeliveries(Long tenantId, String username, String role, int limit) {
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
            enforceDeliveryAccess(scopedTenantId, log.getWorkspaceId(), log.getResourceType(),
                    log.getResourceId(), username, role);
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
            // 根据前序判断结果进入后续条件分支。
            } else if (STATUS_FAILED.equals(retryLog.getStatus())) {
                failed++;
            } else {
                pending++;
            }
        }
        return new BiDeliveryRetryResult(retryable.size(), logs.size(), delivered, pending, failed, logs);
    }

    /**
     * 执行 deliverChannel 流程，围绕 deliver channel 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param receivers receivers 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param notificationType 类型标识，用于选择对应处理分支。
     * @return 返回 deliverChannel 流程生成的业务结果。
     */
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

    /**
     * 执行 deliverChannel 流程，围绕 deliver channel 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param receivers receivers 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param notificationType 类型标识，用于选择对应处理分支。
     * @param retryCount retry count 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @return 返回 deliverChannel 流程生成的业务结果。
     */
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

    /**
     * 执行 deliverChannel 流程，围绕 deliver channel 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param receivers receivers 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param attachments attachments 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param notificationType 类型标识，用于选择对应处理分支。
     * @return 返回 deliverChannel 流程生成的业务结果。
     */
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

    /**
     * 执行 deliverChannel 流程，围绕 deliver channel 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param receivers receivers 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param attachments attachments 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param notificationType 类型标识，用于选择对应处理分支。
     * @param retryCount retry count 参数，用于 deliverChannel 流程中的校验、计算或对象转换。
     * @return 返回 deliverChannel 流程生成的业务结果。
     */
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
        // In-app delivery is synchronous; external channels go through adapters and may become retryable.
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
                // Email materializes attachments lazily so other channels do not download binary payloads.
                emailAttachments = emailAttachments(normalizedChannel, tenantId, attachments, payload);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param alert alert 参数，用于 queryMetric 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private BigDecimal queryMetric(Long tenantId, String username, String role, BiAlertRuleDO alert) {
        try {
            BiQueryResult result = queryExecutionService.execute(new BiQueryRequest(
                    datasetKey(alert.getDatasetId()),
                    List.of(),
                    List.of(alert.getMetricKey()),
                    List.of(),
                    List.of(),
                    /**
                     * 执行 BiQueryContext 流程，围绕 bi query context 完成校验、计算或结果组装。
                     *
                     * @param tenantId 租户 ID，用于限定数据隔离范围。
                     * @param username 操作人标识，用于审计和权限判断。
                     * @return 返回 BiQueryContext 流程生成的业务结果。
                     */
                    1), new BiQueryContext(tenantId, username, role));
            if (result.rows().isEmpty()) {
                return BigDecimal.ZERO;
            }
            Object value = result.rows().get(0).get(alert.getMetricKey());
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(String.valueOf(value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            insertLog(tenantId, alert.getWorkspaceId(), JOB_ALERT, alert.getId(), alert.getAlertKey(),
                    "DATASET", alert.getDatasetId(), CHANNEL_EVALUATION, map(alert.getReceiverJson()),
                    payload("BI alert metric query failed", alert.getName(), Map.of("metricKey", alert.getMetricKey())),
                    null, STATUS_FAILED, null, e.getMessage(), username);
            throw e;
        }
    }

    /**
     * 执行 datasetKey 流程，围绕 dataset key 完成校验、计算或结果组装。
     *
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 dataset key 生成的文本或业务键。
     */
    private String datasetKey(Long datasetId) {
        BiDatasetDO dataset = datasetMapper.selectById(datasetId);
        if (dataset == null || dataset.getDatasetKey() == null || dataset.getDatasetKey().isBlank()) {
            throw new IllegalArgumentException("BI alert dataset not found: " + datasetId);
        }
        return dataset.getDatasetKey();
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param alert alert 参数，用于 evaluateAlert 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param condition condition 参数，用于 evaluateAlert 流程中的校验、计算或对象转换。
     * @return 返回 evaluateAlert 流程生成的业务结果。
     */
    private AlertEvaluation evaluateAlert(Long tenantId,
                                          BiAlertRuleDO alert,
                                          BigDecimal value,
                                          Map<String, Object> condition) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AlertEvaluation(
                matched,
                matched ? STATUS_TRIGGERED : STATUS_SKIPPED,
                matched ? "BI alert threshold matched" : "BI alert threshold not matched",
                matched ? "Alert condition matched" : "Alert condition not matched",
                null);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param String string 参数，用于 evaluateSilence 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 evaluateSilence 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 evaluateSilence 流程生成的业务结果。
     */
    private AlertSilence evaluateSilence(Map<String, Object> condition, LocalDateTime now) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> silence = silenceConfig(condition);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AlertSilence(enabled, silencePayload(enabled, silence, now, null));
    }

    /**
     * 执行 silenceConfig 流程，围绕 silence config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 silenceConfig 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 silenceConfig 流程中的校验、计算或对象转换。
     * @return 返回 silenceConfig 流程生成的业务结果。
     */
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

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param String string 参数，用于 copyIfPresent 流程中的校验、计算或对象转换。
     * @param source source 参数，用于 copyIfPresent 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 copyIfPresent 流程中的校验、计算或对象转换。
     * @param target target 参数，用于 copyIfPresent 流程中的校验、计算或对象转换。
     * @param targetKey 业务键，用于在同一租户下定位资源。
     * @param sourceKeys source keys 参数，用于 copyIfPresent 流程中的校验、计算或对象转换。
     */
    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String targetKey, String... sourceKeys) {
        Object value = source.get(targetKey);
        if (value == null) {
            value = firstValue(source, sourceKeys);
        }
        if (value != null && !target.containsKey(targetKey)) {
            target.put(targetKey, value);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 objectMap 流程生成的业务结果。
     */
    private Map<String, Object> objectMap(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        raw.forEach((key, nestedValue) -> {
            if (key != null) {
                result.put(String.valueOf(key), nestedValue);
            }
        });
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 silencePayload 流程，围绕 silence payload 完成校验、计算或结果组装。
     *
     * @param silenced silenced 参数，用于 silencePayload 流程中的校验、计算或对象转换。
     * @param silence silence 参数，用于 silencePayload 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param until until 参数，用于 silencePayload 流程中的校验、计算或对象转换。
     * @return 返回 silencePayload 流程生成的业务结果。
     */
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

    /**
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        String text = String.valueOf(value).trim();
        try {
            return LocalDateTime.parse(text);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(text).atStartOfDay();
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    /**
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalTime parseTime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(String.valueOf(value).trim());
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * 执行 timeWindowMatches 流程，围绕 time window matches 完成校验、计算或结果组装。
     *
     * @param current current 参数，用于 timeWindowMatches 流程中的校验、计算或对象转换。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @return 返回 time window matches 的布尔判断结果。
     */
    private boolean timeWindowMatches(LocalTime current, LocalTime from, LocalTime to) {
        if (from.equals(to)) {
            return true;
        }
        if (from.isBefore(to)) {
            return !current.isBefore(from) && !current.isAfter(to);
        }
        // A reversed window spans midnight, for example 23:00 through 02:00.
        return !current.isBefore(from) || !current.isAfter(to);
    }

    /**
     * 执行 dayMatches 流程，围绕 day matches 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 dayMatches 流程中的校验、计算或对象转换。
     * @param silence silence 参数，用于 dayMatches 流程中的校验、计算或对象转换。
     * @param current current 参数，用于 dayMatches 流程中的校验、计算或对象转换。
     * @return 返回 day matches 的布尔判断结果。
     */
    private boolean dayMatches(Map<String, Object> silence, DayOfWeek current) {
        Object days = firstValue(silence, "daysOfWeek", "days", "weekdays");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(days instanceof List<?> values) || values.isEmpty()) {
            return true;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return values.stream()
                .map(value -> String.valueOf(value).trim().toUpperCase(Locale.ROOT))
                .anyMatch(value -> value.equals(current.name()) || value.equals(String.valueOf(current.getValue())));
    }

    /**
     * 执行 conditionMatches 流程，围绕 condition matches 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param String string 参数，用于 conditionMatches 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 conditionMatches 流程中的校验、计算或对象转换。
     * @return 返回 condition matches 的布尔判断结果。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param alert alert 参数，用于 evaluateAnomaly 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param condition condition 参数，用于 evaluateAnomaly 流程中的校验、计算或对象转换。
     * @return 返回 evaluateAnomaly 流程生成的业务结果。
     */
    private AnomalyEvaluation evaluateAnomaly(Long tenantId,
                                              BiAlertRuleDO alert,
                                              BigDecimal value,
                                              Map<String, Object> condition) {
        // Each anomaly model reads the same evaluation history but interprets the newest samples differently.
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
        String model = anomalyModel(condition);
        int comparisonWindow = intConfig(condition, 1, "comparisonWindow", "recentWindow", "currentWindow");
        comparisonWindow = Math.max(1, Math.min(comparisonWindow, 50));
        int historyLimit = anomalyHistoryLimit(condition, model, baselineWindow, comparisonWindow);

        List<BiDeliveryLogDO> history = safeList(deliveryLogMapper.selectList(new LambdaQueryWrapper<BiDeliveryLogDO>()
                        .eq(BiDeliveryLogDO::getTenantId, tenantId)
                        .eq(BiDeliveryLogDO::getJobType, JOB_ALERT)
                        .eq(BiDeliveryLogDO::getJobId, alert.getId())
                        .eq(BiDeliveryLogDO::getChannel, CHANNEL_EVALUATION)
                        .isNotNull(BiDeliveryLogDO::getMetricValue)
                        .orderByDesc(BiDeliveryLogDO::getCreatedAt)
                        .orderByDesc(BiDeliveryLogDO::getId)
                        .last("LIMIT " + historyLimit)));
        List<Double> samples = history
                .stream()
                .map(BiDeliveryLogDO::getMetricValue)
                .filter(metricValue -> metricValue != null)
                .map(BigDecimal::doubleValue)
                .toList();
        if ("MOVING_AVERAGE".equals(model)) {
            return evaluateMovingAverageAnomaly(
                    value,
                    direction,
                    baselineWindow,
                    minSamples,
                    sensitivity,
                    minDelta,
                    minDeltaPercent,
                    comparisonWindow,
                    samples,
                    condition);
        }
        if ("PERIOD_OVER_PERIOD".equals(model)) {
            return evaluatePeriodOverPeriodAnomaly(
                    value,
                    direction,
                    baselineWindow,
                    minSamples,
                    sensitivity,
                    minDelta,
                    minDeltaPercent,
                    history,
                    condition);
        }
        if (samples.size() < minSamples) {
            return new AnomalyEvaluation(false, true, anomalyPayload(
                    "POINT",
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
                    null,
                    null,
                    null,
                    null,
                    false,
                    true));
        }

        double average = average(samples);
        double standardDeviation = standardDeviation(samples, average);
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
                "POINT",
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
                average,
                standardDeviation,
                delta,
                deltaPercent,
                threshold,
                matched,
                false));
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param condition condition 参数，用于 anomalyHistoryLimit 流程中的校验、计算或对象转换。
     * @param model model 参数，用于 anomalyHistoryLimit 流程中的校验、计算或对象转换。
     * @param baselineWindow baseline window 参数，用于 anomalyHistoryLimit 流程中的校验、计算或对象转换。
     * @param comparisonWindow comparison window 参数，用于 anomalyHistoryLimit 流程中的校验、计算或对象转换。
     * @return 返回 anomaly history limit 计算得到的数量、金额或指标值。
     */
    private int anomalyHistoryLimit(Map<String, Object> condition,
                                    String model,
                                    int baselineWindow,
                                    int comparisonWindow) {
        if ("MOVING_AVERAGE".equals(model)) {
            return Math.min(100, baselineWindow + comparisonWindow - 1);
        }
        if ("PERIOD_OVER_PERIOD".equals(model)) {
            int configured = intConfig(condition, 100, "historyLimit", "calendarHistoryLimit", "periodHistoryLimit");
            return Math.max(baselineWindow, Math.min(configured, 200));
        }
        return baselineWindow;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param direction direction 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param baselineWindow baseline window 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param minSamples min samples 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param sensitivity sensitivity 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param minDelta min delta 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param minDeltaPercent min delta percent 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param comparisonWindow comparison window 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param samples samples 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 evaluateMovingAverageAnomaly 流程中的校验、计算或对象转换。
     * @return 返回 evaluateMovingAverageAnomaly 流程生成的业务结果。
     */
    private AnomalyEvaluation evaluateMovingAverageAnomaly(BigDecimal value,
                                                           String direction,
                                                           int baselineWindow,
                                                           int minSamples,
                                                           double sensitivity,
                                                           double minDelta,
                                                           double minDeltaPercent,
                                                           int comparisonWindow,
                                                           List<Double> samples,
                                                           Map<String, Object> condition) {
        // 准备本次处理所需的上下文和中间变量。
        int minComparisonSamples = intConfig(condition, comparisonWindow, "minComparisonSamples", "minimumComparisonSamples");
        minComparisonSamples = Math.max(1, Math.min(minComparisonSamples, comparisonWindow));
        List<Double> comparisonSamples = new ArrayList<>();
        comparisonSamples.add(value == null ? 0.0 : value.doubleValue());
        int recentHistoryCount = Math.min(Math.max(0, comparisonWindow - 1), samples.size());
        comparisonSamples.addAll(samples.subList(0, recentHistoryCount));
        int baselineStart = recentHistoryCount;
        int baselineEnd = Math.min(samples.size(), baselineStart + baselineWindow);
        List<Double> baselineSamples = baselineStart >= baselineEnd
                ? List.of()
                : samples.subList(baselineStart, baselineEnd);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (baselineSamples.size() < minSamples || comparisonSamples.size() < minComparisonSamples) {
            return new AnomalyEvaluation(false, true, anomalyPayload(
                    "MOVING_AVERAGE",
                    value,
                    direction,
                    baselineSamples.size(),
                    baselineWindow,
                    minSamples,
                    sensitivity,
                    comparisonWindow,
                    comparisonSamples.size(),
                    minComparisonSamples,
                    comparisonSamples.isEmpty() ? null : average(comparisonSamples),
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true));
        }
        double baselineAverage = average(baselineSamples);
        double standardDeviation = standardDeviation(baselineSamples, baselineAverage);
        double comparisonAverage = average(comparisonSamples);
        double delta = comparisonAverage - baselineAverage;
        double absoluteDelta = Math.abs(delta);
        double deltaPercent = baselineAverage == 0.0 ? absoluteDelta : absoluteDelta / Math.abs(baselineAverage);
        double threshold = Math.max(minDelta, standardDeviation * sensitivity);
        boolean directionMatches = switch (direction) {
            case "DROP" -> delta < 0;
            case "RISE" -> delta > 0;
            default -> delta != 0;
        };
        boolean matched = directionMatches
                && absoluteDelta >= threshold
                && deltaPercent >= minDeltaPercent;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AnomalyEvaluation(matched, false, anomalyPayload(
                "MOVING_AVERAGE",
                value,
                direction,
                baselineSamples.size(),
                baselineWindow,
                minSamples,
                sensitivity,
                comparisonWindow,
                comparisonSamples.size(),
                minComparisonSamples,
                comparisonAverage,
                baselineAverage,
                standardDeviation,
                delta,
                deltaPercent,
                threshold,
                matched,
                false));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param direction direction 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param baselineWindow baseline window 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param minSamples min samples 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param sensitivity sensitivity 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param minDelta min delta 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param minDeltaPercent min delta percent 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param history history 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 evaluatePeriodOverPeriodAnomaly 流程中的校验、计算或对象转换。
     * @return 返回 evaluatePeriodOverPeriodAnomaly 流程生成的业务结果。
     */
    private AnomalyEvaluation evaluatePeriodOverPeriodAnomaly(BigDecimal value,
                                                              String direction,
                                                              int baselineWindow,
                                                              int minSamples,
                                                              double sensitivity,
                                                              double minDelta,
                                                              double minDeltaPercent,
                                                              List<BiDeliveryLogDO> history,
                                                              Map<String, Object> condition) {
        // 准备本次处理所需的上下文和中间变量。
        String period = anomalyPeriod(condition);
        int calendarWindowHours = intConfig(condition, 24,
                "calendarWindowHours", "periodWindowHours", "periodToleranceHours", "toleranceHours");
        calendarWindowHours = Math.max(1, Math.min(calendarWindowHours, 24 * 370));
        PeriodTargetWindow targetWindow = periodTargetWindow(LocalDateTime.now(), period, calendarWindowHours, condition);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<Double> baselineSamples = history.stream()
                .filter(row -> row.getCreatedAt() != null)
                .filter(row -> !row.getCreatedAt().isBefore(targetWindow.start())
                        && !row.getCreatedAt().isAfter(targetWindow.end()))
                .map(BiDeliveryLogDO::getMetricValue)
                .filter(metricValue -> metricValue != null)
                .limit(baselineWindow)
                .map(BigDecimal::doubleValue)
                .toList();
        if (baselineSamples.size() < minSamples) {
            Map<String, Object> payload = anomalyPayload(
                    "PERIOD_OVER_PERIOD",
                    value,
                    direction,
                    baselineSamples.size(),
                    baselineWindow,
                    minSamples,
                    sensitivity,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    true);
            addPeriodPayload(payload, period, calendarWindowHours, targetWindow);
            return new AnomalyEvaluation(false, true, payload);
        }
        double baselineAverage = average(baselineSamples);
        double standardDeviation = standardDeviation(baselineSamples, baselineAverage);
        double current = value == null ? 0.0 : value.doubleValue();
        double delta = current - baselineAverage;
        double absoluteDelta = Math.abs(delta);
        double deltaPercent = baselineAverage == 0.0 ? absoluteDelta : absoluteDelta / Math.abs(baselineAverage);
        double threshold = Math.max(minDelta, standardDeviation * sensitivity);
        boolean directionMatches = switch (direction) {
            case "DROP" -> delta < 0;
            case "RISE" -> delta > 0;
            default -> delta != 0;
        };
        boolean matched = directionMatches
                && absoluteDelta >= threshold
                && deltaPercent >= minDeltaPercent;
        Map<String, Object> payload = anomalyPayload(
                "PERIOD_OVER_PERIOD",
                value,
                direction,
                baselineSamples.size(),
                baselineWindow,
                minSamples,
                sensitivity,
                null,
                null,
                null,
                null,
                baselineAverage,
                standardDeviation,
                delta,
                deltaPercent,
                threshold,
                matched,
                false);
        addPeriodPayload(payload, period, calendarWindowHours, targetWindow);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AnomalyEvaluation(matched, false, payload);
    }

    /**
     * 处理集合、映射或字段拷贝逻辑。
     *
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param period period 参数，用于 addPeriodPayload 流程中的校验、计算或对象转换。
     * @param calendarWindowHours calendar window hours 参数，用于 addPeriodPayload 流程中的校验、计算或对象转换。
     * @param targetWindow target window 参数，用于 addPeriodPayload 流程中的校验、计算或对象转换。
     */
    private void addPeriodPayload(Map<String, Object> payload,
                                  String period,
                                  int calendarWindowHours,
                                  PeriodTargetWindow targetWindow) {
        payload.put("period", period);
        payload.put("calendarWindowHours", calendarWindowHours);
        payload.put("targetWindowStart", targetWindow.start().toString());
        payload.put("targetWindowEnd", targetWindow.end().toString());
        payload.put("naturalBoundary", targetWindow.naturalBoundary());
        payload.put("holidayAdjusted", targetWindow.holidayAdjusted());
        if (targetWindow.holidayComparisonDate() != null) {
            payload.put("holidayComparisonDate", targetWindow.holidayComparisonDate());
        }
        if (targetWindow.holidayName() != null) {
            payload.put("holidayName", targetWindow.holidayName());
        }
    }

    /**
     * 执行 anomalyPayload 流程，围绕 anomaly payload 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param direction direction 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param sampleCount sample count 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param baselineWindow baseline window 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param minSamples min samples 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param sensitivity sensitivity 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param baselineAverage baseline average 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param standardDeviation standard deviation 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param delta delta 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param deltaPercent delta percent 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param threshold threshold 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param matched matched 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param insufficientBaseline insufficient baseline 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @return 返回 anomalyPayload 流程生成的业务结果。
     */
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return anomalyPayload(
                "POINT",
                value,
                direction,
                sampleCount,
                baselineWindow,
                minSamples,
                sensitivity,
                null,
                null,
                null,
                null,
                baselineAverage,
                standardDeviation,
                delta,
                deltaPercent,
                threshold,
                matched,
                insufficientBaseline);
    }

    /**
     * 执行 anomalyPayload 流程，围绕 anomaly payload 完成校验、计算或结果组装。
     *
     * @param model model 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param direction direction 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param sampleCount sample count 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param baselineWindow baseline window 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param minSamples min samples 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param sensitivity sensitivity 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param comparisonWindow comparison window 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param comparisonSampleCount comparison sample count 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param minComparisonSamples min comparison samples 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param comparisonAverage comparison average 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param baselineAverage baseline average 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param standardDeviation standard deviation 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param delta delta 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param deltaPercent delta percent 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param threshold threshold 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param matched matched 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @param insufficientBaseline insufficient baseline 参数，用于 anomalyPayload 流程中的校验、计算或对象转换。
     * @return 返回 anomalyPayload 流程生成的业务结果。
     */
    private Map<String, Object> anomalyPayload(String model,
                                               BigDecimal value,
                                               String direction,
                                               int sampleCount,
                                               int baselineWindow,
                                               int minSamples,
                                               double sensitivity,
                                               Integer comparisonWindow,
                                               Integer comparisonSampleCount,
                                               Integer minComparisonSamples,
                                               Double comparisonAverage,
                                               Double baselineAverage,
                                               Double standardDeviation,
                                               Double delta,
                                               Double deltaPercent,
                                               Double threshold,
                                               boolean matched,
                                               boolean insufficientBaseline) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "ANOMALY");
        payload.put("model", model);
        payload.put("direction", direction);
        payload.put("sampleCount", sampleCount);
        payload.put("baselineSampleCount", sampleCount);
        payload.put("baselineWindow", baselineWindow);
        payload.put("minSamples", minSamples);
        payload.put("sensitivity", sensitivity);
        payload.put("comparisonWindow", comparisonWindow);
        payload.put("comparisonSampleCount", comparisonSampleCount);
        payload.put("minComparisonSamples", minComparisonSamples);
        payload.put("comparisonAverage", comparisonAverage);
        payload.put("currentValue", value);
        payload.put("baselineAverage", baselineAverage);
        payload.put("standardDeviation", standardDeviation);
        payload.put("delta", delta);
        payload.put("deltaPercent", deltaPercent);
        payload.put("threshold", threshold);
        payload.put("matched", matched);
        payload.put("insufficientBaseline", insufficientBaseline);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return payload;
    }

    /**
     * 执行 average 流程，围绕 average 完成校验、计算或结果组装。
     *
     * @param values values 参数，用于 average 流程中的校验、计算或对象转换。
     * @return 返回 average 计算得到的数量、金额或指标值。
     */
    private double average(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * 执行 standardDeviation 流程，围绕 standard deviation 完成校验、计算或结果组装。
     *
     * @param values values 参数，用于 standardDeviation 流程中的校验、计算或对象转换。
     * @param average average 参数，用于 standardDeviation 流程中的校验、计算或对象转换。
     * @return 返回 standard deviation 计算得到的数量、金额或指标值。
     */
    private double standardDeviation(List<Double> values, double average) {
        double variance = values.stream()
                .mapToDouble(sample -> Math.pow(sample - average, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param String string 参数，用于 isAnomalyCondition 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 isAnomalyCondition 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
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

    /**
     * 执行 anomalyModel 流程，围绕 anomaly model 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 anomalyModel 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 anomalyModel 流程中的校验、计算或对象转换。
     * @return 返回 anomaly model 生成的文本或业务键。
     */
    private String anomalyModel(Map<String, Object> condition) {
        String model = String.valueOf(firstValue(condition, "model", "windowModel", "anomalyModel"))
                .trim()
                .toUpperCase(Locale.ROOT);
        return switch (model) {
            case "MOVING_AVERAGE", "MOVING_AVG", "ROLLING_AVERAGE", "ROLLING_AVG", "RECENT_AVERAGE" -> "MOVING_AVERAGE";
            case "PERIOD_OVER_PERIOD", "PERIOD", "CALENDAR", "CALENDAR_WINDOW", "DOD", "WOW", "MOM", "YOY",
                    "DAY_OVER_DAY", "WEEK_OVER_WEEK", "MONTH_OVER_MONTH", "YEAR_OVER_YEAR" -> "PERIOD_OVER_PERIOD";
            default -> "POINT";
        };
    }

    /**
     * 执行 anomalyPeriod 流程，围绕 anomaly period 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 anomalyPeriod 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 anomalyPeriod 流程中的校验、计算或对象转换。
     * @return 返回 anomaly period 生成的文本或业务键。
     */
    private String anomalyPeriod(Map<String, Object> condition) {
        String period = String.valueOf(firstValue(condition, "period", "calendarPeriod", "comparePeriod", "periodType"))
                .trim()
                .toUpperCase(Locale.ROOT);
        if (period.isBlank()) {
            period = String.valueOf(firstValue(condition, "model", "windowModel", "anomalyModel"))
                    .trim()
                    .toUpperCase(Locale.ROOT);
        }
        return switch (period) {
            case "DOD", "DAY", "DAILY", "DAY_OVER_DAY" -> "DAY_OVER_DAY";
            case "WOW", "WEEK", "WEEKLY", "WEEK_OVER_WEEK" -> "WEEK_OVER_WEEK";
            case "MOM", "MONTH", "MONTHLY", "MONTH_OVER_MONTH" -> "MONTH_OVER_MONTH";
            case "YOY", "YEAR", "YEARLY", "YEAR_OVER_YEAR" -> "YEAR_OVER_YEAR";
            default -> "DAY_OVER_DAY";
        };
    }

    /**
     * 执行 periodTarget 流程，围绕 period target 完成校验、计算或结果组装。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param period period 参数，用于 periodTarget 流程中的校验、计算或对象转换。
     * @return 返回 periodTarget 流程生成的业务结果。
     */
    private LocalDateTime periodTarget(LocalDateTime now, String period) {
        LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
        return switch (period) {
            case "WEEK_OVER_WEEK" -> evaluatedAt.minusWeeks(1);
            case "MONTH_OVER_MONTH" -> evaluatedAt.minusMonths(1);
            case "YEAR_OVER_YEAR" -> evaluatedAt.minusYears(1);
            default -> evaluatedAt.minusDays(1);
        };
    }

    /**
     * 执行 periodTargetWindow 流程，围绕 period target window 完成校验、计算或结果组装。
     *
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @param period period 参数，用于 periodTargetWindow 流程中的校验、计算或对象转换。
     * @param calendarWindowHours calendar window hours 参数，用于 periodTargetWindow 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 periodTargetWindow 流程中的校验、计算或对象转换。
     * @return 返回 periodTargetWindow 流程生成的业务结果。
     */
    private PeriodTargetWindow periodTargetWindow(LocalDateTime now,
                                                  String period,
                                                  int calendarWindowHours,
                                                  Map<String, Object> condition) {
        LocalDateTime evaluatedAt = now == null ? LocalDateTime.now() : now;
        HolidayComparison holiday = holidayComparison(evaluatedAt.toLocalDate(), condition);
        if (holiday.comparisonDate() != null) {
            // Holiday comparisons override normal calendar offsets because campaign peaks rarely align by weekday.
            int holidayWindowDays = intConfig(condition, 1,
                    "holidayWindowDays", "holidayDurationDays", "holidayCalendarWindowDays");
            holidayWindowDays = Math.max(1, Math.min(holidayWindowDays, 31));
            LocalDateTime start = holiday.comparisonDate().atStartOfDay();
            LocalDateTime end = start.plusDays(holidayWindowDays).minusNanos(1);
            return new PeriodTargetWindow(
                    start,
                    end,
                    true,
                    true,
                    holiday.comparisonDate().toString(),
                    blankToNull(holiday.name()));
        }
        if (booleanConfig(condition,
                "naturalBoundary", "alignNaturalBoundary", "alignToNaturalBoundary",
                "calendarBoundary", "naturalPeriodBoundary")) {
            return naturalPeriodTargetWindow(evaluatedAt, period);
        }
        LocalDateTime target = periodTarget(evaluatedAt, period);
        return new PeriodTargetWindow(
                target.minusHours(calendarWindowHours),
                target.plusHours(calendarWindowHours),
                false,
                false,
                null,
                null);
    }

    /**
     * 执行 naturalPeriodTargetWindow 流程，围绕 natural period target window 完成校验、计算或结果组装。
     *
     * @param evaluatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param period period 参数，用于 naturalPeriodTargetWindow 流程中的校验、计算或对象转换。
     * @return 返回 naturalPeriodTargetWindow 流程生成的业务结果。
     */
    private PeriodTargetWindow naturalPeriodTargetWindow(LocalDateTime evaluatedAt, String period) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDate currentDate = (evaluatedAt == null ? LocalDateTime.now() : evaluatedAt).toLocalDate();
        LocalDateTime start = switch (period) {
            case "WEEK_OVER_WEEK" -> currentDate
                    .minusDays(currentDate.getDayOfWeek().getValue() - 1L)
                    .minusWeeks(1)
                    .atStartOfDay();
            case "MONTH_OVER_MONTH" -> currentDate
                    .withDayOfMonth(1)
                    .minusMonths(1)
                    .atStartOfDay();
            case "YEAR_OVER_YEAR" -> LocalDate
                    .of(currentDate.getYear() - 1, 1, 1)
                    .atStartOfDay();
            default -> currentDate.minusDays(1).atStartOfDay();
        };
        LocalDateTime end = switch (period) {
            case "WEEK_OVER_WEEK" -> start.plusWeeks(1).minusNanos(1);
            case "MONTH_OVER_MONTH" -> start.plusMonths(1).minusNanos(1);
            case "YEAR_OVER_YEAR" -> start.plusYears(1).minusNanos(1);
            default -> start.plusDays(1).minusNanos(1);
        };
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PeriodTargetWindow(start, end, true, false, null, null);
    }

    /**
     * 执行 holidayComparison 流程，围绕 holiday comparison 完成校验、计算或结果组装。
     *
     * @param currentDate 时间参数，用于计算窗口、过期或审计时间。
     * @param String string 参数，用于 holidayComparison 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 holidayComparison 流程中的校验、计算或对象转换。
     * @return 返回 holidayComparison 流程生成的业务结果。
     */
    private HolidayComparison holidayComparison(LocalDate currentDate, Map<String, Object> condition) {
        // 准备本次处理所需的上下文和中间变量。
        Object direct = firstValue(condition,
                "holidayComparisonDate", "holidayBaselineDate", "holidayTargetDate", "holidayDate");
        LocalDate directDate = parseDate(direct);
        String directName = stringValue(firstValue(condition, "holidayName", "holiday"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (directDate != null) {
            return new HolidayComparison(directDate, directName);
        }
        Object configured = firstValue(condition, "holidayComparisons", "holidayCalendar", "holidayMap");
        if (!(configured instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return new HolidayComparison(null, null);
        }
        String currentKey = currentDate == null ? LocalDate.now().toString() : currentDate.toString();
        Object entry = rawMap.get(currentKey);
        if (entry == null) {
            entry = rawMap.get("default");
        }
        if (entry instanceof Map<?, ?> entryMap) {
            LocalDate date = parseDate(firstMapValue(entryMap,
                    "comparisonDate", "baselineDate", "targetDate", "date"));
            String name = stringValue(firstMapValue(entryMap, "name", "holidayName", "holiday"));
            return new HolidayComparison(date, name.isBlank() ? directName : name);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new HolidayComparison(parseDate(entry), directName);
    }

    /**
     * 执行 anomalyDirection 流程，围绕 anomaly direction 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 anomalyDirection 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 anomalyDirection 流程中的校验、计算或对象转换。
     * @return 返回 anomaly direction 生成的文本或业务键。
     */
    private String anomalyDirection(Map<String, Object> condition) {
        // 准备本次处理所需的上下文和中间变量。
        String direction = String.valueOf(condition.getOrDefault("direction", "")).trim().toUpperCase(Locale.ROOT);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "BOTH";
    }

    /**
     * 规范化输入值。
     *
     * @param direction direction 参数，用于 normalizeAnomalyDirection 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeAnomalyDirection(String direction) {
        return switch (direction) {
            case "DROP", "DECREASE", "DOWN" -> "DROP";
            case "RISE", "INCREASE", "SPIKE", "UP" -> "RISE";
            default -> "BOTH";
        };
    }

    /**
     * 执行 intConfig 流程，围绕 int config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 intConfig 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 intConfig 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param keys keys 参数，用于 intConfig 流程中的校验、计算或对象转换。
     * @return 返回 int config 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 执行 doubleConfig 流程，围绕 double config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 doubleConfig 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 doubleConfig 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param keys keys 参数，用于 doubleConfig 流程中的校验、计算或对象转换。
     * @return 返回 double config 计算得到的数量、金额或指标值。
     */
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
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 执行 firstValue 流程，围绕 first value 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstValue 流程中的校验、计算或对象转换。
     * @return 返回 firstValue 流程生成的业务结果。
     */
    private Object firstValue(Map<String, Object> condition, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (condition == null || keys == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            if (condition.containsKey(key)) {
                return condition.get(key);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 执行 firstMapValue 流程，围绕 first map value 完成校验、计算或结果组装。
     *
     * @param map map 参数，用于 firstMapValue 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 firstMapValue 流程中的校验、计算或对象转换。
     * @return 返回 firstMapValue 流程生成的业务结果。
     */
    private Object firstMapValue(Map<?, ?> map, String... keys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (map == null || keys == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 执行 booleanConfig 流程，围绕 boolean config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 booleanConfig 流程中的校验、计算或对象转换。
     * @param condition condition 参数，用于 booleanConfig 流程中的校验、计算或对象转换。
     * @param keys keys 参数，用于 booleanConfig 流程中的校验、计算或对象转换。
     * @return 返回 boolean config 的布尔判断结果。
     */
    private boolean booleanConfig(Map<String, Object> condition, String... keys) {
        Object value = firstValue(condition, keys);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = stringValue(value);
        return "true".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text)
                || "y".equalsIgnoreCase(text)
                || "1".equals(text)
                || "natural".equalsIgnoreCase(text)
                || "calendar".equalsIgnoreCase(text);
    }

    /**
     * 解析并校验输入数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDate parseDate(Object value) {
        String text = stringValue(value);
        if (text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text).toLocalDate();
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param receiver receiver 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param errorMessage error message 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param triggeredBy triggered by 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @return 返回 insertLog 流程生成的业务结果。
     */
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

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param jobType 类型标识，用于选择对应处理分支。
     * @param jobId 业务对象 ID，用于定位具体记录。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param channel channel 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param receiver receiver 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param metricValue 待处理值，用于规则计算或转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param errorMessage error message 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param triggeredBy triggered by 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @param retryCount retry count 参数，用于 insertLog 流程中的校验、计算或对象转换。
     * @return 返回 insertLog 流程生成的业务结果。
     */
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
        // Store retry scheduling on the log row so retries can be resumed by any node after a restart.
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDeliveryLogView toView(BiDeliveryLogDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 执行 channels 流程，围绕 channels 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 channels 流程中的校验、计算或对象转换。
     * @param receivers receivers 参数，用于 channels 流程中的校验、计算或对象转换。
     * @return 返回 channels 汇总后的集合、分页或映射视图。
     */
    private List<String> channels(Map<String, Object> receivers) {
        Object configured = receivers.get("channels");
        if (configured instanceof List<?> values && !values.isEmpty()) {
            return values.stream().map(String::valueOf).filter(this::hasText).toList();
        }
        return List.of("IN_APP");
    }

    /**
     * 执行 receiverUsers 流程，围绕 receiver users 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 receiverUsers 流程中的校验、计算或对象转换。
     * @param receivers receivers 参数，用于 receiverUsers 流程中的校验、计算或对象转换。
     * @param fallbackUser fallback user 参数，用于 receiverUsers 流程中的校验、计算或对象转换。
     * @return 返回 receiver users 汇总后的集合、分页或映射视图。
     */
    private List<String> receiverUsers(Map<String, Object> receivers, String fallbackUser) {
        Object configured = receivers.get("users");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (configured instanceof List<?> values && !values.isEmpty()) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            return values.stream()
                    .map(String::valueOf)
                    .filter(this::hasText)
                    .map(value -> "CURRENT_USER".equalsIgnoreCase(value) ? defaultUser(fallbackUser) : value)
                    .distinct()
                    .toList();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.of(defaultUser(fallbackUser));
    }

    /**
     * 执行 payload 流程，围绕 payload 完成校验、计算或结果组装。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param title title 参数，用于 payload 流程中的校验、计算或对象转换。
     * @param String string 参数，用于 payload 流程中的校验、计算或对象转换。
     * @param extra extra 参数，用于 payload 流程中的校验、计算或对象转换。
     * @return 返回 payload 流程生成的业务结果。
     */
    private Map<String, Object> payload(String message, String title, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("message", message);
        payload.put("url", payloadUrl(extra));
        if (extra != null) {
            payload.put("extra", extra);
        }
        return payload;
    }

    /**
     * 执行 payloadUrl 流程，围绕 payload url 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 payloadUrl 流程中的校验、计算或对象转换。
     * @param extra extra 参数，用于 payloadUrl 流程中的校验、计算或对象转换。
     * @return 返回 payload url 生成的文本或业务键。
     */
    private String payloadUrl(Map<String, Object> extra) {
        if (extra == null) {
            return "/bi";
        }
        Object url = extra.get("url");
        return url == null || String.valueOf(url).isBlank() ? "/bi" : String.valueOf(url);
    }

    /**
     * 执行 attachmentPayload 流程，围绕 attachment payload 完成校验、计算或结果组装。
     *
     * @param attachments attachments 参数，用于 attachmentPayload 流程中的校验、计算或对象转换。
     * @return 返回 attachmentPayload 流程生成的业务结果。
     */
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

    /**
     * 执行 emailAttachments 流程，围绕 email attachments 完成校验、计算或结果组装。
     *
     * @param channel channel 参数，用于 emailAttachments 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param attachments attachments 参数，用于 emailAttachments 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 email attachments 汇总后的集合、分页或映射视图。
     */
    private List<BiEmailAttachment> emailAttachments(String channel,
                                                     Long tenantId,
                                                     List<BiDeliveryAttachmentView> attachments,
                                                     Map<String, Object> payload) {
        if (!"EMAIL".equals(channel) || attachmentService == null) {
            return List.of();
        }
        List<BiEmailAttachment> emailAttachments = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        // Merge generated subscription attachments and explicit payload attachments without duplicate downloads.
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

    /**
     * 执行 payloadAttachmentIds 流程，围绕 payload attachment ids 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 payloadAttachmentIds 流程中的校验、计算或对象转换。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 payload attachment ids 汇总后的集合、分页或映射视图。
     */
    private List<Long> payloadAttachmentIds(Map<String, Object> payload) {
        Object extra = payload == null ? null : payload.get("extra");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(extra instanceof Map<?, ?> extraMap)) {
            return List.of();
        }
        Object attachments = extraMap.get("attachments");
        if (!(attachments instanceof List<?> values)) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return values.stream()
                .filter(value -> value instanceof Map<?, ?>)
                .map(value -> ((Map<?, ?>) value).get("id"))
                .map(this::toLong)
                .filter(id -> id != null && id > 0)
                .toList();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回组装或转换后的结果对象。
     */
    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 执行 resourceUrl 流程，围绕 resource url 完成校验、计算或结果组装。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @return 返回 resource url 生成的文本或业务键。
     */
    private String resourceUrl(String resourceType, Long resourceId) {
        return BiDeliveryResourceUrls.workbenchUrl(resourceType, resourceId);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI delivery payload", e);
        }
    }

    /**
     * 执行 truncate 流程，围绕 truncate 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 执行 notificationType 流程，围绕 notification type 完成校验、计算或结果组装。
     *
     * @param jobType 类型标识，用于选择对应处理分支。
     * @return 返回 notification type 生成的文本或业务键。
     */
    private String notificationType(String jobType) {
        return JOB_ALERT.equals(jobType) ? "BI_ALERT" : "BI_SUBSCRIPTION";
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param log log 参数，用于 retryAttempt 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private int retryAttempt(BiDeliveryLogDO log) {
        return Math.max(0, log == null || log.getRetryCount() == null ? 0 : log.getRetryCount()) + 1;
    }

    /**
     * 执行 nextRetryAt 流程，围绕 next retry at 完成校验、计算或结果组装。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param retryCount retry count 参数，用于 nextRetryAt 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 nextRetryAt 流程生成的业务结果。
     */
    private LocalDateTime nextRetryAt(String status, int retryCount, LocalDateTime now) {
        if (!retryableStatus(status) || maxRetryCount <= 0 || retryCount >= maxRetryCount) {
            return null;
        }
        return now.plusMinutes(retryDelayMinutes(retryCount + 1));
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param retryCount retry count 参数，用于 retryExhaustedAt 流程中的校验、计算或对象转换。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回流程执行后的业务结果。
     */
    private LocalDateTime retryExhaustedAt(String status, int retryCount, LocalDateTime now) {
        if (!retryableStatus(status) || maxRetryCount <= 0) {
            return null;
        }
        return retryCount >= maxRetryCount ? now : null;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回流程执行后的业务结果。
     */
    private boolean retryableStatus(String status) {
        return STATUS_PENDING_ADAPTER.equals(status) || STATUS_FAILED.equals(status);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回流程执行后的业务结果。
     */
    private boolean retryAttemptAvailable(BiDeliveryLogDO row) {
        int retryCount = Math.max(0, row == null || row.getRetryCount() == null ? 0 : row.getRetryCount());
        int maxAttempts = Math.max(0, row == null || row.getMaxRetryCount() == null ? maxRetryCount : row.getMaxRetryCount());
        return maxAttempts > 0 && retryCount < maxAttempts;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param nextAttempt next attempt 参数，用于 retryDelayMinutes 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private long retryDelayMinutes(int nextAttempt) {
        int attempt = Math.max(1, nextAttempt);
        double multiplier = Math.pow(retryBackoffMultiplier, attempt - 1);
        long delay = Math.round(retryInitialDelayMinutes * multiplier);
        return Math.max(1, Math.min(delay, retryMaxDelayMinutes));
    }

    /**
     * 执行 enforceDeliveryAccess 流程，围绕 enforce delivery access 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     */
    private void enforceDeliveryAccess(Long tenantId,
                                       Long workspaceId,
                                       String resourceType,
                                       Long resourceId,
                                       String username,
                                       String role) {
        if (permissionService == null) {
            return;
        }
        permissionService.enforceResourceAccess(
                normalizeTenant(tenantId),
                workspaceId,
                resourceType,
                resourceId,
                new BiQueryContext(normalizeTenant(tenantId), defaultUser(username), role),
                BiPermissionService.ACTION_SUBSCRIBE);
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    /**
     * SetLike 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class SetLike {
        /**
         * 执行 SetLike 流程，围绕 set like 完成校验、计算或结果组装。
         *
         * @return 返回 SetLike 流程生成的业务结果。
         */
        private SetLike() {
        }

        /**
         * 执行 inApp 流程，围绕 in app 完成校验、计算或结果组装。
         *
         * @param channel channel 参数，用于 inApp 流程中的校验、计算或对象转换。
         * @return 返回 in app 的布尔判断结果。
         */
        private static boolean inApp(String channel) {
            return "IN_APP".equals(channel) || "NOTIFICATION".equals(channel) || "MESSAGE_CENTER".equals(channel);
        }
    }

    /**
     * PeriodTargetWindow 数据记录。
     */
    private record PeriodTargetWindow(
            LocalDateTime start,
            LocalDateTime end,
            boolean naturalBoundary,
            boolean holidayAdjusted,
            String holidayComparisonDate,
            String holidayName
    ) {
    }

    /**
     * HolidayComparison 数据记录。
     */
    private record HolidayComparison(
            LocalDate comparisonDate,
            String name
    ) {
    }

    /**
     * AlertEvaluation 数据记录。
     */
    private record AlertEvaluation(
            boolean matched,
            String status,
            String payloadMessage,
            String logMessage,
            AnomalyEvaluation anomaly
    ) {
    }

    /**
     * AnomalyEvaluation 数据记录。
     */
    private record AnomalyEvaluation(
            boolean matched,
            boolean insufficientBaseline,
            Map<String, Object> payload
    ) {
    }

    /**
     * AlertSilence 数据记录。
     */
    private record AlertSilence(
            boolean silenced,
            Map<String, Object> payload
    ) {
    }
}
