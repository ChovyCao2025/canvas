package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.canvas.UserInputService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserInputHandlerTest {

    @Test
    void rejectsMissingFormSchema() {
        UserInputHandler handler = new UserInputHandler(mock(UserInputService.class));

        NodeResult result = handler.executeAsync(Map.of(MapFieldKeys.NODE_ID_INTERNAL, "input-1"), ctx()).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("formSchema");
    }

    @Test
    void createsPendingResponseAndReturnsTraceOutput() {
        UserInputService service = mock(UserInputService.class);
        when(service.createPending(any(), eq("input-1"), any(), eq("done-1"), eq("timeout-1"), any()))
                .thenReturn(new UserInputService.PendingInput(
                        11L, 12L, UserInputService.STATUS_PENDING,
                        LocalDateTime.now().plusMinutes(30), "timeout-1"));
        UserInputHandler handler = new UserInputHandler(service);

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "input-1",
                "formSchema", List.of(Map.of("key", "email", "type", "text")),
                "completedNodeId", "done-1",
                MapFieldKeys.TIMEOUT_NODE_ID, "timeout-1",
                MapFieldKeys.MAX_WAIT, Map.of("value", 30, "unit", "MINUTES")
        ), ctx()).block();

        assertThat(result.pending()).isTrue();
        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.output()).containsEntry("inputResponseId", 12L);
        assertThat(result.output()).containsEntry("inputStatus", UserInputService.STATUS_PENDING);
        verify(service).createPending(any(), eq("input-1"), any(), eq("done-1"), eq("timeout-1"), any());
    }

    @Test
    void completedResumeRoutesToCompletedNodeAndOutputsResponse() {
        ExecutionContext ctx = ctx();
        ctx.getTriggerPayload().put(MapFieldKeys.WAIT_RESUME_STATUS, UserInputService.STATUS_COMPLETED);
        ctx.getTriggerPayload().put("inputResponseId", 12L);
        ctx.getTriggerPayload().put("inputResponse", Map.of("email", "a@example.com"));
        ctx.getTriggerPayload().put("completedNodeId", "done-1");
        UserInputHandler handler = new UserInputHandler(mock(UserInputService.class));

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "input-1",
                "formSchema", List.of(Map.of("key", "email")),
                MapFieldKeys.NEXT_NODE_ID, "fallback"
        ), ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "done-1");
        assertThat(result.output()).containsEntry("inputStatus", UserInputService.STATUS_COMPLETED);
        assertThat(result.output()).containsEntry("inputResponseId", 12L);
    }

    @Test
    void timeoutResumeRoutesToTimeoutBranch() {
        ExecutionContext ctx = ctx();
        ctx.getTriggerPayload().put(MapFieldKeys.WAIT_RESUME_STATUS, UserInputService.STATUS_EXPIRED);
        ctx.getTriggerPayload().put("inputResponseId", 12L);
        ctx.getTriggerPayload().put(MapFieldKeys.TIMEOUT_NODE_ID, "timeout-1");
        UserInputHandler handler = new UserInputHandler(mock(UserInputService.class));

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "input-1",
                "formSchema", List.of(Map.of("key", "email"))
        ), ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
        assertThat(result.routes()).containsEntry("timeout", "timeout-1");
        assertThat(result.output()).containsEntry("inputStatus", UserInputService.STATUS_EXPIRED);
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(7L);
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(20L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
