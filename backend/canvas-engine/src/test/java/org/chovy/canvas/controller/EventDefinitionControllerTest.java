package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.enums.CanvasStatusEnum;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.EventDefinitionDO;
import org.chovy.canvas.domain.meta.EventDefinitionCacheService;
import org.chovy.canvas.dal.mapper.EventDefinitionMapper;
import org.chovy.canvas.dal.dataobject.EventLogDO;
import org.chovy.canvas.dal.mapper.EventLogMapper;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.wait.WaitResumeService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventDefinitionControllerTest {

    private EventDefinitionMapper eventMapper;
    private EventLogMapper logMapper;
    private CanvasDisruptorService disruptorService;
    private TriggerRouteService triggerRouteService;
    private EventDefinitionCacheService eventDefinitionCacheService;
    private WaitResumeService waitResumeService;
    private EventDefinitionController controller;

    @BeforeEach
    void setUp() {
        eventMapper = Mockito.mock(EventDefinitionMapper.class);
        logMapper = Mockito.mock(EventLogMapper.class);
        disruptorService = Mockito.mock(CanvasDisruptorService.class);
        triggerRouteService = Mockito.mock(TriggerRouteService.class);
        eventDefinitionCacheService = Mockito.mock(EventDefinitionCacheService.class);
        waitResumeService = Mockito.mock(WaitResumeService.class);
        controller = new EventDefinitionController(
                eventMapper, logMapper, disruptorService, triggerRouteService, new ObjectMapper(),
                eventDefinitionCacheService, waitResumeService);
    }

    @Test
    void reportEvent_setsCorrectCanvasCount() {
        // Arrange: valid event definition exists
        EventDefinitionDO def = new EventDefinitionDO();
        def.setEventCode("ORDER_COMPLETE");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventDefinitionCacheService.getPublishedByCode("ORDER_COMPLETE")).thenReturn(def);

        // Two canvases are subscribed to this event
        when(triggerRouteService.getCanvasByBehavior("ORDER_COMPLETE"))
                .thenReturn(Set.of("1", "2"));

        // Capture the EventLogDO passed to logMapper.insert()
        ArgumentCaptor<EventLogDO> captor = ArgumentCaptor.forClass(EventLogDO.class);
        doAnswer(invocation -> null).when(logMapper).insert(captor.capture());

        // Build request
        EventReportReq req = new EventReportReq();
        req.setEventCode("ORDER_COMPLETE");
        req.setUserId("user-99");
        req.setAttributes(Map.of("orderId", "ORD-001"));

        // Act
        Map<String, Object> result = controller.reportEvent(req)
                .block()
                .getData();

        // Assert: EventLogDO stored with real counts
        EventLogDO insertedLog = captor.getValue();
        assertThat(insertedLog.getCanvasTriggered()).isEqualTo(2);
        assertThat(insertedLog.getCanvasCount()).isEqualTo(2);

        // Assert: response also reflects the real count
        assertThat(result.get("canvasTriggered")).isEqualTo(2);
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
    }

    @Test
    void reportEvent_setsZeroCanvasCount_whenNoCanvasSubscribed() {
        // Arrange: valid event definition exists but no canvas subscribed
        EventDefinitionDO def = new EventDefinitionDO();
        def.setEventCode("UNUSED_EVENT");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventDefinitionCacheService.getPublishedByCode("UNUSED_EVENT")).thenReturn(def);

        when(triggerRouteService.getCanvasByBehavior("UNUSED_EVENT"))
                .thenReturn(Set.of());

        ArgumentCaptor<EventLogDO> captor = ArgumentCaptor.forClass(EventLogDO.class);
        doAnswer(invocation -> null).when(logMapper).insert(captor.capture());

        EventReportReq req = new EventReportReq();
        req.setEventCode("UNUSED_EVENT");
        req.setUserId("user-99");

        // Act
        controller.reportEvent(req).block();

        // Assert: EventLogDO stored with 0 (no canvases)
        EventLogDO insertedLog = captor.getValue();
        assertThat(insertedLog.getCanvasTriggered()).isEqualTo(0);
        assertThat(insertedLog.getCanvasCount()).isEqualTo(0);
    }

    @Test
    void reportEvent_usesEventDefinitionCacheService_onEveryCall() {
        // Arrange: valid event definition exists
        EventDefinitionDO def = new EventDefinitionDO();
        def.setEventCode("CACHED_EVENT");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventDefinitionCacheService.getPublishedByCode("CACHED_EVENT")).thenReturn(def);

        when(triggerRouteService.getCanvasByBehavior("CACHED_EVENT"))
                .thenReturn(Set.of());

        doAnswer(invocation -> null).when(logMapper).insert(any(EventLogDO.class));

        EventReportReq req = new EventReportReq();
        req.setEventCode("CACHED_EVENT");
        req.setUserId("user-cache-test");

        // Act: call reportEvent twice with the same eventCode
        controller.reportEvent(req).block();
        controller.reportEvent(req).block();

        // Assert: controller delegates definition lookup to the cache service.
        verify(eventDefinitionCacheService, times(2)).getPublishedByCode("CACHED_EVENT");
    }

    @Test
    void reportEventPersistsPerfRunIdAndPublishesOriginalPayload() {
        EventDefinitionDO def = new EventDefinitionDO();
        def.setEventCode("PERF_EVENT");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventDefinitionCacheService.getPublishedByCode("PERF_EVENT")).thenReturn(def);
        when(triggerRouteService.getCanvasByBehavior("PERF_EVENT")).thenReturn(Set.of("42"));

        ArgumentCaptor<EventLogDO> logCaptor = ArgumentCaptor.forClass(EventLogDO.class);
        doAnswer(invocation -> null).when(logMapper).insert(logCaptor.capture());

        Map<String, Object> attributes = Map.of("perfRunId", "perf_20260523_001", "amount", 88);
        EventReportReq req = new EventReportReq();
        req.setEventCode("PERF_EVENT");
        req.setUserId("user-99");
        req.setAttributes(attributes);

        controller.reportEvent(req).block();

        assertThat(logCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_001");
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(disruptorService).publish(
                eq(42L),
                eq("user-99"),
                eq(TriggerType.EVENT),
                eq(NodeType.EVENT_TRIGGER),
                eq("PERF_EVENT"),
                payloadCaptor.capture(),
                anyString());
        assertThat(payloadCaptor.getValue()).isSameAs(attributes);
    }
}
