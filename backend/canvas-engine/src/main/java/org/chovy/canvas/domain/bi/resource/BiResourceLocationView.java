package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

/**
 * BiResourceLocationView 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param folderKey folderKey 字段。
 * @param sortOrder sortOrder 字段。
 * @param movedBy movedBy 字段。
 * @param movedAt movedAt 字段。
 */
public record BiResourceLocationView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String folderKey,
        Integer sortOrder,
        String movedBy,
        LocalDateTime movedAt) {
}
