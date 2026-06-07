package org.chovy.canvas.domain.bi.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BiQueryCompiler {

    private static final int MAX_LIMIT = 10000;

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

    private void bindSqlParameters(BiDatasetSpec dataset, BiQueryRequest request, List<Object> parameters) {
        for (BiSqlParameterSpec spec : dataset.sqlParameters()) {
            String value = request.sqlParameters().get(spec.key());
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

    private BiFieldSpec dimension(BiDatasetSpec dataset, String key) {
        BiFieldSpec field = dataset.fields().get(key);
        if (field == null || field.role() != BiFieldSpec.Role.DIMENSION) {
            throw new IllegalArgumentException("Unknown dimension: " + key);
        }
        return field;
    }

    private BiMetricSpec metric(BiDatasetSpec dataset, String key) {
        BiMetricSpec metric = dataset.metrics().get(key);
        if (metric == null) {
            throw new IllegalArgumentException("Unknown metric: " + key);
        }
        return metric;
    }

    private void enforceMetricDimensions(BiMetricSpec metric, List<String> dimensions) {
        if (metric.allowedDimensions().isEmpty() || dimensions == null || dimensions.isEmpty()) {
            return;
        }
        for (String dimension : dimensions) {
            if (!metric.allowedDimensions().contains(dimension)) {
                throw new IllegalArgumentException("Metric " + metric.metricKey()
                        + " does not allow dimension: " + dimension);
            }
        }
    }

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

    private String single(String sql, Object value, List<Object> parameters) {
        parameters.add(value);
        return sql;
    }

    private String contains(String column, Object value, List<Object> parameters) {
        parameters.add("%" + String.valueOf(value) + "%");
        return column + " LIKE ?";
    }

    private String between(String column, Object value, List<Object> parameters) {
        List<?> values = listValue(value);
        if (values.size() != 2) {
            throw new IllegalArgumentException("BETWEEN filter requires exactly two values");
        }
        parameters.add(values.get(0));
        parameters.add(values.get(1));
        return column + " BETWEEN ? AND ?";
    }

    private String in(String column, Object value, List<Object> parameters) {
        List<?> values = listValue(value);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("IN filter requires at least one value");
        }
        parameters.addAll(values);
        return column + " IN (" + String.join(", ", values.stream().map(v -> "?").toList()) + ")";
    }

    private List<?> listValue(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value instanceof Collection<?> collection) {
            return List.copyOf(collection);
        }
        throw new IllegalArgumentException("filter value must be a list");
    }

    private String compileSorts(BiDatasetSpec dataset, List<BiSort> sorts) {
        List<String> parts = new ArrayList<>();
        for (BiSort sort : sorts) {
            if (sort == null || sort.field() == null) {
                throw new IllegalArgumentException("sort field is required");
            }
            if (dataset.fields().containsKey(sort.field())) {
                parts.add(dataset.fields().get(sort.field()).columnExpression() + " " + direction(sort));
            } else if (dataset.metrics().containsKey(sort.field())) {
                parts.add(sort.field() + " " + direction(sort));
            } else {
                throw new IllegalArgumentException("Unknown sort field: " + sort.field());
            }
        }
        return String.join(", ", parts);
    }

    private String direction(BiSort sort) {
        return sort.direction() == null ? BiSort.Direction.ASC.name() : sort.direction().name();
    }
}
