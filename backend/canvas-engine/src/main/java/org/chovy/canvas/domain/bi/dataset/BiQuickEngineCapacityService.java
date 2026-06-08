package org.chovy.canvas.domain.bi.dataset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiDatasetExtractRefreshRunDO;
import org.chovy.canvas.dal.dataobject.BiQuickEngineCapacityPolicyDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiDatasetExtractRefreshRunMapper;
import org.chovy.canvas.dal.mapper.BiQuickEngineCapacityPolicyMapper;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryItem;
import org.chovy.canvas.domain.bi.query.BiQueryHistoryReader;
import org.chovy.canvas.domain.bi.subscription.BiDeliverySchedulerLeaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
/**
 * BiQuickEngineCapacityService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiQuickEngineCapacityService {

    private static final String CATEGORY_DATASET_ACCELERATION = "DATASET_ACCELERATION";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String RETENTION_DROPPED = "DROPPED";
    private static final String ALERT_DISABLED = "DISABLED";
    private static final String ALERT_NORMAL = "NORMAL";
    private static final String ALERT_WARNING = "WARNING";
    private static final String ALERT_CRITICAL = "CRITICAL";
    private static final String AUDIT_ACTION = "BI_QUICK_ENGINE_CAPACITY_POLICY_UPDATE";
    private static final String AUDIT_POOL_ACTION = "BI_QUICK_ENGINE_TENANT_POOL_POLICY_UPDATE";
    private static final String AUDIT_RESOURCE_TYPE = "BI_QUICK_ENGINE_CAPACITY_POLICY";
    private static final String AUDIT_POOL_RESOURCE_TYPE = "BI_QUICK_ENGINE_TENANT_POOL_POLICY";
    private static final String DEFAULT_POOL_KEY = "STANDARD";
    private static final int DEFAULT_WARNING_THRESHOLD_PERCENT = 80;
    private static final int DEFAULT_CRITICAL_THRESHOLD_PERCENT = 95;
    private static final int DEFAULT_MAX_CONCURRENT_QUERIES = 8;
    private static final int DEFAULT_QUEUE_LIMIT = 50;
    private static final int DEFAULT_QUEUE_TIMEOUT_SECONDS = 120;
    private static final int DEFAULT_POOL_WEIGHT = 100;
    private static final int MAX_NOTIFICATION_CHANNELS = 8;
    private static final int MAX_NOTIFICATION_RECEIVERS = 50;
    private static final int MAX_CONCURRENT_QUERIES = 10_000;
    private static final int MAX_QUEUE_LIMIT = 1_000_000;
    private static final int MAX_QUEUE_TIMEOUT_SECONDS = 86_400;
    private static final int MAX_POOL_WEIGHT = 10_000;
    private static final String SLOT_LEASE_KEY_PREFIX = "BI_QUICK_ENGINE_POOL_";
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final BiQuickEngineCapacityPolicyMapper policyMapper;
    private final BiDatasetExtractRefreshRunMapper runMapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final long defaultCapacityLimitRows;
    private final BiQueryHistoryReader historyReader;
    private final BiDeliverySchedulerLeaseService slotLeaseService;
    private final long slotLeaseTtlSeconds;
    private final Map<Long, AtomicInteger> liveRunningQueries = new ConcurrentHashMap<>();
    private final Map<Long, AtomicInteger> liveQueuedQueries = new ConcurrentHashMap<>();
    private final Map<Long, Object> queueMonitors = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<Long, Deque<String>>> acquiredSlotLeases = ThreadLocal.withInitial(HashMap::new);

    @Autowired
    /**
     * 初始化 BiQuickEngineCapacityService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param historyReaderProvider history reader provider 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     * @param slotLeaseServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param defaultCapacityLimitRows default capacity limit rows 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     * @param slotLeaseTtlSeconds slot lease ttl seconds 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     */
    public BiQuickEngineCapacityService(
            BiQuickEngineCapacityPolicyMapper policyMapper,
            BiDatasetExtractRefreshRunMapper runMapper,
            BiAuditLogMapper auditLogMapper,
            ObjectMapper objectMapper,
            ObjectProvider<BiQueryHistoryReader> historyReaderProvider,
            ObjectProvider<BiDeliverySchedulerLeaseService> slotLeaseServiceProvider,
            @Value("${canvas.bi.quick-engine.capacity.limit-rows:1000000}") long defaultCapacityLimitRows,
            @Value("${canvas.bi.quick-engine.capacity.slot-lease-ttl-seconds:300}") long slotLeaseTtlSeconds) {
        this(policyMapper, runMapper, auditLogMapper, objectMapper, Clock.systemUTC(), defaultCapacityLimitRows,
                historyReaderProvider == null
                        ? BiQueryHistoryReader.empty()
                        : historyReaderProvider.getIfAvailable(BiQueryHistoryReader::empty),
                slotLeaseServiceProvider == null ? null : slotLeaseServiceProvider.getIfAvailable(),
                slotLeaseTtlSeconds);
    }

    /**
     * 初始化 BiQuickEngineCapacityService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param defaultCapacityLimitRows default capacity limit rows 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     */
    public BiQuickEngineCapacityService(BiQuickEngineCapacityPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        Clock clock,
                                        long defaultCapacityLimitRows) {
        this(policyMapper, runMapper, auditLogMapper, objectMapper, clock, defaultCapacityLimitRows,
                BiQueryHistoryReader.empty());
    }

    /**
     * 初始化 BiQuickEngineCapacityService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param defaultCapacityLimitRows default capacity limit rows 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     * @param historyReader history reader 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     */
    public BiQuickEngineCapacityService(BiQuickEngineCapacityPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        Clock clock,
                                        long defaultCapacityLimitRows,
                                        BiQueryHistoryReader historyReader) {
        this(policyMapper, runMapper, auditLogMapper, objectMapper, clock, defaultCapacityLimitRows,
                historyReader, null, 300L);
    }

    /**
     * 初始化 BiQuickEngineCapacityService 实例。
     *
     * @param policyMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param runMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param auditLogMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param defaultCapacityLimitRows default capacity limit rows 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     * @param historyReader history reader 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     * @param slotLeaseService 依赖组件，用于完成数据访问或外部能力调用。
     * @param slotLeaseTtlSeconds slot lease ttl seconds 参数，用于 BiQuickEngineCapacityService 流程中的校验、计算或对象转换。
     */
    public BiQuickEngineCapacityService(BiQuickEngineCapacityPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        Clock clock,
                                        long defaultCapacityLimitRows,
                                        BiQueryHistoryReader historyReader,
                                        BiDeliverySchedulerLeaseService slotLeaseService,
                                        long slotLeaseTtlSeconds) {
        this.policyMapper = policyMapper;
        this.runMapper = runMapper;
        this.auditLogMapper = auditLogMapper;
        this.objectMapper = (objectMapper == null ? new ObjectMapper() : objectMapper.copy()).findAndRegisterModules();
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.defaultCapacityLimitRows = Math.max(1L, defaultCapacityLimitRows);
        this.historyReader = historyReader == null ? BiQueryHistoryReader.empty() : historyReader;
        this.slotLeaseService = slotLeaseService;
        this.slotLeaseTtlSeconds = Math.max(1L, slotLeaseTtlSeconds);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 summary 流程生成的业务结果。
     */
    public BiQuickEngineCapacitySummaryView summary(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineCapacityAlertPolicyView policy = alertPolicy(scopedTenantId);
        BiQuickEngineTenantPoolPolicyView poolPolicy = tenantPoolPolicy(scopedTenantId);
        BiQuickEngineConcurrencyQueueView concurrencyQueue = concurrencyQueue(scopedTenantId, limit, poolPolicy);
        Map<String, ResourceAccumulator> resources = new TreeMap<>();
        Map<String, UserAccumulator> users = new TreeMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetExtractRefreshRunDO row : loadRuns(scopedTenantId, limit)) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!isActiveSuccess(row)) {
                continue;
            }
            String resourceKey = trimToNull(row.getDatasetKey());
            String table = trimToNull(row.getMaterializedTable());
            if (resourceKey == null || table == null) {
                continue;
            }
            long rowCount = Math.max(0L, row.getRowCount() == null ? 0L : row.getRowCount());
            String owner = actor(row.getRequestedBy());
            resources.computeIfAbsent(resourceKey, ResourceAccumulator::new)
                    .add(row, table, rowCount, owner);
            users.computeIfAbsent(owner, UserAccumulator::new)
                    .add(resourceKey, table, rowCount);
        }

        List<BiQuickEngineCapacityUsageDetailView> details = resources.values().stream()
                .map(ResourceAccumulator::view)
                .filter(detail -> detail.usedRows() > 0)
                .sorted(Comparator.comparingLong(BiQuickEngineCapacityUsageDetailView::usedRows)
                        .reversed()
                        .thenComparing(BiQuickEngineCapacityUsageDetailView::resourceKey))
                .toList();
        long usedRows = details.stream().mapToLong(BiQuickEngineCapacityUsageDetailView::usedRows).sum();
        double usagePercent = usagePercent(usedRows, policy.capacityLimitRows());
        List<BiQuickEngineCapacityUserUsageView> userRankings = users.values().stream()
                .map(UserAccumulator::view)
                .filter(user -> user.usedRows() > 0)
                .sorted(Comparator.comparingLong(BiQuickEngineCapacityUserUsageView::usedRows)
                        .reversed()
                        .thenComparing(BiQuickEngineCapacityUserUsageView::user))
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQuickEngineCapacitySummaryView(
                scopedTenantId,
                policy.capacityLimitRows(),
                usedRows,
                usagePercent,
                alertLevel(policy, usagePercent),
                policy.enabled(),
                policy,
                poolPolicy,
                concurrencyQueue,
                List.of(new BiQuickEngineCapacityCategoryUsageView(
                        CATEGORY_DATASET_ACCELERATION,
                        usedRows,
                        details.size())),
                details,
                userRankings);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 alertPolicy 流程生成的业务结果。
     */
    public BiQuickEngineCapacityAlertPolicyView alertPolicy(Long tenantId) {
        return view(findPolicy(normalizeTenant(tenantId)));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenantPoolPolicy 流程生成的业务结果。
     */
    public BiQuickEngineTenantPoolPolicyView tenantPoolPolicy(Long tenantId) {
        return tenantPoolView(findPolicy(normalizeTenant(tenantId)));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 admitQuery 流程生成的业务结果。
     */
    public BiQuickEngineAdmissionDecision admitQuery(Long tenantId, int limit) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineTenantPoolPolicyView poolPolicy = tenantPoolPolicy(scopedTenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (slotLeaseService != null) {
            return admitDistributedSlot(scopedTenantId, limit, poolPolicy);
        }
        AtomicInteger liveCounter = liveRunningQueries.computeIfAbsent(scopedTenantId, ignored -> new AtomicInteger());
        synchronized (liveCounter) {
            BiQuickEngineConcurrencyQueueView queue = concurrencyQueue(scopedTenantId, limit, poolPolicy);
            if (queue.runningQueries() >= poolPolicy.maxConcurrentQueries()) {
                return new BiQuickEngineAdmissionDecision(
                        false,
                        "BLOCKED",
                        "Quick Engine tenant pool " + poolPolicy.poolKey()
                                + " max concurrent queries reached: "
                                + queue.runningQueries() + "/" + poolPolicy.maxConcurrentQueries(),
                        poolPolicy,
                        queue);
            }
            if (queue.queuedQueries() >= poolPolicy.queueLimit()) {
                return new BiQuickEngineAdmissionDecision(
                        false,
                        "BLOCKED",
                        "Quick Engine tenant pool " + poolPolicy.poolKey()
                                + " queue limit reached: "
                                + queue.queuedQueries() + "/" + poolPolicy.queueLimit(),
                        poolPolicy,
                        queue);
            }
            liveCounter.incrementAndGet();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQuickEngineAdmissionDecision(
                true,
                "ADMITTED",
                "Quick Engine tenant pool " + poolPolicy.poolKey() + " admitted",
                poolPolicy,
                concurrencyQueue(scopedTenantId, limit, poolPolicy));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 admitQueryOrWait 流程生成的业务结果。
     */
    public BiQuickEngineAdmissionDecision admitQueryOrWait(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineTenantPoolPolicyView poolPolicy = tenantPoolPolicy(scopedTenantId);
        BiQuickEngineAdmissionDecision immediate = admitQuery(scopedTenantId, limit);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (immediate == null || immediate.allowed()) {
            return immediate;
        }
        BiQuickEngineConcurrencyQueueView queue = concurrencyQueue(scopedTenantId, limit, poolPolicy);
        if (queue.queuedQueries() >= poolPolicy.queueLimit()) {
            return queueLimitDecision(poolPolicy, queue);
        }

        incrementQueued(scopedTenantId);
        boolean queued = true;
        long deadlineNanos = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(Math.max(1, poolPolicy.queueTimeoutSeconds()));
        try {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            while (true) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0L) {
                    return queueTimeoutDecision(scopedTenantId, limit, poolPolicy);
                }
                if (!waitForQueueSignal(scopedTenantId, remainingNanos)) {
                    return queueInterruptedDecision(scopedTenantId, limit, poolPolicy);
                }
                releaseQueued(scopedTenantId);
                queued = false;

                BiQuickEngineAdmissionDecision decision = admitQuery(scopedTenantId, limit);
                if (decision == null || decision.allowed()) {
                    return admittedAfterQueueDecision(scopedTenantId, limit, poolPolicy);
                }
                queue = concurrencyQueue(scopedTenantId, limit, poolPolicy);
                if (queue.queuedQueries() >= poolPolicy.queueLimit()) {
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return queueLimitDecision(poolPolicy, queue);
                }
                incrementQueued(scopedTenantId);
                queued = true;
            }
        } finally {
            if (queued) {
                releaseQueued(scopedTenantId);
            }
        }
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    public void releaseQuery(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String leaseKey = pollAcquiredSlotLease(scopedTenantId);
        if (leaseKey != null) {
            try {
                slotLeaseService.release(scopedTenantId, leaseKey);
            } catch (RuntimeException ignored) {
                // The slot lease will expire by TTL if release storage is temporarily unavailable.
            }
            releaseLive(scopedTenantId);
            return;
        }
        releaseLive(scopedTenantId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param poolPolicy pool policy 参数，用于 admitDistributedSlot 流程中的校验、计算或对象转换。
     * @return 返回 admitDistributedSlot 流程生成的业务结果。
     */
    private BiQuickEngineAdmissionDecision admitDistributedSlot(
            Long tenantId,
            int limit,
            BiQuickEngineTenantPoolPolicyView poolPolicy) {
        BiQuickEngineConcurrencyQueueView queue = concurrencyQueue(tenantId, limit, poolPolicy);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (queue.queuedQueries() >= poolPolicy.queueLimit()) {
            return new BiQuickEngineAdmissionDecision(
                    false,
                    "BLOCKED",
                    "Quick Engine tenant pool " + poolPolicy.poolKey()
                            + " queue limit reached: "
                            + queue.queuedQueries() + "/" + poolPolicy.queueLimit(),
                    poolPolicy,
                    queue);
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int slot = 0; slot < poolPolicy.maxConcurrentQueries(); slot++) {
            String leaseKey = slotLeaseKey(poolPolicy.poolKey(), slot);
            if (slotLeaseService.acquire(tenantId, leaseKey, slotLeaseTtl())) {
                rememberAcquiredSlotLease(tenantId, leaseKey);
                incrementLive(tenantId);
                return new BiQuickEngineAdmissionDecision(
                        true,
                        "ADMITTED",
                        "Quick Engine tenant pool " + poolPolicy.poolKey() + " admitted on slot " + slot,
                        poolPolicy,
                        concurrencyQueue(tenantId, limit, poolPolicy));
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQuickEngineAdmissionDecision(
                false,
                "BLOCKED",
                "Quick Engine tenant pool " + poolPolicy.poolKey()
                        + " distributed slots are busy: "
                        + poolPolicy.maxConcurrentQueries() + "/" + poolPolicy.maxConcurrentQueries(),
                poolPolicy,
                queue);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param poolPolicy pool policy 参数，用于 queueLimitDecision 流程中的校验、计算或对象转换。
     * @param queue queue 参数，用于 queueLimitDecision 流程中的校验、计算或对象转换。
     * @return 返回 queueLimitDecision 流程生成的业务结果。
     */
    private BiQuickEngineAdmissionDecision queueLimitDecision(
            BiQuickEngineTenantPoolPolicyView poolPolicy,
            BiQuickEngineConcurrencyQueueView queue) {
        return new BiQuickEngineAdmissionDecision(
                false,
                "BLOCKED",
                "Quick Engine tenant pool " + poolPolicy.poolKey()
                        + " queue limit reached: "
                        + queue.queuedQueries() + "/" + poolPolicy.queueLimit(),
                poolPolicy,
                queue);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param poolPolicy pool policy 参数，用于 queueTimeoutDecision 流程中的校验、计算或对象转换。
     * @return 返回 queueTimeoutDecision 流程生成的业务结果。
     */
    private BiQuickEngineAdmissionDecision queueTimeoutDecision(
            Long tenantId,
            int limit,
            BiQuickEngineTenantPoolPolicyView poolPolicy) {
        BiQuickEngineConcurrencyQueueView queue = concurrencyQueue(tenantId, limit, poolPolicy);
        return new BiQuickEngineAdmissionDecision(
                false,
                "BLOCKED",
                "Quick Engine tenant pool " + poolPolicy.poolKey()
                        + " queue wait timed out after "
                        + poolPolicy.queueTimeoutSeconds() + " seconds",
                poolPolicy,
                queue);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param poolPolicy pool policy 参数，用于 queueInterruptedDecision 流程中的校验、计算或对象转换。
     * @return 返回 queueInterruptedDecision 流程生成的业务结果。
     */
    private BiQuickEngineAdmissionDecision queueInterruptedDecision(
            Long tenantId,
            int limit,
            BiQuickEngineTenantPoolPolicyView poolPolicy) {
        BiQuickEngineConcurrencyQueueView queue = concurrencyQueue(tenantId, limit, poolPolicy);
        return new BiQuickEngineAdmissionDecision(
                false,
                "BLOCKED",
                "Quick Engine tenant pool " + poolPolicy.poolKey() + " queue wait interrupted",
                poolPolicy,
                queue);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param poolPolicy pool policy 参数，用于 admittedAfterQueueDecision 流程中的校验、计算或对象转换。
     * @return 返回 admittedAfterQueueDecision 流程生成的业务结果。
     */
    private BiQuickEngineAdmissionDecision admittedAfterQueueDecision(
            Long tenantId,
            int limit,
            BiQuickEngineTenantPoolPolicyView poolPolicy) {
        return new BiQuickEngineAdmissionDecision(
                true,
                "ADMITTED_AFTER_QUEUE",
                "Quick Engine tenant pool " + poolPolicy.poolKey() + " admitted after queued wait",
                poolPolicy,
                concurrencyQueue(tenantId, limit, poolPolicy));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private void incrementLive(Long tenantId) {
        liveRunningQueries.computeIfAbsent(normalizeTenant(tenantId), ignored -> new AtomicInteger())
                .incrementAndGet();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private void incrementQueued(Long tenantId) {
        liveQueuedQueries.computeIfAbsent(normalizeTenant(tenantId), ignored -> new AtomicInteger())
                .incrementAndGet();
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private void releaseQueued(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        AtomicInteger queuedCounter = liveQueuedQueries.get(scopedTenantId);
        if (queuedCounter == null) {
            notifyQueueWaiters(scopedTenantId);
            return;
        }
        synchronized (queuedCounter) {
            int current = queuedCounter.get();
            if (current <= 1) {
                liveQueuedQueries.remove(scopedTenantId, queuedCounter);
            } else {
                queuedCounter.decrementAndGet();
            }
        }
        notifyQueueWaiters(scopedTenantId);
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private void releaseLive(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        AtomicInteger liveCounter = liveRunningQueries.get(scopedTenantId);
        if (liveCounter == null) {
            notifyQueueWaiters(scopedTenantId);
            return;
        }
        synchronized (liveCounter) {
            int current = liveCounter.get();
            if (current <= 1) {
                liveRunningQueries.remove(scopedTenantId, liveCounter);
            } else {
                liveCounter.decrementAndGet();
            }
        }
        notifyQueueWaiters(scopedTenantId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param remainingNanos remaining nanos 参数，用于 waitForQueueSignal 流程中的校验、计算或对象转换。
     * @return 返回 wait for queue signal 的布尔判断结果。
     */
    private boolean waitForQueueSignal(Long tenantId, long remainingNanos) {
        Object monitor = queueMonitors.computeIfAbsent(normalizeTenant(tenantId), ignored -> new Object());
        long waitMillis = Math.max(1L, Math.min(100L, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
        synchronized (monitor) {
            try {
                monitor.wait(waitMillis);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private void notifyQueueWaiters(Long tenantId) {
        Object monitor = queueMonitors.get(normalizeTenant(tenantId));
        if (monitor == null) {
            return;
        }
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param leaseKey 业务键，用于在同一租户下定位资源。
     */
    private void rememberAcquiredSlotLease(Long tenantId, String leaseKey) {
        acquiredSlotLeases.get()
                .computeIfAbsent(normalizeTenant(tenantId), ignored -> new ArrayDeque<>())
                .push(leaseKey);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 poll acquired slot lease 生成的文本或业务键。
     */
    private String pollAcquiredSlotLease(Long tenantId) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        Map<Long, Deque<String>> slotsByTenant = acquiredSlotLeases.get();
        Deque<String> slots = slotsByTenant.get(scopedTenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (slots == null || slots.isEmpty()) {
            return null;
        }
        String leaseKey = slots.pop();
        if (slots.isEmpty()) {
            slotsByTenant.remove(scopedTenantId);
        }
        if (slotsByTenant.isEmpty()) {
            acquiredSlotLeases.remove();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return leaseKey;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 slotLeaseTtl 流程生成的业务结果。
     */
    private Duration slotLeaseTtl() {
        return Duration.ofSeconds(slotLeaseTtlSeconds);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param poolKey 业务键，用于在同一租户下定位资源。
     * @param slot slot 参数，用于 slotLeaseKey 流程中的校验、计算或对象转换。
     * @return 返回 slot lease key 生成的文本或业务键。
     */
    private String slotLeaseKey(String poolKey, int slot) {
        return SLOT_LEASE_KEY_PREFIX + poolKey(poolKey, DEFAULT_POOL_KEY) + "_SLOT_" + slot;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public BiQuickEngineCapacityAlertPolicyView upsertAlertPolicy(
            Long tenantId,
            BiQuickEngineCapacityAlertPolicyCommand command,
            String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineCapacityPolicyDO existing = findPolicy(scopedTenantId);
        BiQuickEngineCapacityAlertPolicyView before = view(existing);
        BiQuickEngineCapacityAlertPolicyCommand safeCommand = command == null
                ? new BiQuickEngineCapacityAlertPolicyCommand(null, null, null, null, null, null)
                : command;
        List<String> channels = safeCommand.notificationChannels() == null
                ? before.notificationChannels()
                : normalizedList(safeCommand.notificationChannels(), MAX_NOTIFICATION_CHANNELS,
                "notification channels");
        List<String> receivers = safeCommand.notificationReceivers() == null
                ? before.notificationReceivers()
                : normalizedList(safeCommand.notificationReceivers(), MAX_NOTIFICATION_RECEIVERS,
                "notification receivers");
        int warningThreshold = threshold(
                safeCommand.warningThresholdPercent(),
                before.warningThresholdPercent(),
                "warning threshold");
        int criticalThreshold = threshold(
                safeCommand.criticalThresholdPercent(),
                before.criticalThresholdPercent(),
                "critical threshold");
        if (warningThreshold >= criticalThreshold) {
            throw new IllegalArgumentException("warning threshold must be lower than critical threshold");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        BiQuickEngineCapacityPolicyDO row = existing == null ? new BiQuickEngineCapacityPolicyDO() : existing;
        row.setTenantId(scopedTenantId);
        row.setEnabled(safeCommand.enabled() == null ? before.enabled() : safeCommand.enabled());
        row.setCapacityLimitRows(positive(safeCommand.capacityLimitRows(), before.capacityLimitRows()));
        row.setWarningThresholdPercent(warningThreshold);
        row.setCriticalThresholdPercent(criticalThreshold);
        row.setNotificationChannels(writeList(channels));
        row.setNotificationReceivers(writeList(receivers));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(actor(actor));
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            row.setCreatedAt(now);
            policyMapper.insert(row);
        } else {
            policyMapper.updateById(row);
        }

        BiQuickEngineCapacityAlertPolicyView after = alertPolicy(scopedTenantId);
        auditUpdate(scopedTenantId, actor(actor), before, after);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return after;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回流程执行后的业务结果。
     */
    public BiQuickEngineTenantPoolPolicyView upsertTenantPoolPolicy(
            Long tenantId,
            BiQuickEngineTenantPoolPolicyCommand command,
            String actor) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineCapacityPolicyDO existing = findPolicy(scopedTenantId);
        BiQuickEngineTenantPoolPolicyView before = tenantPoolView(existing);
        BiQuickEngineCapacityAlertPolicyView existingAlert = view(existing);
        BiQuickEngineTenantPoolPolicyCommand safeCommand = command == null
                ? new BiQuickEngineTenantPoolPolicyCommand(null, null, null, null, null)
                : command;

        LocalDateTime now = LocalDateTime.now(clock);
        BiQuickEngineCapacityPolicyDO row = existing == null ? new BiQuickEngineCapacityPolicyDO() : existing;
        row.setTenantId(scopedTenantId);
        row.setPoolKey(poolKey(safeCommand.poolKey(), before.poolKey()));
        row.setMaxConcurrentQueries(boundedPositive(
                safeCommand.maxConcurrentQueries(),
                before.maxConcurrentQueries(),
                MAX_CONCURRENT_QUERIES,
                "max concurrent queries"));
        row.setQueueLimit(boundedPositive(
                safeCommand.queueLimit(),
                before.queueLimit(),
                MAX_QUEUE_LIMIT,
                "queue limit"));
        row.setQueueTimeoutSeconds(boundedPositive(
                safeCommand.queueTimeoutSeconds(),
                before.queueTimeoutSeconds(),
                MAX_QUEUE_TIMEOUT_SECONDS,
                "queue timeout seconds"));
        row.setPoolWeight(boundedPositive(
                safeCommand.poolWeight(),
                before.poolWeight(),
                MAX_POOL_WEIGHT,
                "pool weight"));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedBy(actor(actor));
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            row.setEnabled(existingAlert.enabled());
            row.setCapacityLimitRows(existingAlert.capacityLimitRows());
            row.setWarningThresholdPercent(existingAlert.warningThresholdPercent());
            row.setCriticalThresholdPercent(existingAlert.criticalThresholdPercent());
            row.setNotificationChannels(writeList(existingAlert.notificationChannels()));
            row.setNotificationReceivers(writeList(existingAlert.notificationReceivers()));
            row.setCreatedAt(now);
            policyMapper.insert(row);
        } else {
            policyMapper.updateById(row);
        }

        BiQuickEngineTenantPoolPolicyView after = tenantPoolPolicy(scopedTenantId);
        auditPoolUpdate(scopedTenantId, actor(actor), before, after);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return after;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiQuickEngineCapacityPolicyDO findPolicy(Long tenantId) {
        List<BiQuickEngineCapacityPolicyDO> rows = policyMapper.selectList(
                new LambdaQueryWrapper<BiQuickEngineCapacityPolicyDO>()
                        .eq(BiQuickEngineCapacityPolicyDO::getTenantId, tenantId)
                        .orderByDesc(BiQuickEngineCapacityPolicyDO::getUpdatedAt)
                        .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 load runs 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasetExtractRefreshRunDO> loadRuns(Long tenantId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 500));
        List<BiDatasetExtractRefreshRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<BiDatasetExtractRefreshRunDO>()
                        .eq(BiDatasetExtractRefreshRunDO::getTenantId, tenantId)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getFinishedAt)
                        .last("LIMIT " + boundedLimit));
        return rows == null ? List.of() : rows;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 view 流程生成的业务结果。
     */
    private BiQuickEngineCapacityAlertPolicyView view(BiQuickEngineCapacityPolicyDO row) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            return new BiQuickEngineCapacityAlertPolicyView(
                    false,
                    defaultCapacityLimitRows,
                    DEFAULT_WARNING_THRESHOLD_PERCENT,
                    DEFAULT_CRITICAL_THRESHOLD_PERCENT,
                    List.of(),
                    List.of(),
                    "system",
                    null);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQuickEngineCapacityAlertPolicyView(
                Boolean.TRUE.equals(row.getEnabled()),
                Math.max(1L, value(row.getCapacityLimitRows(), defaultCapacityLimitRows)),
                threshold(row.getWarningThresholdPercent(), DEFAULT_WARNING_THRESHOLD_PERCENT, "warning threshold"),
                threshold(row.getCriticalThresholdPercent(), DEFAULT_CRITICAL_THRESHOLD_PERCENT, "critical threshold"),
                readList(row.getNotificationChannels()),
                readList(row.getNotificationReceivers()),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                actor(row.getUpdatedBy()),
                row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 tenantPoolView 流程生成的业务结果。
     */
    private BiQuickEngineTenantPoolPolicyView tenantPoolView(BiQuickEngineCapacityPolicyDO row) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            return new BiQuickEngineTenantPoolPolicyView(
                    DEFAULT_POOL_KEY,
                    DEFAULT_MAX_CONCURRENT_QUERIES,
                    DEFAULT_QUEUE_LIMIT,
                    DEFAULT_QUEUE_TIMEOUT_SECONDS,
                    DEFAULT_POOL_WEIGHT,
                    "system",
                    null);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiQuickEngineTenantPoolPolicyView(
                poolKey(row.getPoolKey(), DEFAULT_POOL_KEY),
                boundedPositive(row.getMaxConcurrentQueries(), DEFAULT_MAX_CONCURRENT_QUERIES,
                        MAX_CONCURRENT_QUERIES, "max concurrent queries"),
                boundedPositive(row.getQueueLimit(), DEFAULT_QUEUE_LIMIT, MAX_QUEUE_LIMIT, "queue limit"),
                boundedPositive(row.getQueueTimeoutSeconds(), DEFAULT_QUEUE_TIMEOUT_SECONDS,
                        MAX_QUEUE_TIMEOUT_SECONDS, "queue timeout seconds"),
                boundedPositive(row.getPoolWeight(), DEFAULT_POOL_WEIGHT, MAX_POOL_WEIGHT, "pool weight"),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                actor(row.getUpdatedBy()),
                row.getUpdatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param poolPolicy pool policy 参数，用于 concurrencyQueue 流程中的校验、计算或对象转换。
     * @return 返回 concurrencyQueue 流程生成的业务结果。
     */
    private BiQuickEngineConcurrencyQueueView concurrencyQueue(Long tenantId,
                                                               int limit,
                                                               BiQuickEngineTenantPoolPolicyView poolPolicy) {
        List<BiQueryHistoryItem> items = loadHistory(tenantId, limit);
        int running = liveRunning(tenantId) + countStatus(items, "RUNNING");
        int queued = liveQueued(tenantId) + countStatus(items, "QUEUED");
        int blocked = countStatus(items, "BLOCKED");
        int failed = countStatus(items, "FAILED");
        int successful = countStatus(items, "SUCCESS") + countStatus(items, "CACHE_HIT");
        double concurrencyUsage = usagePercent(running, poolPolicy.maxConcurrentQueries());
        double queueUsage = usagePercent(queued, poolPolicy.queueLimit());
        return new BiQuickEngineConcurrencyQueueView(
                running,
                queued,
                blocked,
                successful,
                failed,
                concurrencyUsage,
                queueUsage,
                pressureState(concurrencyUsage, queueUsage));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 live running 计算得到的数量、金额或指标值。
     */
    private int liveRunning(Long tenantId) {
        AtomicInteger counter = liveRunningQueries.get(normalizeTenant(tenantId));
        return counter == null ? 0 : Math.max(0, counter.get());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 live queued 计算得到的数量、金额或指标值。
     */
    private int liveQueued(Long tenantId) {
        AtomicInteger counter = liveQueuedQueries.get(normalizeTenant(tenantId));
        return counter == null ? 0 : Math.max(0, counter.get());
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 load history 汇总后的集合、分页或映射视图。
     */
    private List<BiQueryHistoryItem> loadHistory(Long tenantId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 500));
        try {
            List<BiQueryHistoryItem> rows = historyReader.recent(tenantId, boundedLimit);
            return rows == null ? List.of() : rows;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param items items 参数，用于 countStatus 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回统计数量。
     */
    private int countStatus(List<BiQueryHistoryItem> items, String status) {
        return (int) items.stream()
                .filter(item -> status.equals(normalize(item == null ? null : item.status())))
                .count();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回布尔判断结果。
     */
    private boolean isActiveSuccess(BiDatasetExtractRefreshRunDO row) {
        if (row == null || !STATUS_SUCCESS.equals(normalize(row.getStatus()))) {
            return false;
        }
        return !RETENTION_DROPPED.equals(normalize(row.getRetentionStatus()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param usedRows used rows 参数，用于 usagePercent 流程中的校验、计算或对象转换。
     * @param capacityLimitRows capacity limit rows 参数，用于 usagePercent 流程中的校验、计算或对象转换。
     * @return 返回 usage percent 计算得到的数量、金额或指标值。
     */
    private double usagePercent(long usedRows, long capacityLimitRows) {
        if (capacityLimitRows <= 0) {
            return 0D;
        }
        return Math.round((usedRows * 1000D) / capacityLimitRows) / 10D;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param policy policy 参数，用于 alertLevel 流程中的校验、计算或对象转换。
     * @param usagePercent usage percent 参数，用于 alertLevel 流程中的校验、计算或对象转换。
     * @return 返回 alert level 生成的文本或业务键。
     */
    private String alertLevel(BiQuickEngineCapacityAlertPolicyView policy, double usagePercent) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!policy.enabled()) {
            return ALERT_DISABLED;
        }
        if (usagePercent >= policy.criticalThresholdPercent()) {
            return ALERT_CRITICAL;
        }
        if (usagePercent >= policy.warningThresholdPercent()) {
            return ALERT_WARNING;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return ALERT_NORMAL;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param concurrencyUsagePercent concurrency usage percent 参数，用于 pressureState 流程中的校验、计算或对象转换。
     * @param queueUsagePercent queue usage percent 参数，用于 pressureState 流程中的校验、计算或对象转换。
     * @return 返回 pressure state 生成的文本或业务键。
     */
    private String pressureState(double concurrencyUsagePercent, double queueUsagePercent) {
        if (concurrencyUsagePercent >= 100D || queueUsagePercent >= 100D) {
            return ALERT_CRITICAL;
        }
        if (concurrencyUsagePercent >= 80D || queueUsagePercent >= 80D) {
            return ALERT_WARNING;
        }
        return ALERT_NORMAL;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param values values 参数，用于 normalizedList 流程中的校验、计算或对象转换。
     * @param maxSize max size 参数，用于 normalizedList 流程中的校验、计算或对象转换。
     * @param label label 参数，用于 normalizedList 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizedList(List<String> values, int maxSize, String label) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String value : values == null ? List.<String>of() : values) {
            String trimmed = trimToNull(value);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (trimmed != null) {
                normalized.add(trimmed);
            }
        }
        if (normalized.size() > maxSize) {
            throw new IllegalArgumentException(label + " must contain at most " + maxSize + " items");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(normalized);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 read list 汇总后的集合、分页或映射视图。
     */
    private List<String> readList(String json) {
        String normalized = trimToNull(json);
        if (normalized == null) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(normalized, STRING_LIST);
            return normalizedList(values, Integer.MAX_VALUE, "values");
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param values values 参数，用于 writeList 流程中的校验、计算或对象转换。
     * @return 返回 write list 生成的文本或业务键。
     */
    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param before before 参数，用于 auditUpdate 流程中的校验、计算或对象转换。
     * @param after after 参数，用于 auditUpdate 流程中的校验、计算或对象转换。
     */
    private void auditUpdate(Long tenantId,
                             String actor,
                             BiQuickEngineCapacityAlertPolicyView before,
                             BiQuickEngineCapacityAlertPolicyView after) {
        if (auditLogMapper == null) {
            return;
        }
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(tenantId);
        row.setActorId(actor(actor));
        row.setActionKey(AUDIT_ACTION);
        row.setResourceType(AUDIT_RESOURCE_TYPE);
        row.setDetailJson(toJson(Map.of(
                "before", before,
                "after", after)));
        row.setCreatedAt(LocalDateTime.now(clock));
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Capacity policy updates should not fail when audit storage is unavailable.
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actor 操作人标识，用于审计和权限判断。
     * @param before before 参数，用于 auditPoolUpdate 流程中的校验、计算或对象转换。
     * @param after after 参数，用于 auditPoolUpdate 流程中的校验、计算或对象转换。
     */
    private void auditPoolUpdate(Long tenantId,
                                 String actor,
                                 BiQuickEngineTenantPoolPolicyView before,
                                 BiQuickEngineTenantPoolPolicyView after) {
        if (auditLogMapper == null) {
            return;
        }
        BiAuditLogDO row = new BiAuditLogDO();
        row.setTenantId(tenantId);
        row.setActorId(actor(actor));
        row.setActionKey(AUDIT_POOL_ACTION);
        row.setResourceType(AUDIT_POOL_RESOURCE_TYPE);
        row.setDetailJson(toJson(Map.of(
                "before", before,
                "after", after)));
        row.setCreatedAt(LocalDateTime.now(clock));
        try {
            auditLogMapper.insert(row);
        } catch (RuntimeException ignored) {
            // Tenant pool policy updates should not fail when audit storage is unavailable.
        }
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
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param actor 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(String actor) {
        String trimmed = trimToNull(actor);
        return trimmed == null ? "system" : trimmed;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private long value(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private long positive(Long value, long fallback) {
        long resolved = value(value, fallback);
        if (resolved <= 0) {
            throw new IllegalArgumentException("capacity limit rows must be greater than 0");
        }
        return resolved;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 boundedPositive 流程中的校验、计算或对象转换。
     * @param maxValue 待处理值，用于规则计算或转换。
     * @param label label 参数，用于 boundedPositive 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedPositive(Integer value, int fallback, int maxValue, String label) {
        int resolved = value == null ? fallback : value;
        if (resolved <= 0 || resolved > maxValue) {
            throw new IllegalArgumentException(label + " must be between 1 and " + maxValue);
        }
        return resolved;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 poolKey 流程中的校验、计算或对象转换。
     * @return 返回 pool key 生成的文本或业务键。
     */
    private String poolKey(String value, String fallback) {
        String resolved = trimToNull(value);
        if (resolved == null) {
            resolved = trimToNull(fallback);
        }
        resolved = resolved == null ? DEFAULT_POOL_KEY : resolved.toUpperCase(Locale.ROOT);
        if (!resolved.matches("[A-Z0-9_-]{1,64}")) {
            throw new IllegalArgumentException("pool key must contain 1-64 letters, numbers, '_' or '-'");
        }
        return resolved;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 threshold 流程中的校验、计算或对象转换。
     * @param label label 参数，用于 threshold 流程中的校验、计算或对象转换。
     * @return 返回 threshold 计算得到的数量、金额或指标值。
     */
    private int threshold(Integer value, int fallback, String label) {
        int resolved = value == null ? fallback : value;
        if (resolved <= 0 || resolved > 100) {
            throw new IllegalArgumentException(label + " must be between 1 and 100");
        }
        return resolved;
    }

    /**
     * ResourceAccumulator 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class ResourceAccumulator {
        private final String resourceKey;
        private final Set<String> tables = new LinkedHashSet<>();
        private long usedRows;
        private Long latestRunId;
        private LocalDateTime latestFinishedAt;
        private Long latestRowCount;
        private String owner = "system";

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param resourceKey 业务键，用于在同一租户下定位资源。
         * @return 返回 ResourceAccumulator 流程生成的业务结果。
         */
        private ResourceAccumulator(String resourceKey) {
            this.resourceKey = resourceKey;
        }

        /**
         * 创建业务对象并完成必要的初始化。
         *
         * @param row 持久化行数据，承载数据库记录内容。
         * @param table table 参数，用于 add 流程中的校验、计算或对象转换。
         * @param rowCount row count 参数，用于 add 流程中的校验、计算或对象转换。
         * @param owner owner 参数，用于 add 流程中的校验、计算或对象转换。
         */
        private void add(BiDatasetExtractRefreshRunDO row, String table, long rowCount, String owner) {
            if (tables.add(table)) {
                usedRows += rowCount;
            }
            if (latestFinishedAt == null
                    || isAfter(row.getFinishedAt(), latestFinishedAt)
                    || Objects.equals(row.getId(), latestRunId)) {
                latestRunId = row.getId();
                latestFinishedAt = row.getFinishedAt();
                latestRowCount = row.getRowCount();
                this.owner = owner;
            }
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 view 流程生成的业务结果。
         */
        private BiQuickEngineCapacityUsageDetailView view() {
            return new BiQuickEngineCapacityUsageDetailView(
                    CATEGORY_DATASET_ACCELERATION,
                    resourceKey,
                    usedRows,
                    tables.size(),
                    latestRunId,
                    latestFinishedAt,
                    latestRowCount,
                    owner);
        }
    }

    /**
     * UserAccumulator 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static final class UserAccumulator {
        private final String user;
        private final Set<String> tables = new LinkedHashSet<>();
        private final Set<String> resources = new LinkedHashSet<>();
        private long usedRows;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param user 操作人标识，用于审计和权限判断。
         * @return 返回 UserAccumulator 流程生成的业务结果。
         */
        private UserAccumulator(String user) {
            this.user = user;
        }

        /**
         * 创建业务对象并完成必要的初始化。
         *
         * @param resourceKey 业务键，用于在同一租户下定位资源。
         * @param table table 参数，用于 add 流程中的校验、计算或对象转换。
         * @param rowCount row count 参数，用于 add 流程中的校验、计算或对象转换。
         */
        private void add(String resourceKey, String table, long rowCount) {
            resources.add(resourceKey);
            if (tables.add(resourceKey + ":" + table)) {
                usedRows += rowCount;
            }
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 view 流程生成的业务结果。
         */
        private BiQuickEngineCapacityUserUsageView view() {
            return new BiQuickEngineCapacityUserUsageView(user, usedRows, tables.size(), resources.size());
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param left left 参数，用于 isAfter 流程中的校验、计算或对象转换。
     * @param right right 参数，用于 isAfter 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isAfter(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return left.isAfter(right);
    }
}
