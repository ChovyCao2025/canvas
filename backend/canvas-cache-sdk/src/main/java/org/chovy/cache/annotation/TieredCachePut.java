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
    /**
     * 指定要写入的缓存实例名称。
     *
     * <p>切面会通过该名称定位已注册缓存；不存在时会抛出配置错误，避免静默丢失缓存写入。
     *
     * @return 缓存实例名称
     */
    String name();

    /**
     * 指定写入 key 的 SpEL 表达式。
     *
     * <p>表达式可引用方法参数和返回结果变量 {@code #result}，用于把更新后的对象写回对应缓存 key。
     *
     * @return SpEL key 表达式
     */
    String key();

    /**
     * 是否等待事务提交后再写入缓存。
     *
     * <p>默认在事务提交后写入，避免数据库回滚但缓存已经保存新返回值导致脏读。
     *
     * @return true 表示提交后写入，false 表示目标方法返回后立即写入
     */
    boolean afterCommit() default true;
}
