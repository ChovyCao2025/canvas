package org.chovy.canvas.domain.bi.dashboard;

import java.time.LocalDateTime;
import java.util.Map;

public record BiDashboardRuntimeStateView(
        String dashboardKey,
        String username,
        Map<String, Object> parameters,
        LocalDateTime updatedAt) {

    public BiDashboardRuntimeStateView {
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
}
