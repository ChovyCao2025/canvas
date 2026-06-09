package org.chovy.canvas.domain.approval;

import java.util.List;

/**
 * CanvasPublishApprovalStatusView 承载 domain.approval 场景中的不可变数据快照。
 * @param canvasId canvasId 字段。
 * @param draftVersionId draftVersionId 字段。
 * @param approvalRequired approvalRequired 字段。
 * @param riskLevel riskLevel 字段。
 * @param riskReasons riskReasons 字段。
 * @param latestApproved latestApproved 字段。
 */
public record CanvasPublishApprovalStatusView(
        Long canvasId,
        Long draftVersionId,
        boolean approvalRequired,
        String riskLevel,
        List<String> riskReasons,
        ApprovalInstanceView latestApproved) {
}
