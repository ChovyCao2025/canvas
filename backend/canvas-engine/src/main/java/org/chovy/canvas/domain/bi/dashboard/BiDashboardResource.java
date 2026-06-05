package org.chovy.canvas.domain.bi.dashboard;

public record BiDashboardResource(
        BiDashboardPreset preset,
        String status,
        int version,
        String source
) {
}
