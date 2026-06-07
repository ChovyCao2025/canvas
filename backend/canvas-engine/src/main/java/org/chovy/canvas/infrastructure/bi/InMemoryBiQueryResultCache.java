package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiQueryCacheStats;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "canvas.bi.query.cache.provider", havingValue = "memory", matchIfMissing = true)
public class InMemoryBiQueryResultCache implements BiQueryResultCache {

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final Duration ttl;
    private final int maxSize;
    private final Clock clock;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    @Autowired
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
            missCount.incrementAndGet();
            return Optional.empty();
        }
        Instant now = clock.instant();
        if (entry.expiresAt().isBefore(now) || entry.expiresAt().equals(now)) {
            if (entries.remove(sqlHash, entry)) {
                evictionCount.incrementAndGet();
            }
            missCount.incrementAndGet();
            return Optional.empty();
        }
        hitCount.incrementAndGet();
        return Optional.of(entry.result());
    }

    @Override
    public void put(String sqlHash, BiQueryResult result) {
        put(sqlHash, result, ttl);
    }

    @Override
    public void put(String sqlHash, BiQueryResult result, Duration ttl) {
        if (!enabled || sqlHash == null || sqlHash.isBlank() || result == null) {
            return;
        }
        pruneExpired();
        if (entries.size() >= maxSize) {
            evictOldest();
        }
        Duration effectiveTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? this.ttl : ttl;
        entries.put(sqlHash, new Entry(result, clock.instant().plus(effectiveTtl)));
        putCount.incrementAndGet();
    }

    @Override
    public boolean evict(String sqlHash) {
        if (sqlHash == null || sqlHash.isBlank()) {
            return false;
        }
        boolean removed = entries.remove(sqlHash) != null;
        if (removed) {
            evictionCount.incrementAndGet();
        }
        return removed;
    }

    @Override
    public int evictDataset(String datasetKey) {
        if (datasetKey == null || datasetKey.isBlank()) {
            return 0;
        }
        int before = entries.size();
        entries.entrySet().removeIf(entry -> datasetKey.equals(entry.getValue().result().datasetKey()));
        int deleted = before - entries.size();
        evictionCount.addAndGet(Math.max(0, deleted));
        return deleted;
    }

    @Override
    public int clear() {
        int before = entries.size();
        entries.clear();
        evictionCount.addAndGet(before);
        return before;
    }

    @Override
    public BiQueryCacheStats stats() {
        return new BiQueryCacheStats(
                "memory",
                enabled,
                size(),
                maxSize,
                ttl.toSeconds(),
                hitCount.get(),
                missCount.get(),
                putCount.get(),
                evictionCount.get());
    }

    int size() {
        pruneExpired();
        return entries.size();
    }

    private void pruneExpired() {
        Instant now = clock.instant();
        AtomicInteger removed = new AtomicInteger();
        entries.entrySet().removeIf(entry -> {
            boolean expired = !entry.getValue().expiresAt().isAfter(now);
            if (expired) {
                removed.incrementAndGet();
            }
            return expired;
        });
        evictionCount.addAndGet(removed.get());
    }

    private void evictOldest() {
        entries.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().expiresAt()))
                .ifPresent(entry -> {
                    if (entries.remove(entry.getKey(), entry.getValue())) {
                        evictionCount.incrementAndGet();
                    }
                });
    }

    private record Entry(BiQueryResult result, Instant expiresAt) {
    }
}
