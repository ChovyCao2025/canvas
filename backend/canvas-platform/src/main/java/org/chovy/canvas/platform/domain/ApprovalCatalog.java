package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ApprovalCatalog {

    private final List<Map<String, Object>> instances = new ArrayList<>();

    public ApprovalCatalog() {
        instances.add(instance(9001L, 7001L, 7L, "CANVAS", "canvas-101", "PENDING",
                "operator-1", "OPERATOR", "lark-approval-8001"));
        instances.add(instance(9002L, 7002L, 7L, "CAMPAIGN", "campaign-2000", "PENDING",
                "operator-1", "OPERATOR", "lark-approval-8002"));
        instances.add(instance(9003L, 7003L, 7L, "SEARCH_MARKETING", "source-10", "PENDING",
                "operator-1", "OPERATOR", "lark-approval-8003"));
        instances.add(instance(9001L, 7001L, 8L, "CANVAS", "canvas-tenant-8", "PENDING",
                "operator-1", "OPERATOR", "lark-approval-tenant-8"));
    }

    public synchronized List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status) {
        return instances.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> status.equals(row.get("status")))
                .filter(row -> canSeeTask(row, actor, role))
                .sorted(Comparator.comparing(row -> (Long) row.get("taskId")))
                .map(ApprovalCatalog::taskView)
                .toList();
    }

    public synchronized List<Map<String, Object>> instances(Long tenantId, String targetType, String targetId,
                                                            String status) {
        String normalizedTargetType = normalizeOptional(targetType);
        String normalizedTargetId = trimToNull(targetId);
        return instances.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> normalizedTargetType == null || normalizedTargetType.equals(row.get("targetType")))
                .filter(row -> normalizedTargetId == null || normalizedTargetId.equals(row.get("targetId")))
                .filter(row -> status == null || status.equals(row.get("status")))
                .sorted(Comparator.comparing(row -> (Long) row.get("instanceId")))
                .map(ApprovalCatalog::copy)
                .toList();
    }

    public synchronized Map<String, Object> decide(Long tenantId, Long taskId, String actor, String role,
                                                   String comment, String decision) {
        Map<String, Object> instance = requireTask(tenantId, taskId);
        requireAssigneeOrAdmin(instance, actor, role);
        if (!"PENDING".equals(instance.get("status"))) {
            throw new IllegalArgumentException("approval task is not pending: " + taskId);
        }
        instance.put("status", decision);
        instance.put("decision", decision);
        instance.put("comment", trimToNull(comment));
        instance.put("operator", actor);
        instance.put("updatedBy", actor);
        instance.put("operatorRole", role);
        instance.put("decidedAt", Instant.EPOCH.toString());
        return copy(instance);
    }

    public synchronized Map<String, Object> syncLarkApprovals(Long tenantId, int limit, String actor) {
        int synced = 0;
        for (Map<String, Object> instance : instances) {
            if (synced >= limit) {
                break;
            }
            if (Objects.equals(instance.get("tenantId"), tenantId)
                    && "PENDING".equals(instance.get("status"))
                    && instance.get("externalInstanceId") != null) {
                instance.put("externalStatus", "SYNCED");
                instance.put("externalSyncedAt", Instant.EPOCH.toString());
                instance.put("externalProvider", "LARK");
                instance.put("externalSyncedBy", actor);
                synced++;
            }
        }
        return ordered(
                "tenantId", tenantId,
                "limit", limit,
                "synced", synced,
                "provider", "LARK",
                "operator", actor,
                "syncedAt", Instant.EPOCH.toString());
    }

    public synchronized Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor) {
        Map<String, Object> instance = requireInstance(tenantId, instanceId);
        instance.put("externalStatus", "SYNCED");
        instance.put("externalSyncedAt", Instant.EPOCH.toString());
        instance.put("externalProvider", "LARK");
        instance.put("externalSyncedBy", actor);
        return copy(instance);
    }

    private static boolean canSeeTask(Map<String, Object> row, String actor, String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return true;
        }
        return Objects.equals(row.get("assignee"), actor) || Objects.equals(row.get("role"), role);
    }

    private Map<String, Object> requireTask(Long tenantId, Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("taskId is required");
        }
        return instances.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("taskId"), taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("approval task not found: " + taskId));
    }

    private static void requireAssigneeOrAdmin(Map<String, Object> instance, String actor, String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return;
        }
        if (Objects.equals(instance.get("assignee"), actor)) {
            return;
        }
        throw new SecurityException("approval task is not assigned to actor: " + actor);
    }

    private Map<String, Object> requireInstance(Long tenantId, Long instanceId) {
        if (instanceId == null || instanceId <= 0) {
            throw new IllegalArgumentException("approval instance id is required");
        }
        return instances.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> Objects.equals(row.get("instanceId"), instanceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("approval instance not found: " + instanceId));
    }

    private static Map<String, Object> instance(Long instanceId, Long taskId, Long tenantId, String targetType,
                                                String targetId, String status, String assignee, String role,
                                                String externalInstanceId) {
        return ordered(
                "id", instanceId,
                "instanceId", instanceId,
                "taskId", taskId,
                "tenantId", tenantId,
                "targetType", targetType,
                "targetId", targetId,
                "status", status,
                "assignee", assignee,
                "role", role,
                "externalInstanceId", externalInstanceId,
                "externalStatus", "PENDING",
                "createdAt", Instant.EPOCH.toString());
    }

    private static Map<String, Object> taskView(Map<String, Object> row) {
        return ordered(
                "id", row.get("taskId"),
                "taskId", row.get("taskId"),
                "instanceId", row.get("instanceId"),
                "tenantId", row.get("tenantId"),
                "targetType", row.get("targetType"),
                "targetId", row.get("targetId"),
                "status", row.get("status"),
                "assignee", row.get("assignee"),
                "role", row.get("role"),
                "createdAt", row.get("createdAt"));
    }

    private static String normalizeOptional(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
