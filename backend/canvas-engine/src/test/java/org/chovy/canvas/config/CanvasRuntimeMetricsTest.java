package org.chovy.canvas.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasRuntimeMetricsTest {

    @Test
    void recordsOperationalCountersAndGauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasRuntimeMetrics metrics = new CanvasRuntimeMetrics(registry);

        metrics.recordExecutionFailure("MQ", "TimeoutException");
        metrics.recordRouteRebuildFailure("canvas_parse");
        metrics.recordCacheInvalidationFailure("canvas_config", "redis_down");
        metrics.recordShutdownDrainTimeout("background_executor");
        metrics.setDlqBacklog(12);
        metrics.setRedisAvailable(false);
        metrics.setMqAvailable(true);
        metrics.setLanePressure("HIGH", 75, 100);
        metrics.setDisruptorPressure(512, 1024);

        assertThat(registry.get("canvas.runtime.execution.failures")
                .tag("triggerType", "mq")
                .tag("reason", "timeoutexception")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("canvas.runtime.route.rebuild.failures")
                .tag("reason", "canvas_parse")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("canvas.runtime.cache.invalidation.failures")
                .tag("cache", "canvas_config")
                .tag("reason", "redis_down")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("canvas.runtime.shutdown.drain.timeout")
                .tag("component", "background_executor")
                .counter().count()).isEqualTo(1d);
        assertThat(registry.get("canvas.runtime.dlq.backlog").gauge().value()).isEqualTo(12d);
        assertThat(registry.get("canvas.runtime.redis.available").gauge().value()).isZero();
        assertThat(registry.get("canvas.runtime.mq.available").gauge().value()).isEqualTo(1d);
        assertThat(registry.get("canvas.runtime.lane.pressure")
                .tag("lane", "high")
                .gauge().value()).isEqualTo(0.75d);
        assertThat(registry.get("canvas.runtime.disruptor.pressure").gauge().value()).isEqualTo(0.5d);
    }
}
