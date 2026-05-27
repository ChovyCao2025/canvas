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
    String name();

    String key();

    Class<?> valueType();

    String condition() default "";

    String unless() default "";

    boolean cacheNull() default true;

    PenetrationProtectionStrategy penetration() default PenetrationProtectionStrategy.CACHE_NULL_SHORT_TTL;

    BreakdownProtectionStrategy breakdown() default BreakdownProtectionStrategy.LOCAL_SINGLE_FLIGHT;

    AvalancheProtectionStrategy avalanche() default AvalancheProtectionStrategy.TTL_JITTER;

    int l1MaxSize() default 1000;

    String l1RefreshAfterWrite() default "1h";

    String l2KeyPrefix() default "cache:";

    String l2Ttl() default "24h";

    double l2TtlJitter() default 0.1;

    int keySchemaVersion() default 1;

    String nullValueTtl() default "1m";

    String emptyValueTtl() default "1m";

    String lockTtl() default "30s";

    String refreshAhead() default "0ms";

    String staleTtl() default "30m";

    boolean hotspotProtection() default false;

    LoaderFailureStrategy onLoaderFailure() default LoaderFailureStrategy.THROW;

    RedisFailureStrategy onRedisReadFailure() default RedisFailureStrategy.FALLTHROUGH;

    RedisFailureStrategy onRedisWriteFailure() default RedisFailureStrategy.FALLTHROUGH;

    DeserializeFailureStrategy onDeserializeFailure() default DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
}
