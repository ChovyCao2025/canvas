package org.chovy.canvas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.CanvasStatusEnum;
import org.chovy.canvas.domain.meta.EventDefinition;
import org.chovy.canvas.domain.meta.EventDefinitionMapper;
import org.chovy.canvas.domain.meta.EventLog;
import org.chovy.canvas.domain.meta.EventLogMapper;
import org.chovy.canvas.dto.EventReportReq;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventDefinitionControllerTest {

    private EventDefinitionMapper eventMapper;
    private EventLogMapper logMapper;
    private CanvasDisruptorService disruptorService;
    private TriggerRouteService triggerRouteService;
    private EventDefinitionController controller;

    @BeforeEach
    void setUp() {
        eventMapper = Mockito.mock(EventDefinitionMapper.class);
        logMapper = Mockito.mock(EventLogMapper.class);
        disruptorService = Mockito.mock(CanvasDisruptorService.class);
        triggerRouteService = Mockito.mock(TriggerRouteService.class);
        controller = new EventDefinitionController(
                eventMapper, logMapper, disruptorService, triggerRouteService, new ObjectMapper());
    }

    @Test
    void reportEvent_setsCorrectCanvasCount() {
        // Arrange: valid event definition exists
        EventDefinition def = new EventDefinition();
        def.setEventCode("ORDER_COMPLETE");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventMapper.selectOne(any())).thenReturn(def);

        // Two canvases are subscribed to this event
        when(triggerRouteService.getCanvasByBehavior("ORDER_COMPLETE"))
                .thenReturn(Set.of("1", "2"));

        // Capture the EventLog passed to logMapper.insert()
        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
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

        // Assert: EventLog stored with real counts
        EventLog insertedLog = captor.getValue();
        assertThat(insertedLog.getCanvasTriggered()).isEqualTo(2);
        assertThat(insertedLog.getCanvasCount()).isEqualTo(2);

        // Assert: response also reflects the real count
        assertThat(result.get("canvasTriggered")).isEqualTo(2);
        assertThat(result.get("status")).isEqualTo("ACCEPTED");
    }

    @Test
    void reportEvent_setsZeroCanvasCount_whenNoCanvasSubscribed() {
        // Arrange: valid event definition exists but no canvas subscribed
        EventDefinition def = new EventDefinition();
        def.setEventCode("UNUSED_EVENT");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventMapper.selectOne(any())).thenReturn(def);

        when(triggerRouteService.getCanvasByBehavior("UNUSED_EVENT"))
                .thenReturn(Set.of());

        ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
        doAnswer(invocation -> null).when(logMapper).insert(captor.capture());

        EventReportReq req = new EventReportReq();
        req.setEventCode("UNUSED_EVENT");
        req.setUserId("user-99");

        // Act
        controller.reportEvent(req).block();

        // Assert: EventLog stored with 0 (no canvases)
        EventLog insertedLog = captor.getValue();
        assertThat(insertedLog.getCanvasTriggered()).isEqualTo(0);
        assertThat(insertedLog.getCanvasCount()).isEqualTo(0);
    }

    @Test
    void reportEvent_usesEventDefinitionCache_onSecondCall() {
        // Arrange: valid event definition exists
        EventDefinition def = new EventDefinition();
        def.setEventCode("CACHED_EVENT");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventMapper.selectOne(any())).thenReturn(def);

        when(triggerRouteService.getCanvasByBehavior("CACHED_EVENT"))
                .thenReturn(Set.of());

        doAnswer(invocation -> null).when(logMapper).insert(any(EventLog.class));

        EventReportReq req = new EventReportReq();
        req.setEventCode("CACHED_EVENT");
        req.setUserId("user-cache-test");

        // Act: call reportEvent twice with the same eventCode
        controller.reportEvent(req).block();
        controller.reportEvent(req).block();

        // Assert: DB was only hit once; second call served from cache
        verify(eventMapper, times(1)).selectOne(any());
    }
}
