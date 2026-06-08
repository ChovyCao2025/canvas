package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

/**
 * BiSqlDatasetLineageView 承载 domain.bi.dataset 场景中的不可变数据快照。
 * @param dataSourceConfigId dataSourceConfigId 字段。
 * @param sourceTables sourceTables 字段。
 * @param parameterKeys parameterKeys 字段。
 * @param tenantColumn tenantColumn 字段。
 * @param referencedFields referencedFields 字段。
 * @param referencedMetrics referencedMetrics 字段。
 * @param approvalRequired approvalRequired 字段。
 */
public record BiSqlDatasetLineageView(
        Long dataSourceConfigId,
        List<String> sourceTables,
        List<String> parameterKeys,
        String tenantColumn,
        List<String> referencedFields,
        List<String> referencedMetrics,
        boolean approvalRequired
) {
    public BiSqlDatasetLineageView {
        sourceTables = sourceTables == null ? List.of() : List.copyOf(sourceTables);
        parameterKeys = parameterKeys == null ? List.of() : List.copyOf(parameterKeys);
        referencedFields = referencedFields == null ? List.of() : List.copyOf(referencedFields);
        referencedMetrics = referencedMetrics == null ? List.of() : List.copyOf(referencedMetrics);
    }
}
