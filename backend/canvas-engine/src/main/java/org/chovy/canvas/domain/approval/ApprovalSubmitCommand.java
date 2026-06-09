package org.chovy.canvas.domain.approval;

import java.util.List;

/**
 * ApprovalSubmitCommand 承载 domain.approval 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param definitionKey definitionKey 字段。
 * @param domain domain 字段。
 * @param targetType targetType 字段。
 * @param targetId targetId 字段。
 * @param targetVersionId targetVersionId 字段。
 * @param submitter submitter 字段。
 * @param submitReason submitReason 字段。
 * @param riskLevel riskLevel 字段。
 * @param riskReasonsJson riskReasonsJson 字段。
 * @param snapshotJson snapshotJson 字段。
 * @param approvers approvers 字段。
 * @param dueHours dueHours 字段。
 * @param autoAction autoAction 字段。
 */
public record ApprovalSubmitCommand(
        Long tenantId,
        String definitionKey,
        String domain,
        String targetType,
        String targetId,
        Long targetVersionId,
        String submitter,
        String submitReason,
        String riskLevel,
        String riskReasonsJson,
        String snapshotJson,
        List<String> approvers,
        Integer dueHours,
        String autoAction) {
}
