package org.chovy.canvas.bi.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record BiDashboard(
        Long id,
        Long tenantId,
        Long workspaceId,
        BiResourceKey dashboardKey,
        String name,
        String description,
        Map<String, Object> theme,
        Map<String, Object> filters,
        List<String> chartKeys,
        BiResourceStatus status,
        int version,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public BiDashboard {
        tenantId = tenantId == null ? 0L : tenantId;
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        if (dashboardKey == null) {
            throw new IllegalArgumentException("dashboardKey is required");
        }
        name = name == null || name.isBlank() ? dashboardKey.value() : name.trim();
        theme = theme == null ? Map.of() : Map.copyOf(theme);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        chartKeys = chartKeys == null ? List.of() : List.copyOf(chartKeys);
        status = status == null ? BiResourceStatus.DRAFT : status;
        version = Math.max(version, 1);
    }

    public BiDashboard withId(Long newId) {
        return new BiDashboard(newId, tenantId, workspaceId, dashboardKey, name, description, theme, filters,
                chartKeys, status, version, createdBy, createdAt, updatedAt);
    }
}
