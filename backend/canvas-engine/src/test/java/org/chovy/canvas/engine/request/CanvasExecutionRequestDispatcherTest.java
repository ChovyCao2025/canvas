package org.chovy.canvas.engine.request;

import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestDispatcherTest {

    @Test
    void dispatchDueRequestsPublishesEachDueRequestToDisruptor() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestDispatcher dispatcher = new CanvasExecutionRequestDispatcher(
                mapper, disruptorService, 100, 300);
        when(mapper.selectDueRequests(eq(100), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(request("req-1", 10L), request("req-2", 10L)));

        dispatcher.dispatchDueRequests();

        verify(disruptorService).publishRequest("req-1");
        verify(disruptorService).publishRequest("req-2");
    }

    @Test
    void dispatchDueRequestsLimitsPerCanvasWithinBatch() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestDispatcher dispatcher = new CanvasExecutionRequestDispatcher(
                mapper, disruptorService, 10, 300, 1);
        when(mapper.selectDueRequests(eq(10), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(
                        request("req-1", 10L),
                        request("req-2", 10L),
                        request("req-3", 20L)
                ));

        dispatcher.dispatchDueRequests();

        verify(disruptorService).publishRequest("req-1");
        verify(disruptorService, never()).publishRequest("req-2");
        verify(disruptorService).publishRequest("req-3");
    }

    private CanvasExecutionRequest request(String id, Long canvasId) {
        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setId(id);
        request.setCanvasId(canvasId);
        return request;
    }
}
