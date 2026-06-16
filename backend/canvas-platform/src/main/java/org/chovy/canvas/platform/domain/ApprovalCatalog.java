package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 审批目录，保存审批实例并提供任务查询、决策和飞书同步演示逻辑。
 */
public class ApprovalCatalog {

    /**
     * 内存中的审批实例列表。
     */
    private final List<Map<String, Object>> instances = new ArrayList<>();

    /**
     * 创建审批目录并写入固定审批实例。
     */
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

    /**
     * 查询操作者可见的审批任务。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @param role 操作者角色
     * @param status 任务状态
     * @return 审批任务列表
     */
    public synchronized List<Map<String, Object>> tasks(Long tenantId, String actor, String role, String status) {
        return instances.stream()
                .filter(row -> Objects.equals(row.get("tenantId"), tenantId))
                .filter(row -> status.equals(row.get("status")))
                .filter(row -> canSeeTask(row, actor, role))
                .sorted(Comparator.comparing(row -> (Long) row.get("taskId")))
                .map(ApprovalCatalog::taskView)
                .toList();
    }

    /**
     * 查询审批实例。
     *
     * @param tenantId 租户标识
     * @param targetType 审批目标类型
     * @param targetId 审批目标标识
     * @param status 实例状态
     * @return 审批实例列表
     */
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

    /**
     * 对审批任务作出决策。
     *
     * @param tenantId 租户标识
     * @param taskId 审批任务标识
     * @param actor 操作者
     * @param role 操作者角色
     * @param comment 审批意见
     * @param decision 决策状态
     * @return 更新后的审批实例
     */
    public synchronized Map<String, Object> decide(Long tenantId, Long taskId, String actor, String role,
                                                   String comment, String decision) {
        Map<String, Object> instance = requireTask(tenantId, taskId);
        requireAssigneeOrAdmin(instance, actor, role);
        if (!"PENDING".equals(instance.get("status"))) {
            throw new IllegalArgumentException("approval task is not pending: " + taskId);
        }
        // 决策写入同一实例记录，保持任务视图和实例视图观察到一致状态。
        instance.put("status", decision);
        instance.put("decision", decision);
        instance.put("comment", trimToNull(comment));
        instance.put("operator", actor);
        instance.put("updatedBy", actor);
        instance.put("operatorRole", role);
        instance.put("decidedAt", Instant.EPOCH.toString());
        return copy(instance);
    }

    /**
     * 批量同步飞书审批实例。
     *
     * @param tenantId 租户标识
     * @param limit 最大同步数量
     * @param actor 操作者
     * @return 同步结果摘要
     */
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

    /**
     * 同步单个飞书审批实例。
     *
     * @param tenantId 租户标识
     * @param instanceId 审批实例标识
     * @param actor 操作者
     * @return 更新后的审批实例
     */
    public synchronized Map<String, Object> syncLarkApprovalInstance(Long tenantId, Long instanceId, String actor) {
        Map<String, Object> instance = requireInstance(tenantId, instanceId);
        instance.put("externalStatus", "SYNCED");
        instance.put("externalSyncedAt", Instant.EPOCH.toString());
        instance.put("externalProvider", "LARK");
        instance.put("externalSyncedBy", actor);
        return copy(instance);
    }

    /**
     * 判断操作者是否可见指定任务。
     *
     * @param row 审批实例记录
     * @param actor 操作者
     * @param role 操作者角色
     * @return 可见时返回 true
     */
    private static boolean canSeeTask(Map<String, Object> row, String actor, String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return true;
        }
        return Objects.equals(row.get("assignee"), actor) || Objects.equals(row.get("role"), role);
    }

    /**
     * 查询并校验审批任务存在。
     *
     * @param tenantId 租户标识
     * @param taskId 审批任务标识
     * @return 审批实例记录
     */
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

    /**
     * 校验操作者是任务处理人或管理员。
     *
     * @param instance 审批实例
     * @param actor 操作者
     * @param role 操作者角色
     */
    private static void requireAssigneeOrAdmin(Map<String, Object> instance, String actor, String role) {
        if ("ADMIN".equals(role) || "TENANT_ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return;
        }
        if (Objects.equals(instance.get("assignee"), actor)) {
            return;
        }
        throw new SecurityException("approval task is not assigned to actor: " + actor);
    }

    /**
     * 查询并校验审批实例存在。
     *
     * @param tenantId 租户标识
     * @param instanceId 审批实例标识
     * @return 审批实例记录
     */
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

    /**
     * 构造审批实例记录。
     *
     * @param instanceId 审批实例标识
     * @param taskId 审批任务标识
     * @param tenantId 租户标识
     * @param targetType 审批目标类型
     * @param targetId 审批目标标识
     * @param status 审批状态
     * @param assignee 处理人
     * @param role 处理角色
     * @param externalInstanceId 飞书审批实例标识
     * @return 审批实例记录
     */
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

    /**
     * 将审批实例转换为任务视图。
     *
     * @param row 审批实例记录
     * @return 审批任务视图
     */
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

    /**
     * 标准化可选代码值。
     *
     * @param value 原始文本
     * @return 大写代码值；空白值返回 null
     */
    private static String normalizeOptional(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /**
     * 修剪文本并将空白值转为 null。
     *
     * @param value 原始文本
     * @return 修剪后的文本；空白值返回 null
     */
    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 复制记录，避免调用方直接修改内部状态。
     *
     * @param source 原始记录
     * @return 复制后的记录
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    /**
     * 按参数顺序构造有序 Map。
     *
     * @param pairs 键值交替排列的参数
     * @return 有序 Map
     */
    private static Map<String, Object> ordered(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }
}
