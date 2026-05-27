package org.chovy.canvas.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Overflow Retry Consumer 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
@ExtendWith(MockitoExtension.class)
class OverflowRetryConsumerTest {

    @Mock
    CanvasDisruptorService disruptor;

    @Mock
    CanvasExecutionDlqMapper dlqMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TriggerPriorityConfig priorityConfig = new TriggerPriorityConfig();

    private OverflowRetryConsumer consumer;

    @BeforeEach
    void setUp() {
        priorityConfig.setOverflowMaxRetry(3);
        consumer = new OverflowRetryConsumer(disruptor, priorityConfig, objectMapper, dlqMapper);
    }

    @Test
    void publishesRetryMessageWithAccumulatedChainRetryCount() throws Exception {
        OverflowRetryMessage retryMessage = new OverflowRetryMessage(
                100L,
                "user-1",
                "MQ",
                "MQ_TRIGGER",
                "topic-a",
                Map.of("orderId", "o-1", OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY, 99),
                "business-msg-1",
                1
        );
        MessageExt message = messageExt(retryMessage, 1);

        consumer.onMessage(message);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(disruptor).publishOverflowRetry(
                eq(100L),
                eq("user-1"),
                eq("MQ"),
                eq("MQ_TRIGGER"),
                eq("topic-a"),
                payloadCaptor.capture(),
                eq("business-msg-1"),
                eq(2)
        );
        assertThat(payloadCaptor.getValue())
                .containsEntry("orderId", "o-1")
                .doesNotContainKey(OverflowRetryMessage.CHAIN_RETRY_PAYLOAD_KEY);
        verify(dlqMapper, never()).insert(org.mockito.ArgumentMatchers.any(CanvasExecutionDlqDO.class));
    }

    @Test
    void recordsDlqAndAcksWhenRetryLimitReached() throws Exception {
        OverflowRetryMessage retryMessage = new OverflowRetryMessage(
                200L,
                "user-2",
                "BEHAVIOR",
                "EVENT_TRIGGER",
                "event-a",
                Map.of("eventId", "e-1", "perfRunId", "perf_20260523_005"),
                "business-msg-2",
                2
        );
        MessageExt message = messageExt(retryMessage, 1);

        consumer.onMessage(message);

        verify(disruptor, never()).publishOverflowRetry(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                anyInt()
        );
        ArgumentCaptor<CanvasExecutionDlqDO> dlqCaptor = ArgumentCaptor.forClass(CanvasExecutionDlqDO.class);
        verify(dlqMapper).insert(dlqCaptor.capture());
        CanvasExecutionDlqDO dlq = dlqCaptor.getValue();
        assertThat(dlq.getExecutionId()).isEqualTo("business-msg-2");
        assertThat(dlq.getCanvasId()).isEqualTo(200L);
        assertThat(dlq.getUserId()).isEqualTo("user-2");
        assertThat(dlq.getPerfRunId()).isEqualTo("perf_20260523_005");
        assertThat(dlq.getFailedNodeId()).isEqualTo("OVERFLOW_RETRY");
        assertThat(dlq.getFailedNodeType()).isEqualTo("EVENT_TRIGGER");
        assertThat(dlq.getErrorMsg()).contains("overflow_max_retry");
        assertThat(dlq.getRetryCount()).isEqualTo(3);
        assertThat(dlq.getTriggerPayload()).contains("\"eventId\":\"e-1\"");
        assertThat(dlq.getTriggerType()).isEqualTo("BEHAVIOR");
        assertThat(dlq.getTriggerNodeType()).isEqualTo("EVENT_TRIGGER");
        assertThat(dlq.getMatchKey()).isEqualTo("event-a");
        assertThat(dlq.getFailedAt()).isNotNull();
    }

    @Test
    void recordsDlqWithRocketMqMsgIdWhenBusinessMsgIdMissing() throws Exception {
        OverflowRetryMessage retryMessage = new OverflowRetryMessage(
                201L,
                "user-2",
                "BEHAVIOR",
                "EVENT_TRIGGER",
                "event-a",
                Map.of("eventId", "e-1"),
                null,
                3
        );
        MessageExt message = messageExt(retryMessage, 0);
        message.setMsgId("rocket-msg-without-business-id");

        consumer.onMessage(message);

        ArgumentCaptor<CanvasExecutionDlqDO> dlqCaptor = ArgumentCaptor.forClass(CanvasExecutionDlqDO.class);
        verify(dlqMapper).insert(dlqCaptor.capture());
        assertThat(dlqCaptor.getValue().getExecutionId()).isEqualTo("rocket-msg-without-business-id");
    }

    @Test
    void parseFailureThrowsForRocketMqRetryOrBrokerDlq() {
        MessageExt message = new MessageExt();
        message.setBody("{not-json".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        message.setMsgId("rocket-msg-1");

        assertThatThrownBy(() -> consumer.onMessage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("溢出重试消息体格式错误");

        verify(disruptor, never()).publishOverflowRetry(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                anyInt()
        );
        verify(dlqMapper, never()).insert(org.mockito.ArgumentMatchers.any(CanvasExecutionDlqDO.class));
    }

    @Test
    void dlqInsertFailureThrowsForRocketMqRetryOrBrokerDlq() throws Exception {
        OverflowRetryMessage retryMessage = new OverflowRetryMessage(
                300L,
                "user-3",
                "MQ",
                "MQ_TRIGGER",
                "topic-b",
                Map.of("orderId", "o-2"),
                "business-msg-3",
                3
        );
        MessageExt message = messageExt(retryMessage, 0);
        when(dlqMapper.insert(any(CanvasExecutionDlqDO.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> consumer.onMessage(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("溢出重试DLQ写入失败");

        verify(disruptor, never()).publishOverflowRetry(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                anyInt()
        );
    }

    private MessageExt messageExt(OverflowRetryMessage retryMessage, int reconsumeTimes) throws Exception {
        MessageExt message = new MessageExt();
        message.setBody(objectMapper.writeValueAsBytes(retryMessage));
        message.setMsgId("rocket-msg-" + retryMessage.getMsgId());
        message.setReconsumeTimes(reconsumeTimes);
        return message;
    }
}
