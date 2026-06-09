package org.chovy.canvas.domain.approval;

/**
 * LarkApprovalTaskActionRequest 承载 domain.approval 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param instanceCode instanceCode 字段。
 * @param taskId taskId 字段。
 * @param actor actor 字段。
 * @param comment comment 字段。
 */
public record LarkApprovalTaskActionRequest(
        Long tenantId,
        String instanceCode,
        String taskId,
        String actor,
        String comment) {
}
