package org.chovy.canvas.domain.bi.query;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.Map;

/**
 * BiQueryRequest 承载 domain.bi.query 场景中的不可变数据快照。
 * @param datasetKey datasetKey 字段。
 * @param dashboardKey dashboardKey 字段。
 * @param dimensions dimensions 字段。
 * @param metrics metrics 字段。
 * @param filters filters 字段。
 * @param sorts sorts 字段。
 * @param limit limit 字段。
 * @param offset offset 字段。
 * @param sqlParameters sqlParameters 字段。
 */
public record BiQueryRequest(
        String datasetKey,
        String dashboardKey,
        List<String> dimensions,
        List<String> metrics,
        List<BiFilter> filters,
        @JsonAlias("sort")
        List<BiSort> sorts,
        int limit,
        int offset,
        Map<String, String> sqlParameters
) {
    public BiQueryRequest {
        dashboardKey = dashboardKey == null || dashboardKey.isBlank() ? null : dashboardKey.trim();
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        metrics = metrics == null ? List.of() : List.copyOf(metrics);
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        offset = Math.max(0, offset);
        sqlParameters = sqlParameters == null ? Map.of() : Map.copyOf(sqlParameters);
    }

    /**
     * 执行 BiQueryRequest 流程，围绕 bi query request 完成校验、计算或结果组装。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param dimensions dimensions 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param sorts sorts 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     */
    public BiQueryRequest(String datasetKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit) {
        this(datasetKey, null, dimensions, metrics, filters, sorts, limit, 0, Map.of());
    }

    /**
     * 创建 BiQueryRequest 实例并注入 domain.bi.query 场景依赖。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param dimensions dimensions 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param sorts sorts 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param offset 分页或数量限制，避免一次处理过多数据。
     */
    public BiQueryRequest(String datasetKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit,
                          int offset) {
        this(datasetKey, null, dimensions, metrics, filters, sorts, limit, offset, Map.of());
    }

    /**
     * 创建 BiQueryRequest 实例并注入 domain.bi.query 场景依赖。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param dimensions dimensions 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param sorts sorts 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     */
    public BiQueryRequest(String datasetKey,
                          String dashboardKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit) {
        this(datasetKey, dashboardKey, dimensions, metrics, filters, sorts, limit, 0, Map.of());
    }

    /**
     * 创建 BiQueryRequest 实例并注入 domain.bi.query 场景依赖。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param dimensions dimensions 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param filters filters 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param sorts sorts 参数，用于 BiQueryRequest 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param offset 分页或数量限制，避免一次处理过多数据。
     */
    public BiQueryRequest(String datasetKey,
                          String dashboardKey,
                          List<String> dimensions,
                          List<String> metrics,
                          List<BiFilter> filters,
                          List<BiSort> sorts,
                          int limit,
                          int offset) {
        this(datasetKey, dashboardKey, dimensions, metrics, filters, sorts, limit, offset, Map.of());
    }
}
