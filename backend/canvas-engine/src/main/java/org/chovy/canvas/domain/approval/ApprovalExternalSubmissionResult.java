package org.chovy.canvas.domain.approval;

import java.util.Map;

/**
 * ApprovalExternalSubmissionResult 承载 domain.approval 场景中的不可变数据快照。
 * @param externalInstanceId externalInstanceId 字段。
 * @param externalTaskIdsByLocalTaskId externalTaskIdsByLocalTaskId 字段。
 */
public record ApprovalExternalSubmissionResult(
        String externalInstanceId,
        Map<Long, String> externalTaskIdsByLocalTaskId) {
}
