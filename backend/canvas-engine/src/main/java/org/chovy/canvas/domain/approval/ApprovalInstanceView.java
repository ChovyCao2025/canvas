package org.chovy.canvas.domain.approval;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ApprovalInstanceView 承载 domain.approval 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param definitionKey definitionKey 字段。
 * @param domain domain 字段。
 * @param targetType targetType 字段。
 * @param targetId targetId 字段。
 * @param targetVersionId targetVersionId 字段。
 * @param status status 字段。
 * @param submitter submitter 字段。
 * @param submitReason submitReason 字段。
 * @param riskLevel riskLevel 字段。
 * @param riskReasonsJson riskReasonsJson 字段。
 * @param snapshotJson snapshotJson 字段。
 * @param externalInstanceId externalInstanceId 字段。
 * @param requestedAt requestedAt 字段。
 * @param completedAt completedAt 字段。
 * @param completedBy completedBy 字段。
 * @param resultComment resultComment 字段。
 * @param autoAction autoAction 字段。
 * @param autoActionStatus autoActionStatus 字段。
 * @param autoActionError autoActionError 字段。
 * @param pendingTasks pendingTasks 字段。
 */
public record ApprovalInstanceView(
        Long id,
        Long tenantId,
        String definitionKey,
        String domain,
        String targetType,
        String targetId,
        Long targetVersionId,
        String status,
        String submitter,
        String submitReason,
        String riskLevel,
        String riskReasonsJson,
        String snapshotJson,
        String externalInstanceId,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String completedBy,
        String resultComment,
        String autoAction,
        String autoActionStatus,
        String autoActionError,
        List<ApprovalTaskView> pendingTasks) {
}
