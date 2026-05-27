package org.chovy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式分层缓存写入注解。
 *
 * <p>标注方法执行成功后会将返回值写入指定缓存，可用于主动刷新热点 key 或同步更新结果。
 * <p>写入行为由切面统一处理，调用方不需要手动获取 TieredCache 实例。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TieredCachePut {
    String name();

    String key();

    boolean afterCommit() default true;
}
