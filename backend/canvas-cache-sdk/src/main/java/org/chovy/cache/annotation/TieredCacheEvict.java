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
     * 返回 TieredCacheEvict 的 name 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String name();

    /**
     * 返回 TieredCacheEvict 的 key 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String key();

    /**
     * 执行 before Invocation 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    boolean beforeInvocation() default false;

    /**
     * 执行 after Commit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    boolean afterCommit() default true;
}
