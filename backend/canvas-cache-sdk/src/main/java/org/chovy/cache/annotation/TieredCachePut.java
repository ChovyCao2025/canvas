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
     * 返回 TieredCachePut 的 name 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String name();

    /**
     * 返回 TieredCachePut 的 key 配置值。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 转换或查询得到的字符串结果
     */
    String key();

    /**
     * 执行 after Commit 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @return 判断结果，true 表示校验通过或条件成立
     */
    boolean afterCommit() default true;
}
