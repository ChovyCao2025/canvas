package org.chovy.canvas.engine.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.infrastructure.concurrent.ManagedVirtualThreadExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ExecutionDlqWriterTest {

    @Test
    void writesReplayableDlqThroughManagedBackgroundExecutor() {
        CanvasExecutionDlqMapper dlqMapper = mock(CanvasExecutionDlqMapper.class);
        CanvasMetrics metrics = mock(CanvasMetrics.class);
        ExecutionDlqWriter writer = new ExecutionDlqWriter(
                dlqMapper,
                metrics,
                new ObjectMapper(),
                ManagedVirtualThreadExecutor.direct());
        ExecutionContext ctx = context();

        writer.write(ctx, "node-1", "TEST_NODE", new RuntimeException("boom"), 3);

        ArgumentCaptor<CanvasExecutionDlqDO> captor = ArgumentCaptor.forClass(CanvasExecutionDlqDO.class);
        verify(dlqMapper).insert(captor.capture());
        verify(metrics).recordDlq("TEST_NODE");
        CanvasExecutionDlqDO dlq = captor.getValue();
        assertThat(dlq.getExecutionId()).isEqualTo("exec-1");
        assertThat(dlq.getCanvasId()).isEqualTo(10L);
        assertThat(dlq.getUserId()).isEqualTo("user-1");
        assertThat(dlq.getFailedNodeId()).isEqualTo("node-1");
        assertThat(dlq.getFailedNodeType()).isEqualTo("TEST_NODE");
        assertThat(dlq.getRetryCount()).isEqualTo(3);
        assertThat(dlq.getTriggerType()).isEqualTo("DIRECT_CALL");
        assertThat(dlq.getTriggerNodeType()).isEqualTo("MANUAL");
        assertThat(dlq.getMatchKey()).isEqualTo("manual");
        assertThat(dlq.getTriggerPayload()).contains("\"source\":\"unit-test\"");
        assertThat(dlq.getFailedAt()).isNotNull();
    }

    @Test
    void truncatesLongErrorMessagesToColumnSafeLength() {
        CanvasExecutionDlqMapper dlqMapper = mock(CanvasExecutionDlqMapper.class);
        ExecutionDlqWriter writer = new ExecutionDlqWriter(
                dlqMapper,
                mock(CanvasMetrics.class),
                new ObjectMapper(),
                ManagedVirtualThreadExecutor.direct());

        writer.write(context(), "node-1", "TEST_NODE", new RuntimeException("x".repeat(600)), 2);

        ArgumentCaptor<CanvasExecutionDlqDO> captor = ArgumentCaptor.forClass(CanvasExecutionDlqDO.class);
        verify(dlqMapper).insert(captor.capture());
        assertThat(captor.getValue().getErrorMsg()).hasSize(500);
    }

    @Test
    void doesNotInsertWhenBackgroundExecutorRejectsTask() {
        CanvasExecutionDlqMapper dlqMapper = mock(CanvasExecutionDlqMapper.class);
        ManagedVirtualThreadExecutor backgroundExecutor = mock(ManagedVirtualThreadExecutor.class);
        doThrow(new RejectedExecutionException("closed"))
                .when(backgroundExecutor)
                .submit(any(), any(Runnable.class));
        ExecutionDlqWriter writer = new ExecutionDlqWriter(
                dlqMapper,
                mock(CanvasMetrics.class),
                new ObjectMapper(),
                backgroundExecutor);

        writer.write(context(), "node-1", "TEST_NODE", new RuntimeException("boom"), 1);

        verify(dlqMapper, never()).insert(any(CanvasExecutionDlqDO.class));
    }

    private ExecutionContext context() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        ctx.setTriggerType("DIRECT_CALL");
        ctx.setTriggerNodeType("MANUAL");
        ctx.setMatchKey("manual");
        ctx.setTriggerPayload(Map.of("source", "unit-test"));
        return ctx;
    }
}
