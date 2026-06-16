package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.Locale;
/**
 * BiPermissionGrant 不可变数据载体。
 */
public record BiPermissionGrant(
        /**
         * 唯一标识。
         */
        Long id,
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
         * subjectType 字段值。
         */
        String subjectType,
        /**
         * subjectId 对应的标识。
         */
        String subjectId,
        /**
         * actionKey 对应的业务键。
         */
        String actionKey,
        /**
         * effect 字段值。
         */
        String effect,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
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
    /**
     * 返回带有指定变更的新对象。
     */
    public BiPermissionGrant withId(Long newId) {
        return new BiPermissionGrant(newId, tenantId, workspaceId, resourceType, resourceId, subjectType, subjectId,
                actionKey, effect, createdBy, createdAt);
    }
    /**
     * 执行 required Upper 相关处理。
     */
    static String requiredUpper(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
