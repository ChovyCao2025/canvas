package org.chovy.canvas.execution.adapter.messaging;

@FunctionalInterface
public interface MqTriggerRejectedSink {

    void reject(MqTriggerRejectedEvent event);
}
