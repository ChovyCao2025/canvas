package org.chovy.canvas.cdp.application;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.chovy.canvas.cdp.api.CdpWarehouseMetricLineageFacade;
import org.springframework.stereotype.Service;

/**
 * 编排 CdpWarehouseMetricLineage 的应用服务流程。
 */
@Service
public class CdpWarehouseMetricLineageApplicationService implements CdpWarehouseMetricLineageFacade {

    /**
     * EXPRESSION FIELD。
     */
    private static final String EXPRESSION_FIELD = "EXPRESSION_FIELD";

    /**
     * 执行 of 对应的 CDP 业务操作。
     */
    private static final Map<String, MetricSpec> BUILT_IN_METRICS = Map.of(
            key("canvas_daily_stats", "success_rate"),
            new MetricSpec("SUM(success_count)", "PERCENT", List.of("stat_date"), List.of("success_count")));

    /**
     * 执行 impact 对应的 CDP 业务操作。
     */
    @Override
    public MetricImpactView impact(Long tenantId, String datasetKey, String metricKey) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String scopedDatasetKey = required(datasetKey, "datasetKey");
        String scopedMetricKey = required(metricKey, "metricKey");
        MetricSpec metric = BUILT_IN_METRICS.getOrDefault(key(scopedDatasetKey, scopedMetricKey),
                new MetricSpec(scopedMetricKey, "DECIMAL", List.of(), List.of()));
        return new MetricImpactView(
                scopedTenantId,
                scopedDatasetKey,
                scopedMetricKey,
                metric.expression(),
                metric.valueType(),
                metric.allowedDimensions(),
                fieldDependencies(metric),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * 执行 fieldDependencies 对应的 CDP 业务操作。
     */
    private static List<FieldDependencyView> fieldDependencies(MetricSpec metric) {
        return metric.fields().stream()
                .filter(field -> referencesField(metric.expression(), field))
                .map(field -> new FieldDependencyView(field, EXPRESSION_FIELD))
                .toList();
    }

    /**
     * 执行 referencesField 对应的 CDP 业务操作。
     */
    private static boolean referencesField(String expression, String field) {
        return Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(field) + "(?![A-Za-z0-9_])")
                .matcher(expression)
                .find();
    }

    /**
     * 读取并校验必填的d。
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 执行 key 对应的 CDP 业务操作。
     */
    private static String key(String datasetKey, String metricKey) {
        return datasetKey + ":" + metricKey;
    }

    /**
     * 表示 MetricSpec 的业务数据或处理组件。
     */
    private static final class MetricSpec {

        /**
         * expression。
         */
        private final String expression;

        /**
         * 值类型。
         */
        private final String valueType;

        /**
         * allowed Dimensions。
         */
        private final List<String> allowedDimensions;

        /**
         * fields。
         */
        private final List<String> fields;

        /**
         * 使用记录字段创建 MetricSpec。
         */
        private MetricSpec(
                String expression,
                String valueType,
                List<String> allowedDimensions,
                List<String> fields) {
            this.expression = expression;
            this.valueType = valueType;
            this.allowedDimensions = allowedDimensions;
            this.fields = fields;
        }

        /**
         * 返回expression。
         */
        public String expression() {
            return expression;
        }

        /**
         * 返回值类型。
         */
        public String valueType() {
            return valueType;
        }

        /**
         * 返回allowed Dimensions。
         */
        public List<String> allowedDimensions() {
            return allowedDimensions;
        }

        /**
         * 返回fields。
         */
        public List<String> fields() {
            return fields;
        }

        /**
         * 按所有字段比较 MetricSpec。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MetricSpec that = (MetricSpec) o;
            return java.util.Objects.equals(expression, that.expression)
                    && java.util.Objects.equals(valueType, that.valueType)
                    && java.util.Objects.equals(allowedDimensions, that.allowedDimensions)
                    && java.util.Objects.equals(fields, that.fields);
        }

        /**
         * 根据所有字段计算 MetricSpec 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(expression, valueType, allowedDimensions, fields);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "MetricSpec[" + "expression=" + expression + ", valueType=" + valueType + ", allowedDimensions=" + allowedDimensions + ", fields=" + fields + "]";
        }
    }
}
