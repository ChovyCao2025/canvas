package org.chovy.canvas.domain.bi.dashboard;

public record BiDashboardImportCommand(
        BiDashboardExportPackage packagePayload,
        String dashboardKey,
        String title,
        Boolean overwrite) {
}
