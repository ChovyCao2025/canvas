package org.chovy.canvas.domain.bi.dashboard;

import java.time.LocalDateTime;

public record BiDashboardVersionView(
        Long id,
        String dashboardKey,
        Integer version,
        String status,
        BiDashboardPreset preset,
        String publishedBy,
        LocalDateTime createdAt) {
}
