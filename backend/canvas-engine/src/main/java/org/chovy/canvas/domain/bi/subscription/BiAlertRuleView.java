package org.chovy.canvas.domain.bi.subscription;

import java.time.LocalDateTime;
import java.util.Map;

public record BiAlertRuleView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String alertKey,
        String name,
        String datasetKey,
        Long datasetId,
        String metricKey,
        Map<String, Object> condition,
        Map<String, Object> receivers,
        Boolean enabled,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiAlertRuleView {
        condition = condition == null ? Map.of() : Map.copyOf(condition);
        receivers = receivers == null ? Map.of() : Map.copyOf(receivers);
    }
}
