package org.chovy.canvas.engine.scheduler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasMetricsTest {

    @Test
    void capacityGaugesPublishLatestNonNegativeValues() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasMetrics metrics = new CanvasMetrics(registry);

        metrics.setPoolSaturationPercent("hikari", 91);
        metrics.setPoolSaturationPercent("hikari", -1);
        metrics.setLaneSaturationPercent("standard", 87);
        metrics.setQueueDepth("rocketmq:CANVAS_MQ_TRIGGER", 1200);
        metrics.setRedisMemoryBytes(64_000_000L);
        metrics.setDlqBacklog("canvas_execution_dlq", 7);

        assertThat(registry.get("canvas.capacity.pool.saturation.percent")
                .tag("resource", "hikari").gauge().value()).isZero();
        assertThat(registry.get("canvas.capacity.lane.saturation.percent")
                .tag("lane", "standard").gauge().value()).isEqualTo(87.0);
        assertThat(registry.get("canvas.capacity.queue.depth")
                .tag("queue", "rocketmq:CANVAS_MQ_TRIGGER").gauge().value()).isEqualTo(1200.0);
        assertThat(registry.get("canvas.capacity.redis.memory.bytes")
                .tag("resource", "redis").gauge().value()).isEqualTo(64_000_000.0);
        assertThat(registry.get("canvas.capacity.dlq.backlog")
                .tag("queue", "canvas_execution_dlq").gauge().value()).isEqualTo(7.0);
    }

    @Test
    void traceDropCounterUsesReasonTag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasMetrics metrics = new CanvasMetrics(registry);

        metrics.recordTraceDropped("buffer_full");

        assertThat(registry.get("canvas.trace.dropped.total")
                .tag("reason", "buffer_full").counter().count()).isEqualTo(1.0);
    }

    @Test
    void runtimeGateMetricsPublishOperatorDashboardSignals() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasMetrics metrics = new CanvasMetrics(registry);

        metrics.setExecutionActiveByLane("standard", 12);
        metrics.setRetryBacklog("execution-request", 8);
        metrics.setDeliveryOutboxStatusCount("DEAD", 3);
        metrics.recordRedisRegistryLatency("route-refresh", 42);
        metrics.setMysqlPoolPressurePercent("hikari", 76);
        metrics.setTraceBufferPending(99);
        metrics.recordDownstreamLatency("sms", "send", 180);

        assertThat(registry.get("canvas.execution.active")
                .tag("lane", "standard").gauge().value()).isEqualTo(12.0);
        assertThat(registry.get("canvas.retry.backlog")
                .tag("queue", "execution-request").gauge().value()).isEqualTo(8.0);
        assertThat(registry.get("canvas.delivery.outbox.status.count")
                .tag("status", "DEAD").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("canvas.redis.registry.latency")
                .tag("operation", "route-refresh").timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(42.0);
        assertThat(registry.get("canvas.mysql.pool.pressure.percent")
                .tag("pool", "hikari").gauge().value()).isEqualTo(76.0);
        assertThat(registry.get("canvas.trace.buffer.pending").gauge().value()).isEqualTo(99.0);
        assertThat(registry.get("canvas.downstream.latency")
                .tag("system", "sms")
                .tag("operation", "send")
                .timer().totalTime(java.util.concurrent.TimeUnit.MILLISECONDS))
                .isEqualTo(180.0);
    }
}
