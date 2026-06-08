package org.chovy.canvas.domain.bi.permission;

import java.time.LocalDateTime;

/**
 * BiPermissionRequestView 承载 domain.bi.permission 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param workspaceId workspaceId 字段。
 * @param resourceType resourceType 字段。
 * @param resourceKey resourceKey 字段。
 * @param requestedAction requestedAction 字段。
 * @param requestedBy requestedBy 字段。
 * @param requestedAt requestedAt 字段。
 * @param reason reason 字段。
 * @param status status 字段。
 * @param reviewedBy reviewedBy 字段。
 * @param reviewedAt reviewedAt 字段。
 * @param reviewComment reviewComment 字段。
 * @param grantedPermissionId grantedPermissionId 字段。
 */
public record BiPermissionRequestView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String resourceType,
        String resourceKey,
        String requestedAction,
        String requestedBy,
        LocalDateTime requestedAt,
        String reason,
        String status,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment,
        Long grantedPermissionId) {
}
