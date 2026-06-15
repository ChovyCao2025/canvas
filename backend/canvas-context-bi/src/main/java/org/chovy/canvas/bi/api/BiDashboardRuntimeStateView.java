package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiDashboardRuntimeStateView(
        Long tenantId,
        String dashboardKey,
        Map<String, Object> parameters,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public BiDashboardRuntimeStateView {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
