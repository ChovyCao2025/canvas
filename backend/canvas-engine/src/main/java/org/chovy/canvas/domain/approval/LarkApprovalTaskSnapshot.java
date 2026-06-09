package org.chovy.canvas.domain.approval;

/**
 * LarkApprovalTaskSnapshot 承载 domain.approval 场景中的不可变数据快照。
 * @param taskId taskId 字段。
 * @param status status 字段。
 * @param userId userId 字段。
 */
public record LarkApprovalTaskSnapshot(
        String taskId,
        String status,
        String userId) {
    /**
     * 创建 LarkApprovalTaskSnapshot 实例并注入 domain.approval 场景依赖。
     * @param taskId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     */
    public LarkApprovalTaskSnapshot(String taskId, String status) {
        this(taskId, status, null);
    }
}
