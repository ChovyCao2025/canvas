package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 保存运维审计演示数据并执行缓存、运行时和应急动作。
 */
public class OpsCatalog {

    /**
     * 内存中的运维审计事件。
     */
    private final List<Map<String, Object>> audits = new ArrayList<>();

    /**
     * 下一条审计事件标识。
     */
    private long nextAuditId = 9001L;

    /**
     * 创建运维目录。
     */
    public OpsCatalog() {
    }

    /**
     * 失效画布运行时缓存并记录审计。
     *
     * @param tenantId 租户标识
     * @param canvasId 画布标识
     * @param actor 操作者
     * @return 缓存失效结果
     */
    public Map<String, Object> invalidateCache(Long tenantId, Long canvasId, String actor) {
        requireCanvasId(canvasId);
        audits.add(audit(tenantId, "CACHE_INVALIDATE", canvasId, actor, "OPERATOR", "manual invalidate"));
        return ordered(
                "tenantId", tenantId,
                "canvasId", canvasId,
                "invalidated", true,
                "publishedVersionInvalidated", true,
                "canaryVersionInvalidated", true,
                "operator", actor);
    }

    /**
     * 重建租户运行时状态并记录审计。
     *
     * @param tenantId 租户标识
     * @param actor 操作者
     * @return 重建结果
     */
    public Map<String, Object> rebuildRuntimeState(Long tenantId, String actor) {
        audits.add(audit(tenantId, "RUNTIME_REBUILD", null, actor, "OPERATOR", "manual rebuild"));
        return ordered(
                "tenantId", tenantId,
                "rebuilt", true,
                "routeCount", 3,
                "schedulerCount", 2,
                "operator", actor);
    }

    /**
     * 查询运行时状态。
     *
     * @param tenantId 租户标识
     * @param role 操作者角色
     * @param actor 操作者
     * @return 运行时状态记录
     */
    public Map<String, Object> runtimeStatus(Long tenantId, String role, String actor) {
        return ordered(
                "status", "UP",
                "role", role,
                "tenantId", tenantId,
                "username", actor);
    }

    /**
     * 查询运维审计事件。
     *
     * @param tenantId 租户标识
     * @param limit 最大返回数量
     * @return 审计事件列表
     */
    public List<Map<String, Object>> auditEvents(Long tenantId, int limit) {
        return audits.stream()
                .filter(item -> Objects.equals(item.get("tenantId"), tenantId))
                .limit(limit)
                .map(OpsCatalog::copy)
                .toList();
    }

    /**
     * 执行画布应急动作并写入审计。
     *
     * @param tenantId 租户标识
     * @param canvasId 画布标识
     * @param action 应急动作
     * @param payload 应急动作参数
     * @param role 操作者角色
     * @param actor 操作者
     * @return 应急动作结果
     */
    public Map<String, Object> emergencyAction(Long tenantId, Long canvasId, String action, Map<String, Object> payload,
                                               String role, String actor) {
        requireCanvasId(canvasId);
        requireEmergencyPermission(role);
        String normalizedAction = normalizeAction(action);
        String reason = requireReason(payload);
        String mode = mode(payload);
        Map<String, Object> audit = audit(tenantId, normalizedAction, canvasId, actor, role, reason);
        audits.add(audit);
        return ordered(
                "tenantId", tenantId,
                "canvasId", canvasId,
                "action", normalizedAction,
                "status", statusFor(normalizedAction),
                "mode", mode,
                "reason", reason,
                "operator", actor,
                "auditId", audit.get("id"));
    }

    /**
     * 校验画布标识。
     *
     * @param canvasId 画布标识
     */
    private static void requireCanvasId(Long canvasId) {
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
    }

    /**
     * 校验操作者是否具备应急动作权限。
     *
     * @param role 操作者角色
     */
    private static void requireEmergencyPermission(String role) {
        if (!"SUPER_ADMIN".equals(role) && !"TENANT_ADMIN".equals(role)) {
            throw new IllegalArgumentException("operator is not allowed to execute ops emergency action");
        }
    }

    /**
     * 从请求体读取必填原因。
     *
     * @param payload 请求体
     * @return 修剪后的原因
     */
    private static String requireReason(Map<String, Object> payload) {
        Object value = payload.get("reason");
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        return String.valueOf(value).trim();
    }

    /**
     * 从请求体读取执行模式。
     *
     * @param payload 请求体
     * @return 执行模式；缺失时返回 GRACEFUL
     */
    private static String mode(Map<String, Object> payload) {
        Object value = payload.get("mode");
        return value == null || String.valueOf(value).isBlank() ? "GRACEFUL" : String.valueOf(value).trim();
    }

    /**
     * 标准化应急动作名称。
     *
     * @param action 原始动作
     * @return 大写动作名称
     */
    private static String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
        return action.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 将应急动作映射为结果状态。
     *
     * @param action 标准化动作
     * @return 动作结果状态
     */
    private static String statusFor(String action) {
        return switch (action) {
            case "PAUSE" -> "PAUSED";
            case "OFFLINE" -> "OFFLINE";
            case "RESUME" -> "RESUMED";
            case "KILL" -> "KILLED";
            case "ROLLBACK" -> "ROLLED_BACK";
            default -> action;
        };
    }

    /**
     * 构造审计事件并递增审计标识。
     *
     * @param tenantId 租户标识
     * @param action 审计动作
     * @param canvasId 画布标识
     * @param actor 操作者
     * @param role 操作者角色
     * @param reason 操作原因
     * @return 审计事件
     */
    private Map<String, Object> audit(Long tenantId, String action, Long canvasId, String actor, String role,
                                      String reason) {
        return ordered(
                "id", nextAuditId++,
                "tenantId", tenantId,
                "action", action,
                "canvasId", canvasId,
                "operator", actor,
                "role", role,
                "reason", reason,
                "createdAt", Instant.EPOCH.toString());
    }

    /**
     * 复制审计记录。
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
