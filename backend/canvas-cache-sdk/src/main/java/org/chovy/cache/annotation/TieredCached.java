package org.chovy.cache.annotation;

import org.chovy.cache.strategy.DeserializeFailureStrategy;
import org.chovy.cache.strategy.LoaderFailureStrategy;
import org.chovy.cache.strategy.RedisFailureStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TieredCached {
    String name();

    String key();

    Class<?> valueType();

    int l1MaxSize() default 1000;

    String l1RefreshAfterWrite() default "1h";

    String l2KeyPrefix() default "cache:";

    String l2Ttl() default "24h";

    double l2TtlJitter() default 0.1;

    int keySchemaVersion() default 1;

    String nullValueTtl() default "5m";

    boolean hotspotProtection() default false;

    LoaderFailureStrategy onLoaderFailure() default LoaderFailureStrategy.THROW;

    RedisFailureStrategy onRedisReadFailure() default RedisFailureStrategy.FALLTHROUGH;

    RedisFailureStrategy onRedisWriteFailure() default RedisFailureStrategy.FALLTHROUGH;

    DeserializeFailureStrategy onDeserializeFailure() default DeserializeFailureStrategy.FALLTHROUGH_TO_L3;
}
