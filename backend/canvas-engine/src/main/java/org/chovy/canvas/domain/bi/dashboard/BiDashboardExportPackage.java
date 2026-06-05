package org.chovy.canvas.domain.bi.dashboard;

import java.time.LocalDateTime;

public record BiDashboardExportPackage(
        String resourceType,
        Integer schemaVersion,
        String sourceDashboardKey,
        Integer sourceVersion,
        BiDashboardPreset preset,
        String exportedBy,
        LocalDateTime exportedAt) {
}
