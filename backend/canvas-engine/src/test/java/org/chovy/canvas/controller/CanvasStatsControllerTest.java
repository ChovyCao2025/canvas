package org.chovy.canvas.web;

import org.chovy.canvas.dal.dataobject.CanvasExecutionDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionMapper;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasStatsControllerTest {

    @Test
    void getTraceReturnsEmptyWhenExecutionDoesNotBelongToCanvas() {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        CanvasStatsController controller = new CanvasStatsController(executionMapper, traceMapper);
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.setId("exec-1");
        execution.setCanvasId(20L);
        when(executionMapper.selectById("exec-1")).thenReturn(execution);

        var response = controller.getTrace(10L, "exec-1").block();

        assertThat(response.getData()).isEmpty();
        verify(traceMapper, never()).selectList(any());
    }
}
