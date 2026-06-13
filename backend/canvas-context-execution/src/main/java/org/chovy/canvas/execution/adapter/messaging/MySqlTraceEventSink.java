package org.chovy.canvas.execution.adapter.messaging;

import java.util.Objects;

public class MySqlTraceEventSink implements TraceEventSink {

    private final TraceEventWriter writer;

    public MySqlTraceEventSink(TraceEventWriter writer) {
        this.writer = Objects.requireNonNull(writer, "writer is required");
    }

    @Override
    public void accept(TraceEvent event) {
        writer.write(event);
    }

    @FunctionalInterface
    public interface TraceEventWriter {
        void write(TraceEvent event);
    }
}
