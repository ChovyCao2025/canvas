package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.dal.dataobject.CanvasManualApprovalDO;
import org.chovy.canvas.dal.mapper.CanvasManualApprovalMapper;
import org.chovy.canvas.domain.notification.NotificationEventService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
