package org.chovy.canvas.controller;

import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.domain.execution.CanvasExecutionRequest;
import org.chovy.canvas.domain.execution.CanvasExecutionRequestMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanvasExecutionRequestManagementControllerTest {

    @Test
    void replayResetsRequestAndPublishesItBackToDisruptor() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptorService);

        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setId("req-1");
        request.setCanvasId(10L);
        request.setUserId("user-7");
        request.setTriggerType(TriggerType.MQ);
        request.setTriggerNodeType(NodeType.MQ_TRIGGER);
        request.setStatus(CanvasExecutionRequestStatus.FAILED);
        when(mapper.selectById("req-1")).thenReturn(request);
        when(mapper.markPendingForReplay(eq("req-1"), any(), eq("system"), eq("修复临时渠道失败"))).thenReturn(1);

        R<Map<String, Object>> response = controller.replay("req-1", "修复临时渠道失败", false).block();

        verify(mapper).markPendingForReplay(eq("req-1"), any(), eq("system"), eq("修复临时渠道失败"));
        verify(disruptorService).publishRequest("req-1");
        assertThat(response.getData()).containsEntry("requestId", "req-1");
        assertThat(response.getData()).containsEntry("status", "QUEUED");
    }

    @Test
    void replayRejectsNonRetryableStatusUnlessForceIsEnabled() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptorService);

        CanvasExecutionRequest request = request("req-2", CanvasExecutionRequestStatus.SUCCEEDED);
        when(mapper.selectById("req-2")).thenReturn(request);

        assertThatThrownBy(() -> controller.replay("req-2", "误触重放", false).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("只能重放 FAILED/RETRY");
    }

    @Test
    void replayForceAllowsNonRetryableStatus() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptorService);

        CanvasExecutionRequest request = request("req-2", CanvasExecutionRequestStatus.SUCCEEDED);
        when(mapper.selectById("req-2")).thenReturn(request);
        when(mapper.markPendingForReplay(eq("req-2"), any(), eq("system"), eq("人工强制重放"))).thenReturn(1);

        controller.replay("req-2", "人工强制重放", true).block();

        verify(mapper).markPendingForReplay(eq("req-2"), any(), eq("system"), eq("人工强制重放"));
        verify(disruptorService).publishRequest("req-2");
    }

    @Test
    void replayStillQueuesRequestWhenImmediateDisruptorPublishFails() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptorService);

        CanvasExecutionRequest request = request("req-3", CanvasExecutionRequestStatus.FAILED);
        when(mapper.selectById("req-3")).thenReturn(request);
        when(mapper.markPendingForReplay(eq("req-3"), any(), eq("system"), eq("等待调度器补偿"))).thenReturn(1);
        doThrow(new IllegalStateException("ring full")).when(disruptorService).publishRequest("req-3");

        R<Map<String, Object>> response = controller.replay("req-3", "等待调度器补偿", false).block();

        verify(mapper).markPendingForReplay(eq("req-3"), any(), eq("system"), eq("等待调度器补偿"));
        verify(disruptorService).publishRequest("req-3");
        assertThat(response.getData()).containsEntry("requestId", "req-3");
        assertThat(response.getData()).containsEntry("status", "QUEUED");
        assertThat(response.getData()).containsEntry("immediateDispatch", false);
    }

    @Test
    void replayBatchLimitsAndPublishesSelectedRequests() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptorService);
        when(mapper.selectList(any())).thenReturn(List.of(
                request("req-1", CanvasExecutionRequestStatus.FAILED),
                request("req-2", CanvasExecutionRequestStatus.RETRY)
        ));
        when(mapper.markPendingForReplay(eq("req-1"), any(), eq("system"), eq("批量恢复渠道失败"))).thenReturn(1);
        when(mapper.markPendingForReplay(eq("req-2"), any(), eq("system"), eq("批量恢复渠道失败"))).thenReturn(1);

        R<Map<String, Object>> response = controller.replayBatch(
                null, null, null, null, 10, "批量恢复渠道失败", false).block();

        verify(mapper).markPendingForReplay(eq("req-1"), any(), eq("system"), eq("批量恢复渠道失败"));
        verify(mapper).markPendingForReplay(eq("req-2"), any(), eq("system"), eq("批量恢复渠道失败"));
        verify(disruptorService).publishRequest("req-1");
        verify(disruptorService).publishRequest("req-2");
        verify(mapper, times(2)).markPendingForReplay(any(), any(), any(), any());
        assertThat(response.getData()).containsEntry("count", 2);
        assertThat(response.getData()).containsEntry("limit", 10);
    }

    @Test
    void replayBatchContinuesWhenOneImmediateDispatchFails() {
        CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
        CanvasDisruptorService disruptorService = mock(CanvasDisruptorService.class);
        CanvasExecutionRequestManagementController controller =
                new CanvasExecutionRequestManagementController(mapper, disruptorService);
        when(mapper.selectList(any())).thenReturn(List.of(
                request("req-1", CanvasExecutionRequestStatus.FAILED),
                request("req-2", CanvasExecutionRequestStatus.RETRY)
        ));
        when(mapper.markPendingForReplay(eq("req-1"), any(), eq("system"), eq("批量恢复"))).thenReturn(1);
        when(mapper.markPendingForReplay(eq("req-2"), any(), eq("system"), eq("批量恢复"))).thenReturn(1);
        doThrow(new IllegalStateException("ring full")).when(disruptorService).publishRequest("req-1");

        R<Map<String, Object>> response = controller.replayBatch(
                null, null, null, null, 10, "批量恢复", false).block();

        verify(disruptorService).publishRequest("req-1");
        verify(disruptorService).publishRequest("req-2");
        assertThat(response.getData()).containsEntry("count", 2);
        assertThat(response.getData()).containsEntry("dispatchFailureCount", 1);
        assertThat(response.getData().get("dispatchFailedRequestIds")).isEqualTo(List.of("req-1"));
    }

    private CanvasExecutionRequest request(String id, String status) {
        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setId(id);
        request.setCanvasId(10L);
        request.setUserId("user-7");
        request.setTriggerType(TriggerType.MQ);
        request.setTriggerNodeType(NodeType.MQ_TRIGGER);
        request.setStatus(status);
        return request;
    }
}
