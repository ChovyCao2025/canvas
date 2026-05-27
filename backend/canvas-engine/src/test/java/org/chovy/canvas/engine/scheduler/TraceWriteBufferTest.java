package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Trace Write Buffer 测试类。
 *
 * <p>覆盖该后端组件在典型输入、边界条件和异常场景下的行为，确保重构或性能优化不会改变既有契约。
 * <p>测试代码只构造必要的依赖与数据，断言重点放在可观察结果、状态变更和关键副作用上。
 */
class TraceWriteBufferTest {

    @Test
    void flushDrainsPendingCounterWithBatchInsert() {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);

        buffer.offer(trace("exec-1", "node-1"));
        buffer.offer(trace("exec-1", "node-2"));
        assertThat(buffer.pendingCount()).isEqualTo(2);

        buffer.flush();

        ArgumentCaptor<List<CanvasExecutionTraceDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(buffer.pendingCount()).isZero();
    }

    private CanvasExecutionTraceDO trace(String executionId, String nodeId) {
        return CanvasExecutionTraceDO.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeType("TEST")
                .status(1)
                .build();
    }
}
