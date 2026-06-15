package org.chovy.canvas.execution.api;

import java.util.List;
import java.util.Map;

public interface ExecutionRerunFacade {

    RerunResult rerun(Long tenantId, String operator, boolean admin, Long canvasId, RerunCommand command);

    AuditRow audit(Long tenantId, Long id);

    List<AuditRow> audits(Long tenantId, Long canvasId);

    record RerunCommand(
            String mode,
            String reason,
            String userId,
            Long testUserId,
            String originalExecutionId,
            Map<String, Object> inputParams,
            Map<String, Object> graphJson) {
    }

    record RerunResult(Long auditId, String mode, String status, Map<String, Object> execution) {
    }

    record AuditRow(
            Long id,
            Long tenantId,
            Long canvasId,
            String userId,
            Long testUserId,
            String originalExecutionId,
            String mode,
            String reason,
            String operator,
            String status,
            Map<String, Object> inputParams,
            String createdAt,
            String updatedAt) {
    }
}
