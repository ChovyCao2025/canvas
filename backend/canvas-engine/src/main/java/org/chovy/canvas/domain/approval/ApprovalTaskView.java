package org.chovy.canvas.domain.approval;

import java.time.LocalDateTime;

/**
 * ApprovalTaskView 承载 domain.approval 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param instanceId instanceId 字段。
 * @param stepNo stepNo 字段。
 * @param approver approver 字段。
 * @param status status 字段。
 * @param externalTaskId externalTaskId 字段。
 * @param dueAt dueAt 字段。
 * @param actedAt actedAt 字段。
 * @param actionComment actionComment 字段。
 */
public record ApprovalTaskView(
        Long id,
        Long tenantId,
        Long instanceId,
        Integer stepNo,
        String approver,
        String status,
        String externalTaskId,
        LocalDateTime dueAt,
        LocalDateTime actedAt,
        String actionComment) {
}
