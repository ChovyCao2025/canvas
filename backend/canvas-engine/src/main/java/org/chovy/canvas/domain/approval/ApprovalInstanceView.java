package org.chovy.canvas.domain.approval;

import java.time.LocalDateTime;
import java.util.List;

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
