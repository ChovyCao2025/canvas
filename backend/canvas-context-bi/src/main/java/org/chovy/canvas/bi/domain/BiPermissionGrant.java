package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.Locale;

public record BiPermissionGrant(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        Long resourceId,
        String subjectType,
        String subjectId,
        String actionKey,
        String effect,
        String createdBy,
        LocalDateTime createdAt
) {
    public BiPermissionGrant {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        resourceType = requiredUpper(resourceType, "resourceType");
        if (resourceId == null || resourceId <= 0) {
            throw new IllegalArgumentException("resourceId is required");
        }
        subjectType = requiredUpper(subjectType, "subjectType");
        subjectId = subjectId == null || subjectId.isBlank() ? "*" : subjectId.trim();
        actionKey = requiredUpper(actionKey, "actionKey");
        effect = requiredUpper(effect, "effect");
        if (!"ALLOW".equals(effect) && !"DENY".equals(effect)) {
            throw new IllegalArgumentException("unsupported BI permission effect: " + effect);
        }
    }

    public BiPermissionGrant withId(Long newId) {
        return new BiPermissionGrant(newId, tenantId, workspaceId, resourceType, resourceId, subjectType, subjectId,
                actionKey, effect, createdBy, createdAt);
    }

    static String requiredUpper(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
