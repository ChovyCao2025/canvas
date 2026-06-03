package org.chovy.canvas.engine.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionRequestDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.scheduler.CanvasMetrics;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestExecutorTest {

    @Test
    void terminalRequestIsNotExecutedAgain() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
        request.setId("mq-10-abc");
        request.setStatus(CanvasExecutionRequestStatus.SUCCEEDED);
        when(mapper.selectById("mq-10-abc")).thenReturn(request);

        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
                5_000L, 5, 300L, 60_000L);

        executor.execute("mq-10-abc").block();

        verify(mapper, never()).markRunning(anyString(), any(), any(), anyString());
        verify(executionService, never()).triggerFromExecutionRequest(
                anyLong(), nullable(String.class), anyString(), anyString(), anyString(),
                anyMap(), nullable(String.class), anyInt(), nullable(String.class));
    }

    @Test
    void missingRequestIsSkippedWithoutExecution() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);

        CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
                mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
                5_000L, 5, 300L, 60_000L);

        executor.execute("missing").block();

        verify(executionService, never()).triggerFromExecutionRequest(
                anyLong(), nullable(String.class), anyString(), anyString(), anyString(),
                anyMap(), nullable(String.class), anyInt(), nullable(String.class));
    }
}
