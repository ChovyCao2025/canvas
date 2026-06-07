package org.chovy.canvas.domain.approval;

import java.util.Map;

public record ApprovalExternalSubmissionResult(
        String externalInstanceId,
        Map<Long, String> externalTaskIdsByLocalTaskId) {
}
