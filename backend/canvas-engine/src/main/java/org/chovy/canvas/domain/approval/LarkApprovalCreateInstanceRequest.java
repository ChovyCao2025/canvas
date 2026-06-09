package org.chovy.canvas.domain.approval;

/**
 * LarkApprovalCreateInstanceRequest 承载 domain.approval 场景中的不可变数据快照。
 * @param tenantId tenantId 字段。
 * @param approvalCode approvalCode 字段。
 * @param uuid uuid 字段。
 * @param openId openId 字段。
 * @param userId userId 字段。
 * @param departmentId departmentId 字段。
 * @param form form 字段。
 */
public record LarkApprovalCreateInstanceRequest(
        Long tenantId,
        String approvalCode,
        String uuid,
        String openId,
        String userId,
        String departmentId,
        String form) {
}
