package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryBiQueryResultCache implements BiQueryResultCache {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final Duration ttl;
    private final int maxSize;
    private final Clock clock;

    public InMemoryBiQueryResultCache(
            @Value("${canvas.bi.query.cache.enabled:true}") boolean enabled,
            @Value("${canvas.bi.query.cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${canvas.bi.query.cache.max-size:500}") int maxSize) {
        this(enabled, Duration.ofSeconds(Math.max(1, ttlSeconds)), Math.max(1, maxSize), Clock.systemUTC());
    }

    InMemoryBiQueryResultCache(boolean enabled, Duration ttl, int maxSize, Clock clock) {
        this.enabled = enabled;
        this.ttl = ttl;
        this.maxSize = Math.max(1, maxSize);
        this.clock = clock;
    }

    @Override
    public Optional<BiQueryResult> get(String sqlHash) {
        if (!enabled || sqlHash == null || sqlHash.isBlank()) {
            return Optional.empty();
        }
        Entry entry = entries.get(sqlHash);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(clock.instant()) || entry.expiresAt().equals(clock.instant())) {
            entries.remove(sqlHash, entry);
            return Optional.empty();
        }
        return Optional.of(entry.result());
    }

    @Override
    public void put(String sqlHash, BiQueryResult result) {
        if (!enabled || sqlHash == null || sqlHash.isBlank() || result == null) {
            return;
        }
        pruneExpired();
        if (entries.size() >= maxSize) {
            evictOldest();
        }
        entries.put(sqlHash, new Entry(result, clock.instant().plus(ttl)));
    }

    int size() {
        pruneExpired();
        return entries.size();
    }

    private void pruneExpired() {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private void evictOldest() {
        entries.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .ifPresent(entry -> entries.remove(entry.getKey(), entry.getValue()));
    }

    private record Entry(BiQueryResult result, Instant expiresAt) {
    }
}
