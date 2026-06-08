package org.chovy.canvas.domain.bi.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * BiQueryCompiler 编排 domain.bi.query 场景的领域业务规则。
 */
public class BiQueryCompiler {

    private static final int MAX_LIMIT = 10000;

    /**
     * 将受控 BI 查询请求编译为参数化 SQL。
     *
     * <p>编译过程会校验数据集 key、租户 ID、limit/offset、维度、指标、SQL 参数和指标允许维度，
     * 并强制追加租户列过滤条件；过滤条件和数据集参数均进入参数列表，避免调用方拼接任意 SQL。</p>
     *
     * @param dataset 数据集规格，包含表表达式、租户列、字段、指标和 SQL 参数定义
     * @param request 查询请求，包含维度、指标、过滤、排序、分页和可选数据集参数
     * @param tenantId 当前租户 ID，必须写入 WHERE 条件形成租户隔离
     * @return 参数化 SQL 与参数列表
     */
    public BiCompiledQuery compile(BiDatasetSpec dataset, BiQueryRequest request, Long tenantId) {
        if (dataset == null) {
            throw new IllegalArgumentException("dataset is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!dataset.datasetKey().equals(request.datasetKey())) {
            throw new IllegalArgumentException("dataset does not match request");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (request.limit() <= 0 || request.limit() > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        if (request.offset() < 0) {
            throw new IllegalArgumentException("offset must be greater than or equal to 0");
        }

        // 先按数据集白名单解析 SELECT 与 GROUP BY，避免前端字段名直接进入 SQL。
        List<String> selectParts = new ArrayList<>();
        List<String> groupByParts = new ArrayList<>();
        List<String> requestedDimensions = request.dimensions() == null ? List.of() : request.dimensions();
        List<String> requestedMetrics = request.metrics() == null ? List.of() : request.metrics();
        for (String dimension : requestedDimensions) {
            BiFieldSpec field = dimension(dataset, dimension);
            selectParts.add(field.columnExpression() + " AS " + dimension);
            groupByParts.add(field.columnExpression());
        }
        for (String metric : requestedMetrics) {
            BiMetricSpec spec = metric(dataset, metric);
            enforceMetricDimensions(spec, requestedDimensions);
            selectParts.add(spec.expression() + " AS " + metric);
        }
        if (selectParts.isEmpty()) {
            throw new IllegalArgumentException("at least one dimension or metric is required");
        }

        // 数据集参数、租户 ID 和过滤条件统一走 JDBC 参数绑定，保持 SQL 口径可审计。
        List<Object> parameters = new ArrayList<>();
        bindSqlParameters(dataset, request, parameters);
        parameters.add(tenantId);
        List<String> whereParts = new ArrayList<>();
        whereParts.add(dataset.tenantColumn() + " = ?");
        for (BiFilter filter : request.filters()) {
            whereParts.add(compileFilter(dataset, filter, parameters));
        }

        StringBuilder sql = new StringBuilder()
                .append("SELECT ")
                .append(String.join(", ", selectParts))
                .append("\nFROM ")
                .append(dataset.tableExpression())
                .append("\nWHERE ")
                .append(String.join(" AND ", whereParts));
        if (!groupByParts.isEmpty()) {
            sql.append("\nGROUP BY ").append(String.join(", ", groupByParts));
        }
        if (!request.sorts().isEmpty()) {
            sql.append("\nORDER BY ").append(compileSorts(dataset, request.sorts()));
        }
        sql.append("\nLIMIT ").append(request.limit());
        if (request.offset() > 0) {
            sql.append("\nOFFSET ").append(request.offset());
        }
        return new BiCompiledQuery(sql.toString(), parameters);
    }

    /**
     * 执行 bindSqlParameters 流程，围绕 bind sql parameters 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 bindSqlParameters 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @param parameters parameters 参数，用于 bindSqlParameters 流程中的校验、计算或对象转换。
     */
    private void bindSqlParameters(BiDatasetSpec dataset, BiQueryRequest request, List<Object> parameters) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiSqlParameterSpec spec : dataset.sqlParameters()) {
            String value = request.sqlParameters().get(spec.key());
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (value == null || value.isBlank()) {
                value = spec.defaultValue();
            }
            if ((value == null || value.isBlank()) && spec.required()) {
                throw new IllegalArgumentException("SQL parameter is required: " + spec.key());
            }
            if (value == null || value.isBlank()) {
                parameters.add(null);
                continue;
            }
            if (!spec.allowedValues().isEmpty() && !spec.allowedValues().contains(value)) {
                throw new IllegalArgumentException("SQL parameter value is not allowed: " + spec.key());
            }
            parameters.add(value);
        }
    }

    /**
     * 执行 dimension 流程，围绕 dimension 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 dimension 流程中的校验、计算或对象转换。
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回 dimension 流程生成的业务结果。
     */
    private BiFieldSpec dimension(BiDatasetSpec dataset, String key) {
        BiFieldSpec field = dataset.fields().get(key);
        if (field == null || field.role() != BiFieldSpec.Role.DIMENSION) {
            throw new IllegalArgumentException("Unknown dimension: " + key);
        }
        return field;
    }

    /**
     * 按数据集白名单解析指标定义。
     *
     * <p>指标表达式只来自后端数据集规格，调用方传入的 metric key 不能直接拼接 SQL。</p>
     */
    private BiMetricSpec metric(BiDatasetSpec dataset, String key) {
        BiMetricSpec metric = dataset.metrics().get(key);
        if (metric == null) {
            throw new IllegalArgumentException("Unknown metric: " + key);
        }
        return metric;
    }

    /**
     * 校验指标是否允许和当前维度组合查询。
     *
     * <p>该约束用于避免比率、窗口指标或聚合粒度受限指标被放到不支持的维度下解释。</p>
     */
    private void enforceMetricDimensions(BiMetricSpec metric, List<String> dimensions) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metric.allowedDimensions().isEmpty() || dimensions == null || dimensions.isEmpty()) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String dimension : dimensions) {
            if (!metric.allowedDimensions().contains(dimension)) {
                throw new IllegalArgumentException("Metric " + metric.metricKey()
                        + " does not allow dimension: " + dimension);
            }
        }
    }

    /**
     * 将受控过滤条件编译为参数化 SQL 片段。
     *
     * <p>过滤字段必须存在于数据集字段目录中，操作符只支持枚举集合，值统一写入参数列表，
     * 防止前端提交任意列名或 SQL 片段。</p>
     */
    private String compileFilter(BiDatasetSpec dataset, BiFilter filter, List<Object> parameters) {
        if (filter == null || filter.field() == null || filter.operator() == null) {
            throw new IllegalArgumentException("filter field and operator are required");
        }
        BiFieldSpec field = dataset.fields().get(filter.field());
        if (field == null) {
            throw new IllegalArgumentException("Unknown filter field: " + filter.field());
        }
        String column = field.columnExpression();
        return switch (filter.operator()) {
            case EQ -> single(column + " = ?", filter.value(), parameters);
            case NEQ -> single(column + " != ?", filter.value(), parameters);
            case GT -> single(column + " > ?", filter.value(), parameters);
            case GTE -> single(column + " >= ?", filter.value(), parameters);
            case LT -> single(column + " < ?", filter.value(), parameters);
            case LTE -> single(column + " <= ?", filter.value(), parameters);
            case CONTAINS -> contains(column, filter.value(), parameters);
            case BETWEEN -> between(column, filter.value(), parameters);
            case IN -> in(column, filter.value(), parameters);
        };
    }

    /**
     * 执行 single 流程，围绕 single 完成校验、计算或结果组装。
     *
     * @param sql sql 参数，用于 single 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param parameters parameters 参数，用于 single 流程中的校验、计算或对象转换。
     * @return 返回 single 生成的文本或业务键。
     */
    private String single(String sql, Object value, List<Object> parameters) {
        parameters.add(value);
        return sql;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param column column 参数，用于 contains 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param parameters parameters 参数，用于 contains 流程中的校验、计算或对象转换。
     * @return 返回 contains 生成的文本或业务键。
     */
    private String contains(String column, Object value, List<Object> parameters) {
        parameters.add("%" + String.valueOf(value) + "%");
        return column + " LIKE ?";
    }

    /**
     * 执行 between 流程，围绕 between 完成校验、计算或结果组装。
     *
     * @param column column 参数，用于 between 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param parameters parameters 参数，用于 between 流程中的校验、计算或对象转换。
     * @return 返回 between 生成的文本或业务键。
     */
    private String between(String column, Object value, List<Object> parameters) {
        List<?> values = listValue(value);
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN filter requires exactly two values");
        }
        parameters.add(values.get(0));
        parameters.add(values.get(1));
        return column + " BETWEEN ? AND ?";
    }

    /**
     * 执行 in 流程，围绕 in 完成校验、计算或结果组装。
     *
     * @param column column 参数，用于 in 流程中的校验、计算或对象转换。
     * @param value 待处理值，用于规则计算或转换。
     * @param parameters parameters 参数，用于 in 流程中的校验、计算或对象转换。
     * @return 返回 in 生成的文本或业务键。
     */
    private String in(String column, Object value, List<Object> parameters) {
        List<?> values = listValue(value);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN filter requires at least one value");
        }
        parameters.addAll(values);
        return column + " IN (" + String.join(", ", values.stream().map(v -> "?").toList()) + ")";
    }

    /**
     * 查询或读取业务数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<?> listValue(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }
        throw new IllegalArgumentException("filter value must be a list");
    }

    /**
     * 将排序字段编译为 ORDER BY 片段。
     *
     * <p>排序字段只能来自已注册字段或指标别名，方向来自枚举值；未知字段会直接失败以避免排序注入。</p>
     */
    private String compileSorts(BiDatasetSpec dataset, List<BiSort> sorts) {
        List<String> parts = new ArrayList<>();
        for (BiSort sort : sorts) {
            if (sort == null || sort.field() == null) {
                throw new IllegalArgumentException("sort field is required");
            }
            if (dataset.fields().containsKey(sort.field())) {
                parts.add(dataset.fields().get(sort.field()).columnExpression() + " " + direction(sort));
            // 根据前序判断结果进入后续条件分支。
            } else if (dataset.metrics().containsKey(sort.field())) {
                parts.add(sort.field() + " " + direction(sort));
            } else {
                throw new IllegalArgumentException("Unknown sort field: " + sort.field());
            }
        }
        return String.join(", ", parts);
    }

    /**
     * 执行 direction 流程，围绕 direction 完成校验、计算或结果组装。
     *
     * @param sort sort 参数，用于 direction 流程中的校验、计算或对象转换。
     * @return 返回 direction 生成的文本或业务键。
     */
    private String direction(BiSort sort) {
        return sort.direction() == null ? BiSort.Direction.ASC.name() : sort.direction().name();
    }
}
