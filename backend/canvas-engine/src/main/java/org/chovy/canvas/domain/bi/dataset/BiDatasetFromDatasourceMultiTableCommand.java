package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiDatasetFromDatasourceMultiTableCommand 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param dataSourceConfigId dataSourceConfigId 字段。
 * @param datasetKey datasetKey 字段。
 * @param name name 字段。
 * @param baseTableName baseTableName 字段。
 * @param tenantColumn tenantColumn 字段。
 * @param tables tables 字段。
 * @param joins joins 字段。
 * @param graph graph 字段。
 */
public record BiDatasetFromDatasourceMultiTableCommand(
        Long dataSourceConfigId,
        String datasetKey,
        String name,
        String baseTableName,
        String tenantColumn,
        List<BiDatasetFromDatasourceTableCommand> tables,
        List<BiDatasetFromDatasourceJoinCommand> joins,
        BiDatasetFromDatasourceGraphCommand graph
) {
    /**
     * 创建 BiDatasetFromDatasourceMultiTableCommand 实例并注入 domain.bi.dataset 场景依赖。
     * @param dataSourceConfigId 业务对象 ID，用于定位具体记录。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param name 名称文本，用于展示或唯一性校验。
     * @param baseTableName 名称文本，用于展示或唯一性校验。
     * @param tenantColumn tenant column 参数，用于 BiDatasetFromDatasourceMultiTableCommand 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 BiDatasetFromDatasourceMultiTableCommand 流程中的校验、计算或对象转换。
     * @param joins joins 参数，用于 BiDatasetFromDatasourceMultiTableCommand 流程中的校验、计算或对象转换。
     */
    public BiDatasetFromDatasourceMultiTableCommand(Long dataSourceConfigId,
                                                    String datasetKey,
                                                    String name,
                                                    String baseTableName,
                                                    String tenantColumn,
                                                    List<BiDatasetFromDatasourceTableCommand> tables,
                                                    List<BiDatasetFromDatasourceJoinCommand> joins) {
        this(dataSourceConfigId, datasetKey, name, baseTableName, tenantColumn, tables, joins, null);
    }

    public BiDatasetFromDatasourceMultiTableCommand {
        tables = tables == null ? List.of() : List.copyOf(tables);
        joins = joins == null ? List.of() : List.copyOf(joins);
    }
}
