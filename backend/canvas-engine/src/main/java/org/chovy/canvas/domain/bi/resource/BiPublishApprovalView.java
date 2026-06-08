package org.chovy.canvas.domain.bi.resource;

import java.time.LocalDateTime;

/**
 * BiPublishApprovalView 承载 domain.bi.resource 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param status status 字段。
 * @param reason reason 字段。
 * @param requestedBy requestedBy 字段。
 * @param requestedAt requestedAt 字段。
 * @param reviewedBy reviewedBy 字段。
 * @param reviewedAt reviewedAt 字段。
 * @param reviewComment reviewComment 字段。
 */
public record BiPublishApprovalView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String status,
        String reason,
        String requestedBy,
        LocalDateTime requestedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment) {
}
