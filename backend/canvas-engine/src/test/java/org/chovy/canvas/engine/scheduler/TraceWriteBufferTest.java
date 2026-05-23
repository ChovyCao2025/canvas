package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.domain.execution.CanvasExecutionTrace;
import org.chovy.canvas.domain.execution.CanvasExecutionTraceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TraceWriteBufferTest {

    @Test
    void flushDrainsPendingCounterWithBatchInsert() {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);

        buffer.offer(trace("exec-1", "node-1"));
        buffer.offer(trace("exec-1", "node-2"));
        assertThat(buffer.pendingCount()).isEqualTo(2);

        buffer.flush();

        ArgumentCaptor<List<CanvasExecutionTrace>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(buffer.pendingCount()).isZero();
    }

    private CanvasExecutionTrace trace(String executionId, String nodeId) {
        return CanvasExecutionTrace.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeType("TEST")
                .status(1)
                .build();
    }
}
