package org.chovy.canvas.bi.domain;

import org.chovy.canvas.bi.api.BiColumnPermissionCommand;
import org.chovy.canvas.bi.api.BiColumnPermissionView;
import org.chovy.canvas.bi.api.BiPermissionAuditEntryView;
import org.chovy.canvas.bi.api.BiPermissionRequestCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestReviewCommand;
import org.chovy.canvas.bi.api.BiPermissionRequestView;
import org.chovy.canvas.bi.api.BiResourcePermissionCommand;
import org.chovy.canvas.bi.api.BiResourcePermissionView;
import org.chovy.canvas.bi.api.BiRowPermissionCommand;
import org.chovy.canvas.bi.api.BiRowPermissionView;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BiPermissionAdministrationCatalog {

    private static final Long WORKSPACE_ID = 5L;
    private final Map<Long, BiResourcePermissionView> resourcePermissions = new LinkedHashMap<>();
    private final Map<Long, BiRowPermissionView> rowPermissions = new LinkedHashMap<>();
    private final Map<Long, BiColumnPermissionView> columnPermissions = new LinkedHashMap<>();
    private final Map<Long, BiPermissionRequestView> permissionRequests = new LinkedHashMap<>();
    private final Map<Long, BiPermissionAuditEntryView> auditEntries = new LinkedHashMap<>();
    private final Map<Long, Long> auditTenants = new LinkedHashMap<>();
    private long nextResourcePermissionId = 1L;
    private long nextRowPermissionId = 1L;
    private long nextColumnPermissionId = 1L;
    private long nextRequestId = 1L;
    private long nextAuditId = 1L;

    public synchronized List<BiResourcePermissionView> listResourcePermissions(
            Long tenantId,
            String resourceType,
            String resourceKey,
            Long resourceId) {
        Long scopedTenantId = tenant(tenantId);
        String type = nullableType(resourceType);
        String key = nullableKey(resourceKey);
        return resourcePermissions.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> type == null || type.equals(view.resourceType()))
                .filter(view -> key == null || key.equals(view.resourceKey()))
                .filter(view -> resourceId == null || resourceId.equals(view.resourceId()))
                .sorted(Comparator.comparing(BiResourcePermissionView::resourceType)
                        .thenComparing(BiResourcePermissionView::resourceKey)
                        .thenComparing(BiResourcePermissionView::subjectType)
                        .thenComparing(BiResourcePermissionView::subjectId)
                        .thenComparing(BiResourcePermissionView::actionKey)
                        .thenComparing(BiResourcePermissionView::id))
                .toList();
    }

    public synchronized BiResourcePermissionView upsertResourcePermission(
            Long tenantId,
            BiResourcePermissionCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("resource permission command is required");
        }
        Long scopedTenantId = tenant(tenantId);
        String type = requiredType(command.resourceType());
        String key = command.resourceKey() == null || command.resourceKey().isBlank()
                ? "resource-" + requiredResourceId(command.resourceId())
                : key(command.resourceKey());
        Long resourceId = command.resourceId() == null ? stableId(key) : requiredResourceId(command.resourceId());
        String subjectType = upper(command.subjectType(), "subjectType");
        String subjectId = subjectId(command.subjectId());
        String actionKey = upper(command.actionKey(), "actionKey");
        String effect = effect(command.effect());
        Long workspaceId = command.workspaceId() == null ? WORKSPACE_ID : command.workspaceId();
        BiResourcePermissionView existing = resourcePermissions.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> type.equals(view.resourceType()))
                .filter(view -> resourceId.equals(view.resourceId()))
                .filter(view -> subjectType.equals(view.subjectType()))
                .filter(view -> subjectId.equals(view.subjectId()))
                .filter(view -> actionKey.equals(view.actionKey()))
                .findFirst()
                .orElse(null);
        BiResourcePermissionView view = new BiResourcePermissionView(
                existing == null ? nextResourcePermissionId++ : existing.id(),
                scopedTenantId,
                workspaceId,
                type,
                key,
                resourceId,
                subjectType,
                subjectId,
                actionKey,
                effect,
                actor(actor),
                existing == null ? now : existing.createdAt());
        resourcePermissions.put(view.id(), view);
            audit(scopedTenantId, actor, "BI_PERMISSION_CHANGE", "BI_PERMISSION", detail(type, key, actionKey, effect),
                    now);
        return view;
    }

    public synchronized BiResourcePermissionView deleteResourcePermission(
            Long tenantId,
            String actor,
            Long id,
            LocalDateTime now) {
        BiResourcePermissionView removed = removeScoped(resourcePermissions, tenantId, id);
        if (removed != null) {
            audit(tenant(tenantId), actor, "BI_PERMISSION_DELETE", "BI_PERMISSION", detail(removed.resourceType(),
                    removed.resourceKey(), removed.actionKey(), removed.effect()), now);
        }
        return removed;
    }

    public synchronized List<BiRowPermissionView> listRowPermissions(Long tenantId, String datasetKey) {
        Long scopedTenantId = tenant(tenantId);
        String key = nullableKey(datasetKey);
        return rowPermissions.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> key == null || key.equals(view.datasetKey()))
                .sorted(Comparator.comparing(BiRowPermissionView::datasetKey)
                        .thenComparing(BiRowPermissionView::ruleKey)
                        .thenComparing(BiRowPermissionView::subjectType)
                        .thenComparing(BiRowPermissionView::subjectId)
                        .thenComparing(BiRowPermissionView::id))
                .toList();
    }

    public synchronized BiRowPermissionView upsertRowPermission(
            Long tenantId,
            BiRowPermissionCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("row permission command is required");
        }
        Long scopedTenantId = tenant(tenantId);
        String datasetKey = key(command.datasetKey());
        String ruleKey = key(command.ruleKey());
        String subjectType = upper(command.subjectType(), "subjectType");
        String subjectId = subjectId(command.subjectId());
        BiRowPermissionView existing = rowPermissions.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> datasetKey.equals(view.datasetKey()))
                .filter(view -> ruleKey.equals(view.ruleKey()))
                .filter(view -> subjectType.equals(view.subjectType()))
                .filter(view -> subjectId.equals(view.subjectId()))
                .findFirst()
                .orElse(null);
        BiRowPermissionView view = new BiRowPermissionView(
                existing == null ? nextRowPermissionId++ : existing.id(),
                scopedTenantId,
                datasetKey,
                stableId(datasetKey),
                ruleKey,
                subjectType,
                subjectId,
                filtersJson(command),
                command.enabled() == null || command.enabled(),
                existing == null ? now : existing.createdAt());
        rowPermissions.put(view.id(), view);
        audit(scopedTenantId, actor, "BI_PERMISSION_CHANGE", "BI_ROW_PERMISSION",
                detail("DATASET", datasetKey, ruleKey, "ROW"), now);
        return view;
    }

    public synchronized void deleteRowPermission(Long tenantId, String actor, Long id, LocalDateTime now) {
        BiRowPermissionView removed = removeScoped(rowPermissions, tenantId, id);
        if (removed != null) {
            audit(tenant(tenantId), actor, "BI_PERMISSION_DELETE", "BI_ROW_PERMISSION",
                    detail("DATASET", removed.datasetKey(), removed.ruleKey(), "ROW"), now);
        }
    }

    public synchronized List<BiColumnPermissionView> listColumnPermissions(Long tenantId, String datasetKey) {
        Long scopedTenantId = tenant(tenantId);
        String key = nullableKey(datasetKey);
        return columnPermissions.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> key == null || key.equals(view.datasetKey()))
                .sorted(Comparator.comparing(BiColumnPermissionView::datasetKey)
                        .thenComparing(BiColumnPermissionView::fieldKey)
                        .thenComparing(BiColumnPermissionView::subjectType)
                        .thenComparing(BiColumnPermissionView::subjectId)
                        .thenComparing(BiColumnPermissionView::id))
                .toList();
    }

    public synchronized BiColumnPermissionView upsertColumnPermission(
            Long tenantId,
            BiColumnPermissionCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("column permission command is required");
        }
        Long scopedTenantId = tenant(tenantId);
        String datasetKey = key(command.datasetKey());
        String fieldKey = key(command.fieldKey());
        String subjectType = upper(command.subjectType(), "subjectType");
        String subjectId = subjectId(command.subjectId());
        String policy = columnPolicy(command.policy());
        BiColumnPermissionView existing = columnPermissions.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> datasetKey.equals(view.datasetKey()))
                .filter(view -> fieldKey.equals(view.fieldKey()))
                .filter(view -> subjectType.equals(view.subjectType()))
                .filter(view -> subjectId.equals(view.subjectId()))
                .findFirst()
                .orElse(null);
        BiColumnPermissionView view = new BiColumnPermissionView(
                existing == null ? nextColumnPermissionId++ : existing.id(),
                scopedTenantId,
                datasetKey,
                stableId(datasetKey),
                fieldKey,
                subjectType,
                subjectId,
                policy,
                json(command.mask()),
                command.enabled() == null || command.enabled(),
                existing == null ? now : existing.createdAt());
        columnPermissions.put(view.id(), view);
        audit(scopedTenantId, actor, "BI_PERMISSION_CHANGE", "BI_COLUMN_PERMISSION",
                detail("DATASET", datasetKey, fieldKey, policy), now);
        return view;
    }

    public synchronized void deleteColumnPermission(Long tenantId, String actor, Long id, LocalDateTime now) {
        BiColumnPermissionView removed = removeScoped(columnPermissions, tenantId, id);
        if (removed != null) {
            audit(tenant(tenantId), actor, "BI_PERMISSION_DELETE", "BI_COLUMN_PERMISSION",
                    detail("DATASET", removed.datasetKey(), removed.fieldKey(), removed.policy()), now);
        }
    }

    public synchronized List<BiPermissionAuditEntryView> audit(Long tenantId, int limit) {
        int boundedLimit = Math.max(0, Math.min(limit, 100));
        Long scopedTenantId = tenant(tenantId);
        return auditEntries.values().stream()
                .filter(entry -> scopedTenantId.equals(auditTenants.get(entry.id())))
                .sorted(Comparator.comparingInt(BiPermissionAdministrationCatalog::auditPriority)
                        .thenComparing(Comparator.comparing(BiPermissionAuditEntryView::createdAt).reversed())
                        .thenComparing(Comparator.comparing(BiPermissionAuditEntryView::id).reversed()))
                .limit(boundedLimit)
                .toList();
    }

    public synchronized List<BiPermissionRequestView> listPermissionRequests(
            Long tenantId,
            String resourceType,
            String resourceKey,
            String status) {
        Long scopedTenantId = tenant(tenantId);
        String type = nullableType(resourceType);
        String key = nullableKey(resourceKey);
        String normalizedStatus = nullableUpper(status);
        return permissionRequests.values().stream()
                .filter(view -> scopedTenantId.equals(view.tenantId()))
                .filter(view -> type == null || type.equals(view.resourceType()))
                .filter(view -> key == null || key.equals(view.resourceKey()))
                .filter(view -> normalizedStatus == null || normalizedStatus.equals(view.status()))
                .sorted(Comparator.comparing(BiPermissionRequestView::requestedAt).reversed()
                        .thenComparing(BiPermissionRequestView::id))
                .toList();
    }

    public synchronized BiPermissionRequestView requestPermission(
            Long tenantId,
            BiPermissionRequestCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null) {
            throw new IllegalArgumentException("permission request command is required");
        }
        BiPermissionRequestView view = new BiPermissionRequestView(
                nextRequestId++,
                tenant(tenantId),
                WORKSPACE_ID,
                requiredType(command.resourceType()),
                key(command.resourceKey()),
                upper(command.requestedAction(), "requestedAction"),
                actor(actor),
                now,
                text(command.reason()),
                "PENDING",
                null,
                null,
                null,
                null);
        permissionRequests.put(view.id(), view);
        audit(view.tenantId(), actor, "BI_PERMISSION_REQUEST", "BI_PERMISSION_REQUEST",
                detail(view.resourceType(), view.resourceKey(), view.requestedAction(), view.status()), now);
        return view;
    }

    public synchronized BiPermissionRequestView reviewPermissionRequest(
            Long tenantId,
            BiPermissionRequestReviewCommand command,
            String actor,
            LocalDateTime now) {
        if (command == null || command.requestId() == null || command.requestId() <= 0) {
            throw new IllegalArgumentException("requestId is required");
        }
        Long scopedTenantId = tenant(tenantId);
        BiPermissionRequestView existing = permissionRequests.get(command.requestId());
        if (existing == null || !scopedTenantId.equals(existing.tenantId())) {
            throw new IllegalArgumentException("BI permission request not found: " + command.requestId());
        }
        if (!"PENDING".equals(existing.status())) {
            throw new IllegalStateException("BI permission request is not pending: " + command.requestId());
        }
        String status = reviewStatus(command.status());
        Long grantId = null;
        if ("APPROVED".equals(status)) {
            BiResourcePermissionView grant = upsertResourcePermission(scopedTenantId, new BiResourcePermissionCommand(
                    existing.workspaceId(),
                    existing.resourceType(),
                    existing.resourceKey(),
                    stableId(existing.resourceKey()),
                    "USER",
                    existing.requestedBy(),
                    existing.requestedAction(),
                    "ALLOW"), actor, now);
            grantId = grant.id();
        }
        BiPermissionRequestView reviewed = new BiPermissionRequestView(
                existing.id(),
                existing.tenantId(),
                existing.workspaceId(),
                existing.resourceType(),
                existing.resourceKey(),
                existing.requestedAction(),
                existing.requestedBy(),
                existing.requestedAt(),
                existing.reason(),
                status,
                actor(actor),
                now,
                text(command.reviewComment()),
                grantId);
        permissionRequests.put(reviewed.id(), reviewed);
        audit(reviewed.tenantId(), actor, "BI_PERMISSION_REQUEST_REVIEW", "BI_PERMISSION_REQUEST",
                detail(reviewed.resourceType(), reviewed.resourceKey(), reviewed.requestedAction(), reviewed.status()),
                now);
        return reviewed;
    }

    private <T> T removeScoped(Map<Long, T> values, Long tenantId, Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        T existing = values.get(id);
        if (existing instanceof BiResourcePermissionView view && tenant(tenantId).equals(view.tenantId())) {
            return values.remove(id);
        }
        if (existing instanceof BiRowPermissionView view && tenant(tenantId).equals(view.tenantId())) {
            return values.remove(id);
        }
        if (existing instanceof BiColumnPermissionView view && tenant(tenantId).equals(view.tenantId())) {
            return values.remove(id);
        }
        return null;
    }

    private void audit(Long tenantId, String actor, String actionKey, String resourceType, String detailJson,
                       LocalDateTime now) {
        Long id = nextAuditId++;
        auditEntries.put(id, new BiPermissionAuditEntryView(
                id,
                actor(actor),
                actionKey,
                resourceType,
                detailJson,
                now));
        auditTenants.put(id, tenant(tenantId));
    }

    private static int auditPriority(BiPermissionAuditEntryView entry) {
        return "BI_PERMISSION_REQUEST_REVIEW".equals(entry.actionKey()) ? 0 : 1;
    }

    private static Long tenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String actor(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String text(String value) {
        return value == null ? null : value.trim();
    }

    private static String requiredType(String value) {
        return upper(value, "resourceType");
    }

    private static String nullableType(String value) {
        return value == null || value.isBlank() ? null : requiredType(value);
    }

    private static String nullableUpper(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String upper(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String effect(String value) {
        String effect = upper(value, "effect");
        if (!"ALLOW".equals(effect) && !"DENY".equals(effect)) {
            throw new IllegalArgumentException("unsupported BI permission effect: " + effect);
        }
        return effect;
    }

    private static String columnPolicy(String value) {
        String policy = upper(value, "policy");
        if (!"ALLOW".equals(policy) && !"DENY".equals(policy) && !"MASK".equals(policy)) {
            throw new IllegalArgumentException("unsupported BI column permission policy: " + policy);
        }
        return policy;
    }

    private static String reviewStatus(String value) {
        String status = upper(value, "status");
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("unsupported BI permission request status: " + status);
        }
        return status;
    }

    private static String subjectId(String value) {
        return value == null || value.isBlank() ? "*" : value.trim();
    }

    private static String nullableKey(String value) {
        return value == null || value.isBlank() ? null : key(value);
    }

    private static String key(String value) {
        return BiResourceKey.of(value, "resourceKey").value();
    }

    private static Long requiredResourceId(Long resourceId) {
        if (resourceId == null || resourceId <= 0) {
            throw new IllegalArgumentException("resourceId is required");
        }
        return resourceId;
    }

    private static Long stableId(String key) {
        return (long) (key.hashCode() & 0x7fffffff);
    }

    private static String filtersJson(BiRowPermissionCommand command) {
        if (command.filters() != null && !command.filters().isEmpty()) {
            return json(command.filters());
        }
        return json(command.filter());
    }

    private static String detail(String type, String key, String action, String result) {
        return "{\"resourceType\":\"" + escape(type) + "\",\"resourceKey\":\"" + escape(key)
                + "\",\"action\":\"" + escape(action) + "\",\"result\":\"" + escape(result) + "\"}";
    }

    private static String json(Object value) {
        if (value == null) {
            return "{}";
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> "\"" + escape(String.valueOf(entry.getKey())) + "\":" + jsonValue(entry.getValue()))
                    .reduce((left, right) -> left + "," + right)
                    .map(body -> "{" + body + "}")
                    .orElse("{}");
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(BiPermissionAdministrationCatalog::jsonValue)
                    .reduce((left, right) -> left + "," + right)
                    .map(body -> "[" + body + "]")
                    .orElse("[]");
        }
        return jsonValue(value);
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> || value instanceof List<?>) {
            return json(value);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
