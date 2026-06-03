package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.common.enums.ApprovalStatus;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ManualApprovalHandlerTest {

    @Test
    void firstEntryReturnsPendingResultInsteadOfSuccessfulTerminalResult() {
        CanvasManualApprovalMapper approvalMapper = mock(CanvasManualApprovalMapper.class);
        NotificationEventService notificationEventService = mock(NotificationEventService.class);
        ManualApprovalHandler handler = new ManualApprovalHandler(
                approvalMapper, new ObjectMapper(), notificationEventService);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");

        var result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "approval-1",
                "approvers", List.of("ops")
        ), ctx).block();

        assertThat(result.pending()).isTrue();
        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.nextNodeId()).isNull();
        verify(approvalMapper).insert(any(CanvasManualApprovalDO.class));
        verify(notificationEventService).approvalPending(any(CanvasManualApprovalDO.class), any());
    }

    @Test
    void insertFailureReturnsFailedResultInsteadOfPendingForever() {
        CanvasManualApprovalMapper approvalMapper = mock(CanvasManualApprovalMapper.class);
        NotificationEventService notificationEventService = mock(NotificationEventService.class);
        doThrow(new RuntimeException("db down")).when(approvalMapper).insert(any(CanvasManualApprovalDO.class));
        ManualApprovalHandler handler = new ManualApprovalHandler(
                approvalMapper, new ObjectMapper(), notificationEventService);
        ExecutionContext ctx = baseContext();

        var result = handler.executeAsync(Map.of(MapFieldKeys.NODE_ID_INTERNAL, "approval-1"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.pending()).isFalse();
        assertThat(result.errorMessage()).contains("创建审批记录失败");
        verify(notificationEventService, never()).approvalPending(any(), any());
    }

    @Test
    void notificationFailureDeletesApprovalAndReturnsFailedResult() {
        CanvasManualApprovalMapper approvalMapper = mock(CanvasManualApprovalMapper.class);
        NotificationEventService notificationEventService = mock(NotificationEventService.class);
        doThrow(new RuntimeException("notify down"))
                .when(notificationEventService).approvalPending(any(CanvasManualApprovalDO.class), any());
        ManualApprovalHandler handler = new ManualApprovalHandler(
                approvalMapper, new ObjectMapper(), notificationEventService);
        ExecutionContext ctx = baseContext();

        var result = handler.executeAsync(Map.of(MapFieldKeys.NODE_ID_INTERNAL, "approval-1"), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.pending()).isFalse();
        assertThat(result.errorMessage()).contains("审批通知发送失败");
        verify(approvalMapper).deleteById("exec-1:approval-1");
    }

    @Test
    void rejectedApprovalRoutesToRejectNodeWhenConfigured() {
        ManualApprovalHandler handler = new ManualApprovalHandler(
                mock(CanvasManualApprovalMapper.class), new ObjectMapper(), mock(NotificationEventService.class));
        ExecutionContext ctx = baseContext();
        ctx.putTriggerPayloadValue(ManualApprovalHandler.APPROVAL_RESULT_KEY + "approval-1",
                ApprovalStatus.REJECTED);

        var result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "approval-1",
                MapFieldKeys.APPROVE_NODE_ID, "approve-next",
                MapFieldKeys.REJECT_NODE_ID, "reject-next"
        ), ctx).block();

        assertThat(result.success()).isTrue();
        assertThat(result.nextNodeId()).isEqualTo("reject-next");
    }

    @Test
    void rejectedApprovalWithoutRejectNodeFailsEvenIfApproveNodeExists() {
        ManualApprovalHandler handler = new ManualApprovalHandler(
                mock(CanvasManualApprovalMapper.class), new ObjectMapper(), mock(NotificationEventService.class));
        ExecutionContext ctx = baseContext();
        ctx.putTriggerPayloadValue(ManualApprovalHandler.APPROVAL_RESULT_KEY + "approval-1",
                ApprovalStatus.REJECTED);

        var result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "approval-1",
                MapFieldKeys.APPROVE_NODE_ID, "approve-next"
        ), ctx).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("人工审批被拒绝");
    }

    private ExecutionContext baseContext() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
