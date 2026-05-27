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
     * 返回 TieredCached 的 name 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String name();

    /**
     * 返回 TieredCached 的 key 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String key();

    /**
     * 执行 value Type 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    Class<?> valueType();

    /**
     * 执行 condition 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String condition() default "";

    /**
     * 执行 unless 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String unless() default "";

    /**
     * 执行 cache Null 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    boolean cacheNull() default true;

    /**
     * 执行 penetration 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    PenetrationProtectionStrategy penetration() default PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL;

    /**
     * 执行 breakdown 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    BreakdownProtectionStrategy breakdown() default BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT;

    /**
     * 执行 avalanche 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    AvalancheProtectionStrategy avalanche() default AvalancheProtectionStrategy.TTL_JITTER;

    /**
     * 执行 l1 Max Size 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 计算得到的数值结果
     */
    int l1MaxSize() default 1000;

    /**
     * 执行 l1 Refresh After Write 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String l1RefreshAfterWrite() default "1h";

    /**
     * 执行 l2 Key Prefix 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String l2KeyPrefix() default "cache:";

    /**
     * 执行 l2 Ttl 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String l2Ttl() default "24h";

    /**
     * 执行 l2 Ttl Jitter 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 计算得到的数值结果
     */
    double l2TtlJitter() default 0.1;

    /**
     * 执行 key Schema Version 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 计算得到的数值结果
     */
    int keySchemaVersion() default 1;

    /**
     * 执行 null Value Ttl 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String nullValueTtl() default "1m";

    /**
     * 执行 empty Value Ttl 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String emptyValueTtl() default "1m";

    /**
     * 执行 lock Ttl 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String lockTtl() default "30s";

    /**
     * 更新或刷新 refresh Ahead 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String refreshAhead() default "0ms";

    /**
     * 执行 stale Ttl 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String staleTtl() default "30m";

    /**
     * 执行 hotspot Protection 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    boolean hotspotProtection() default false;

    /**
     * 消费或监听 on Loader Failure 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    LoaderFailureStrategy onLoaderFailure() default LoaderFailureStrategy.THROW;

    /**
     * 消费或监听 on Redis Read Failure 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    RedisFailureStrategy onRedisReadFailure() default RedisFailureStrategy.FALLTHROUGH;

    /**
     * 消费或监听 on Redis Write Failure 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    RedisFailureStrategy onRedisWriteFailure() default RedisFailureStrategy.FALLTHROUGH;

    /**
     * 消费或监听 on Deserialize Failure 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 方法执行后的业务结果
     */
    DeserializeFailureStrategy onDeserializeFailure() default DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
}
