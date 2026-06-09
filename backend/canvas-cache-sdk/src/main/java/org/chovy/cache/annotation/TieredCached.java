package org.chovy.cache.annotation;

import org.chovy.cache.strategy.DeserializeFailureStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.RedisFailureStrategy;
import org.chovy.cache.strategy.AvalancheProtectionStrategy;
import org.chovy.cache.strategy.BreakdownProtectionStrategy;
import org.chovy.cache.strategy.PenetrationProtectionStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式分层缓存读取注解。
 *
 * <p>标注在方法上后由 AOP 根据 SpEL key 计算缓存键，命中缓存则跳过原方法，未命中时执行方法并回填缓存。
 * <p>该注解用于把缓存策略从业务方法中剥离，保持业务代码只表达数据获取逻辑。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TieredCached {
    /**
     * 指定读取使用的缓存实例名称。
     *
     * <p>首次解析时会按该名称创建或复用缓存实例，后续命中缓存时将跳过目标方法。
     *
     * @return 缓存实例名称
     */
    String name();

    /**
     * 指定业务缓存 key 的 SpEL 表达式。
     *
     * <p>表达式可引用方法参数名、{@code #p0}/{@code #a0} 等变量，解析结果会作为 L1/L2 的业务 key。
     *
     * @return SpEL key 表达式
     */
    String key();

    /**
     * 指定缓存值类型。
     *
     * <p>该类型用于 L2 Redis JSON 反序列化，应与目标方法返回值类型保持一致。
     *
     * @return 缓存值 Java 类型
     */
    Class<?> valueType();

    /**
     * 指定缓存读取的前置条件 SpEL。
     *
     * <p>表达式为空时默认启用缓存；表达式结果不是 {@code Boolean.TRUE} 时会直接执行目标方法且不读写缓存。
     *
     * @return 前置条件表达式
     */
    String condition() default "";

    /**
     * 指定缓存写回的排除条件 SpEL。
     *
     * <p>目标方法执行后可通过 {@code #result} 判断是否跳过写回；表达式为 true 时不缓存本次结果。
     *
     * @return 后置排除表达式
     */
    String unless() default "";

    /**
     * 是否缓存目标方法返回的 null。
     *
     * <p>为 true 时 null 会按 nullValueTtl 写入空值占位，降低不存在数据反复回源造成的穿透压力。
     *
     * @return true 表示缓存 null 占位
     */
    boolean cacheNull() default true;

    /**
     * 指定缓存穿透保护策略。
     *
     * <p>影响非法 key、布隆过滤器判空和 null/空结果的处理方式，默认短 TTL 缓存 null 占位。
     *
     * @return 穿透保护策略
     */
    PenetrationProtectionStrategy penetration() default PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL;

    /**
     * 指定缓存击穿保护策略。
     *
     * <p>影响未命中回源时是否合并本地并发、使用 Redis 分布式锁或组合互斥。
     *
     * @return 击穿保护策略
     */
    BreakdownProtectionStrategy breakdown() default BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT;

    /**
     * 指定缓存雪崩保护策略。
     *
     * <p>默认通过 L2 TTL 抖动分散过期时间，降低大量 key 同时失效后的集中回源风险。
     *
     * @return 雪崩保护策略
     */
    AvalancheProtectionStrategy avalanche() default AvalancheProtectionStrategy.TTL_JITTER;

    /**
     * 指定 L1 本地缓存最大条目数。
     *
     * <p>该值只限制当前 JVM 的 Caffeine 缓存，不影响 Redis L2 容量。
     *
     * @return L1 最大条目数
     */
    int l1MaxSize() default 1000;

    /**
     * 指定 L1 写入后的刷新间隔。
     *
     * <p>支持 ms/s/m/h/d 后缀或 ISO-8601 Duration；刷新仍会经过 L2/L3 缓存链路。
     *
     * @return 刷新间隔配置
     */
    String l1RefreshAfterWrite() default "1h";

    /**
     * 指定 L2 Redis key 前缀。
     *
     * <p>用于隔离不同业务域或环境中的 Redis 缓存数据。
     *
     * @return Redis key 前缀
     */
    String l2KeyPrefix() default "cache:";

    /**
     * 指定 L2 Redis 正常值 TTL。
     *
     * <p>业务值写入 Redis 时使用该时间；null 和空结果使用各自短 TTL。
     *
     * @return 正常值 TTL 配置
     */
    String l2Ttl() default "24h";

    /**
     * 指定 L2 TTL 抖动比例。
     *
     * <p>开启 TTL 抖动时实际过期时间会随机延长，避免大批 key 同时过期。
     *
     * @return 抖动比例，范围 0 到 1
     */
    double l2TtlJitter() default 0.1;

    /**
     * 指定缓存 key 结构版本。
     *
     * <p>业务 key 规则或值类型不兼容变更时提升版本，可让新旧 L2 数据自然隔离。
     *
     * @return key schema 版本号
     */
    int keySchemaVersion() default 1;

    /**
     * 指定 null 占位 TTL。
     *
     * <p>缓存 null 时使用该短 TTL，平衡穿透保护与新数据可见性。
     *
     * @return null 占位 TTL 配置
     */
    String nullValueTtl() default "1m";

    /**
     * 指定空集合或空结果 TTL。
     *
     * <p>用于非 null 但业务为空的结果，避免空查询频繁打到 L3。
     *
     * @return 空结果 TTL 配置
     */
    String emptyValueTtl() default "1m";

    /**
     * 指定分布式加载锁 TTL。
     *
     * <p>启用 Redis 分布式锁击穿保护时使用该过期时间，避免加载节点异常后锁长期残留。
     *
     * @return 分布式锁 TTL 配置
     */
    String lockTtl() default "30s";

    /**
     * 指定提前刷新窗口。
     *
     * <p>为 0 时不启用；非 0 值用于接近过期时提前刷新热点缓存。
     *
     * @return 提前刷新窗口配置
     */
    String refreshAhead() default "0ms";

    /**
     * 指定旧值兜底可用时间。
     *
     * <p>加载器失败且策略允许返回旧值时，最近成功值在该时间窗口内可作为兜底。
     *
     * @return 旧值兜底 TTL 配置
     */
    String staleTtl() default "30m";

    /**
     * 是否启用热点 key 保护。
     *
     * <p>开启后缓存构建器会增强击穿保护，减少热点 key 在多节点同时回源。
     *
     * @return true 表示启用热点保护
     */
    boolean hotspotProtection() default false;

    /**
     * 指定 L3 加载器失败策略。
     *
     * <p>影响目标方法或 loaderOverride 抛异常时，是抛出、返回空结果，还是使用旧值兜底。
     *
     * @return 加载器失败策略
     */
    LoaderFailureStrategy onLoaderFailure() default LoaderFailureStrategy.THROW;

    /**
     * 指定 Redis 读取失败策略。
     *
     * <p>默认降级继续回源 L3，避免 L2 短暂不可用直接导致业务读取失败。
     *
     * @return Redis 读取失败策略
     */
    RedisFailureStrategy onRedisReadFailure() default RedisFailureStrategy.FALLTHROUGH;

    /**
     * 指定 Redis 写入失败策略。
     *
     * <p>默认忽略 L2 写入失败并保留业务返回值，避免缓存写入故障扩大为主链路故障。
     *
     * @return Redis 写入失败策略
     */
    RedisFailureStrategy onRedisWriteFailure() default RedisFailureStrategy.FALLTHROUGH;

    /**
     * 指定 L2 反序列化失败策略。
     *
     * <p>默认跳过损坏的 Redis 值并回源 L3，减少脏缓存对业务读取的影响。
     *
     * @return 反序列化失败策略
     */
    DeserializeFailureStrategy onDeserializeFailure() default DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
}
