package org.chovy.canvas.execution.adapter.messaging;

/**
 * 定义 MqTriggerRejectedSink 的执行上下文数据结构或业务契约。
 */
@FunctionalInterface
public interface MqTriggerRejectedSink {

    /**
     * 执行 reject 对应的业务处理。
     * @param event event 参数
     */
    void reject(MqTriggerRejectedEvent event);
}
