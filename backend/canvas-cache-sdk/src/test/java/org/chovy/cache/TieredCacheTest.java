package org.chovy.cache;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.chovy.cache.strategy.AvalancheProtectionStrategy;
import org.chovy.cache.strategy.BreakdownProtectionStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.PenetrationProtectionStrategy;

/**
 * 分层缓存核心实现测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class TieredCacheTest {

    /**
     * 模拟的同步 Redis 模板。
     */
    @Mock StringRedisTemplate redis;

    /**
     * 模拟的响应式 Redis 模板。
     */
    @Mock ReactiveStringRedisTemplate reactiveRedis;

    /**
     * 模拟的同步 Redis value 操作。
     */
    @Mock ValueOperations<String, String> values;

    /**
     * 模拟的响应式 Redis value 操作。
     */
    @Mock ReactiveValueOperations<String, String> reactiveValues;

    /**
     * 每个测试用例使用的缓存管理器。
     */
    private TieredCacheManager manager;

    /**
     * 初始化 Redis mock 和缓存管理器。
     */
    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(reactiveRedis.opsForValue()).thenReturn(reactiveValues);
        lenient().when(values.get(contains(":__invalidate__:"))).thenReturn(null);
        manager = new TieredCacheManager(redis, reactiveRedis, new SimpleMeterRegistry(), null);
    }

    /**
     * 验证首次读取回源加载且第二次读取命中 L1。
     */
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

    /**
     * 验证 L1 条目的过期时间受配置的 L2 TTL 约束。
     */
    @Test
    void l1EntryExpiresWithConfiguredL2Ttl() throws Exception {
        when(values.get("short:v1:1")).thenReturn(null, null);
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("short")
                .l2KeyPrefix("short:")
                .l2Ttl(Duration.ofMillis(50))
                .l1RefreshAfterWrite(Duration.ofSeconds(10))
                .loader(key -> new SampleValue("value-" + loads.incrementAndGet()))
                .valueType(SampleValue.class)
                .build(manager);

        Optional<SampleValue> first = cache.get(1);
        Thread.sleep(90);
        Optional<SampleValue> second = cache.get(1);

        assertThat(first).contains(new SampleValue("value-1"));
        assertThat(second).contains(new SampleValue("value-2"));
        assertThat(loads).hasValue(2);
        verify(values, times(2)).get("short:v1:1");
    }

    /**
     * 验证读取方法可使用调用级 loaderOverride 回源。
     */
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

    /**
     * 验证命中 null 哨兵时返回空结果且不回源。
     */
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

    /**
     * 验证加载器返回 null 时写入 null 哨兵。
     */
    @Test
    void get_writesNullSentinelWhenLoaderReturnsNull() {
        when(values.get("sample:v1:7")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> null);

        Optional<SampleValue> result = cache.get(7);

        assertThat(result).isEmpty();
        verify(values).set("sample:v1:7", "__NULL__", Duration.ofMinutes(5));
    }

    /**
     * 验证失效操作删除 L2 并发布失效消息。
     */
    @Test
    void invalidate_deletesL2AndPublishesInvalidation() {
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("unused"));
        when(values.increment("sample:v1:__invalidate__:42")).thenReturn(1L);

        cache.invalidate(42);

        verify(redis).delete("sample:v1:42");
        verify(redis).expire("sample:v1:__invalidate__:42", Duration.ofHours(2));
        verify(redis).convertAndSend("tiered-cache:sample:invalidate", "42");
    }

    /**
     * 验证失效操作会调用外部失效发布器。
     */
    @Test
    void invalidatePublishesExternalInvalidationTransport() {
        CacheInvalidationPublisher publisher = mock(CacheInvalidationPublisher.class);
        manager = new TieredCacheManager(redis, reactiveRedis, new SimpleMeterRegistry(), null, List.of(publisher));
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("unused"));
        when(values.increment("sample:v1:__invalidate__:42")).thenReturn(3L);

        cache.invalidate(42);

        verify(publisher).publish(new CacheInvalidationEvent("sample", "42", 3L));
    }

    /**
     * 验证安全写入会异步调度第二次失效且不阻塞调用方。
     */
    @Test
    void safeWriteSchedulesDelayedSecondInvalidationWithoutBlockingCaller() throws Exception {
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("unused"));
        when(values.increment("sample:v1:__invalidate__:42")).thenReturn(1L, 2L);

        long start = System.nanoTime();
        cache.safeWrite(42, () -> {}, 500);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(elapsedMs).isLessThan(100);
        verify(redis, times(1)).delete("sample:v1:42");

        Thread.sleep(650);
        verify(redis, times(2)).delete("sample:v1:42");
        verify(values, times(2)).increment("sample:v1:__invalidate__:42");
        verify(redis, times(2)).convertAndSend("tiered-cache:sample:invalidate", "42");
    }

    /**
     * 验证失效版本推进后本地缓存会被视为过期。
     */
    @Test
    void getInvalidatesLocalEntryWhenInvalidationVersionAdvanced() {
        when(values.get("sample:v1:42")).thenReturn(null, null);
        when(values.get("sample:v1:__invalidate__:42")).thenReturn(null, "1", "1");
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = sampleCache(key ->
                new SampleValue("value-" + loads.incrementAndGet()));

        Optional<SampleValue> first = cache.get(42);
        Optional<SampleValue> second = cache.get(42);

        assertThat(first).contains(new SampleValue("value-1"));
        assertThat(second).contains(new SampleValue("value-2"));
        assertThat(loads).hasValue(2);
        verify(values, times(2)).get("sample:v1:42");
    }

    /**
     * 验证默认 Redis 读取失败策略会降级回源。
     */
    @Test
    void get_fallsThroughToLoaderWhenRedisReadFailsByDefault() {
        when(values.get("sample:v1:5")).thenThrow(new RuntimeException("redis down"));
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("fallback"));

        Optional<SampleValue> result = cache.get(5);

        assertThat(result).contains(new SampleValue("fallback"));
    }

    /**
     * 验证配置为快速失败时 Redis 读取异常会向外抛出。
     */
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

    /**
     * 验证 L2 损坏值会被删除并重新回源加载。
     */
    @Test
    void get_deletesBadL2ValueAndReloadsFromLoader() {
        when(values.get("sample:v1:13")).thenReturn("{bad-json");
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("reloaded"));

        Optional<SampleValue> result = cache.get(13);

        assertThat(result).contains(new SampleValue("reloaded"));
        verify(redis).delete("sample:v1:13");
    }

    /**
     * 验证响应式视图复用底层同步缓存状态。
     */
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

    /**
     * 验证构建器拒绝缺失必要参数的无效配置。
     */
    @Test
    void builderRejectsInvalidConfiguration() {
        assertThatThrownBy(() -> TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("bad")
                .l1MaxSize(0)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("l1MaxSize");

        assertThatThrownBy(() -> TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("bad")
                .l2TtlJitter(1.5)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("l2TtlJitter");
    }

    /**
     * 验证更换 ObjectMapper 后仍保留 TypeReference 泛型类型。
     */
    @Test
    void objectMapperChangePreservesTypeReferenceGenericType() {
        when(values.get("list:v1:1")).thenReturn("[{\"value\":\"a\"}]");
        TieredCache<Integer, java.util.List<SampleValue>> cache =
                TieredCacheBuilder.<Integer, java.util.List<SampleValue>>builder()
                        .name("list")
                        .l2KeyPrefix("list:")
                        .valueType(new com.fasterxml.jackson.core.type.TypeReference<java.util.List<SampleValue>>() {})
                        .objectMapper(new com.fasterxml.jackson.databind.ObjectMapper())
                        .loader(key -> java.util.List.of())
                        .build(manager);

        Optional<java.util.List<SampleValue>> result = cache.get(1);

        assertThat(result).isPresent();
        assertThat(result.get().get(0)).isEqualTo(new SampleValue("a"));
    }

    /**
     * 验证批量读取只对未命中 key 执行批量加载。
     */
    @Test
    void getAllUsesBatchLoaderOnlyForMissingKeys() {
        when(values.get("sample:v1:1")).thenReturn("{\"value\":\"one\"}");
        when(values.get("sample:v1:2")).thenReturn(null);
        when(values.get("sample:v1:3")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("unused"));

        Map<Integer, Optional<SampleValue>> result = cache.getAll(List.of(1, 2, 3),
                missing -> Map.of(2, new SampleValue("two"), 3, new SampleValue("three")));

        assertThat(result).containsEntry(1, Optional.of(new SampleValue("one")));
        assertThat(result).containsEntry(2, Optional.of(new SampleValue("two")));
        assertThat(result).containsEntry(3, Optional.of(new SampleValue("three")));
        verify(values).set(eq("sample:v1:2"), contains("two"), any(Duration.class));
        verify(values).set(eq("sample:v1:3"), contains("three"), any(Duration.class));
    }

    /**
     * 验证批量加载器返回 null 时按空结果处理。
     */
    @Test
    void getAllTreatsNullBatchLoaderResultAsEmptyResult() {
        when(values.get("sample:v1:1")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("unused"));

        Map<Integer, Optional<SampleValue>> result = cache.getAll(List.of(1), missing -> null);

        assertThat(result).containsEntry(1, Optional.empty());
    }

    /**
     * 验证批量加载失败且策略返回空时得到空结果。
     */
    @Test
    void getAllReturnsEmptyWhenBatchLoaderFailsAndStrategyReturnsEmpty() {
        when(values.get("sample:v1:1")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .onLoaderFailure(LoaderFailureStrategy.RETURN_EMPTY)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);

        Map<Integer, Optional<SampleValue>> result = cache.getAll(List.of(1), missing -> {
            throw new IllegalStateException("db down");
        });

        assertThat(result).containsEntry(1, Optional.empty());
    }

    /**
     * 验证批量加载失败且启用旧值兜底时返回最近旧值。
     */
    @Test
    void getAllReturnsStaleValueWhenBatchLoaderFailsAndStaleOnErrorIsEnabled() {
        when(values.get("sample:v1:88")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .avalanche(AvalancheProtectionStrategy.STALE_ON_ERROR)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);
        cache.put(88, new SampleValue("stale"));
        cache.invalidate(88);

        Map<Integer, Optional<SampleValue>> result = cache.getAll(List.of(88), missing -> {
            throw new IllegalStateException("db down");
        });

        assertThat(result).containsEntry(88, Optional.of(new SampleValue("stale")));
    }

    /**
     * 验证预热会加载 key 且统计快照反映缓存活动。
     */
    @Test
    void warmupLoadsKeysAndStatsExposeCacheActivity() {
        when(values.get("sample:v1:1")).thenReturn(null);
        when(values.get("sample:v1:2")).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = sampleCache(key -> new SampleValue("value-" + loads.incrementAndGet()));

        cache.warmup(List.of(1, 2));
        cache.get(1);

        assertThat(loads).hasValue(2);
        assertThat(cache.stats().name()).isEqualTo("sample");
        assertThat(cache.stats().l1HitCount()).isGreaterThanOrEqualTo(1);
        assertThat(cache.stats().l1Size()).isEqualTo(2);
    }

    /**
     * 验证 key 校验器在 Redis 和加载器前拒绝非法 key。
     */
    @Test
    void penetrationValidatorRejectsInvalidKeyBeforeRedisAndLoader() {
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .penetration(PenetrationProtectionStrategy.KEY_VALIDATOR)
                .keyValidator(key -> key > 0)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);

        assertThat(cache.get(-1)).isEmpty();

        verify(values, never()).get(anyString());
    }

    /**
     * 验证布隆过滤器在 Redis 和加载器前拒绝明确不存在的 key。
     */
    @Test
    void penetrationBloomRejectsDefinitelyMissingKeyBeforeRedisAndLoader() {
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .penetration(PenetrationProtectionStrategy.BLOOM_FILTER)
                .bloomFilter(new CacheBloomFilter<>() {
                    @Override public boolean mightContain(Integer key) { return false; }
                    @Override public void put(Integer key) {}
                })
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);

        assertThat(cache.get(123)).isEmpty();

        verify(values, never()).get(anyString());
    }

    /**
     * 验证空集合结果使用短 TTL 写入 L2。
     */
    @Test
    void emptyCollectionUsesShortEmptyValueTtl() {
        when(values.get("list:v1:1")).thenReturn(null);
        TieredCache<Integer, List<SampleValue>> cache =
                TieredCacheBuilder.<Integer, List<SampleValue>>builder()
                        .name("list")
                        .l2KeyPrefix("list:")
                        .penetration(PenetrationProtectionStrategy.CACHE_EMPTY_SHORT_TTL)
                        .emptyValueTtl(Duration.ofSeconds(20))
                        .loader(key -> List.of())
                        .valueType(new com.fasterxml.jackson.core.type.TypeReference<List<SampleValue>>() {})
                        .build(manager);

        assertThat(cache.get(1)).contains(List.of());

        verify(values).set("list:v1:1", "[]", Duration.ofSeconds(20));
    }

    /**
     * 验证相同 key 的并发 loaderOverride 调用会复用本地 single-flight 结果。
     */
    @Test
    void localSingleFlightSharesConcurrentLoaderOverrideForSameKey() throws Exception {
        when(values.get("sample:v1:77")).thenReturn(null);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .breakdown(BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> cache.get(77, () -> {
                loads.incrementAndGet();
                entered.countDown();
                try {
                    release.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new SampleValue("shared");
            }));
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> cache.get(77, () -> {
                loads.incrementAndGet();
                return new SampleValue("duplicate");
            }));
            Thread.sleep(100);
            release.countDown();

            assertThat(first.get()).contains(new SampleValue("shared"));
            assertThat(second.get()).contains(new SampleValue("shared"));
        }
        assertThat(loads).hasValue(1);
    }

    /**
     * 验证加载失败时可返回最近一次成功写入的旧值。
     */
    @Test
    void staleOnErrorReturnsLastKnownValueWhenLoaderFails() {
        when(values.get("sample:v1:88")).thenReturn(null);
        TieredCache<Integer, SampleValue> cache = TieredCacheBuilder.<Integer, SampleValue>builder()
                .name("sample")
                .l2KeyPrefix("sample:")
                .avalanche(AvalancheProtectionStrategy.STALE_ON_ERROR)
                .loader(key -> new SampleValue("unused"))
                .valueType(SampleValue.class)
                .build(manager);
        cache.put(88, new SampleValue("stale"));
        cache.invalidate(88);

        Optional<SampleValue> result = cache.get(88, () -> {
            throw new IllegalStateException("db down");
        });

        assertThat(result).contains(new SampleValue("stale"));
    }

    /**
     * 构造使用标准测试配置的样例缓存。
     *
     * @param loader 测试场景提供的 L3 加载器
     * @return 可用于断言的样例缓存
     */
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

    /**
     * 测试用缓存值对象。
     */
    private static final class SampleValue {
        /**
         * 用于断言缓存命中和序列化结果的字符串值。
         */
        @JsonProperty("value")
        private final String value;

        /**
         * 创建测试缓存值。
         *
         * @param value 用于断言的字符串值
         */
        @JsonCreator
        private SampleValue(@JsonProperty("value") String value) {
            this.value = value;
        }

        /**
         * 返回用于断言的字符串值。
         *
         * @return 字符串值
         */
        public String value() {
            return value;
        }

        /**
         * 按字符串值比较测试缓存值。
         *
         * @param o 待比较对象
         * @return 字符串值相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SampleValue that)) {
                return false;
            }
            return Objects.equals(value, that.value);
        }

        /**
         * 生成与字符串值匹配的哈希值。
         *
         * @return 测试缓存值哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        /**
         * 返回与 record 语义一致的测试缓存值字符串。
         *
         * @return 测试缓存值字符串
         */
        @Override
        public String toString() {
            return "SampleValue[value=" + value + ']';
        }
    }
}
