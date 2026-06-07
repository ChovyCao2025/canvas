package org.chovy.canvas.domain.bi.dataset;

import java.util.List;

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
