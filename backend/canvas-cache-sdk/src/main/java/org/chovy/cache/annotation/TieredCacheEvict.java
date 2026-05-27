package org.chovy.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式分层缓存失效注解。
 *
 * <p>标注方法执行后按 SpEL key 清理对应缓存项，用于更新、删除或状态变更后的缓存一致性维护。
 * <p>该注解只表达失效意图，具体是否延迟双删或级联清理由缓存实现策略决定。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TieredCacheEvict {
    String name();

    String key();

    boolean beforeInvocation() default false;

    boolean afterCommit() default true;
}
