package org.chovy.canvas.execution.adapter.messaging;

import java.util.Objects;

/**
 * 定义 MySqlTraceEventSink 的执行上下文数据结构或业务契约。
 */
public class MySqlTraceEventSink implements TraceEventSink {

    /**
     * 保存 writer 对应的状态或配置。
     */
    private final TraceEventWriter writer;

    /**
     * 执行 MySqlTraceEventSink 对应的业务处理。
     * @param writer writer 参数
     */
    public MySqlTraceEventSink(TraceEventWriter writer) {
        this.writer = Objects.requireNonNull(writer, "writer is required");
    }

    /**
     * 执行 accept 对应的业务处理。
     * @param event event 参数
     */
    @Override
    public void accept(TraceEvent event) {
        writer.write(event);
    }

    /**
     * 定义 TraceEventWriter 的执行上下文数据结构或业务契约。
     */
    @FunctionalInterface
    public interface TraceEventWriter {
        /**
         * 执行 write 对应的业务处理。
         * @param event event 参数
         */
        void write(TraceEvent event);
    }
}
