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

/**
 * TestTieredCacheConfig 是分层缓存 SDK 的测试支撑组件。
 *
 * <p>用于在业务或单元测试中替代真实 Redis/多级缓存依赖，使缓存调用可以在内存中稳定验证。
 * <p>该组件只服务测试场景，不应承载生产缓存策略。
 */
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
