package org.chovy.canvas.infrastructure.bi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.bi.query.BiQueryCacheStats;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.chovy.canvas.domain.bi.query.BiQueryResultCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "canvas.bi.query.cache.provider", havingValue = "redis")
public class RedisBiQueryResultCache implements BiQueryResultCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration ttl;
    private final String keyPrefix;
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong evictionCount = new AtomicLong();

    @Autowired
    public RedisBiQueryResultCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            @Value("${canvas.bi.query.cache.enabled:true}") boolean enabled,
            @Value("${canvas.bi.query.cache.ttl-seconds:300}") long ttlSeconds,
            @Value("${canvas.bi.query.cache.redis.key-prefix:canvas:bi:query-cache:}") String keyPrefix) {
        this(redis, objectMapper, enabled, Duration.ofSeconds(Math.max(1, ttlSeconds)), keyPrefix);
    }

    RedisBiQueryResultCache(StringRedisTemplate redis,
                            ObjectMapper objectMapper,
                            boolean enabled,
                            Duration ttl,
                            String keyPrefix) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttl = ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
        this.keyPrefix = normalizePrefix(keyPrefix);
    }

    @Override
    public Optional<BiQueryResult> get(String sqlHash) {
        if (!enabled || sqlHash == null || sqlHash.isBlank()) {
            return Optional.empty();
        }
        String key = key(sqlHash);
        try {
            String payload = redis.opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                missCount.incrementAndGet();
                return Optional.empty();
            }
            BiQueryResult result = objectMapper.readValue(payload, BiQueryResult.class);
            hitCount.incrementAndGet();
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            missCount.incrementAndGet();
            if (Boolean.TRUE.equals(redis.delete(key))) {
                evictionCount.incrementAndGet();
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            missCount.incrementAndGet();
            return Optional.empty();
        }
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
        Duration effectiveTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? this.ttl : ttl;
        try {
            redis.opsForValue().set(key(sqlHash), objectMapper.writeValueAsString(result), effectiveTtl);
            putCount.incrementAndGet();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("BI query result is not JSON serializable: " + sqlHash, e);
        } catch (RuntimeException e) {
            // Cache availability must not block BI query execution.
        }
    }

    @Override
    public boolean evict(String sqlHash) {
        if (sqlHash == null || sqlHash.isBlank()) {
            return false;
        }
        try {
            Boolean deleted = redis.delete(key(sqlHash));
            boolean removed = Boolean.TRUE.equals(deleted);
            if (removed) {
                evictionCount.incrementAndGet();
            }
            return removed;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public int evictDataset(String datasetKey) {
        if (datasetKey == null || datasetKey.isBlank()) {
            return 0;
        }
        int deleted = 0;
        for (String redisKey : keys()) {
            try {
                String payload = redis.opsForValue().get(redisKey);
                if (payload == null || payload.isBlank()) {
                    continue;
                }
                BiQueryResult result = objectMapper.readValue(payload, BiQueryResult.class);
                if (datasetKey.equals(result.datasetKey()) && Boolean.TRUE.equals(redis.delete(redisKey))) {
                    deleted++;
                    evictionCount.incrementAndGet();
                }
            } catch (JsonProcessingException e) {
                if (Boolean.TRUE.equals(redis.delete(redisKey))) {
                    evictionCount.incrementAndGet();
                }
            } catch (RuntimeException ignored) {
                // Cache invalidation visibility should not break BI administration calls.
            }
        }
        return deleted;
    }

    @Override
    public int clear() {
        Set<String> keys = keys();
        if (keys.isEmpty()) {
            return 0;
        }
        try {
            Long deleted = redis.delete((Collection<String>) keys);
            int count = deleted == null ? 0 : deleted.intValue();
            evictionCount.addAndGet(count);
            return count;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    @Override
    public BiQueryCacheStats stats() {
        return new BiQueryCacheStats(
                "redis",
                enabled,
                enabled ? keys().size() : 0,
                -1,
                ttl.toSeconds(),
                hitCount.get(),
                missCount.get(),
                putCount.get(),
                evictionCount.get());
    }

    private String key(String sqlHash) {
        return keyPrefix + sqlHash.trim();
    }

    private Set<String> keys() {
        try {
            Set<String> keys = redis.keys(keyPrefix + "*");
            return keys == null ? Set.of() : keys;
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    private static String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "canvas:bi:query-cache:";
        }
        return keyPrefix;
    }
}
