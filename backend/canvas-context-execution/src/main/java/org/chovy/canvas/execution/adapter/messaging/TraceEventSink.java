package org.chovy.canvas.execution.adapter.messaging;

@FunctionalInterface
public interface TraceEventSink {

    void accept(TraceEvent event);
}
