package org.chovy.canvas.domain.approval;

import java.util.List;

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
