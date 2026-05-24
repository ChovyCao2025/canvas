package org.chovy.canvas.infrastructure.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasMqTriggerRejectedDO;
import org.chovy.canvas.dal.mapper.CanvasMqTriggerRejectedMapper;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
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
    private CanvasExecutionRequestService requestService;
    private CanvasMqTriggerRejectedMapper rejectedMapper;
    private CanvasMetrics metrics;
    private NotificationEventService notificationEventService;
    private MqTriggerConsumer consumer;

    @BeforeEach
    void setUp() {
        routeService = mock(TriggerRouteService.class);
        disruptorService = mock(CanvasDisruptorService.class);
        requestService = mock(CanvasExecutionRequestService.class);
        rejectedMapper = mock(CanvasMqTriggerRejectedMapper.class);
        metrics = mock(CanvasMetrics.class);
        notificationEventService = mock(NotificationEventService.class);
        consumer = new MqTriggerConsumer(
                new ObjectMapper(),
                routeService,
                disruptorService,
                requestService,
                rejectedMapper,
                metrics,
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
        assertThat(annotation.consumeMode()).isEqualTo(ConsumeMode.ORDERLY);
        assertThat(annotation.messageModel()).isEqualTo(MessageModel.CLUSTERING);
        assertThat(annotation.consumeThreadNumber()).isEqualTo(20);
    }

    @Test
    void onMessagePublishesMqTriggerForEveryCanvasRoute() {
        when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("101", "202"));
        when(requestService.enqueue(eq(101L), eq("user-7"), eq(TriggerType.MQ), eq(NodeType.MQ_TRIGGER),
                eq("ORDER_PAID"), any(), eq("MSG-1"))).thenReturn("req-101");
        when(requestService.enqueue(eq(202L), eq("user-7"), eq(TriggerType.MQ), eq(NodeType.MQ_TRIGGER),
                eq("ORDER_PAID"), any(), eq("MSG-1"))).thenReturn("req-202");

        consumer.onMessage(message("ORDER_PAID", "MSG-1",
                "{\"userId\":\"user-7\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-1\",\"amount\":12}}"));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestService).enqueue(eq(101L), eq("user-7"), eq(TriggerType.MQ),
                eq(NodeType.MQ_TRIGGER), eq("ORDER_PAID"), payloadCaptor.capture(), eq("MSG-1"));
        assertThat(payloadCaptor.getValue()).containsEntry("orderId", "O-1");
        assertThat(payloadCaptor.getValue()).containsEntry("amount", 12);

        verify(disruptorService).publishRequest("req-101");
        verify(disruptorService).publishRequest("req-202");
    }

    @Test
    void onMessageDropsMessageWhenNoCanvasRouteMatchesTag() {
        when(routeService.getCanvasByMqTopic("UNUSED")).thenReturn(Set.of());

        consumer.onMessage(message("UNUSED", "MSG-2",
                "{\"userId\":\"user-8\",\"messageCode\":\"NOOP\",\"payload\":{\"k\":\"v\"}}"));

        verify(disruptorService, never()).publishRequest(any());
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
    void onMessageStoresRejectedMessageAndAcksWhenBodyIsInvalidJson() {
        consumer.onMessage(message("ORDER_PAID", "MSG-3", "{bad-json"));

        verify(metrics).recordMqTriggerRejected("INVALID_BODY", "ORDER_PAID");
        ArgumentCaptor<CanvasMqTriggerRejectedDO> captor = ArgumentCaptor.forClass(CanvasMqTriggerRejectedDO.class);
        verify(rejectedMapper).insert(captor.capture());
        assertThat(captor.getValue().getMsgId()).isEqualTo("MSG-3");
        assertThat(captor.getValue().getTag()).isEqualTo("ORDER_PAID");
        assertThat(captor.getValue().getReason()).isEqualTo("INVALID_BODY");
        verify(notificationEventService).systemAlert(
                eq("MQ_TRIGGER_PARSE_FAILED"),
                eq("MQ 触发消息解析失败"),
                any(),
                eq("/mq-config"),
                eq("MQ_TRIGGER"),
                eq("MSG-3"),
                eq("mq:parse:MSG-3"),
                eq(null));
        verify(disruptorService, never()).publishRequest(any());
    }

    @Test
    void onMessageStoresRejectedMessageAndAcksWhenRequiredFieldsAreMissing() {
        consumer.onMessage(message("ORDER_PAID", "MSG-5",
                "{\"userId\":\"\",\"messageCode\":\"PAYMENT\",\"payload\":{}}"));

        verify(metrics).recordMqTriggerRejected("INVALID_MESSAGE", "ORDER_PAID");
        verify(rejectedMapper).insert(any(CanvasMqTriggerRejectedDO.class));
        verify(notificationEventService).systemAlert(
                eq("MQ_TRIGGER_VALIDATE_FAILED"),
                eq("MQ 触发消息校验失败"),
                any(),
                eq("/mq-config"),
                eq("MQ_TRIGGER"),
                eq("MSG-5"),
                eq("mq:validate:MSG-5"),
                eq(null));
        verify(disruptorService, never()).publishRequest(any());
    }

    @Test
    void onMessageSkipsDirtyRouteIdsAndStillPublishesValidRoutes() {
        when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("bad-route", "101"));
        when(requestService.enqueue(eq(101L), eq("user-7"), eq(TriggerType.MQ), eq(NodeType.MQ_TRIGGER),
                eq("ORDER_PAID"), any(), eq("MSG-8"))).thenReturn("req-101");

        consumer.onMessage(message("ORDER_PAID", "MSG-8",
                "{\"userId\":\"user-7\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-8\"}}"));

        verify(metrics).recordMqRouteRejected("INVALID_CANVAS_ID", "ORDER_PAID");
        verify(disruptorService).publishRequest("req-101");
    }

    @Test
    void onMessagePropagatesDisruptorFailureSoRocketMqCanRetry() {
        when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("101"));
        when(requestService.enqueue(eq(101L), any(), any(), any(), any(), any(), any())).thenReturn("req-101");
        doThrow(new IllegalStateException("ring buffer unavailable"))
                .when(disruptorService)
                .publishRequest("req-101");

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
