package org.chovy.canvas.platform.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class OpsCatalog {

    private final List<Map<String, Object>> audits = new ArrayList<>();
    private long nextAuditId = 9001L;

    public OpsCatalog() {
    }

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

    public Map<String, Object> rebuildRuntimeState(Long tenantId, String actor) {
        audits.add(audit(tenantId, "RUNTIME_REBUILD", null, actor, "OPERATOR", "manual rebuild"));
        return ordered(
                "tenantId", tenantId,
                "rebuilt", true,
                "routeCount", 3,
                "schedulerCount", 2,
                "operator", actor);
    }

    public Map<String, Object> runtimeStatus(Long tenantId, String role, String actor) {
        return ordered(
                "status", "UP",
                "role", role,
                "tenantId", tenantId,
                "username", actor);
    }

    public List<Map<String, Object>> auditEvents(Long tenantId, int limit) {
        return audits.stream()
                .filter(item -> Objects.equals(item.get("tenantId"), tenantId))
                .limit(limit)
                .map(OpsCatalog::copy)
                .toList();
    }

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

    private static void requireCanvasId(Long canvasId) {
        if (canvasId == null || canvasId <= 0) {
            throw new IllegalArgumentException("canvasId is required");
        }
    }

    private static void requireEmergencyPermission(String role) {
        if (!"SUPER_ADMIN".equals(role) && !"TENANT_ADMIN".equals(role)) {
            throw new IllegalArgumentException("operator is not allowed to execute ops emergency action");
        }
    }

    private static String requireReason(Map<String, Object> payload) {
        Object value = payload.get("reason");
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        return String.valueOf(value).trim();
    }

    private static String mode(Map<String, Object> payload) {
        Object value = payload.get("mode");
        return value == null || String.valueOf(value).isBlank() ? "GRACEFUL" : String.valueOf(value).trim();
    }

    private static String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action is required");
        }
        return action.trim().toUpperCase(Locale.ROOT);
    }

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
