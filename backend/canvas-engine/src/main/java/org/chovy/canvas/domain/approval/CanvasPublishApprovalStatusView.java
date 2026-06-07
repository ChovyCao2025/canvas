package org.chovy.canvas.domain.approval;

import java.util.List;

public record CanvasPublishApprovalStatusView(
        Long canvasId,
        Long draftVersionId,
        boolean approvalRequired,
        String riskLevel,
        List<String> riskReasons,
        ApprovalInstanceView latestApproved) {
}
