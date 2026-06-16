package org.chovy.canvas.bi.api;

import java.util.List;
import java.util.Map;
/**
 * BiDashboardPresetFilterView 视图。
 */
public record BiDashboardPresetFilterView(
        /**
         * filterKey 对应的业务键。
         */
        String filterKey,
        /**
         * fieldKey 对应的业务键。
         */
        String fieldKey,
        /**
         * label 字段值。
         */
        String label,
        /**
         * controlType 字段值。
         */
        String controlType,
        /**
         * required 字段值。
         */
        boolean required,
        /**
         * defaultValue 字段值。
         */
        String defaultValue,
        /**
         * targetWidgetKeys 对应的数据集合。
         */
        List<String> targetWidgetKeys,
        /**
         * parentFilterKeys 对应的数据集合。
         */
        List<String> parentFilterKeys,
        /**
         * parentFieldMapping 字段值。
         */
        Map<String, String> parentFieldMapping,
        /**
         * cascadeMode 字段值。
         */
        String cascadeMode,
        /**
         * optionDatasetKey 对应的业务键。
         */
        String optionDatasetKey,
        /**
         * optionFieldKey 对应的业务键。
         */
        String optionFieldKey,
        boolean hidden) {

    public BiDashboardPresetFilterView {
        targetWidgetKeys = targetWidgetKeys == null ? List.of() : List.copyOf(targetWidgetKeys);
        parentFilterKeys = parentFilterKeys == null ? List.of() : List.copyOf(parentFilterKeys);
        parentFieldMapping = parentFieldMapping == null ? Map.of() : Map.copyOf(parentFieldMapping);
        cascadeMode = cascadeMode == null || cascadeMode.isBlank() ? "SAME_SOURCE" : cascadeMode;
    }
}
