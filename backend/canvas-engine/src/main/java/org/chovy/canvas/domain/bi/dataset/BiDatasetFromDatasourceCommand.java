package org.chovy.canvas.domain.bi.dataset;

import java.util.List;
import java.util.Map;

/**
 * BiDatasetFromDatasourceCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param dataSourceConfigId dataSourceConfigId 字段。
 * @param tableName tableName 字段。
 * @param datasetKey datasetKey 字段。
 * @param name name 字段。
 * @param tenantColumn tenantColumn 字段。
 * @param selectedColumns selectedColumns 字段。
 * @param apiResponseVariables apiResponseVariables 字段。
 */
public record BiDatasetFromDatasourceCommand(
        Long dataSourceConfigId,
        String tableName,
        String datasetKey,
        String name,
        String tenantColumn,
        List<String> selectedColumns,
        Map<String, String> apiResponseVariables
) {
    public BiDatasetFromDatasourceCommand {
        selectedColumns = selectedColumns == null ? List.of() : List.copyOf(selectedColumns);
        apiResponseVariables = apiResponseVariables == null ? Map.of() : Map.copyOf(apiResponseVariables);
    }

    /**
     * 创建 BiDatasetFromDatasourceCommand 实例并注入 domain.bi.dataset 场景依赖。
     * @param dataSourceConfigId 业务对象 ID，用于定位具体记录。
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param tenantColumn tenant column 参数，用于 BiDatasetFromDatasourceCommand 流程中的校验、计算或对象转换。
     * @param selectedColumns selected columns 参数，用于 BiDatasetFromDatasourceCommand 流程中的校验、计算或对象转换。
     */
    public BiDatasetFromDatasourceCommand(Long dataSourceConfigId,
                                          String tableName,
                                          String datasetKey,
                                          String name,
                                          String tenantColumn,
                                          List<String> selectedColumns) {
        this(dataSourceConfigId, tableName, datasetKey, name, tenantColumn, selectedColumns, Map.of());
    }
}
