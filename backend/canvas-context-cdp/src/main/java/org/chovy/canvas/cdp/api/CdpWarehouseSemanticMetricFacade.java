package org.chovy.canvas.cdp.api;

import java.util.List;

/**
 * 定义 CdpWarehouseSemanticMetricFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseSemanticMetricFacade {

    /**
     * 查询Metrics列表。
     */
    List<SemanticMetricView> listMetrics(Long tenantId, String datasetKey);

    /**
     * 表示 SemanticMetricView 的业务数据或处理组件。
     */
    final class SemanticMetricView {

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * dataset Key。
         */
        private final String datasetKey;

        /**
         * metric Key。
         */
        private final String metricKey;

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
         * dimension Policy。
         */
        private final String dimensionPolicy;

        /**
         * source。
         */
        private final String source;

        /**
         * 使用记录字段创建 SemanticMetricView。
         */
        public SemanticMetricView(
                Long tenantId,
                String datasetKey,
                String metricKey,
                String expression,
                String valueType,
                List<String> allowedDimensions,
                String dimensionPolicy,
                String source) {
            allowedDimensions = allowedDimensions == null ? List.of() : List.copyOf(allowedDimensions);
            this.tenantId = tenantId;
            this.datasetKey = datasetKey;
            this.metricKey = metricKey;
            this.expression = expression;
            this.valueType = valueType;
            this.allowedDimensions = allowedDimensions;
            this.dimensionPolicy = dimensionPolicy;
            this.source = source;
        }

        /**
         * 返回租户标识。
         */
        public Long tenantId() {
            return tenantId;
        }

        /**
         * 返回dataset Key。
         */
        public String datasetKey() {
            return datasetKey;
        }

        /**
         * 返回metric Key。
         */
        public String metricKey() {
            return metricKey;
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
         * 返回dimension Policy。
         */
        public String dimensionPolicy() {
            return dimensionPolicy;
        }

        /**
         * 返回source。
         */
        public String source() {
            return source;
        }

        /**
         * 按所有字段比较 SemanticMetricView。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SemanticMetricView that = (SemanticMetricView) o;
            return java.util.Objects.equals(tenantId, that.tenantId)
                    && java.util.Objects.equals(datasetKey, that.datasetKey)
                    && java.util.Objects.equals(metricKey, that.metricKey)
                    && java.util.Objects.equals(expression, that.expression)
                    && java.util.Objects.equals(valueType, that.valueType)
                    && java.util.Objects.equals(allowedDimensions, that.allowedDimensions)
                    && java.util.Objects.equals(dimensionPolicy, that.dimensionPolicy)
                    && java.util.Objects.equals(source, that.source);
        }

        /**
         * 根据所有字段计算 SemanticMetricView 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, datasetKey, metricKey, expression, valueType, allowedDimensions, dimensionPolicy, source);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "SemanticMetricView[" + "tenantId=" + tenantId + ", datasetKey=" + datasetKey + ", metricKey=" + metricKey + ", expression=" + expression + ", valueType=" + valueType + ", allowedDimensions=" + allowedDimensions + ", dimensionPolicy=" + dimensionPolicy + ", source=" + source + "]";
        }
    }
}
