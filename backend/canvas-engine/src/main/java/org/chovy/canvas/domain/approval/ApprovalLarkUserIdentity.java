package org.chovy.canvas.domain.approval;

/**
 * ApprovalLarkUserIdentity 承载 domain.approval 场景中的不可变数据快照。
 * @param openId openId 字段。
 * @param userId userId 字段。
 * @param departmentId departmentId 字段。
 */
public record ApprovalLarkUserIdentity(
        String openId,
        String userId,
        String departmentId) {
}
