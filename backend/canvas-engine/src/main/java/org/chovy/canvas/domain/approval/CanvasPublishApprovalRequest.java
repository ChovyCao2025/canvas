package org.chovy.canvas.domain.approval;

/**
 * CanvasPublishApprovalRequest 承载 domain.approval 场景中的不可变数据快照。
 * @param reason reason 字段。
 * @param larkOpenId larkOpenId 字段。
 * @param larkUserId larkUserId 字段。
 * @param larkDepartmentId larkDepartmentId 字段。
 */
public record CanvasPublishApprovalRequest(String reason,
                                           String larkOpenId,
                                           String larkUserId,
                                           String larkDepartmentId) {
    /**
     * 创建 CanvasPublishApprovalRequest 实例并注入 domain.approval 场景依赖。
     * @param reason 原因说明，用于记录状态变化的业务依据。
     */
    public CanvasPublishApprovalRequest(String reason) {
        this(reason, null, null, null);
    }
}
