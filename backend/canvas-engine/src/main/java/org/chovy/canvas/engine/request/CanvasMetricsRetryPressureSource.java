package org.chovy.canvas.engine.request;

import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CanvasMetricsRetryPressureSource implements ExecutionRequestRetryPressureSource {

    private final CanvasMetrics metrics;
    private final long laneSaturationGatePercent;
    private final double downstreamErrorGatePercent;
    private final long dlqBacklogGate;

    public CanvasMetricsRetryPressureSource(
            CanvasMetrics metrics,
            @Value("${canvas.execution-request.retry-pressure.lane-saturation-gate-percent:90}") long laneSaturationGatePercent,
            @Value("${canvas.execution-request.retry-pressure.downstream-error-gate-percent:5}") double downstreamErrorGatePercent,
            @Value("${canvas.execution-request.retry-pressure.dlq-backlog-gate:100}") long dlqBacklogGate) {
        this.metrics = metrics;
        this.laneSaturationGatePercent = Math.max(1L, laneSaturationGatePercent);
        this.downstreamErrorGatePercent = Math.max(0.1d, downstreamErrorGatePercent);
        this.dlqBacklogGate = Math.max(1L, dlqBacklogGate);
    }

    @Override
    public Snapshot snapshot() {
        long lanePressure = Math.max(
                metrics.currentTaggedGaugeValue("canvas.capacity.lane.saturation.percent", "lane", "LIGHT"),
                metrics.currentTaggedGaugeValue("canvas.capacity.lane.saturation.percent", "lane", "STANDARD"));
        double downstreamErrors = metrics.currentTaggedGaugeValue(
                "canvas.downstream.error.percent", "system", "ALL");
        long dlqBacklog = metrics.currentTaggedGaugeValue(
                "canvas.capacity.dlq.backlog", "queue", "execution-request");
        return new Snapshot(
                new AdaptiveRetryBackoffPolicy.LanePressureSnapshot(lanePressure, laneSaturationGatePercent),
                new AdaptiveRetryBackoffPolicy.DownstreamErrorSnapshot(downstreamErrors, downstreamErrorGatePercent),
                new AdaptiveRetryBackoffPolicy.DlqGrowthSnapshot(dlqBacklog, dlqBacklogGate));
    }
}
