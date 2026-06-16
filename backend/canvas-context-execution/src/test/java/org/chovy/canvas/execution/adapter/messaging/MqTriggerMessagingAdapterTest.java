package org.chovy.canvas.execution.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.junit.jupiter.api.Test;

/**
 * 定义 MqTriggerMessagingAdapterTest 的执行上下文数据结构或业务契约。
 */
class MqTriggerMessagingAdapterTest {

    /**
     * 执行 consumerMapsMessageEnvelopeToExecutionCommand 对应的业务处理。
     */
    @Test
    void consumerMapsMessageEnvelopeToExecutionCommand() {
        RecordingExecutionFacade facade = new RecordingExecutionFacade(false);
        List<MqTriggerRejectedEvent> rejected = new ArrayList<>();
        MqTriggerConsumer consumer = new MqTriggerConsumer(facade, rejected::add);

        consumer.consume(new MqTriggerMessage(
                3L,
                4L,
                5L,
                "MQ",
                "orders.created",
                "msg-7",
                Map.of("orderId", "o-1")));

        assertThat(facade.commands).hasSize(1);
        CanvasExecutionFacade.ExecutionRequestCommand command = facade.commands.get(0);
        assertThat(command.tenantId()).isEqualTo(3L);
        assertThat(command.canvasId()).isEqualTo(4L);
        assertThat(command.versionId()).isEqualTo(5L);
        assertThat(command.triggerType()).isEqualTo("MQ");
        assertThat(command.payload()).containsEntry("orderId", "o-1");
        assertThat(rejected).isEmpty();
    }

    /**
     * 执行 failedConsumerHandoffRoutesRejection 对应的业务处理。
     */
    @Test
    void failedConsumerHandoffRoutesRejection() {
        RecordingExecutionFacade facade = new RecordingExecutionFacade(true);
        List<MqTriggerRejectedEvent> rejected = new ArrayList<>();
        MqTriggerConsumer consumer = new MqTriggerConsumer(facade, rejected::add);
        MqTriggerMessage message = new MqTriggerMessage(
                3L,
                4L,
                5L,
                "MQ",
                "orders.created",
                "msg-8",
                Map.of());

        consumer.consume(message);

        assertThat(rejected).containsExactly(new MqTriggerRejectedEvent(
                "msg-8",
                "MQ",
                "orders.created",
                "execution handoff failed",
                message));
    }

    /**
     * 执行 publisherBuildsRocketMqDestinationFromTopicAndTag 对应的业务处理。
     */
    @Test
    void publisherBuildsRocketMqDestinationFromTopicAndTag() {
        RocketMqTriggerPublisher publisher = new RocketMqTriggerPublisher(null);

        assertThat(publisher.destination("canvas-trigger", "orders.created"))
                .isEqualTo("canvas-trigger:orders.created");
    }

    /**
     * 定义 RecordingExecutionFacade 的执行上下文数据结构或业务契约。
     */
    private static final class RecordingExecutionFacade implements CanvasExecutionFacade {
        /**
         * 保存 fail 对应的状态或配置。
         */
        private final boolean fail;
        private final List<ExecutionRequestCommand> commands = new ArrayList<>();

        /**
         * 执行 RecordingExecutionFacade 对应的业务处理。
         * @param fail fail 参数
         */
        private RecordingExecutionFacade(boolean fail) {
            this.fail = fail;
        }

        /**
         * 执行 trigger 对应的业务处理。
         * @param command command 参数
         * @return 处理后的结果
         */
        @Override
        public ExecutionResultView trigger(ExecutionRequestCommand command) {
            commands.add(command);
            if (fail) {
                throw new IllegalStateException("execution handoff failed");
            }
            return new ExecutionResultView("exec-1", "STARTED");
        }

        /**
         * 执行 trace 对应的业务处理。
         * @param tenantId tenantId 参数
         * @param executionId executionId 参数
         * @return 处理后的结果
         */
        @Override
        public ExecutionTraceView trace(Long tenantId, String executionId) {
            throw new UnsupportedOperationException();
        }
    }
}
