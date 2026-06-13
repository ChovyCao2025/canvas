package org.chovy.canvas.bi.domain;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record BiAccessRequest(
        Long tenantId,
        Long workspaceId,
        String resourceType,
        Long resourceId,
        String actor,
        Set<String> roles,
        String actionKey
) {
    public BiAccessRequest {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        resourceType = BiPermissionGrant.requiredUpper(resourceType, "resourceType");
        if (resourceId == null || resourceId <= 0) {
            throw new IllegalArgumentException("resourceId is required");
        }
        actor = actor == null || actor.isBlank() ? "system" : actor.trim();
        roles = roles == null ? Set.of() : roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        actionKey = BiPermissionGrant.requiredUpper(actionKey, "actionKey");
    }
}
