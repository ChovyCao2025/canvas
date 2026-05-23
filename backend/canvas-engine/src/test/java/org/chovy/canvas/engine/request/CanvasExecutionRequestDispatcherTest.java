package org.chovy.canvas.engine.request;

import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestDispatcherTest {

    @Test
    void dispatchDueRequestsPublishesEachDueRequestToDisruptor() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestDispatcher dispatcher = new CanvasExecutionRequestDispatcher(
                mapper, disruptorService, 100, 300);
        when(mapper.selectDue(eq(100), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of("req-1", "req-2"));

        dispatcher.dispatchDueRequests();

        verify(disruptorService).publishRequest("req-1");
        verify(disruptorService).publishRequest("req-2");
    }
}
