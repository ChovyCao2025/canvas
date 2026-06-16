package org.chovy.canvas.execution.adapter.messaging;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;

public class MqTriggerConsumer {

    private final CanvasExecutionFacade executionFacade;
    private final MqTriggerRejectedSink rejectedSink;

    public MqTriggerConsumer(CanvasExecutionFacade executionFacade, MqTriggerRejectedSink rejectedSink) {
        this.executionFacade = executionFacade;
        this.rejectedSink = rejectedSink;
    }

    public void consume(MqTriggerMessage message) {
        try {
            executionFacade.trigger(new CanvasExecutionFacade.ExecutionRequestCommand(
                    message.tenantId(),
                    message.canvasId(),
                    message.versionId(),
                    message.triggerType(),
                    "",
                    message.payload(),
                    false));
        } catch (RuntimeException e) {
            rejectedSink.reject(new MqTriggerRejectedEvent(
                    message.sourceMsgId(),
                    message.triggerType(),
                    message.matchKey(),
                    e.getMessage(),
                    message));
        }
    }
}
