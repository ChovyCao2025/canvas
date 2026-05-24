package org.chovy.canvas.engine.handlers;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.chovy.canvas.dal.dataobject.MqMessageDefinitionDO;
import org.chovy.canvas.dal.mapper.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infrastructure.mq.MqTriggerMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendMqHandlerTest {

    private RocketMQTemplate rocketMQTemplate;
    private MqMessageDefinitionMapper mqMapper;
    private SendMqHandler handler;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        rocketMQTemplate = mock(RocketMQTemplate.class);
        mqMapper = mock(MqMessageDefinitionMapper.class);
        handler = new SendMqHandler(rocketMQTemplate, mqMapper);
        ReflectionTestUtils.setField(handler, "mqTopic", "CANVAS_MQ_TRIGGER");

        context = new ExecutionContext();
        context.setUserId("user-7");
        context.setTriggerPayload(Map.of("orderId", "O-1", "amount", 12));
    }

    @Test
    void executeAsyncFailsWhenMessageCodeKeyIsBlank() {
        NodeResult result = handler.executeAsync(Map.of("messageCodeKey", " "), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SEND_MQ: messageCodeKey 未配置");
        verify(mqMapper, never()).selectOne(any());
        verify(rocketMQTemplate, never()).syncSendOrderly(anyString(), any(Object.class), anyString());
    }

    @Test
    void executeAsyncFailsWhenEnabledMessageDefinitionDoesNotExist() {
        when(mqMapper.selectOne(any())).thenReturn(null);

        NodeResult result = handler.executeAsync(Map.of("messageCodeKey", "order_paid"), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SEND_MQ: 找不到消息定义 messageCode=order_paid");
        verify(rocketMQTemplate, never()).syncSendOrderly(anyString(), any(Object.class), anyString());
    }

    @Test
    void executeAsyncSendsMqTriggerMessageToTopicTagDestination() {
        MqMessageDefinitionDO definition = new MqMessageDefinitionDO();
        definition.setMessageCode("order_paid");
        definition.setTopic("order.paid");
        definition.setEnabled(1);
        when(mqMapper.selectOne(any())).thenReturn(definition);
        when(rocketMQTemplate.syncSendOrderly(anyString(), any(Object.class), anyString()))
                .thenReturn(sendResult(SendStatus.SEND_OK));

        Map<String, Object> config = Map.of(
                "messageCodeKey", "order_paid",
                "nextNodeId", "next-1",
                "params", List.of(
                        Map.of("key", "orderId", "value", "${orderId}"),
                        Map.of("key", "amount", "value", "$${amount}"),
                        Map.of("key", "literal", "value", "fixed")
                )
        );

        NodeResult result = handler.executeAsync(config, context).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("next-1");
        assertThat(result.output()).containsEntry("mqSent", true);

        ArgumentCaptor<MqTriggerMessage> messageCaptor = ArgumentCaptor.forClass(MqTriggerMessage.class);
        verify(rocketMQTemplate).syncSendOrderly(
                eq("CANVAS_MQ_TRIGGER:order.paid"),
                messageCaptor.capture(),
                eq("user-7"));

        MqTriggerMessage message = messageCaptor.getValue();
        assertThat(message.getUserId()).isEqualTo("user-7");
        assertThat(message.getMessageCode()).isEqualTo("order_paid");
        assertThat(message.getPayload()).containsEntry("orderId", "O-1");
        assertThat(message.getPayload()).containsEntry("amount", 12);
        assertThat(message.getPayload()).containsEntry("literal", "fixed");
    }

    @Test
    void executeAsyncReturnsFailureWhenRocketMqSendFails() {
        MqMessageDefinitionDO definition = new MqMessageDefinitionDO();
        definition.setTopic("order.paid");
        definition.setEnabled(1);
        when(mqMapper.selectOne(any())).thenReturn(definition);
        when(rocketMQTemplate.syncSendOrderly(anyString(), any(Object.class), anyString()))
                .thenThrow(new RuntimeException("broker down"));

        NodeResult result = handler.executeAsync(Map.of("messageCodeKey", "order_paid"), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SEND_MQ: 消息发送失败: broker down");
    }

    @Test
    void executeAsyncBuildsPayloadFromUiInputParamsShape() {
        MqMessageDefinitionDO definition = enabledDefinition("order.paid");
        when(mqMapper.selectOne(any())).thenReturn(definition);
        when(rocketMQTemplate.syncSendOrderly(anyString(), any(Object.class), anyString()))
                .thenReturn(sendResult(SendStatus.SEND_OK));

        Map<String, Object> config = Map.of(
                "messageCodeKey", "order_paid",
                "inputParams", Map.of(
                        "orderId", "${orderId}",
                        "amount", "$${amount}",
                        "literal", "fixed"
                )
        );

        NodeResult result = handler.executeAsync(config, context).block();

        assertThat(result.success()).isTrue();

        ArgumentCaptor<MqTriggerMessage> messageCaptor = ArgumentCaptor.forClass(MqTriggerMessage.class);
        verify(rocketMQTemplate).syncSendOrderly(
                eq("CANVAS_MQ_TRIGGER:order.paid"),
                messageCaptor.capture(),
                eq("user-7"));
        assertThat(messageCaptor.getValue().getPayload())
                .containsEntry("orderId", "O-1")
                .containsEntry("amount", 12)
                .containsEntry("literal", "fixed");
    }

    @Test
    void executeAsyncBuildsPayloadFromMapShapedParams() {
        MqMessageDefinitionDO definition = enabledDefinition("order.paid");
        when(mqMapper.selectOne(any())).thenReturn(definition);
        when(rocketMQTemplate.syncSendOrderly(anyString(), any(Object.class), anyString()))
                .thenReturn(sendResult(SendStatus.SEND_OK));

        NodeResult result = handler.executeAsync(Map.of(
                "messageCodeKey", "order_paid",
                "params", Map.of("orderId", "${orderId}")
        ), context).block();

        assertThat(result.success()).isTrue();

        ArgumentCaptor<MqTriggerMessage> messageCaptor = ArgumentCaptor.forClass(MqTriggerMessage.class);
        verify(rocketMQTemplate).syncSendOrderly(
                eq("CANVAS_MQ_TRIGGER:order.paid"),
                messageCaptor.capture(),
                eq("user-7"));
        assertThat(messageCaptor.getValue().getPayload()).containsEntry("orderId", "O-1");
    }

    @Test
    void executeAsyncFailsWhenRocketMqReturnsNonOkStatus() {
        MqMessageDefinitionDO definition = enabledDefinition("order.paid");
        when(mqMapper.selectOne(any())).thenReturn(definition);
        when(rocketMQTemplate.syncSendOrderly(anyString(), any(Object.class), anyString()))
                .thenReturn(sendResult(SendStatus.FLUSH_DISK_TIMEOUT));

        NodeResult result = handler.executeAsync(Map.of("messageCodeKey", "order_paid"), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SEND_MQ: 消息发送失败: RocketMQ send status=FLUSH_DISK_TIMEOUT");
    }

    @Test
    void executeAsyncFailsWhenMessageDefinitionTopicIsBlank() {
        MqMessageDefinitionDO definition = enabledDefinition(" ");
        when(mqMapper.selectOne(any())).thenReturn(definition);

        NodeResult result = handler.executeAsync(Map.of("messageCodeKey", " order_paid "), context).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("SEND_MQ: 消息定义 topic 未配置 messageCode=order_paid");
        verify(rocketMQTemplate, never()).syncSendOrderly(anyString(), any(Object.class), anyString());
    }

    private static MqMessageDefinitionDO enabledDefinition(String topic) {
        MqMessageDefinitionDO definition = new MqMessageDefinitionDO();
        definition.setMessageCode("order_paid");
        definition.setTopic(topic);
        definition.setEnabled(1);
        return definition;
    }

    private static SendResult sendResult(SendStatus status) {
        SendResult result = new SendResult();
        result.setSendStatus(status);
        return result;
    }
}
