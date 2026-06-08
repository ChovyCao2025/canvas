package org.chovy.canvas.domain.bi.dashboard;

import java.util.List;

/**
 * BiDashboardFilter 承载 domain.bi.dashboard 场景中的不可变数据快照。
 * @param filterKey filterKey 字段。
 * @param fieldKey fieldKey 字段。
 * @param label label 字段。
 * @param controlType controlType 字段。
 * @param required required 字段。
 * @param defaultValue defaultValue 字段。
 * @param targetWidgetKeys targetWidgetKeys 字段。
 * @param cascade cascade 字段。
 * @param optionDatasetKey optionDatasetKey 字段。
 * @param optionFieldKey optionFieldKey 字段。
 * @param hidden hidden 字段。
 */
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

    /**
     * 创建 BiDashboardFilter 实例并注入 domain.bi.dashboard 场景依赖。
     * @param filterKey 业务键，用于在同一租户下定位资源。
     * @param fieldKey 业务键，用于在同一租户下定位资源。
     * @param label label 参数，用于 BiDashboardFilter 流程中的校验、计算或对象转换。
     * @param controlType 类型标识，用于选择对应处理分支。
     * @param required required 参数，用于 BiDashboardFilter 流程中的校验、计算或对象转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     */
    public BiDashboardFilter(String filterKey,
                             String fieldKey,
                             String label,
                             String controlType,
                             boolean required,
                             String defaultValue) {
        this(filterKey, fieldKey, label, controlType, required, defaultValue, List.of(), null, null, null, false);
    }
}
