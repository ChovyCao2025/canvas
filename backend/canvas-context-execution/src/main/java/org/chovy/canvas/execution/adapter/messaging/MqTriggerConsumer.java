package org.chovy.canvas.execution.adapter.messaging;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;

/**
 * 定义 MqTriggerConsumer 的执行上下文数据结构或业务契约。
 */
public class MqTriggerConsumer {

    /**
     * 保存 executionFacade 对应的状态或配置。
     */
    private final CanvasExecutionFacade executionFacade;

    /**
     * 保存 rejectedSink 对应的状态或配置。
     */
    private final MqTriggerRejectedSink rejectedSink;

    /**
     * 执行 MqTriggerConsumer 对应的业务处理。
     * @param executionFacade executionFacade 参数
     * @param rejectedSink rejectedSink 参数
     */
    public MqTriggerConsumer(CanvasExecutionFacade executionFacade, MqTriggerRejectedSink rejectedSink) {
        this.executionFacade = executionFacade;
        this.rejectedSink = rejectedSink;
    }

    /**
     * 执行 consume 对应的业务处理。
     * @param message message 参数
     */
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
            // 消费失败不在这里吞掉消息内容，统一交给拒收 Sink 做 DLQ 记录。
            rejectedSink.reject(new MqTriggerRejectedEvent(
                    message.sourceMsgId(),
                    message.triggerType(),
                    message.matchKey(),
                    e.getMessage(),
                    message));
        }
    }
}
