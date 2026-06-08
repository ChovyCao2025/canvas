package org.chovy.canvas.domain.bi.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BiDatasetSpec 承载 domain.bi.query 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param tableExpression tableExpression 字段。
 * @param tenantColumn tenantColumn 字段。
 * @param fields fields 字段。
 * @param metrics metrics 字段。
 * @param sqlParameters sqlParameters 字段。
 * @param model model 字段。
 */
public record BiDatasetSpec(
        String datasetKey,
        String tableExpression,
        String tenantColumn,
        Map<String, BiFieldSpec> fields,
        Map<String, BiMetricSpec> metrics,
        List<BiSqlParameterSpec> sqlParameters,
        Map<String, Object> model
) {
    public BiDatasetSpec {
        fields = Map.copyOf(fields);
        metrics = Map.copyOf(metrics);
        sqlParameters = sqlParameters == null ? List.of() : List.copyOf(sqlParameters);
        model = model == null || model.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(model));
    }

    /**
     * 创建 BiDatasetSpec 实例并注入 domain.bi.query 场景依赖。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param tableExpression table expression 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param fields fields 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param sqlParameters sql parameters 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     */
    public BiDatasetSpec(String datasetKey,
                         String tableExpression,
                         String tenantColumn,
                         Map<String, BiFieldSpec> fields,
                         Map<String, BiMetricSpec> metrics,
                         List<BiSqlParameterSpec> sqlParameters) {
        this(datasetKey, tableExpression, tenantColumn, fields, metrics, sqlParameters, Map.of());
    }

    /**
     * 创建 BiDatasetSpec 实例并注入 domain.bi.query 场景依赖。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param tableExpression table expression 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param fields fields 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 BiDatasetSpec 流程中的校验、计算或对象转换。
     */
    public BiDatasetSpec(String datasetKey,
                         String tableExpression,
                         String tenantColumn,
                         Map<String, BiFieldSpec> fields,
                         Map<String, BiMetricSpec> metrics) {
        this(datasetKey, tableExpression, tenantColumn, fields, metrics, List.of());
    }
}
