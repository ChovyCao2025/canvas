package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

/**
 * BiResourceOwnershipView 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param ownerUser ownerUser 字段。
 * @param transferredBy transferredBy 字段。
 * @param transferredAt transferredAt 字段。
 */
public record BiResourceOwnershipView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String ownerUser,
        String transferredBy,
        LocalDateTime transferredAt) {
}
