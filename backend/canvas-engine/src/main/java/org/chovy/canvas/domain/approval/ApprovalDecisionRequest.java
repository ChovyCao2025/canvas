package org.chovy.canvas.domain.approval;

/**
 * ApprovalDecisionRequest 承载 domain.approval 场景中的不可变数据快照。
 * @param comment comment 字段。
 */
public record ApprovalDecisionRequest(String comment) {
}
