package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

public record BiDashboardFilter(
        String filterKey,
        String fieldKey,
        String label,
        String controlType,
        boolean required,
        String defaultValue,
        List<String> targetWidgetKeys,
        BiDashboardFilterCascade cascade,
        String optionDatasetKey,
        String optionFieldKey,
        boolean hidden
) {
    public BiDashboardFilter {
        targetWidgetKeys = targetWidgetKeys == null ? List.of() : List.copyOf(targetWidgetKeys);
    }

    public BiDashboardFilter(String filterKey,
                             String fieldKey,
                             String label,
                             String controlType,
                             boolean required,
                             String defaultValue) {
        this(filterKey, fieldKey, label, controlType, required, defaultValue, List.of(), null, null, null, false);
    }
}
