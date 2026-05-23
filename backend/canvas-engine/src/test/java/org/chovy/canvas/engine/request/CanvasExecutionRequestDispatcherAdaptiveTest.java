package org.chovy.canvas.engine.request;

import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestDispatcherAdaptiveTest {

    @Test
    void dispatchDueRequestsAdaptsLimitsForHotAndHighPriorityCanvases() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        TriggerPriorityConfig priorityConfig = new TriggerPriorityConfig();
        CanvasExecutionRequestDispatcher dispatcher = new CanvasExecutionRequestDispatcher(
                mapper, disruptorService, metrics, priorityConfig,
                20, 300, 2, true, 2, 50, 1, 2);

        when(mapper.selectDueRequests(eq(20), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        request("hot-1", 10L, TriggerType.MQ),
                        request("hot-2", 10L, TriggerType.MQ),
                        request("hot-3", 10L, TriggerType.MQ),
                        request("hot-4", 10L, TriggerType.MQ),
                        request("high-1", 20L, TriggerType.DIRECT_CALL),
                        request("high-2", 20L, TriggerType.DIRECT_CALL),
                        request("high-3", 20L, TriggerType.DIRECT_CALL),
                        request("idle-1", 30L, TriggerType.MQ),
                        request("idle-2", 30L, TriggerType.MQ),
                        request("idle-3", 30L, TriggerType.MQ)
                ));

        dispatcher.dispatchDueRequests();

        verify(disruptorService).publishRequest("hot-1");
        verify(disruptorService, never()).publishRequest("hot-2");
        verify(disruptorService, never()).publishRequest("hot-3");
        verify(disruptorService, never()).publishRequest("hot-4");

        verify(disruptorService).publishRequest("high-1");
        verify(disruptorService).publishRequest("high-2");
        verify(disruptorService).publishRequest("high-3");

        verify(disruptorService).publishRequest("idle-1");
        verify(disruptorService).publishRequest("idle-2");
        verify(disruptorService).publishRequest("idle-3");

        verify(metrics).recordExecutionRequestSkipped("10", "adaptive_hot_canvas_limit");
    }

    private CanvasExecutionRequest request(String id, Long canvasId, String triggerType) {
        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setId(id);
        request.setCanvasId(canvasId);
        request.setTriggerType(triggerType);
        return request;
    }
}
