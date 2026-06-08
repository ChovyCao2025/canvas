package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

/**
 * BiResourceCommentView 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param widgetKey widgetKey 字段。
 * @param commentText commentText 字段。
 * @param createdBy createdBy 字段。
 * @param createdAt createdAt 字段。
 * @param deletedAt deletedAt 字段。
 */
public record BiResourceCommentView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String widgetKey,
        String commentText,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
}
