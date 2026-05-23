package org.chovy.cache.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.chovy.cache.TieredCacheManager;
import org.chovy.cache.annotation.TieredCacheEvict;
import org.chovy.cache.annotation.TieredCachePut;
import org.chovy.cache.annotation.TieredCached;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TieredCacheAspectTest {
    private AnnotationConfigApplicationContext context;

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (context != null) {
            context.close();
        }
    }

    @Test
    void tieredCached_servesSecondCallFromCacheWithoutInvokingMethod() {
        TestBeans beans = startContext();
        CachedService service = beans.service();

        SampleValue first = service.load("item");
        SampleValue second = service.load("item");

        assertThat(first).isEqualTo(new SampleValue("loaded-1"));
        assertThat(second).isEqualTo(new SampleValue("loaded-1"));
        assertThat(service.loads()).isEqualTo(1);
        verify(beans.values(), times(1)).get("annotation:v1:item");
    }

    @Test
    void tieredCacheEvict_withAfterCommitDefersInvalidationUntilCommit() {
        TestBeans beans = startContext();
        CachedService service = beans.service();

        assertThat(service.load("item")).isEqualTo(new SampleValue("loaded-1"));
        TransactionSynchronizationManager.initSynchronization();
        service.evictAfterCommit("item");

        assertThat(service.load("item")).isEqualTo(new SampleValue("loaded-1"));
        assertThat(service.loads()).isEqualTo(1);

        TransactionSynchronizationUtils.triggerAfterCommit();
        TransactionSynchronizationManager.clearSynchronization();

        assertThat(service.load("item")).isEqualTo(new SampleValue("loaded-2"));
        verify(beans.redis()).delete("annotation:v1:item");
    }

    @Test
    void tieredCachePutStoresReturnedValueInExistingCache() {
        TestBeans beans = startContext();
        CachedService service = beans.service();

        assertThat(service.load("item")).isEqualTo(new SampleValue("loaded-1"));
        assertThat(service.replace("item", "fresh")).isEqualTo(new SampleValue("fresh"));
        assertThat(service.load("item")).isEqualTo(new SampleValue("fresh"));
        assertThat(service.loads()).isEqualTo(1);
        verify(beans.values()).set(eq("annotation:v1:item"), contains("fresh"), any(java.time.Duration.class));
    }

    @Test
    void tieredCacheEvictIsNoOpWhenCacheHasNotBeenCreatedYet() {
        CachedService service = startContext().service();

        service.evictMissing("item");

        assertThat(service.loads()).isZero();
    }

    @Test
    void tieredCachedSkipsCacheWhenConditionIsFalse() {
        CachedService service = startContext().service();

        assertThat(service.loadConditionally("item", false)).isEqualTo(new SampleValue("conditional-1"));
        assertThat(service.loadConditionally("item", false)).isEqualTo(new SampleValue("conditional-2"));

        assertThat(service.conditionalLoads()).isEqualTo(2);
    }

    @Test
    void tieredCachedDoesNotStoreWhenUnlessMatchesResult() {
        CachedService service = startContext().service();

        assertThat(service.loadUnless("item", "skip")).isEqualTo(new SampleValue("skip"));
        assertThat(service.loadUnless("item", "stored")).isEqualTo(new SampleValue("stored"));
        assertThat(service.loadUnless("item", "ignored")).isEqualTo(new SampleValue("stored"));
    }

    @Test
    void tieredCachedUnwrapsAndRewrapsOptionalReturnValues() {
        CachedService service = startContext().service();

        assertThat(service.loadOptional("item")).contains(new SampleValue("optional-1"));
        assertThat(service.loadOptional("item")).contains(new SampleValue("optional-1"));
        assertThat(service.optionalLoads()).isEqualTo(1);
    }

    @Test
    void tieredCachedSupportsMonoReturnValues() {
        CachedService service = startContext().service();

        assertThat(service.loadMono("item").block()).isEqualTo(new SampleValue("mono-1"));
        assertThat(service.loadMono("item").block()).isEqualTo(new SampleValue("mono-1"));
        assertThat(service.monoLoads()).isEqualTo(1);
    }

    private TestBeans startContext() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        return new TestBeans(
                context.getBean(CachedService.class),
                context.getBean(StringRedisTemplate.class),
                context.getBean(ValueOperations.class));
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean
        StringRedisTemplate redis(ValueOperations<String, String> values) {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            when(redis.opsForValue()).thenReturn(values);
            return redis;
        }

        @Bean
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values() {
            ValueOperations<String, String> values = mock(ValueOperations.class);
            when(values.get("annotation:v1:item")).thenReturn(null);
            return values;
        }

        @Bean
        ReactiveStringRedisTemplate reactiveRedis() {
            return mock(ReactiveStringRedisTemplate.class);
        }

        @Bean
        TieredCacheManager cacheManager(StringRedisTemplate redis, ReactiveStringRedisTemplate reactiveRedis) {
            return new TieredCacheManager(redis, reactiveRedis, new SimpleMeterRegistry(), null);
        }

        @Bean
        SpelKeyEvaluator spelKeyEvaluator() {
            return new SpelKeyEvaluator();
        }

        @Bean
        AnnotationCacheResolver annotationCacheResolver(TieredCacheManager manager) {
            return new AnnotationCacheResolver(manager, new ObjectMapper());
        }

        @Bean
        TieredCacheAspect tieredCacheAspect(AnnotationCacheResolver resolver, SpelKeyEvaluator keyEvaluator) {
            return new TieredCacheAspect(resolver, keyEvaluator);
        }

        @Bean
        CachedService cachedService() {
            return new CachedService();
        }
    }

    static class CachedService {
        private final AtomicInteger loads = new AtomicInteger();
        private final AtomicInteger conditionalLoads = new AtomicInteger();
        private final AtomicInteger optionalLoads = new AtomicInteger();
        private final AtomicInteger monoLoads = new AtomicInteger();

        @TieredCached(
                name = "annotation",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "annotation:",
                l1RefreshAfterWrite = "1h",
                l2Ttl = "1h")
        public SampleValue load(String key) {
            return new SampleValue("loaded-" + loads.incrementAndGet());
        }

        @TieredCacheEvict(name = "annotation", key = "#p0", afterCommit = true)
        public void evictAfterCommit(String key) {
        }

        @TieredCachePut(name = "annotation", key = "#p0", afterCommit = false)
        public SampleValue replace(String key, String value) {
            return new SampleValue(value);
        }

        @TieredCacheEvict(name = "missing-cache", key = "#p0", afterCommit = false)
        public void evictMissing(String key) {
        }

        @TieredCached(
                name = "conditional",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "conditional:",
                condition = "#p1")
        public SampleValue loadConditionally(String key, boolean cacheable) {
            return new SampleValue("conditional-" + conditionalLoads.incrementAndGet());
        }

        @TieredCached(
                name = "unless",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "unless:",
                unless = "#result.value() == 'skip'")
        public SampleValue loadUnless(String key, String value) {
            return new SampleValue(value);
        }

        @TieredCached(
                name = "optional",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "optional:")
        public Optional<SampleValue> loadOptional(String key) {
            return Optional.of(new SampleValue("optional-" + optionalLoads.incrementAndGet()));
        }

        @TieredCached(
                name = "mono",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "mono:")
        public Mono<SampleValue> loadMono(String key) {
            return Mono.fromSupplier(() -> new SampleValue("mono-" + monoLoads.incrementAndGet()));
        }

        int loads() {
            return loads.get();
        }

        int conditionalLoads() {
            return conditionalLoads.get();
        }

        int optionalLoads() {
            return optionalLoads.get();
        }

        int monoLoads() {
            return monoLoads.get();
        }
    }

    record SampleValue(String value) {}

    record TestBeans(CachedService service,
                     StringRedisTemplate redis,
                     ValueOperations<String, String> values) {}
}
