package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

/**
 * BiResourceLockView 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param lockToken lockToken 字段。
 * @param lockedBy lockedBy 字段。
 * @param lockedAt lockedAt 字段。
 * @param expiresAt expiresAt 字段。
 * @param locked locked 字段。
 */
public record BiResourceLockView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String lockToken,
        String lockedBy,
        LocalDateTime lockedAt,
        LocalDateTime expiresAt,
        Boolean locked) {
}
