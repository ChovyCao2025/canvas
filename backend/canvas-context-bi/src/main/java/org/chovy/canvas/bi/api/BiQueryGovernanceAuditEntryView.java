package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiQueryGovernanceAuditEntryView(
        Long id,
        Long tenantId,
        String actionKey,
        String datasetKey,
        String actor,
        Map<String, Object> detail,
        LocalDateTime createdAt) {

    public BiQueryGovernanceAuditEntryView {
        detail = detail == null ? Map.of() : Map.copyOf(detail);
    }
}
