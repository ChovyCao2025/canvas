package org.chovy.cache.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.cache.TieredCache;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.annotation.TieredCached;
import org.chovy.cache.aop.AnnotationCacheResolver;
import org.chovy.cache.aop.SpelKeyEvaluator;
import org.chovy.cache.aop.TieredCacheAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class TestTieredCacheConfig {
    @Bean
    @Primary
    public AnnotationCacheResolver testAnnotationCacheResolver() {
        return new AnnotationCacheResolver(null, new ObjectMapper()) {
            private final Map<String, TieredCache<Object, Object>> caches = new ConcurrentHashMap<>();

            @Override
            public TieredCache<Object, Object> resolve(TieredCached annotation) {
                return caches.computeIfAbsent(annotation.name(), name ->
                        InMemoryTieredCache.of(name, key -> {
                            throw new IllegalStateException("Annotation test cache requires loaderOverride");
                        }));
            }

            @Override
            public TieredCache<Object, Object> getExisting(String name) {
                return caches.computeIfAbsent(name, cacheName ->
                        InMemoryTieredCache.of(cacheName, key -> null));
            }

            @Override
            public void evictIfPresent(String name, Object key) {
                TieredCache<Object, Object> cache = caches.get(name);
                if (cache != null) {
                    cache.invalidate(key);
                }
            }
        };
    }

    @Bean
    @Primary
    public SpelKeyEvaluator testSpelKeyEvaluator() {
        return new SpelKeyEvaluator();
    }

    @Bean
    @Primary
    public TieredCacheAspect testTieredCacheAspect(AnnotationCacheResolver resolver,
                                                   SpelKeyEvaluator keyEvaluator) {
        return new TieredCacheAspect(resolver, keyEvaluator);
    }

    @Bean
    @Primary
    public TieredCacheManager testTieredCacheManager() {
        return new TieredCacheManager(null, null, null, null);
    }
}
