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
    /**
     * 指定要失效的缓存实例名称。
     *
     * <p>切面会通过 {@code TieredCacheManager} 按该名称查找缓存，名称必须与构建器注册的缓存一致。
     *
     * @return 缓存实例名称
     */
    String name();

    /**
     * 指定待失效 key 的 SpEL 表达式。
     *
     * <p>表达式可引用方法参数名、{@code #p0}/{@code #a0} 等变量，解析结果作为业务缓存 key。
     *
     * @return SpEL key 表达式
     */
    String key();

    /**
     * 是否在目标方法执行前失效缓存。
     *
     * <p>为 true 时即使目标方法随后失败，缓存也已经被清理；适合强制先清缓存再写入的场景。
     *
     * @return true 表示前置失效，false 表示目标方法成功后失效
     */
    boolean beforeInvocation() default false;

    /**
     * 是否等待事务提交后再执行后置失效。
     *
     * <p>仅在 {@link #beforeInvocation()} 为 false 且存在 Spring 事务同步时生效，可避免事务回滚后误删或提前暴露新状态。
     *
     * @return true 表示事务提交后失效，false 表示目标方法返回后立即失效
     */
    boolean afterCommit() default true;
}
