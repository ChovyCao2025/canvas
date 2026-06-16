package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 维护GrowthActivity相关的内存业务目录。
 */
public class GrowthActivityCatalog {

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<Long, TenantState> tenants = new LinkedHashMap<>();

    /**
     * 创建GrowthActivityCatalog实例。
     */
    public GrowthActivityCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    /**
     * 执行upsertActivity业务操作。
     */
    public Map<String, Object> upsertActivity(Long tenantId, Map<String, Object> payload, String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> safePayload = safeMap(payload);
        String activityKey = key(safePayload, "activityKey", "activity-" + state.nextActivityId);
        Map<String, Object> existing = state.activities.stream()
                .filter(row -> activityKey.equals(row.get("activityKey")))
                .findFirst()
                .orElse(null);
        Map<String, Object> row = existing == null
                ? base(tenantId, "upsertActivity", safePayload, actor, "DRAFT")
                : existing;
        if (existing == null) {
            row.put("id", state.nextActivityId++);
            row.put("activityKey", activityKey);
            row.put("createdBy", actor);
            state.activities.add(row);
        }
        row.putAll(safePayload);
        row.put("activityKey", activityKey);
        row.put("activityType", normalized(row.get("activityType"), null));
        row.put("status", normalized(row.get("status"), existing == null ? "DRAFT" : row.get("status")));
        row.put("updatedBy", actor);
        return copy(row);
    }

    /**
     * 查询activities列表。
     */
    public List<Map<String, Object>> listActivities(Long tenantId, String activityType, String status, int limit) {
        return state(tenantId).activities.stream()
                .filter(row -> matches(row.get("activityType"), activityType))
                .filter(row -> matches(row.get("status"), status))
                .sorted(Comparator.comparing(row -> Long.parseLong(String.valueOf(row.get("id")))))
                .limit(limit)
                .map(GrowthActivityCatalog::copy)
                .toList();
    }

    /**
     * 返回activity字段值。
     */
    public Map<String, Object> getActivity(Long tenantId, Long activityId) {
        return copy(activity(state(tenantId), activityId));
    }

    /**
     * 执行report业务操作。
     */
    public Map<String, Object> report(Long tenantId, Long activityId) {
        TenantState state = state(tenantId);
        activity(state, activityId);
        Map<String, Object> report = base(tenantId, "report", Map.of(), "system", "READY");
        report.put("activityId", activityId);
        report.put("rewardPoolCount", count(state.rewardPools, activityId));
        report.put("grantCount", count(state.grants, activityId));
        report.put("referralCount", count(state.referrals, activityId));
        report.put("taskCount", count(state.tasks, activityId));
        report.put("taskProgressCount", count(state.taskProgress, activityId));
        return report;
    }

    /**
     * 执行readiness业务操作。
     */
    public Map<String, Object> readiness(Long tenantId, Long activityId) {
        TenantState state = state(tenantId);
        activity(state, activityId);
        int poolCount = count(state.rewardPools, activityId);
        int taskCount = count(state.tasks, activityId);
        boolean ready = poolCount > 0 && taskCount > 0;
        Map<String, Object> readiness = base(tenantId, "readiness", Map.of(), "system", ready ? "READY" : "BLOCKED");
        readiness.put("activityId", activityId);
        readiness.put("productionReady", ready);
        readiness.put("rewardPoolCount", poolCount);
        readiness.put("taskCount", taskCount);
        return readiness;
    }

    /**
     * 执行transitionActivity业务操作。
     */
    public Map<String, Object> transitionActivity(Long tenantId, Long activityId, String transition, String actor) {
        Map<String, Object> row = activity(state(tenantId), activityId);
        row.put("status", switch (transition) {
            case "publish" -> "PUBLISHED";
            case "pause" -> "PAUSED";
            case "close" -> "CLOSED";
            default -> throw new IllegalArgumentException("unsupported growth activity transition: " + transition);
        });
        row.put("updatedBy", actor);
        return copy(row);
    }

    /**
     * 执行execute业务操作。
     */
    public Map<String, Object> execute(Long tenantId,
                                       Long activityId,
                                       String operation,
                                       Map<String, Object> payload,
                                       String actor) {
        TenantState state = state(tenantId);
        Map<String, Object> safePayload = safeMap(payload);
        return switch (operation) {
            case "upsertRewardPool" -> upsertByKey(state.rewardPools, state.nextRewardPoolId++, tenantId,
                    activityId, operation, safePayload, actor, "poolKey", "pool", "ACTIVE");
            case "createGrant" -> append(state.grants, state.nextGrantId++, tenantId, activityId, operation,
                    safePayload, actor, "PENDING");
            case "retryGrant" -> transitionById(state.grants, tenantId, activityId, "grantId", safePayload, actor,
                    "RETRYING");
            case "reconcileGrant" -> transitionById(state.grants, tenantId, activityId, "grantId", safePayload, actor,
                    "RECONCILED");
            case "cancelGrant" -> transitionById(state.grants, tenantId, activityId, "grantId", safePayload, actor,
                    "CANCELLED");
            case "generateReferralCode" -> referralCode(state, tenantId, activityId, safePayload, actor);
            case "upsertReferral" -> append(state.referrals, state.nextReferralId++, tenantId, activityId, operation,
                    safePayload, actor, "PENDING");
            case "qualifyReferral" -> transitionById(state.referrals, tenantId, activityId, "relationId",
                    safePayload, actor, Boolean.FALSE.equals(safePayload.get("qualified")) ? "REJECTED" : "QUALIFIED");
            case "upsertTask" -> upsertByKey(state.tasks, state.nextTaskId++, tenantId, activityId, operation,
                    safePayload, actor, "taskKey", "task", "ACTIVE");
            case "recordTaskProgress" -> append(state.taskProgress, state.nextTaskProgressId++, tenantId, activityId,
                    operation, safePayload, actor, "RECORDED");
            case "resetTaskProgress" -> transitionById(state.taskProgress, tenantId, activityId, "progressId",
                    safePayload, actor, "RESET");
            default -> throw new IllegalArgumentException("unsupported growth activity operation: " + operation);
        };
    }

    /**
     * 查询列表。
     */
    public List<Map<String, Object>> list(Long tenantId,
                                          Long activityId,
                                          String resource,
                                          Map<String, Object> criteria,
                                          int limit) {
        TenantState state = state(tenantId);
        List<Map<String, Object>> rows = switch (resource) {
            case "rewardPools" -> state.rewardPools;
            case "grants" -> state.grants;
            case "referralCodes" -> state.referralCodes;
            case "referrals" -> state.referrals;
            case "tasks" -> state.tasks;
            case "taskProgress" -> state.taskProgress;
            default -> throw new IllegalArgumentException("unsupported growth activity operation: " + resource);
        };
        Map<String, Object> safeCriteria = safeMap(criteria);
        return rows.stream()
                .filter(row -> Objects.equals(row.get("activityId"), activityId))
                .filter(row -> criteriaMatch(row, safeCriteria))
                .sorted(Comparator.comparing(row -> Long.parseLong(String.valueOf(row.get("id")))))
                .limit(limit)
                .map(GrowthActivityCatalog::copy)
                .toList();
    }

    /**
     * 执行referralCode业务操作。
     */
    private Map<String, Object> referralCode(TenantState state,
                                             Long tenantId,
                                             Long activityId,
                                             Map<String, Object> payload,
                                             String actor) {
        Object participantId = payload.get("participantId");
        Map<String, Object> row = append(state.referralCodes, state.nextReferralCodeId++, tenantId, activityId,
                "generateReferralCode", payload, actor, "ACTIVE");
        row.put("code", "GA-" + activityId + "-" + participantId);
        return row;
    }

    /**
     * 执行upsertByKey业务操作。
     */
    private Map<String, Object> upsertByKey(List<Map<String, Object>> rows,
                                            long id,
                                            Long tenantId,
                                            Long activityId,
                                            String operation,
                                            Map<String, Object> payload,
                                            String actor,
                                            String keyField,
                                            String fallback,
                                            String status) {
        String businessKey = key(payload, keyField, fallback + "-" + id);
        Map<String, Object> existing = rows.stream()
                .filter(row -> Objects.equals(row.get("activityId"), activityId))
                .filter(row -> businessKey.equals(row.get(keyField)))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.putAll(payload);
            existing.put(keyField, businessKey);
            existing.put("status", normalized(existing.get("status"), status));
            existing.put("updatedBy", actor);
            return copy(existing);
        }
        Map<String, Object> row = append(rows, id, tenantId, activityId, operation, payload, actor, status);
        row.put(keyField, businessKey);
        return row;
    }

    /**
     * 执行append业务操作。
     */
    private Map<String, Object> append(List<Map<String, Object>> rows,
                                       long id,
                                       Long tenantId,
                                       Long activityId,
                                       String operation,
                                       Map<String, Object> payload,
                                       String actor,
                                       String status) {
        Map<String, Object> row = base(tenantId, operation, payload, actor, status);
        row.put("id", id);
        row.put("activityId", activityId);
        row.putAll(payload);
        row.put("status", normalized(row.get("status"), status));
        row.put("createdBy", actor);
        row.put("updatedBy", actor);
        rows.add(row);
        return copy(row);
    }

    /**
     * 执行transitionById业务操作。
     */
    private Map<String, Object> transitionById(List<Map<String, Object>> rows,
                                               Long tenantId,
                                               Long activityId,
                                               String idField,
                                               Map<String, Object> payload,
                                               String actor,
                                               String status) {
        Object targetId = payload.get(idField);
        Map<String, Object> row = rows.stream()
                .filter(existing -> Objects.equals(String.valueOf(existing.get("id")), String.valueOf(targetId)))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> created = base(tenantId, "transition", payload, actor, status);
                    created.put("id", toLong(targetId, rows.size() + 1L));
                    created.put("activityId", activityId);
                    created.put("createdBy", actor);
                    rows.add(created);
                    return created;
                });
        row.putAll(payload);
        row.put("activityId", activityId);
        row.put("status", status);
        row.put("updatedBy", actor);
        return copy(row);
    }

    /**
     * 执行activity业务操作。
     */
    private Map<String, Object> activity(TenantState state, Long activityId) {
        Long requiredId = requiredId(activityId, "activityId");
        return state.activities.stream()
                .filter(row -> Objects.equals(row.get("id"), requiredId))
                .findFirst()
                .orElseGet(() -> {
                    Map<String, Object> row = base(state.tenantId, "getActivity", Map.of(), "system", "DRAFT");
                    row.put("id", requiredId);
                    row.put("activityKey", "activity-" + requiredId);
                    row.put("activityType", "GENERAL");
                    row.put("createdBy", "system");
                    state.activities.add(row);
                    return row;
                });
    }

    /**
     * 执行base业务操作。
     */
    private Map<String, Object> base(Long tenantId,
                                     String operation,
                                     Map<String, Object> payload,
                                     String actor,
                                     String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tenantId", tenantId);
        row.put("operation", operation);
        row.put("payload", new LinkedHashMap<>(payload));
        row.put("status", status);
        row.put("occurredAt", clock.instant().toString());
        row.put("updatedBy", actor);
        return row;
    }

    /**
     * 执行state业务操作。
     */
    private TenantState state(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, TenantState::new);
    }

    /**
     * 执行count业务操作。
     */
    private static int count(List<Map<String, Object>> rows, Long activityId) {
        return (int) rows.stream()
                .filter(row -> Objects.equals(row.get("activityId"), activityId))
                .count();
    }

    /**
     * 执行criteriaMatch业务操作。
     */
    private static boolean criteriaMatch(Map<String, Object> row, Map<String, Object> criteria) {
        for (Map.Entry<String, Object> entry : criteria.entrySet()) {
            if (entry.getValue() == null || String.valueOf(entry.getValue()).isBlank()) {
                continue;
            }
            if (!matches(row.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 执行matches业务操作。
     */
    private static boolean matches(Object rowValue, Object expected) {
        return expected == null || String.valueOf(expected).isBlank()
                || (rowValue != null && String.valueOf(rowValue).equalsIgnoreCase(String.valueOf(expected)));
    }

    /**
     * 规范化d输入值。
     */
    private static String normalized(Object value, Object fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.isBlank()) {
            text = fallback == null ? "" : String.valueOf(fallback);
        }
        return text.isBlank() ? null : text.toUpperCase();
    }

    /**
     * 执行key业务操作。
     */
    private static String key(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 执行safeMap业务操作。
     */
    private static Map<String, Object> safeMap(Map<String, Object> payload) {
        return payload == null ? Map.of() : new LinkedHashMap<>(payload);
    }

    /**
     * 执行copy业务操作。
     */
    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    /**
     * 校验并返回dId必填值。
     */
    private static Long requiredId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 转换为long对象。
     */
    private static long toLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return value == null ? fallback : Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /**
     * 提供TenantState的业务能力。
     */
    private static final class TenantState {
        /**
         * 保存tenantId字段值。
         */
        private final Long tenantId;
        private final List<Map<String, Object>> activities = new ArrayList<>();
        private final List<Map<String, Object>> rewardPools = new ArrayList<>();
        private final List<Map<String, Object>> grants = new ArrayList<>();
        private final List<Map<String, Object>> referralCodes = new ArrayList<>();
        private final List<Map<String, Object>> referrals = new ArrayList<>();
        private final List<Map<String, Object>> tasks = new ArrayList<>();
        private final List<Map<String, Object>> taskProgress = new ArrayList<>();

        /**
         * 保存nextActivityId字段值。
         */
        private long nextActivityId = 1L;

        /**
         * 保存nextRewardPoolId字段值。
         */
        private long nextRewardPoolId = 100L;

        /**
         * 保存nextGrantId字段值。
         */
        private long nextGrantId = 200L;

        /**
         * 保存nextReferralCodeId字段值。
         */
        private long nextReferralCodeId = 300L;

        /**
         * 保存nextReferralId字段值。
         */
        private long nextReferralId = 400L;

        /**
         * 保存nextTaskId字段值。
         */
        private long nextTaskId = 500L;

        /**
         * 保存nextTaskProgressId字段值。
         */
        private long nextTaskProgressId = 600L;

        /**
         * 创建TenantState实例。
         */
        private TenantState(Long tenantId) {
            this.tenantId = tenantId;
        }
    }
}
