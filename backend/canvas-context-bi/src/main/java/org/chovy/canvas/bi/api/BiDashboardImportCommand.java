package org.chovy.canvas.bi.api;

public record BiDashboardImportCommand(
        BiDashboardExportPackageView packageView,
        String dashboardKey,
        String name,
        boolean overwrite
) {
}
