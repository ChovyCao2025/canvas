package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineCapacityAlertPolicyView;
import org.chovy.canvas.bi.api.BiQuickEngineCapacitySummaryView;
import org.chovy.canvas.bi.api.BiQuickEnginePoolView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueItemView;
import org.chovy.canvas.bi.api.BiQuickEngineQueueSnapshotView;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyCommand;
import org.chovy.canvas.bi.api.BiQuickEngineTenantPoolPolicyView;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class BiQuickEngineCapacityCatalog {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final LocalDateTime SNAPSHOT_TIME = LocalDateTime.parse("2026-06-06T10:00:00");
    private static final Pattern POOL_KEY_PATTERN = Pattern.compile("[A-Z0-9_-]{1,64}");

    private final Map<Long, BiQuickEngineCapacityAlertPolicyView> alertPolicies = new ConcurrentHashMap<>();
    private final Map<Long, BiQuickEngineTenantPoolPolicyView> tenantPoolPolicies = new ConcurrentHashMap<>();

    public BiQuickEngineCapacitySummaryView summary(Long tenantId, Integer limit) {
        Long scopedTenantId = safeTenantId(tenantId);
        int normalizedLimit = normalizeLimit(limit);
        BiQuickEngineCapacityAlertPolicyView alertPolicy = alertPolicy(scopedTenantId);
        BiQuickEngineTenantPoolPolicyView tenantPoolPolicy = tenantPoolPolicy(scopedTenantId);
        List<Map<String, Object>> details = capacityDetails().stream()
                .limit(normalizedLimit)
                .toList();
        List<Map<String, Object>> userRankings = userRankings().stream()
                .limit(normalizedLimit)
                .toList();
        return new BiQuickEngineCapacitySummaryView(
                scopedTenantId,
                1_000_000L,
                420_000L,
                42.0,
                "NORMAL",
                false,
                Map.of(
                        "enabled", alertPolicy.enabled(),
                        "capacityLimitRows", alertPolicy.capacityLimitRows(),
                        "warningThresholdPercent", alertPolicy.warningThresholdPercent(),
                        "criticalThresholdPercent", alertPolicy.criticalThresholdPercent(),
                        "notificationChannels", alertPolicy.notificationChannels(),
                        "notificationReceivers", alertPolicy.notificationReceivers()),
                pool(tenantPoolPolicy),
                Map.of(
                        "runningQueries", 2,
                        "queuedQueries", 3,
                        "blockedQueries", 1,
                        "failedQueries", 0,
                        "completedQueries", 9,
                        "runningUsagePercent", 25.0,
                        "queueUsagePercent", 6.0,
                        "pressureLevel", "NORMAL"),
                List.of(Map.of(
                        "category", "DATASET_ACCELERATION",
                        "usedRows", 420_000L,
                        "resourceCount", 3)),
                details,
                userRankings);
    }

    public BiQuickEngineCapacityAlertPolicyView updateAlertPolicy(Long tenantId,
                                                                  BiQuickEngineCapacityAlertPolicyCommand command,
                                                                  String actor,
                                                                  LocalDateTime updatedAt) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiQuickEngineCapacityAlertPolicyView current = alertPolicy(scopedTenantId);
        BiQuickEngineCapacityAlertPolicyCommand safeCommand = command == null
                ? new BiQuickEngineCapacityAlertPolicyCommand(null, null, null, null, null, null)
                : command;
        BiQuickEngineCapacityAlertPolicyView updated = new BiQuickEngineCapacityAlertPolicyView(
                safeCommand.enabled() == null ? current.enabled() : safeCommand.enabled(),
                safeCommand.capacityLimitRows() == null ? current.capacityLimitRows() : safeCommand.capacityLimitRows(),
                safeCommand.warningThresholdPercent() == null
                        ? current.warningThresholdPercent()
                        : safeCommand.warningThresholdPercent(),
                safeCommand.criticalThresholdPercent() == null
                        ? current.criticalThresholdPercent()
                        : safeCommand.criticalThresholdPercent(),
                safeCommand.notificationChannels() == null
                        ? current.notificationChannels()
                        : normalizeValues(safeCommand.notificationChannels(), true, "notificationChannels", 8),
                safeCommand.notificationReceivers() == null
                        ? current.notificationReceivers()
                        : normalizeValues(safeCommand.notificationReceivers(), false, "notificationReceivers", 50),
                actor,
                updatedAt == null ? SNAPSHOT_TIME : updatedAt);
        validateAlertPolicy(updated);
        alertPolicies.put(scopedTenantId, updated);
        return updated;
    }

    public BiQuickEngineTenantPoolPolicyView updateTenantPoolPolicy(Long tenantId,
                                                                    BiQuickEngineTenantPoolPolicyCommand command,
                                                                    String actor,
                                                                    LocalDateTime updatedAt) {
        Long scopedTenantId = safeTenantId(tenantId);
        BiQuickEngineTenantPoolPolicyView current = tenantPoolPolicy(scopedTenantId);
        BiQuickEngineTenantPoolPolicyCommand safeCommand = command == null
                ? new BiQuickEngineTenantPoolPolicyCommand(null, null, null, null, null)
                : command;
        String poolKey = safeCommand.poolKey() == null
                ? current.poolKey()
                : safeCommand.poolKey().trim().toUpperCase(Locale.ROOT);
        BiQuickEngineTenantPoolPolicyView updated = new BiQuickEngineTenantPoolPolicyView(
                poolKey,
                safeCommand.maxConcurrentQueries() == null
                        ? current.maxConcurrentQueries()
                        : safeCommand.maxConcurrentQueries(),
                safeCommand.queueLimit() == null ? current.queueLimit() : safeCommand.queueLimit(),
                safeCommand.queueTimeoutSeconds() == null
                        ? current.queueTimeoutSeconds()
                        : safeCommand.queueTimeoutSeconds(),
                safeCommand.poolWeight() == null ? current.poolWeight() : safeCommand.poolWeight(),
                actor,
                updatedAt == null ? SNAPSHOT_TIME : updatedAt);
        validateTenantPoolPolicy(updated);
        tenantPoolPolicies.put(scopedTenantId, updated);
        return updated;
    }

    public BiQuickEngineQueueSnapshotView queueSnapshot(Long tenantId, String poolKey, String status, Integer limit) {
        String normalizedPoolKey = normalizeOptional(poolKey);
        String normalizedStatus = normalizeOptional(status);
        Long scopedTenantId = safeTenantId(tenantId);
        List<BiQuickEngineQueueItemView> jobs = queueJobs(scopedTenantId).stream()
                .filter(job -> normalizedPoolKey == null || normalizedPoolKey.equals(job.poolKey()))
                .filter(job -> normalizedStatus == null || normalizedStatus.equals(job.status()))
                .sorted(Comparator.comparing(BiQuickEngineQueueItemView::createdAt).reversed()
                        .thenComparing(BiQuickEngineQueueItemView::id))
                .limit(normalizeLimit(limit))
                .toList();
        return new BiQuickEngineQueueSnapshotView(
                scopedTenantId,
                normalizedPoolKey == null ? "ALL" : normalizedPoolKey,
                3L,
                2L,
                9L,
                1L,
                15L,
                jobs);
    }

    private static List<Map<String, Object>> capacityDetails() {
        return List.of(
                Map.of(
                        "category", "DATASET_ACCELERATION",
                        "resourceKey", "canvas_daily_stats",
                        "usedRows", 240_000L,
                        "activeTableCount", 2,
                        "capacityPercent", 24.0,
                        "owner", "analyst",
                        "lastAccessedAt", SNAPSHOT_TIME),
                Map.of(
                        "category", "DATASET_ACCELERATION",
                        "resourceKey", "marketing_campaign_stats",
                        "usedRows", 120_000L,
                        "activeTableCount", 2,
                        "capacityPercent", 12.0,
                        "owner", "operator",
                        "lastAccessedAt", SNAPSHOT_TIME.minusHours(2)),
                Map.of(
                        "category", "DATASET_ACCELERATION",
                        "resourceKey", "audience_segment_stats",
                        "usedRows", 60_000L,
                        "activeTableCount", 1,
                        "capacityPercent", 6.0,
                        "owner", "operator",
                        "lastAccessedAt", SNAPSHOT_TIME.minusDays(1)));
    }

    private static List<BiQuickEngineQueueItemView> queueJobs(Long tenantId) {
        return List.of(
                job(81L, tenantId, "GOLD", "hash-queue-a", "canvas_daily_stats", "analyst", "QUEUED", 0,
                        SNAPSHOT_TIME.minusMinutes(5), null, null),
                job(82L, tenantId, "GOLD", "hash-queue-b", "marketing_campaign_stats", "operator", "QUEUED", 1,
                        SNAPSHOT_TIME.minusMinutes(4), null, null),
                job(83L, tenantId, "STANDARD", "hash-queue-c", "audience_segment_stats", "analyst", "QUEUED", 0,
                        SNAPSHOT_TIME.minusMinutes(3), null, null),
                job(84L, tenantId, "GOLD", "hash-running-a", "canvas_daily_stats", "analyst", "RUNNING", 0,
                        SNAPSHOT_TIME.minusMinutes(8), SNAPSHOT_TIME.minusMinutes(7), "worker-a"),
                job(85L, tenantId, "STANDARD", "hash-running-b", "marketing_campaign_stats", "operator", "RUNNING", 0,
                        SNAPSHOT_TIME.minusMinutes(7), SNAPSHOT_TIME.minusMinutes(6), "worker-b"),
                job(86L, tenantId, "GOLD", "hash-blocked-a", "canvas_daily_stats", "analyst", "BLOCKED", 2,
                        SNAPSHOT_TIME.minusMinutes(20), null, null),
                job(87L, tenantId, "STANDARD", "hash-completed-a", "audience_segment_stats", "operator", "COMPLETED", 0,
                        SNAPSHOT_TIME.minusMinutes(30), SNAPSHOT_TIME.minusMinutes(29), "worker-c"));
    }

    private static List<Map<String, Object>> userRankings() {
        return List.of(
                Map.of(
                        "username", "analyst",
                        "usedRows", 240_000L,
                        "resourceCount", 1,
                        "activeTableCount", 2),
                Map.of(
                        "username", "operator",
                        "usedRows", 180_000L,
                        "resourceCount", 2,
                        "activeTableCount", 3));
    }

    private static BiQuickEngineQueueItemView job(Long id,
                                                  Long tenantId,
                                                  String poolKey,
                                                  String sqlHash,
                                                  String datasetKey,
                                                  String requestedBy,
                                                  String status,
                                                  Integer attempts,
                                                  LocalDateTime createdAt,
                                                  LocalDateTime claimedAt,
                                                  String claimedBy) {
        LocalDateTime completedAt = "COMPLETED".equals(status) ? createdAt.plusMinutes(3) : null;
        String failureReason = "BLOCKED".equals(status) ? "queue admission timeout" : null;
        return new BiQuickEngineQueueItemView(
                id,
                tenantId,
                poolKey,
                sqlHash,
                datasetKey,
                requestedBy,
                status,
                attempts,
                createdAt,
                createdAt.plusMinutes(2),
                claimedBy,
                claimedAt,
                completedAt,
                failureReason,
                createdAt,
                completedAt == null ? createdAt : completedAt);
    }

    private BiQuickEngineCapacityAlertPolicyView alertPolicy(Long tenantId) {
        return alertPolicies.computeIfAbsent(safeTenantId(tenantId), ignored -> defaultAlertPolicy());
    }

    private BiQuickEngineTenantPoolPolicyView tenantPoolPolicy(Long tenantId) {
        return tenantPoolPolicies.computeIfAbsent(safeTenantId(tenantId), ignored -> defaultTenantPoolPolicy());
    }

    private static BiQuickEngineCapacityAlertPolicyView defaultAlertPolicy() {
        return new BiQuickEngineCapacityAlertPolicyView(
                false,
                1_000_000L,
                80,
                95,
                List.of(),
                List.of(),
                "system",
                SNAPSHOT_TIME);
    }

    private static BiQuickEngineTenantPoolPolicyView defaultTenantPoolPolicy() {
        return new BiQuickEngineTenantPoolPolicyView(
                "STANDARD",
                8,
                50,
                120,
                100,
                "analyst",
                SNAPSHOT_TIME);
    }

    private static BiQuickEnginePoolView pool(BiQuickEngineTenantPoolPolicyView policy) {
        return new BiQuickEnginePoolView(
                policy.poolKey(),
                policy.maxConcurrentQueries(),
                policy.queueLimit(),
                policy.queueTimeoutSeconds(),
                policy.poolWeight());
    }

    private static void validateAlertPolicy(BiQuickEngineCapacityAlertPolicyView policy) {
        if (policy.capacityLimitRows() == null || policy.capacityLimitRows() <= 0) {
            throw new IllegalArgumentException("capacityLimitRows must be positive");
        }
        validateThreshold(policy.warningThresholdPercent(), "warningThresholdPercent");
        validateThreshold(policy.criticalThresholdPercent(), "criticalThresholdPercent");
        if (policy.warningThresholdPercent() >= policy.criticalThresholdPercent()) {
            throw new IllegalArgumentException(
                    "warningThresholdPercent must be less than criticalThresholdPercent");
        }
    }

    private static void validateThreshold(Integer value, String fieldName) {
        if (value == null || value < 1 || value > 100) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 100");
        }
    }

    private static void validateTenantPoolPolicy(BiQuickEngineTenantPoolPolicyView policy) {
        if (policy.poolKey() == null || !POOL_KEY_PATTERN.matcher(policy.poolKey()).matches()) {
            throw new IllegalArgumentException("poolKey must match [A-Z0-9_-]{1,64}");
        }
        validatePositiveBounded(policy.maxConcurrentQueries(), "maxConcurrentQueries", 10_000);
        validatePositiveBounded(policy.queueLimit(), "queueLimit", 1_000_000);
        validatePositiveBounded(policy.queueTimeoutSeconds(), "queueTimeoutSeconds", 86_400);
        validatePositiveBounded(policy.poolWeight(), "poolWeight", 10_000);
    }

    private static void validatePositiveBounded(Integer value, String fieldName, int max) {
        if (value == null || value < 1 || value > max) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and " + max);
        }
    }

    private static List<String> normalizeValues(List<String> values, boolean uppercase, String fieldName, int max) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values == null ? List.<String>of() : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            normalized.add(uppercase ? trimmed.toUpperCase(Locale.ROOT) : trimmed);
        }
        if (normalized.size() > max) {
            throw new IllegalArgumentException(fieldName + " must contain at most " + max + " entries");
        }
        return List.copyOf(normalized);
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }
}
