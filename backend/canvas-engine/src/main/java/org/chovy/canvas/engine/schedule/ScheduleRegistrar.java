package org.chovy.canvas.engine.schedule;

/**
 * Abstraction over schedule registration backends.
 */
public interface ScheduleRegistrar {

    /**
     * 注册、调度或初始化 register 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param registration registration 方法执行所需的业务参数
     */
    void register(ScheduleRegistration registration);

    /**
     * 执行 unregister 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param key key 对应的缓存键、配置键或业务键
     */
    void unregister(ScheduleKey key);
}
