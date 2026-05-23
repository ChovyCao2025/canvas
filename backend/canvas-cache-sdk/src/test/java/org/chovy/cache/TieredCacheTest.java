package org.chovy.cache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TieredCacheTest {

    @Mock StringRedisTemplate redis;
    @Mock ReactiveStringRedisTemplate reactiveRedis;
    @Mock ValueOperations<String, String> values;

    private TieredCacheManager manager;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(values);
        manager = new TieredCacheManager(redis, reactiveRedis, new SimpleMeterRegistry(), null);
    }

    @Test
    void get_loadsFromLoaderOnceAndServesSecondReadFromL1() {
        when(values.get("sample:v1:42")).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> {
            loads.incrementAndGet();
            return new SampleValue("value-" + key);
        });

        Optional<SampleValue> first = cache.get(42);
        Optional<SampleValue> second = cache.get(42);

        assertThat(first).contains(new SampleValue("value-42"));
        assertThat(second).contains(new SampleValue("value-42"));
        assertThat(loads).hasValue(1);
        verify(values, times(1)).get("sample:v1:42");
        verify(values).set(eq("sample:v1:42"), contains("value-42"), any(Duration.class));
    }

    @Test
    void get_usesLoaderOverrideForAnnotationStyleLoading() {
        TieredCache<String, SampleValue> cache = TieredCacheBuilder.<String, SampleValue>builder()
                .name("override")
                .l2KeyPrefix("override:")
                .loader(key -> { throw new IllegalStateException("default loader should not run"); })
                .valueType(SampleValue.class)
                .build(manager);
        when(values.get("override:v1:a")).thenReturn(null);

        Optional<SampleValue> first = cache.get("a", () -> new SampleValue("from-method"));
        Optional<SampleValue> second = cache.get("a", () -> new SampleValue("ignored"));

        assertThat(first).contains(new SampleValue("from-method"));
        assertThat(second).contains(new SampleValue("from-method"));
        verify(values, times(1)).get("override:v1:a");
    }

    @Test
    void get_returnsEmptyForNullSentinelWithoutCallingLoader() {
        when(values.get("sample:v1:99")).thenReturn("__NULL__");
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> {
            loads.incrementAndGet();
            return new SampleValue("unused");
        });

        Optional<SampleValue> result = cache.get(99);

        assertThat(result).isEmpty();
        assertThat(loads).hasValue(0);
    }

    @Test
    void get_writesNullSentinelWhenLoaderReturnsNull() {
        when(values.get("sample:v1:7")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> null);

        Optional<SampleValue> result = cache.get(7);

        assertThat(result).isEmpty();
        verify(values).set("sample:v1:7", "__NULL__", Duration.ofMinutes(5));
    }

    @Test
    void invalidate_deletesL2AndPublishesInvalidation() {
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("unused"));

        cache.invalidate(42);

        verify(redis).delete("sample:v1:42");
        verify(redis).convertAndSend("tiered-cache:sample:invalidate", "42");
    }

    @Test
    void get_fallsThroughToLoaderWhenRedisReadFailsByDefault() {
        when(values.get("sample:v1:5")).thenThrow(new RuntimeException("redis down"));
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("fallback"));

        Optional<SampleValue> result = cache.get(5);

        assertThat(result).contains(new SampleValue("fallback"));
    }

    @Test
    void get_failsFastWhenConfiguredForRedisReadFailure() {
        when(values.get("sample:v1:5")).thenThrow(new RuntimeException("redis down"));
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .onRedisReadFailure(org.chovy.cache.strategy.RedisFailureStrategy.FAIL_FAST)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);

        assertThatThrownBy(() -> cache.get(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("redis down");
    }

    @Test
    void get_deletesBadL2ValueAndReloadsFromLoader() {
        when(values.get("sample:v1:13")).thenReturn("{bad-json");
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("reloaded"));

        Optional<SampleValue> result = cache.get(13);

        assertThat(result).contains(new SampleValue("reloaded"));
        verify(redis).delete("sample:v1:13");
    }

    @Test
    void reactiveViewSharesUnderlyingCache() {
        when(values.get("sample:v1:8")).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> {
            loads.incrementAndGet();
            return new SampleValue("reactive");
        });

        Optional<SampleValue> result = cache.asReactive().get(8).block();
        Optional<SampleValue> second = cache.get(8);

        assertThat(result).contains(new SampleValue("reactive"));
        assertThat(second).contains(new SampleValue("reactive"));
        assertThat(loads).hasValue(1);
    }

    private TieredCache<Integer, SampleValue> sampleCache(java.util.function.Function<Integer, SampleValue> loader) {
        return TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .l2Ttl(Duration.ofHours(1))
                .nullValueTtl(Duration.ofMinutes(5))
                .loader(loader)
                .valueType(SampleValue.class)
                .build(manager);
    }

    record SampleValue(String value) {}
}
