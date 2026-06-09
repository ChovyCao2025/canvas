package org.chovy.canvas.domain.warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CdpWarehouseEnterpriseOlapDorisEvidenceClient 定义 domain.warehouse 场景中的扩展契约。
 */
public interface CdpWarehouseEnterpriseOlapDorisEvidenceClient {

    /**
     * 执行 metrics 流程，围绕 metrics 完成校验、计算或结果组装。
     *
     * @return 返回 metrics 流程生成的业务结果。
     */
    DorisMetricsEvidence metrics();

    /**
     * 查询或读取业务数据。
     *
     * @return 返回符合条件的数据列表或视图。
     */
    List<QuerySloEvidence> querySlo();

    /**
     * 执行 workloadGroups 流程，围绕 workload groups 完成校验、计算或结果组装。
     *
     * @return 返回 workload groups 汇总后的集合、分页或映射视图。
     */
    List<WorkloadGroupEvidence> workloadGroups();

    /**
     * DorisMetricsEvidence 数据记录。
     */
    record DorisMetricsEvidence(
            LocalDateTime measuredAt,
            List<MetricsEndpointEvidence> endpoints) {
        public DorisMetricsEvidence {
            endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
        }
    }

    /**
     * MetricsEndpointEvidence 数据记录。
     */
    record MetricsEndpointEvidence(
            String endpoint,
            String role,
            LocalDateTime measuredAt,
            Map<String, Double> metrics) {
        public MetricsEndpointEvidence {
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        }

        /**
         * value 处理 domain.warehouse 场景的业务逻辑。
         * @param name 名称文本，用于展示或唯一性校验。
         * @return 返回 value 计算得到的数量、金额或指标值。
         */
        public double value(String name) {
            Double value = metrics.get(name);
            return value == null ? Double.NaN : value;
        }
    }

    /**
     * WorkloadGroupEvidence 数据记录。
     */
    record WorkloadGroupEvidence(
            String name,
            Double cpuMinPercent,
            Double cpuHardLimitPercent,
            Double memoryMinPercent,
            Double memoryHardLimitPercent,
            Integer maxConcurrency,
            Integer queueSize,
            Long queueTimeoutMs,
            Long scanBytesPerSecond,
            Long remoteScanBytesPerSecond) {

        /**
         * minCpuPercent 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 min cpu percent 计算得到的数量、金额或指标值。
         */
        public Double minCpuPercent() {
            return cpuMinPercent;
        }

        /**
         * maxCpuPercent 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 max cpu percent 计算得到的数量、金额或指标值。
         */
        public Double maxCpuPercent() {
            return cpuHardLimitPercent;
        }

        /**
         * minMemoryPercent 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 min memory percent 计算得到的数量、金额或指标值。
         */
        public Double minMemoryPercent() {
            return memoryMinPercent;
        }

        /**
         * maxMemoryPercent 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 max memory percent 计算得到的数量、金额或指标值。
         */
        public Double maxMemoryPercent() {
            return memoryHardLimitPercent;
        }

        /**
         * maxQueueSize 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 max queue size 计算得到的数量、金额或指标值。
         */
        public Integer maxQueueSize() {
            return queueSize;
        }

        /**
         * readBytesPerSecond 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 read bytes per second 计算得到的数量、金额或指标值。
         */
        public Long readBytesPerSecond() {
            return scanBytesPerSecond;
        }

        /**
         * remoteReadBytesPerSecond 处理 domain.warehouse 场景的业务逻辑。
         * @return 返回 remote read bytes per second 计算得到的数量、金额或指标值。
         */
        public Long remoteReadBytesPerSecond() {
            return remoteScanBytesPerSecond;
        }
    }

    /**
     * QuerySloEvidence 数据记录。
     */
    record QuerySloEvidence(
            String profileKey,
            String workloadGroup,
            Long sampleCount,
            Long errorCount,
            Double p95LatencyMs,
            Double p99LatencyMs,
            Double maxQueueWaitMs,
            Long maxPeakMemoryBytes,
            LocalDateTime measuredAt) {
    }
}
