package org.chovy.canvas.engine.channel;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DownstreamBulkheadRegistry {

    private final boolean available;
    private final Map<Key, BulkheadState> states = new ConcurrentHashMap<>();

    public DownstreamBulkheadRegistry() {
        this(true);
    }

    private DownstreamBulkheadRegistry(boolean available) {
        this.available = available;
    }

    public static DownstreamBulkheadRegistry available() {
        return new DownstreamBulkheadRegistry(true);
    }

    public static DownstreamBulkheadRegistry unavailable() {
        return new DownstreamBulkheadRegistry(false);
    }

    public Decision permit(Long tenantId, String providerKey, String dependencyKind, Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        Key key = key(tenantId, providerKey, dependencyKind);
        if (!available) {
            return new Decision(key.providerKey(), key.dependencyKind(), false, State.OPEN,
                    effectiveNow.plusSeconds(30), "bulkhead registry state unavailable");
        }
        BulkheadState state = states.get(key);
        if (state == null || state.state() == State.CLOSED) {
            return new Decision(key.providerKey(), key.dependencyKind(), true, State.CLOSED, null, "closed");
        }
        if (state.state() == State.OPEN && effectiveNow.isBefore(state.retryAfter())) {
            return new Decision(key.providerKey(), key.dependencyKind(), false, State.OPEN,
                    state.retryAfter(), state.reason());
        }
        BulkheadState halfOpen = new BulkheadState(State.HALF_OPEN, effectiveNow, "half-open recovery probe");
        states.put(key, halfOpen);
        return new Decision(key.providerKey(), key.dependencyKind(), true, State.HALF_OPEN,
                effectiveNow, halfOpen.reason());
    }

    public void open(Long tenantId,
                     String providerKey,
                     String dependencyKind,
                     Duration retryAfter,
                     String reason,
                     Instant now) {
        Instant effectiveNow = now == null ? Instant.now() : now;
        Duration duration = retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()
                ? Duration.ofSeconds(30)
                : retryAfter;
        states.put(key(tenantId, providerKey, dependencyKind),
                new BulkheadState(State.OPEN, effectiveNow.plus(duration), normalizeReason(reason)));
    }

    public void close(Long tenantId, String providerKey, String dependencyKind) {
        states.put(key(tenantId, providerKey, dependencyKind),
                new BulkheadState(State.CLOSED, null, "closed"));
    }

    private Key key(Long tenantId, String providerKey, String dependencyKind) {
        return new Key(
                tenantId == null ? 0L : tenantId,
                normalize(providerKey, "unknown-provider"),
                normalize(dependencyKind, "UNKNOWN"));
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? "bulkhead opened" : reason.trim();
    }

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private record Key(Long tenantId, String providerKey, String dependencyKind) {
        private Key {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(providerKey, "providerKey");
            Objects.requireNonNull(dependencyKind, "dependencyKind");
        }
    }

    private record BulkheadState(State state, Instant retryAfter, String reason) {
    }

    public record Decision(
            String providerKey,
            String dependencyKind,
            boolean permitted,
            State state,
            Instant retryAfter,
            String reason) {
    }
}
