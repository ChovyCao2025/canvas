package org.chovy.canvas.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime operations metrics used by alert rules and operational dashboards.
 */
@Component
@RequiredArgsConstructor
public class CanvasRuntimeMetrics {

    private final MeterRegistry registry;
    private final ConcurrentMap<String, AtomicLong> longGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicReference<Double>> doubleGauges = new ConcurrentHashMap<>();

    public void recordExecutionFailure(String triggerType, String reason) {
        Counter.builder("canvas.runtime.execution.failures")
                .tags("triggerType", normalize(triggerType), "reason", normalize(reason))
                .register(registry)
                .increment();
    }

    public void setDlqBacklog(long count) {
        longGauge("canvas.runtime.dlq.backlog", Tags.empty()).set(Math.max(0L, count));
    }

    public void recordRouteRebuildFailure(String reason) {
        Counter.builder("canvas.runtime.route.rebuild.failures")
                .tag("reason", normalize(reason))
                .register(registry)
                .increment();
    }

    public void recordCacheInvalidationFailure(String cacheName, String reason) {
        Counter.builder("canvas.runtime.cache.invalidation.failures")
                .tags("cache", normalize(cacheName), "reason", normalize(reason))
                .register(registry)
                .increment();
    }

    public void setRedisAvailable(boolean available) {
        longGauge("canvas.runtime.redis.available", Tags.empty()).set(available ? 1L : 0L);
    }

    public void setMqAvailable(boolean available) {
        longGauge("canvas.runtime.mq.available", Tags.empty()).set(available ? 1L : 0L);
    }

    public void setLanePressure(String lane, long active, long limit) {
        double pressure = limit <= 0 ? 1.0d : Math.max(0d, active) / (double) limit;
        doubleGauge("canvas.runtime.lane.pressure", Tags.of("lane", normalize(lane)))
                .set(Math.min(1.0d, pressure));
    }

    public void setDisruptorPressure(long backlog, long ringBufferSize) {
        double pressure = ringBufferSize <= 0 ? 1.0d : Math.max(0d, backlog) / (double) ringBufferSize;
        doubleGauge("canvas.runtime.disruptor.pressure", Tags.empty())
                .set(Math.min(1.0d, pressure));
    }

    public void recordShutdownDrainTimeout(String component) {
        Counter.builder("canvas.runtime.shutdown.drain.timeout")
                .tag("component", normalize(component))
                .register(registry)
                .increment();
    }

    public void recordMarketingIntegrationProbeResult(String providerFamily,
                                                      String environment,
                                                      String status,
                                                      Integer httpStatusCode,
                                                      Long latencyMs,
                                                      String errorType) {
        Tags tags = Tags.of(
                "provider_family", normalize(providerFamily),
                "environment", normalize(environment),
                "status", normalize(status),
                "http.response.status_code", httpStatusCode == null ? "unknown" : String.valueOf(httpStatusCode),
                "error.type", normalizeErrorType(errorType));
        Counter.builder("canvas.marketing.integration.probe.result.total")
                .tags(tags)
                .register(registry)
                .increment();
        Timer.builder("canvas.marketing.integration.probe.latency")
                .tags(tags)
                .register(registry)
                .record(Math.max(0L, latencyMs == null ? 0L : latencyMs), TimeUnit.MILLISECONDS);
    }

    private AtomicLong longGauge(String name, Tags tags) {
        String key = gaugeKey(name, tags);
        return longGauges.computeIfAbsent(key, ignored -> {
            AtomicLong value = new AtomicLong();
            Gauge.builder(name, value, AtomicLong::get)
                    .tags(tags)
                    .register(registry);
            return value;
        });
    }

    private AtomicReference<Double> doubleGauge(String name, Tags tags) {
        String key = gaugeKey(name, tags);
        return doubleGauges.computeIfAbsent(key, ignored -> {
            AtomicReference<Double> value = new AtomicReference<>(0d);
            Gauge.builder(name, value, ref -> ref.get() == null ? 0d : ref.get())
                    .tags(tags)
                    .register(registry);
            return value;
        });
    }

    private String gaugeKey(String name, Iterable<Tag> tags) {
        StringJoiner joiner = new StringJoiner("|", name + "|", "");
        for (Tag tag : tags) {
            joiner.add(tag.getKey() + "=" + tag.getValue());
        }
        return joiner.toString();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeErrorType(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        return normalize(value);
    }
}
