package org.chovy.canvas.domain.bi.dashboard;

public record BiDashboardFilter(
        String filterKey,
        String fieldKey,
        String label,
        String controlType,
        boolean required,
        String defaultValue
) {
}
