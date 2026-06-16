package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseMetricChangeReviewFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseMetricChangeReviewFacade {

    /**
     * status)。
     */
    List<Map<String, Object>> list(Long tenantId, String datasetKey, String metricKey, String status);

    /**
     * command)。
     */
    Map<String, Object> create(Long tenantId, String username, MetricChangeCommand command);

    /**
     * review Note)。
     */
    Map<String, Object> approve(Long tenantId, String username, Long reviewId, String reviewNote);

    /**
     * review Note)。
     */
    Map<String, Object> reject(Long tenantId, String username, Long reviewId, String reviewNote);

    /**
     * review Id)。
     */
    Map<String, Object> apply(Long tenantId, String username, Long reviewId);

    /**
     * 表示 MetricChangeCommand 的业务数据或处理组件。
     */
    final class MetricChangeCommand {

        /**
         * dataset Key。
         */
        private final String datasetKey;

        /**
         * metric Key。
         */
        private final String metricKey;

        /**
         * proposed Expression。
         */
        private final String proposedExpression;

        /**
         * proposed Allowed Dimensions。
         */
        private final List<String> proposedAllowedDimensions;

        /**
         * 原因。
         */
        private final String reason;

        /**
         * 使用记录字段创建 MetricChangeCommand。
         */
        public MetricChangeCommand(
                String datasetKey,
                String metricKey,
                String proposedExpression,
                List<String> proposedAllowedDimensions,
                String reason) {
            proposedAllowedDimensions = proposedAllowedDimensions == null
            ? List.of()
            : List.copyOf(proposedAllowedDimensions);
            this.datasetKey = datasetKey;
            this.metricKey = metricKey;
            this.proposedExpression = proposedExpression;
            this.proposedAllowedDimensions = proposedAllowedDimensions;
            this.reason = reason;
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
         * 返回proposed Expression。
         */
        public String proposedExpression() {
            return proposedExpression;
        }

        /**
         * 返回proposed Allowed Dimensions。
         */
        public List<String> proposedAllowedDimensions() {
            return proposedAllowedDimensions;
        }

        /**
         * 返回原因。
         */
        public String reason() {
            return reason;
        }

        /**
         * 按所有字段比较 MetricChangeCommand。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MetricChangeCommand that = (MetricChangeCommand) o;
            return java.util.Objects.equals(datasetKey, that.datasetKey)
                    && java.util.Objects.equals(metricKey, that.metricKey)
                    && java.util.Objects.equals(proposedExpression, that.proposedExpression)
                    && java.util.Objects.equals(proposedAllowedDimensions, that.proposedAllowedDimensions)
                    && java.util.Objects.equals(reason, that.reason);
        }

        /**
         * 根据所有字段计算 MetricChangeCommand 的哈希值。
         */
        @Override
        public int hashCode() {
            return java.util.Objects.hash(datasetKey, metricKey, proposedExpression, proposedAllowedDimensions, reason);
        }

        /**
         * 返回与记录结构一致的调试字符串。
         */
        @Override
        public String toString() {
            return "MetricChangeCommand[" + "datasetKey=" + datasetKey + ", metricKey=" + metricKey + ", proposedExpression=" + proposedExpression + ", proposedAllowedDimensions=" + proposedAllowedDimensions + ", reason=" + reason + "]";
        }
    }
}
