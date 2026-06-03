package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.dal.dataobject.CanvasExecutionTraceDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionTraceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

    @Test
    void flushPreservesTraceAtBatchBoundary() {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);

        for (int i = 0; i < 201; i++) {
            buffer.offer(trace("exec-" + i, "node-" + i));
        }

        buffer.flush();

        ArgumentCaptor<List<CanvasExecutionTraceDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper, times(2)).insertBatch(captor.capture());
        assertThat(captor.getAllValues().get(0)).hasSize(200);
        List<CanvasExecutionTraceDO> remainingBatch = captor.getAllValues().get(1);
        assertThat(remainingBatch).hasSize(1);
        assertThat(remainingBatch.getFirst().getNodeId()).isEqualTo("node-200");
        assertThat(buffer.pendingCount()).isZero();
    }

    @Test
    void flushDropsBatchAfterWriteFailureAndClearsPendingByDesign() {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        doThrow(new RuntimeException("db down")).when(mapper).insertBatch(anyList());
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);

        buffer.offer(trace("exec-1", "node-1"));
        buffer.offer(trace("exec-1", "node-2"));

        buffer.flush();

        verify(mapper).insertBatch(anyList());
        // Current design treats traces as best-effort audit data once write is attempted.
        assertThat(buffer.pendingCount()).isZero();
    }

    @Test
    void addTrace_nonCriticalSamplesAboveEightyPercentCapacity() {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);

        for (int i = 0; i < 40_000; i++) {
            assertThat(buffer.addTrace(trace("exec-" + i, "node-" + i), false)).isTrue();
        }

        int accepted = 0;
        int rejected = 0;
        for (int i = 0; i < 1_000; i++) {
            if (buffer.addTrace(trace("overflow-" + i, "node-" + i), false)) {
                accepted++;
            } else {
                rejected++;
            }
        }

        assertThat(accepted).isGreaterThan(0);
        assertThat(rejected).isGreaterThan(0);
        assertThat(buffer.pendingCount()).isEqualTo(40_000 + accepted);
        assertThat(buffer.pendingCount()).isLessThan(41_000);
    }

    @Test
    void addTrace_criticalFlushesAndEnqueuesWhenFull() {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);

        for (int i = 0; i < 50_000; i++) {
            buffer.addTrace(trace("exec-" + i, "node-" + i), true);
        }

        assertThat(buffer.pendingCount()).isEqualTo(50_000);
        assertThat(buffer.addTrace(trace("critical", "node-critical"), true)).isTrue();

        assertThat(buffer.pendingCount()).isLessThan(50_000);
        verify(mapper, atLeastOnce()).insertBatch(anyList());
    }

    @Test
    void scheduledFlushUsesDedicatedThread() throws Exception {
        CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
        TraceWriteBuffer buffer = new TraceWriteBuffer(mapper);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        buffer.onScheduledFlushForTest(thread -> {
            threadName.set(thread.getName());
            latch.countDown();
        });
        buffer.startScheduler();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).startsWith("trace-write-buffer-flush-");

        buffer.shutdownFlush();
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
