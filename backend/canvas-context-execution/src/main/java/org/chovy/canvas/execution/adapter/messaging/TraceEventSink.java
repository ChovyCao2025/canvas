package org.chovy.canvas.execution.adapter.messaging;

/**
 * 定义 TraceEventSink 的执行上下文数据结构或业务契约。
 */
@FunctionalInterface
public interface TraceEventSink {

    /**
     * 执行 accept 对应的业务处理。
     * @param event event 参数
     */
    void accept(TraceEvent event);
}
