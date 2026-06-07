package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;
import java.util.Map;

public record BiDashboardFilterCascade(
        List<String> parentFilterKeys,
        Map<String, String> parentFieldMapping,
        String mode
) {
    public BiDashboardFilterCascade {
        parentFilterKeys = parentFilterKeys == null ? List.of() : List.copyOf(parentFilterKeys);
        parentFieldMapping = parentFieldMapping == null ? Map.of() : Map.copyOf(parentFieldMapping);
        mode = mode == null || mode.isBlank() ? "SAME_SOURCE" : mode;
    }
}
