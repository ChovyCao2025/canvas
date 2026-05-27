package org.chovy.cache;

/**
 * Publishes cache invalidation events to an external transport such as MQ.
 */
@FunctionalInterface
public interface CacheInvalidationPublisher {
    /**
     * 发布或发送 publish 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param event event 方法执行所需的业务参数
     */
    void publish(CacheInvalidationEvent event);
}
