package org.chovy.canvas.execution.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.execution.api.trace.ExecutionTraceView;
import org.chovy.canvas.execution.application.ExecutionNodeTraceRecord;
import org.chovy.canvas.execution.application.ExecutionTraceRecord;
import org.junit.jupiter.api.Test;

class MyBatisExecutionTraceRepositoryTest {

    @Test
    void mapsTraceRecordsToExecutionAndTraceRows() {
        MyBatisExecutionTraceRepository mapper = new MyBatisExecutionTraceRepository(null, null);
        ExecutionTraceRecord trace = new ExecutionTraceRecord(
                6L,
                "exec-map-1",
                18L,
                31L,
                "RUNNING",
                Instant.parse("2026-06-10T04:50:00Z"),
                null,
                "");
        ExecutionNodeTraceRecord node = new ExecutionNodeTraceRecord(
                6L,
                "exec-map-1",
                "node-1",
                "IF_CONDITION",
                "SUCCESS",
                "",
                Map.of("matched", true, "response", Map.of("reason", "approved")),
                Instant.parse("2026-06-10T04:50:01Z"));

        CanvasExecutionDO executionRow = mapper.toExecutionRow(trace);
        CanvasExecutionTraceDO traceRow = mapper.toTraceRow(node);

        assertThat(executionRow.id).isEqualTo("exec-map-1");
        assertThat(executionRow.tenantId).isEqualTo(6L);
        assertThat(executionRow.canvasId).isEqualTo(18L);
        assertThat(executionRow.versionId).isEqualTo(31L);
        assertThat(executionRow.status).isEqualTo(0);
        assertThat(traceRow.executionId).isEqualTo("exec-map-1");
        assertThat(traceRow.nodeType).isEqualTo("IF_CONDITION");
        assertThat(traceRow.status).isEqualTo(1);
        assertThat(traceRow.outputData).contains("\"matched\":true");
        assertThat(traceRow.outputData).contains("\"response\":{\"reason\":\"approved\"}");
    }

    @Test
    void getMapsPersistedNodeTraceRowsIntoTraceView() {
        CanvasExecutionMapper executionMapper = mock(CanvasExecutionMapper.class);
        CanvasExecutionTraceMapper traceMapper = mock(CanvasExecutionTraceMapper.class);
        CanvasExecutionDO execution = new CanvasExecutionDO();
        execution.id = "exec-map-2";
        execution.tenantId = 6L;
        execution.canvasId = 18L;
        execution.status = 2;
        execution.createdAt = LocalDateTime.of(2026, 6, 10, 4, 50, 0);
        execution.updatedAt = LocalDateTime.of(2026, 6, 10, 4, 50, 2);
        CanvasExecutionTraceDO node = new CanvasExecutionTraceDO();
        node.executionId = "exec-map-2";
        node.nodeId = "node-1";
        node.nodeType = "IF_CONDITION";
        node.status = 1;
        node.errorMsg = "";
        node.outputData = "{\"matched\":true,\"segment\":\"vip\"}";
        node.startedAt = LocalDateTime.of(2026, 6, 10, 4, 50, 1);
        node.finishedAt = LocalDateTime.of(2026, 6, 10, 4, 50, 1);
        when(executionMapper.selectById("exec-map-2")).thenReturn(execution);
        when(traceMapper.selectByExecutionId("exec-map-2")).thenReturn(List.of(node));

        ExecutionTraceView trace = new MyBatisExecutionTraceRepository(executionMapper, traceMapper)
                .get(6L, "exec-map-2");

        assertThat(trace.nodeResults()).hasSize(1);
        assertThat(trace.nodeResults().getFirst())
                .extracting(
                        ExecutionTraceView.NodeResultView::nodeId,
                        ExecutionTraceView.NodeResultView::nodeType,
                        ExecutionTraceView.NodeResultView::status,
                        ExecutionTraceView.NodeResultView::error)
                .containsExactly("node-1", "IF_CONDITION", "SUCCESS", "");
        assertThat(trace.nodeResults().getFirst().outputData())
                .containsEntry("matched", true)
                .containsEntry("segment", "vip");
    }

    @Test
    void preservesWaitingNodeStatusForPausedTraceRows() {
        MyBatisExecutionTraceRepository mapper = new MyBatisExecutionTraceRepository(null, null);
        ExecutionNodeTraceRecord waitingNode = new ExecutionNodeTraceRecord(
                6L,
                "exec-waiting-1",
                "wait-1",
                "WAIT",
                "WAITING",
                "",
                Map.of("waitStatus", "PENDING"),
                Instant.parse("2026-06-10T04:51:01Z"));

        CanvasExecutionTraceDO row = mapper.toTraceRow(waitingNode);
        ExecutionTraceView.NodeResultView view = mapper.toNodeResultView(row);

        assertThat(view.status()).isEqualTo("WAITING");
    }
}
