package org.chovy.cache.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheBuilder;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.annotation.TieredCached;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationCacheResolver {
    private final TieredCacheManager manager;
    private final ObjectMapper objectMapper;
    private final Map<String, TieredCache<Object, Object>> caches = new ConcurrentHashMap<>();

    public AnnotationCacheResolver(TieredCacheManager manager, ObjectMapper objectMapper) {
        this.manager = manager;
        this.objectMapper = objectMapper;
    }

    public TieredCache<Object, Object> resolve(TieredCached annotation) {
        return caches.computeIfAbsent(annotation.name(), ignored ->
                TieredCacheBuilder.<Object, Object>builder()
                        .name(annotation.name())
                        .l1MaxSize(annotation.l1MaxSize())
                        .l1RefreshAfterWrite(parseDuration(annotation.l1RefreshAfterWrite()))
                        .l2KeyPrefix(annotation.l2KeyPrefix())
                        .l2Ttl(parseDuration(annotation.l2Ttl()))
                        .l2TtlJitter(annotation.l2TtlJitter())
                        .keySchemaVersion(annotation.keySchemaVersion())
                        .nullValueTtl(parseDuration(annotation.nullValueTtl()))
                        .emptyValueTtl(parseDuration(annotation.emptyValueTtl()))
                        .lockTtl(parseDuration(annotation.lockTtl()))
                        .refreshAhead(parseDuration(annotation.refreshAhead()))
                        .staleTtl(parseDuration(annotation.staleTtl()))
                        .hotspotProtection(annotation.hotspotProtection())
                        .penetration(annotation.penetration())
                        .breakdown(annotation.breakdown())
                        .avalanche(annotation.avalanche())
                        .onLoaderFailure(annotation.onLoaderFailure())
                        .onRedisReadFailure(annotation.onRedisReadFailure())
                        .onRedisWriteFailure(annotation.onRedisWriteFailure())
                        .onDeserializeFailure(annotation.onDeserializeFailure())
                        .loader(key -> { throw new IllegalStateException("Annotation cache requires loaderOverride"); })
                        .objectMapper(objectMapper)
                        .valueType(annotation.valueType())
                        .build(manager));
    }

    @SuppressWarnings("unchecked")
    public TieredCache<Object, Object> getExisting(String name) {
        TieredCache<Object, Object> cache = caches.get(name);
        if (cache != null) {
            return cache;
        }
        return (TieredCache<Object, Object>) manager.getCache(name)
                .orElseThrow(() -> new IllegalStateException("Unknown tiered cache: " + name));
    }

    @SuppressWarnings("unchecked")
    public void evictIfPresent(String name, Object key) {
        TieredCache<Object, Object> cache = caches.get(name);
        if (cache == null) {
            cache = (TieredCache<Object, Object>) manager.getCache(name).orElse(null);
        }
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    private Duration parseDuration(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("p")) {
            return Duration.parse(value);
        }
        if (normalized.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(normalized.substring(0, normalized.length() - 2)));
        }
        if (normalized.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        if (normalized.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(normalized.substring(0, normalized.length() - 1)));
        }
        return Duration.ofMillis(Long.parseLong(normalized));
    }
}
