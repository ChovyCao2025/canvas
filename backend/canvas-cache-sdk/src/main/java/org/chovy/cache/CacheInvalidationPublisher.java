package org.chovy.cache;

/**
 * 缓存失效事件外部发布器。
 *
 * <p>用于把本地缓存失效事件桥接到 MQ、事件总线或其他跨集群通道，补充 Redis Pub/Sub 的传播范围。
 */
@FunctionalInterface
public interface CacheInvalidationPublisher {
    /**
     * 发布一条缓存失效事件。
     *
     * <p>调用方会捕获并记录发布异常，因此实现应尽量保证幂等，避免单次发布失败影响缓存主流程。
     *
     * @param event 需要传播的缓存失效事件
     */
    void publish(CacheInvalidationEvent event);
}
