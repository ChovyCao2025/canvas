package org.chovy.canvas.domain.approval;

import java.util.Map;

/**
 * ApprovalExternalSyncResult 承载 domain.approval 场景中的不可变数据快照。
 * @param instanceStatus instanceStatus 字段。
 * @param taskStatusesByExternalTaskId taskStatusesByExternalTaskId 字段。
 */
public record ApprovalExternalSyncResult(
        String instanceStatus,
        Map<String, String> taskStatusesByExternalTaskId) {
}
