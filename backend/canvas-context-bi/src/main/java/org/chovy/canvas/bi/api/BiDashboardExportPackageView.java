package org.chovy.canvas.bi.api;

import java.time.LocalDateTime;
import java.util.Map;

public record BiDashboardExportPackageView(
        String resourceType,
        String sourceDashboardKey,
        BiDashboardView dashboard,
        Map<String, Object> manifest,
        LocalDateTime exportedAt,
        String exportedBy
) {
    public BiDashboardExportPackageView {
        manifest = manifest == null ? Map.of() : Map.copyOf(manifest);
    }
}
