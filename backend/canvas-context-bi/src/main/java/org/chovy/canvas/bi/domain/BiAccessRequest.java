package org.chovy.canvas.bi.domain;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * BiAccessRequest 不可变数据载体。
 */
public record BiAccessRequest(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间标识。
         */
        Long workspaceId,
        /**
         * 资源类型。
         */
        String resourceType,
        /**
         * 资源标识。
         */
        Long resourceId,
        /**
         * 操作者。
         */
        String actor,
        /**
         * roles 对应的数据集合。
         */
        Set<String> roles,
        /**
         * 操作键。
         */
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
