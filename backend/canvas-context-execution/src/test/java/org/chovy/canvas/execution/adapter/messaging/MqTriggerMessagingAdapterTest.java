package org.chovy.canvas.execution.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.CanvasExecutionFacade;
import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.junit.jupiter.api.Test;

class MqTriggerMessagingAdapterTest {

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

    @Test
    void publisherBuildsRocketMqDestinationFromTopicAndTag() {
        RocketMqTriggerPublisher publisher = new RocketMqTriggerPublisher(null);

        assertThat(publisher.destination("canvas-trigger", "orders.created"))
                .isEqualTo("canvas-trigger:orders.created");
    }

    private static final class RecordingExecutionFacade implements CanvasExecutionFacade {
        private final boolean fail;
        private final List<ExecutionRequestCommand> commands = new ArrayList<>();

        private RecordingExecutionFacade(boolean fail) {
            this.fail = fail;
        }

        @Override
        public ExecutionResultView trigger(ExecutionRequestCommand command) {
            commands.add(command);
            if (fail) {
                throw new IllegalStateException("execution handoff failed");
            }
            return new ExecutionResultView("exec-1", "STARTED");
        }

        @Override
        public ExecutionTraceView trace(Long tenantId, String executionId) {
            throw new UnsupportedOperationException();
        }
    }
}
