package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;

public record BiDashboardPresetFilterView(
        String filterKey,
        String fieldKey,
        String label,
        String controlType,
        boolean required,
        String defaultValue,
        List<String> targetWidgetKeys,
        List<String> parentFilterKeys,
        Map<String, String> parentFieldMapping,
        String cascadeMode,
        String optionDatasetKey,
        String optionFieldKey,
        boolean hidden) {

    public BiDashboardPresetFilterView {
        targetWidgetKeys = targetWidgetKeys == null ? List.of() : List.copyOf(targetWidgetKeys);
        parentFilterKeys = parentFilterKeys == null ? List.of() : List.copyOf(parentFilterKeys);
        parentFieldMapping = parentFieldMapping == null ? Map.of() : Map.copyOf(parentFieldMapping);
        cascadeMode = cascadeMode == null || cascadeMode.isBlank() ? "SAME_SOURCE" : cascadeMode;
    }
}
