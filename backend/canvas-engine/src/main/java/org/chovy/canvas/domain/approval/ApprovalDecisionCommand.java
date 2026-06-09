package org.chovy.canvas.domain.approval;

/**
 * ApprovalDecisionCommand 承载 domain.approval 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param taskId taskId 字段。
 * @param actor actor 字段。
 * @param actorRole actorRole 字段。
 * @param comment comment 字段。
 */
public record ApprovalDecisionCommand(
        Long tenantId,
        Long taskId,
        String actor,
        String actorRole,
        String comment) {
}
