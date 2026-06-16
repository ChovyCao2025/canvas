package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
/**
 * BiWorkspace 工作空间模型。
 */
public record BiWorkspace(
        /**
         * 唯一标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工作空间键。
         */
        BiResourceKey workspaceKey,
        /**
         * 展示名称。
         */
        String name,
        /**
         * 说明文本。
         */
        String description,
        /**
         * 状态值。
         */
        BiResourceStatus status,
        /**
         * 创建人。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt
) {
    public BiWorkspace {
        tenantId = tenantId == null ? 0L : tenantId;
        workspaceKey = workspaceKey == null ? BiResourceKey.of(name, "workspaceKey") : workspaceKey;
        name = textOrDefault(name, workspaceKey.value());
        status = status == null ? BiResourceStatus.DRAFT : status;
    }
    /**
     * 返回带有指定变更的新对象。
     */
    public BiWorkspace withId(Long newId) {
        return new BiWorkspace(newId, tenantId, workspaceKey, name, description, status, createdBy, createdAt, updatedAt);
    }
    /**
     * 执行 text Or Default 相关处理。
     */
    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
