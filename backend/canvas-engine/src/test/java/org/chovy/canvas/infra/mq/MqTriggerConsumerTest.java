package org.chovy.canvas.infra.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqTriggerConsumerTest {

    private TriggerRouteService routeService;
    private CanvasDisruptorService disruptorService;
    private NotificationEventService notificationEventService;
    private MqTriggerConsumer consumer;

    @BeforeEach
    void setUp() {
        routeService = mock(TriggerRouteService.class);
        disruptorService = mock(CanvasDisruptorService.class);
        notificationEventService = mock(NotificationEventService.class);
        consumer = new MqTriggerConsumer(
                new ObjectMapper(),
                routeService,
                disruptorService,
                notificationEventService);
    }

    @Test
    void listenerConfigurationSubscribesToMqTriggerTopicConcurrentlyInClusteringMode() {
        RocketMQMessageListener annotation = MqTriggerConsumer.class.getAnnotation(RocketMQMessageListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.topic()).isEqualTo("${canvas.mq.topic:CANVAS_MQ_TRIGGER}");
        assertThat(annotation.consumerGroup()).isEqualTo("${rocketmq.consumer.group:GID_CANVAS_ENGINE}");
        assertThat(annotation.selectorType()).isEqualTo(SelectorType.TAG);
        assertThat(annotation.selectorExpression()).isEqualTo("*");
        assertThat(annotation.consumeMode()).isEqualTo(ConsumeMode.CONCURRENTLY);
        assertThat(annotation.messageModel()).isEqualTo(MessageModel.CLUSTERING);
        assertThat(annotation.consumeThreadNumber()).isEqualTo(20);
    }

    @Test
    void onMessagePublishesMqTriggerForEveryCanvasRoute() {
        when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("101", "202"));

        consumer.onMessage(message("ORDER_PAID", "MSG-1",
                "{\"userId\":\"user-7\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-1\",\"amount\":12}}"));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(disruptorService).publish(eq(101L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("ORDER_PAID"), payloadCaptor.capture(), eq("MSG-1"));
        assertThat(payloadCaptor.getValue()).containsEntry("orderId", "O-1");
        assertThat(payloadCaptor.getValue()).containsEntry("amount", 12);

        verify(disruptorService).publish(eq(202L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("ORDER_PAID"), any(), eq("MSG-1"));
    }

    @Test
    void onMessageDropsMessageWhenNoCanvasRouteMatchesTag() {
        when(routeService.getCanvasByMqTopic("UNUSED")).thenReturn(Set.of());

        consumer.onMessage(message("UNUSED", "MSG-2",
                "{\"userId\":\"user-8\",\"messageCode\":\"NOOP\",\"payload\":{\"k\":\"v\"}}"));

        verify(disruptorService, never()).publish(any(), any(), any(), any(), any(), any(), any());
        verify(notificationEventService).systemAlert(
                eq("MQ_TRIGGER_NO_ROUTE"),
                eq("MQ 触发无匹配画布"),
                any(),
                eq("/mq-config"),
                eq("MQ_TRIGGER"),
                eq("UNUSED"),
                eq("mq:no-route:UNUSED"),
                eq(null));
    }

    @Test
    void onMessageThrowsWhenBodyIsInvalidJsonSoRocketMqCanRetry() {
        assertThatThrownBy(() -> consumer.onMessage(message("ORDER_PAID", "MSG-3", "{bad-json")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid MQ trigger message body");
    }

    @Test
    void onMessageThrowsWhenRequiredFieldsAreMissingSoRocketMqCanRetry() {
        assertThatThrownBy(() -> consumer.onMessage(message("ORDER_PAID", "MSG-5",
                "{\"userId\":\"\",\"messageCode\":\"PAYMENT\",\"payload\":{}}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");

        assertThatThrownBy(() -> consumer.onMessage(message("ORDER_PAID", "MSG-6",
                "{\"userId\":\"user-9\",\"messageCode\":\"\",\"payload\":{}}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageCode is required");

        assertThatThrownBy(() -> consumer.onMessage(message("ORDER_PAID", "MSG-7",
                "{\"userId\":\"user-9\",\"messageCode\":\"PAYMENT\",\"payload\":null}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payload is required");
    }

    @Test
    void onMessagePropagatesDisruptorFailureSoRocketMqCanRetry() {
        when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("101"));
        doThrow(new IllegalStateException("ring buffer unavailable"))
                .when(disruptorService)
                .publish(eq(101L), any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> consumer.onMessage(message("ORDER_PAID", "MSG-4",
                "{\"userId\":\"user-9\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-2\"}}")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ring buffer unavailable");
    }

    private static MessageExt message(String tag, String msgId, String body) {
        MessageExt message = new MessageExt();
        message.setTags(tag);
        message.setMsgId(msgId);
        message.setBody(body.getBytes(StandardCharsets.UTF_8));
        return message;
    }
}
