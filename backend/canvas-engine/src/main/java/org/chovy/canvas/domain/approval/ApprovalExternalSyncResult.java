package org.chovy.canvas.domain.approval;

import java.util.Map;

public record ApprovalExternalSyncResult(
        String instanceStatus,
        Map<String, String> taskStatusesByExternalTaskId) {
}
