package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProviderBackpressureService {

    private final CounterStore counters;
    private final LimitRepository limits;
    private final Clock clock;

    @Autowired
    public ProviderBackpressureService(CounterStore counters, LimitRepository limits) {
        this(counters, limits, Clock.systemUTC());
    }

    public ProviderBackpressureService(CounterStore counters, LimitRepository limits, Clock clock) {
        this.counters = counters;
        this.limits = limits;
        this.clock = clock;
    }

    public Decision decide(Long tenantId, String channel, String provider, String operation, boolean sandboxMode) {
        if (sandboxMode) {
            return Decision.allowed();
        }
        LimitKey key = new LimitKey(
                ChannelConnectorRegistry.tenant(tenantId),
                ChannelConnectorRegistry.normalize(channel),
                ChannelConnectorRegistry.normalizeProvider(provider),
                ChannelConnectorRegistry.normalize(operation == null ? "SEND" : operation));
        ProviderLimit limit = limits.find(key);
        ProviderLimit effectiveLimit = limit == null ? ProviderLimit.defaults() : limit;
        try {
            long secondCount = counters.increment(key.toKey("s", clock.instant().getEpochSecond()));
            if (effectiveLimit.perSecondLimit() != null
                    && effectiveLimit.perSecondLimit() > 0
                    && secondCount > effectiveLimit.perSecondLimit()) {
                return new Decision("THROTTLED_RETRY", "per-second provider limit exceeded", 1L);
            }
            if (effectiveLimit.dailyLimit() != null && effectiveLimit.dailyLimit() > 0) {
                LocalDate day = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
                long dailyCount = counters.increment(key.toKey("d", day));
                if (dailyCount > effectiveLimit.dailyLimit()) {
                    return new Decision("THROTTLED_SKIP", "daily provider limit exceeded", null);
                }
            }
            return Decision.allowed();
        } catch (RuntimeException ex) {
            if (effectiveLimit.failClosed()) {
                return new Decision("REGISTRY_UNAVAILABLE", ex.getMessage(), null);
            }
            return Decision.allowed();
        }
    }

    @FunctionalInterface
    public interface CounterStore {
        long increment(String key);
    }

    public interface LimitRepository {
        ProviderLimit find(LimitKey key);
    }

    public record LimitKey(Long tenantId, String channel, String provider, String operation) {
        String toKey(String bucket, Object value) {
            return "channel:provider:" + tenantId + ":" + channel + ":" + provider + ":" + operation + ":" + bucket + ":" + value;
        }
    }

    public record ProviderLimit(Integer perSecondLimit, Long dailyLimit, boolean failClosed) {
        static ProviderLimit defaults() {
            return new ProviderLimit(100, null, true);
        }
    }

    public record Decision(String status, String reason, Long retryAfterSeconds) {
        static Decision allowed() {
            return new Decision("ALLOWED", null, null);
        }
    }

    public static class InMemoryCounterStore implements CounterStore {

        private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

        @Override
        public long increment(String key) {
            return counters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
        }
    }
}
