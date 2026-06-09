package org.chovy.canvas.infrastructure.mq;

import org.chovy.cache.CacheInvalidationEvent;

/**
 * CanvasMessageBus 定义 infrastructure.mq 场景中的扩展契约。
 */
public interface CanvasMessageBus {

    /**
     * 执行数据写入或状态变更。
     *
     * @param topic 待处理业务值，用于规则计算、转换或外部调用。
     * @param tag 待处理业务值，用于规则计算、转换或外部调用。
     * @param payload 待处理业务值，用于规则计算、转换或外部调用。
     * @param shardingKey 业务键，用于在同一租户下定位资源。
     */
    void publishOrderly(String topic, String tag, Object payload, String shardingKey);

    /**
     * 执行数据写入或状态变更。
     *
     * @param topic 待处理业务值，用于规则计算、转换或外部调用。
     * @param event event 参数，用于 publishCacheInvalidation 流程中的校验、计算或对象转换。
     */
    void publishCacheInvalidation(String topic, CacheInvalidationEvent event);
}
