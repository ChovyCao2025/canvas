package org.chovy.cache.aop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 分层缓存切面测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TieredCacheAspectTest {
    /**
     * 当前测试用例启动的 Spring 注解上下文。
     */
    private AnnotationConfigApplicationContext context;

    /**
     * 清理事务同步状态并关闭 Spring 测试上下文。
     */
    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (context != null) {
            context.close();
        }
    }

    /**
     * 验证声明式读取第二次调用命中缓存且不再执行目标方法。
     */
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

    /**
     * 验证 afterCommit 失效会延迟到事务提交后执行。
     */
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

    /**
     * 验证写入注解会把目标方法返回值放入既有缓存。
     */
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

    /**
     * 验证缓存尚未创建时失效注解保持空操作。
     */
    @Test
    void tieredCacheEvictIsNoOpWhenCacheHasNotBeenCreatedYet() {
        CachedService service = startContext().service();

        service.evictMissing("item");

        assertThat(service.loads()).isZero();
    }

    /**
     * 验证 condition 为 false 时跳过缓存读写。
     */
    @Test
    void tieredCachedSkipsCacheWhenConditionIsFalse() {
        CachedService service = startContext().service();

        assertThat(service.loadConditionally("item", false)).isEqualTo(new SampleValue("conditional-1"));
        assertThat(service.loadConditionally("item", false)).isEqualTo(new SampleValue("conditional-2"));

        assertThat(service.conditionalLoads()).isEqualTo(2);
    }

    /**
     * 验证 unless 命中返回值时不写入缓存。
     */
    @Test
    void tieredCachedDoesNotStoreWhenUnlessMatchesResult() {
        CachedService service = startContext().service();

        assertThat(service.loadUnless("item", "skip")).isEqualTo(new SampleValue("skip"));
        assertThat(service.loadUnless("item", "stored")).isEqualTo(new SampleValue("stored"));
        assertThat(service.loadUnless("item", "ignored")).isEqualTo(new SampleValue("stored"));
    }

    /**
     * 验证 Optional 返回值会正确拆包和重新包装。
     */
    @Test
    void tieredCachedUnwrapsAndRewrapsOptionalReturnValues() {
        CachedService service = startContext().service();

        assertThat(service.loadOptional("item")).contains(new SampleValue("optional-1"));
        assertThat(service.loadOptional("item")).contains(new SampleValue("optional-1"));
        assertThat(service.optionalLoads()).isEqualTo(1);
    }

    /**
     * 验证 Mono 返回值可以通过缓存切面处理。
     */
    @Test
    void tieredCachedSupportsMonoReturnValues() {
        CachedService service = startContext().service();

        assertThat(service.loadMono("item").block()).isEqualTo(new SampleValue("mono-1"));
        assertThat(service.loadMono("item").block()).isEqualTo(new SampleValue("mono-1"));
        assertThat(service.monoLoads()).isEqualTo(1);
    }

    /**
     * 启动包含缓存切面的最小 Spring 测试上下文。
     *
     * @return 测试用服务和 Redis mock 的聚合对象
     */
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
        /**
         * 创建同步 Redis 模板 mock。
         *
         * @param values Redis value 操作 mock
         * @return 同步 Redis 模板 mock
         */
        @Bean
        StringRedisTemplate redis(ValueOperations<String, String> values) {
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            when(redis.opsForValue()).thenReturn(values);
            return redis;
        }

        /**
         * 创建同步 Redis value 操作 mock。
         *
         * @return 同步 Redis value 操作 mock
         */
        @Bean
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values() {
            ValueOperations<String, String> values = mock(ValueOperations.class);
            when(values.get("annotation:v1:item")).thenReturn(null);
            return values;
        }

        /**
         * 创建响应式 Redis 模板 mock。
         *
         * @return 响应式 Redis 模板 mock
         */
        @Bean
        ReactiveStringRedisTemplate reactiveRedis() {
            return mock(ReactiveStringRedisTemplate.class);
        }

        /**
         * 创建测试用缓存管理器。
         *
         * @param redis 同步 Redis 模板 mock
         * @param reactiveRedis 响应式 Redis 模板 mock
         * @return 缓存管理器
         */
        @Bean
        TieredCacheManager cacheManager(StringRedisTemplate redis, ReactiveStringRedisTemplate reactiveRedis) {
            return new TieredCacheManager(redis, reactiveRedis, new SimpleMeterRegistry(), null);
        }

        /**
         * 创建 SpEL key 计算器。
         *
         * @return SpEL key 计算器
         */
        @Bean
        SpelKeyEvaluator spelKeyEvaluator() {
            return new SpelKeyEvaluator();
        }

        /**
         * 创建注解缓存解析器。
         *
         * @param manager 缓存管理器
         * @return 注解缓存解析器
         */
        @Bean
        AnnotationCacheResolver annotationCacheResolver(TieredCacheManager manager) {
            return new AnnotationCacheResolver(manager, new ObjectMapper());
        }

        /**
         * 创建分层缓存切面。
         *
         * @param resolver 注解缓存解析器
         * @param keyEvaluator SpEL key 计算器
         * @return 分层缓存切面
         */
        @Bean
        TieredCacheAspect tieredCacheAspect(AnnotationCacheResolver resolver, SpelKeyEvaluator keyEvaluator) {
            return new TieredCacheAspect(resolver, keyEvaluator);
        }

        /**
         * 创建被切面拦截的测试服务。
         *
         * @return 测试服务
         */
        @Bean
        CachedService cachedService() {
            return new CachedService();
        }
    }

    /**
     * 被缓存注解拦截的测试服务。
     */
    static class CachedService {
        /**
         * 普通缓存读取方法的加载次数。
         */
        private final AtomicInteger loads = new AtomicInteger();

        /**
         * condition 场景的加载次数。
         */
        private final AtomicInteger conditionalLoads = new AtomicInteger();

        /**
         * Optional 返回值场景的加载次数。
         */
        private final AtomicInteger optionalLoads = new AtomicInteger();

        /**
         * Mono 返回值场景的加载次数。
         */
        private final AtomicInteger monoLoads = new AtomicInteger();

        /**
         * 加载普通样例值。
         *
         * @param key 业务缓存 key
         * @return 样例值
         */
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

        /**
         * 提交事务后失效缓存。
         *
         * @param key 业务缓存 key
         */
        @TieredCacheEvict(name = "annotation", key = "#p0", afterCommit = true)
        public void evictAfterCommit(String key) {
        }

        /**
         * 替换缓存中的样例值。
         *
         * @param key 业务缓存 key
         * @param value 新的样例值
         * @return 新样例值
         */
        @TieredCachePut(name = "annotation", key = "#p0", afterCommit = false)
        public SampleValue replace(String key, String value) {
            return new SampleValue(value);
        }

        /**
         * 对不存在的缓存执行失效。
         *
         * @param key 业务缓存 key
         */
        @TieredCacheEvict(name = "missing-cache", key = "#p0", afterCommit = false)
        public void evictMissing(String key) {
        }

        /**
         * 按 condition 条件决定是否使用缓存。
         *
         * @param key 业务缓存 key
         * @param cacheable 是否允许本次调用使用缓存
         * @return 样例值
         */
        @TieredCached(
                name = "conditional",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "conditional:",
                condition = "#p1")
        public SampleValue loadConditionally(String key, boolean cacheable) {
            return new SampleValue("conditional-" + conditionalLoads.incrementAndGet());
        }

        /**
         * 按 unless 条件决定是否写回缓存。
         *
         * @param key 业务缓存 key
         * @param value 返回值内容
         * @return 样例值
         */
        @TieredCached(
                name = "unless",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "unless:",
                unless = "#result.value() == 'skip'")
        public SampleValue loadUnless(String key, String value) {
            return new SampleValue(value);
        }

        /**
         * 加载 Optional 包装的样例值。
         *
         * @param key 业务缓存 key
         * @return Optional 样例值
         */
        @TieredCached(
                name = "optional",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "optional:")
        public Optional<SampleValue> loadOptional(String key) {
            return Optional.of(new SampleValue("optional-" + optionalLoads.incrementAndGet()));
        }

        /**
         * 加载 Mono 包装的样例值。
         *
         * @param key 业务缓存 key
         * @return Mono 样例值
         */
        @TieredCached(
                name = "mono",
                key = "#p0",
                valueType = SampleValue.class,
                l2KeyPrefix = "mono:")
        public Mono<SampleValue> loadMono(String key) {
            return Mono.fromSupplier(() -> new SampleValue("mono-" + monoLoads.incrementAndGet()));
        }

        /**
         * 返回普通缓存读取方法的加载次数。
         *
         * @return 加载次数
         */
        int loads() {
            return loads.get();
        }

        /**
         * 返回 condition 场景的加载次数。
         *
         * @return 加载次数
         */
        int conditionalLoads() {
            return conditionalLoads.get();
        }

        /**
         * 返回 Optional 场景的加载次数。
         *
         * @return 加载次数
         */
        int optionalLoads() {
            return optionalLoads.get();
        }

        /**
         * 返回 Mono 场景的加载次数。
         *
         * @return 加载次数
         */
        int monoLoads() {
            return monoLoads.get();
        }
    }

    /**
     * AOP 测试用缓存值对象。
     */
    private static final class SampleValue {
        /**
         * 用于断言缓存命中和 SpEL unless 的字符串值。
         */
        @JsonProperty("value")
        private final String value;

        /**
         * 创建 AOP 测试缓存值。
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
         * 按字符串值比较 AOP 测试缓存值。
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
         * @return AOP 测试缓存值哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        /**
         * 返回与 record 语义一致的测试缓存值字符串。
         *
         * @return AOP 测试缓存值字符串
         */
        @Override
        public String toString() {
            return "SampleValue[value=" + value + ']';
        }
    }

    /**
     * Spring 测试上下文中常用 bean 的聚合对象。
     */
    private static final class TestBeans {
        /**
         * 被切面拦截的测试服务。
         */
        private final CachedService service;

        /**
         * 同步 Redis 模板 mock。
         */
        private final StringRedisTemplate redis;

        /**
         * 同步 Redis value 操作 mock。
         */
        private final ValueOperations<String, String> values;

        /**
         * 创建测试 bean 聚合对象。
         *
         * @param service 被切面拦截的测试服务
         * @param redis 同步 Redis 模板 mock
         * @param values 同步 Redis value 操作 mock
         */
        private TestBeans(CachedService service,
                          StringRedisTemplate redis,
                          ValueOperations<String, String> values) {
            this.service = service;
            this.redis = redis;
            this.values = values;
        }

        /**
         * 返回被切面拦截的测试服务。
         *
         * @return 测试服务
         */
        private CachedService service() {
            return service;
        }

        /**
         * 返回同步 Redis 模板 mock。
         *
         * @return 同步 Redis 模板 mock
         */
        private StringRedisTemplate redis() {
            return redis;
        }

        /**
         * 返回同步 Redis value 操作 mock。
         *
         * @return 同步 Redis value 操作 mock
         */
        private ValueOperations<String, String> values() {
            return values;
        }

        /**
         * 按聚合对象内的 bean 引用比较。
         *
         * @param o 待比较对象
         * @return 三个引用都相等时返回 true
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestBeans testBeans)) {
                return false;
            }
            return Objects.equals(service, testBeans.service)
                    && Objects.equals(redis, testBeans.redis)
                    && Objects.equals(values, testBeans.values);
        }

        /**
         * 生成与聚合 bean 引用匹配的哈希值。
         *
         * @return 测试 bean 聚合对象哈希值
         */
        @Override
        public int hashCode() {
            return Objects.hash(service, redis, values);
        }

        /**
         * 返回与 record 语义一致的测试 bean 聚合字符串。
         *
         * @return 测试 bean 聚合字符串
         */
        @Override
        public String toString() {
            return "TestBeans[service=" + service + ", redis=" + redis + ", values=" + values + ']';
        }
    }
}
