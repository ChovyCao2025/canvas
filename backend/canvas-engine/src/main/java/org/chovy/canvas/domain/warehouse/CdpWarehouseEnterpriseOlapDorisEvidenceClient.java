package org.chovy.canvas.domain.warehouse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CdpWarehouseEnterpriseOlapDorisEvidenceClient {

    DorisMetricsEvidence metrics();

    List<QuerySloEvidence> querySlo();

    List<WorkloadGroupEvidence> workloadGroups();

    record DorisMetricsEvidence(
            LocalDateTime measuredAt,
            List<MetricsEndpointEvidence> endpoints) {
        public DorisMetricsEvidence {
            endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
        }
    }

    record MetricsEndpointEvidence(
            String endpoint,
            String role,
            LocalDateTime measuredAt,
            Map<String, Double> metrics) {
        public MetricsEndpointEvidence {
            metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
        }

        public double value(String name) {
            Double value = metrics.get(name);
            return value == null ? Double.NaN : value;
        }
    }

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

        public Double minCpuPercent() {
            return cpuMinPercent;
        }

        public Double maxCpuPercent() {
            return cpuHardLimitPercent;
        }

        public Double minMemoryPercent() {
            return memoryMinPercent;
        }

        public Double maxMemoryPercent() {
            return memoryHardLimitPercent;
        }

        public Integer maxQueueSize() {
            return queueSize;
        }

        public Long readBytesPerSecond() {
            return scanBytesPerSecond;
        }

        public Long remoteReadBytesPerSecond() {
            return remoteScanBytesPerSecond;
        }
    }

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
