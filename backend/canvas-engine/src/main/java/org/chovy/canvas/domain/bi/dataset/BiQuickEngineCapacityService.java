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

    public BiQuickEngineCapacityService(BiQuickEngineCapacityPolicyMapper policyMapper,
                                        BiDatasetExtractRefreshRunMapper runMapper,
                                        BiAuditLogMapper auditLogMapper,
                                        ObjectMapper objectMapper,
                                        Clock clock,
                                        long defaultCapacityLimitRows) {
        this(policyMapper, runMapper, auditLogMapper, objectMapper, clock, defaultCapacityLimitRows,
                BiQueryHistoryReader.empty());
    }

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

    public BiQuickEngineCapacitySummaryView summary(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineCapacityAlertPolicyView policy = alertPolicy(scopedTenantId);
        BiQuickEngineTenantPoolPolicyView poolPolicy = tenantPoolPolicy(scopedTenantId);
        BiQuickEngineConcurrencyQueueView concurrencyQueue = concurrencyQueue(scopedTenantId, limit, poolPolicy);
        Map<String, ResourceAccumulator> resources = new TreeMap<>();
        Map<String, UserAccumulator> users = new TreeMap<>();
        for (BiDatasetExtractRefreshRunDO row : loadRuns(scopedTenantId, limit)) {
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

    public BiQuickEngineCapacityAlertPolicyView alertPolicy(Long tenantId) {
        return view(findPolicy(normalizeTenant(tenantId)));
    }

    public BiQuickEngineTenantPoolPolicyView tenantPoolPolicy(Long tenantId) {
        return tenantPoolView(findPolicy(normalizeTenant(tenantId)));
    }

    public BiQuickEngineAdmissionDecision admitQuery(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineTenantPoolPolicyView poolPolicy = tenantPoolPolicy(scopedTenantId);
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
        return new BiQuickEngineAdmissionDecision(
                true,
                "ADMITTED",
                "Quick Engine tenant pool " + poolPolicy.poolKey() + " admitted",
                poolPolicy,
                concurrencyQueue(scopedTenantId, limit, poolPolicy));
    }

    public BiQuickEngineAdmissionDecision admitQueryOrWait(Long tenantId, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiQuickEngineTenantPoolPolicyView poolPolicy = tenantPoolPolicy(scopedTenantId);
        BiQuickEngineAdmissionDecision immediate = admitQuery(scopedTenantId, limit);
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

    private BiQuickEngineAdmissionDecision admitDistributedSlot(
            Long tenantId,
            int limit,
            BiQuickEngineTenantPoolPolicyView poolPolicy) {
        BiQuickEngineConcurrencyQueueView queue = concurrencyQueue(tenantId, limit, poolPolicy);
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
        return new BiQuickEngineAdmissionDecision(
                false,
                "BLOCKED",
                "Quick Engine tenant pool " + poolPolicy.poolKey()
                        + " distributed slots are busy: "
                        + poolPolicy.maxConcurrentQueries() + "/" + poolPolicy.maxConcurrentQueries(),
                poolPolicy,
                queue);
    }

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

    private void incrementLive(Long tenantId) {
        liveRunningQueries.computeIfAbsent(normalizeTenant(tenantId), ignored -> new AtomicInteger())
                .incrementAndGet();
    }

    private void incrementQueued(Long tenantId) {
        liveQueuedQueries.computeIfAbsent(normalizeTenant(tenantId), ignored -> new AtomicInteger())
                .incrementAndGet();
    }

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

    private void notifyQueueWaiters(Long tenantId) {
        Object monitor = queueMonitors.get(normalizeTenant(tenantId));
        if (monitor == null) {
            return;
        }
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }

    private void rememberAcquiredSlotLease(Long tenantId, String leaseKey) {
        acquiredSlotLeases.get()
                .computeIfAbsent(normalizeTenant(tenantId), ignored -> new ArrayDeque<>())
                .push(leaseKey);
    }

    private String pollAcquiredSlotLease(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Map<Long, Deque<String>> slotsByTenant = acquiredSlotLeases.get();
        Deque<String> slots = slotsByTenant.get(scopedTenantId);
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
        return leaseKey;
    }

    private Duration slotLeaseTtl() {
        return Duration.ofSeconds(slotLeaseTtlSeconds);
    }

    private String slotLeaseKey(String poolKey, int slot) {
        return SLOT_LEASE_KEY_PREFIX + poolKey(poolKey, DEFAULT_POOL_KEY) + "_SLOT_" + slot;
    }

    public BiQuickEngineCapacityAlertPolicyView upsertAlertPolicy(
            Long tenantId,
            BiQuickEngineCapacityAlertPolicyCommand command,
            String actor) {
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
        return after;
    }

    public BiQuickEngineTenantPoolPolicyView upsertTenantPoolPolicy(
            Long tenantId,
            BiQuickEngineTenantPoolPolicyCommand command,
            String actor) {
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
        return after;
    }

    private BiQuickEngineCapacityPolicyDO findPolicy(Long tenantId) {
        List<BiQuickEngineCapacityPolicyDO> rows = policyMapper.selectList(
                new LambdaQueryWrapper<BiQuickEngineCapacityPolicyDO>()
                        .eq(BiQuickEngineCapacityPolicyDO::getTenantId, tenantId)
                        .orderByDesc(BiQuickEngineCapacityPolicyDO::getUpdatedAt)
                        .last("LIMIT 1"));
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private List<BiDatasetExtractRefreshRunDO> loadRuns(Long tenantId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 500));
        List<BiDatasetExtractRefreshRunDO> rows = runMapper.selectList(
                new LambdaQueryWrapper<BiDatasetExtractRefreshRunDO>()
                        .eq(BiDatasetExtractRefreshRunDO::getTenantId, tenantId)
                        .orderByDesc(BiDatasetExtractRefreshRunDO::getFinishedAt)
                        .last("LIMIT " + boundedLimit));
        return rows == null ? List.of() : rows;
    }

    private BiQuickEngineCapacityAlertPolicyView view(BiQuickEngineCapacityPolicyDO row) {
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
        return new BiQuickEngineCapacityAlertPolicyView(
                Boolean.TRUE.equals(row.getEnabled()),
                Math.max(1L, value(row.getCapacityLimitRows(), defaultCapacityLimitRows)),
                threshold(row.getWarningThresholdPercent(), DEFAULT_WARNING_THRESHOLD_PERCENT, "warning threshold"),
                threshold(row.getCriticalThresholdPercent(), DEFAULT_CRITICAL_THRESHOLD_PERCENT, "critical threshold"),
                readList(row.getNotificationChannels()),
                readList(row.getNotificationReceivers()),
                actor(row.getUpdatedBy()),
                row.getUpdatedAt());
    }

    private BiQuickEngineTenantPoolPolicyView tenantPoolView(BiQuickEngineCapacityPolicyDO row) {
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
        return new BiQuickEngineTenantPoolPolicyView(
                poolKey(row.getPoolKey(), DEFAULT_POOL_KEY),
                boundedPositive(row.getMaxConcurrentQueries(), DEFAULT_MAX_CONCURRENT_QUERIES,
                        MAX_CONCURRENT_QUERIES, "max concurrent queries"),
                boundedPositive(row.getQueueLimit(), DEFAULT_QUEUE_LIMIT, MAX_QUEUE_LIMIT, "queue limit"),
                boundedPositive(row.getQueueTimeoutSeconds(), DEFAULT_QUEUE_TIMEOUT_SECONDS,
                        MAX_QUEUE_TIMEOUT_SECONDS, "queue timeout seconds"),
                boundedPositive(row.getPoolWeight(), DEFAULT_POOL_WEIGHT, MAX_POOL_WEIGHT, "pool weight"),
                actor(row.getUpdatedBy()),
                row.getUpdatedAt());
    }

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

    private int liveRunning(Long tenantId) {
        AtomicInteger counter = liveRunningQueries.get(normalizeTenant(tenantId));
        return counter == null ? 0 : Math.max(0, counter.get());
    }

    private int liveQueued(Long tenantId) {
        AtomicInteger counter = liveQueuedQueries.get(normalizeTenant(tenantId));
        return counter == null ? 0 : Math.max(0, counter.get());
    }

    private List<BiQueryHistoryItem> loadHistory(Long tenantId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? 50 : limit, 500));
        try {
            List<BiQueryHistoryItem> rows = historyReader.recent(tenantId, boundedLimit);
            return rows == null ? List.of() : rows;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private int countStatus(List<BiQueryHistoryItem> items, String status) {
        return (int) items.stream()
                .filter(item -> status.equals(normalize(item == null ? null : item.status())))
                .count();
    }

    private boolean isActiveSuccess(BiDatasetExtractRefreshRunDO row) {
        if (row == null || !STATUS_SUCCESS.equals(normalize(row.getStatus()))) {
            return false;
        }
        return !RETENTION_DROPPED.equals(normalize(row.getRetentionStatus()));
    }

    private double usagePercent(long usedRows, long capacityLimitRows) {
        if (capacityLimitRows <= 0) {
            return 0D;
        }
        return Math.round((usedRows * 1000D) / capacityLimitRows) / 10D;
    }

    private String alertLevel(BiQuickEngineCapacityAlertPolicyView policy, double usagePercent) {
        if (!policy.enabled()) {
            return ALERT_DISABLED;
        }
        if (usagePercent >= policy.criticalThresholdPercent()) {
            return ALERT_CRITICAL;
        }
        if (usagePercent >= policy.warningThresholdPercent()) {
            return ALERT_WARNING;
        }
        return ALERT_NORMAL;
    }

    private String pressureState(double concurrencyUsagePercent, double queueUsagePercent) {
        if (concurrencyUsagePercent >= 100D || queueUsagePercent >= 100D) {
            return ALERT_CRITICAL;
        }
        if (concurrencyUsagePercent >= 80D || queueUsagePercent >= 80D) {
            return ALERT_WARNING;
        }
        return ALERT_NORMAL;
    }

    private List<String> normalizedList(List<String> values, int maxSize, String label) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values == null ? List.<String>of() : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                normalized.add(trimmed);
            }
        }
        if (normalized.size() > maxSize) {
            throw new IllegalArgumentException(label + " must contain at most " + maxSize + " items");
        }
        return List.copyOf(normalized);
    }

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

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String actor(String actor) {
        String trimmed = trimToNull(actor);
        return trimmed == null ? "system" : trimmed;
    }

    private long value(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private long positive(Long value, long fallback) {
        long resolved = value(value, fallback);
        if (resolved <= 0) {
            throw new IllegalArgumentException("capacity limit rows must be greater than 0");
        }
        return resolved;
    }

    private int boundedPositive(Integer value, int fallback, int maxValue, String label) {
        int resolved = value == null ? fallback : value;
        if (resolved <= 0 || resolved > maxValue) {
            throw new IllegalArgumentException(label + " must be between 1 and " + maxValue);
        }
        return resolved;
    }

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

    private int threshold(Integer value, int fallback, String label) {
        int resolved = value == null ? fallback : value;
        if (resolved <= 0 || resolved > 100) {
            throw new IllegalArgumentException(label + " must be between 1 and 100");
        }
        return resolved;
    }

    private static final class ResourceAccumulator {
        private final String resourceKey;
        private final Set<String> tables = new LinkedHashSet<>();
        private long usedRows;
        private Long latestRunId;
        private LocalDateTime latestFinishedAt;
        private Long latestRowCount;
        private String owner = "system";

        private ResourceAccumulator(String resourceKey) {
            this.resourceKey = resourceKey;
        }

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

    private static final class UserAccumulator {
        private final String user;
        private final Set<String> tables = new LinkedHashSet<>();
        private final Set<String> resources = new LinkedHashSet<>();
        private long usedRows;

        private UserAccumulator(String user) {
            this.user = user;
        }

        private void add(String resourceKey, String table, long rowCount) {
            resources.add(resourceKey);
            if (tables.add(resourceKey + ":" + table)) {
                usedRows += rowCount;
            }
        }

        private BiQuickEngineCapacityUserUsageView view() {
            return new BiQuickEngineCapacityUserUsageView(user, usedRows, tables.size(), resources.size());
        }
    }

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
