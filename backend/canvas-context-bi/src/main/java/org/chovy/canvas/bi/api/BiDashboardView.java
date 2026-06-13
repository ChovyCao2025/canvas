package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiDashboardView(
        Long id,
        Long tenantId,
        Long workspaceId,
        String dashboardKey,
        String name,
        String description,
        Map<String, Object> theme,
        Map<String, Object> filters,
        List<String> chartKeys,
        String status,
        int version,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiDashboardView {
        theme = theme == null ? Map.of() : Map.copyOf(theme);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        chartKeys = chartKeys == null ? List.of() : List.copyOf(chartKeys);
    }
}
