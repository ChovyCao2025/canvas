package org.chovy.canvas.engine.wait;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.handlers.WaitHandler;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WaitEventFilterTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void waitHandlerPersistsEventFilterAndBranches() {
        WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
        WaitHandler handler = new WaitHandler(waitService, OBJECT_MAPPER);

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "wait-1",
                MapFieldKeys.WAIT_TYPE, WaitSubscriptionService.WAIT_TYPE_EVENT,
                MapFieldKeys.EVENT_CODE, "ORDER_PAID",
                MapFieldKeys.EVENT_FILTERS, Map.of("amount", Map.of("gte", 100)),
                MapFieldKeys.MAX_WAIT, Map.of("value", 30, "unit", "MINUTES"),
                MapFieldKeys.NEXT_NODE_ID, "done-1",
                MapFieldKeys.TIMEOUT_NODE_ID, "timeout-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        verify(waitService).createEventWait(
                eq("exec-1"),
                eq(10L),
                eq(20L),
                eq("user-1"),
                eq("wait-1"),
                eq("ORDER_PAID"),
                eq("{\"amount\":{\"gte\":100}}"),
                eq("{\"sourceNodeId\":\"wait-1\",\"successNodeId\":\"done-1\",\"timeoutNodeId\":\"timeout-1\"}"),
                any(LocalDateTime.class));
    }

    @Test
    void matchingPredicateCompletesOnceAndTriggersResumeWithBranchPayload() throws Exception {
        WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(waitService, executionService, OBJECT_MAPPER);
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100},\"tier\":{\"in\":[\"gold\",\"platinum\"]}}");
        wait.setResumePayload("{\"successNodeId\":\"done-1\",\"timeoutNodeId\":\"timeout-1\"}");
        when(waitService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));
        when(waitService.completeWait(eq(99L), any())).thenReturn(1);
        when(executionService.trigger(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(Mono.just(Map.of()));

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1",
                Map.of("amount", 150, "tier", "gold"), "evt-1");

        assertThat(resumed).isEqualTo(1);
        ArgumentCaptor<String> storedPayload = ArgumentCaptor.forClass(String.class);
        verify(waitService).completeWait(eq(99L), storedPayload.capture());
        Map<String, Object> persisted = OBJECT_MAPPER.readValue(storedPayload.getValue(), new TypeReference<>() {});
        assertThat(persisted).containsEntry(MapFieldKeys.SUCCESS_NODE_ID, "done-1");
        assertThat(persisted).containsEntry(MapFieldKeys.TIMEOUT_NODE_ID, "timeout-1");
        assertThat(persisted).containsEntry(MapFieldKeys.WAIT_RESUME_STATUS, WaitSubscriptionService.STATUS_COMPLETED);
        assertThat((Map<String, Object>) persisted.get(MapFieldKeys.EVENT_ATTRIBUTES))
                .containsEntry("amount", 150)
                .containsEntry("tier", "gold");
        verify(executionService).trigger(eq(10L), eq("user-1"), eq(TriggerType.WAIT_RESUME),
                eq(NodeType.WAIT), eq("wait-1"), any(), eq("exec-1:wait:99:COMPLETED"), eq(false));
    }

    @Test
    void nonMatchingEventSkipsResume() {
        WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(waitService, executionService, OBJECT_MAPPER);
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100}}");
        when(waitService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 50), "evt-1");

        assertThat(resumed).isZero();
        verify(waitService, never()).completeWait(any(), any());
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    void conversationReplyResumesOnlyWaitsMatchingIntent() {
        WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(waitService, executionService, OBJECT_MAPPER);
        CanvasWaitSubscriptionDO matching = waitRecord();
        matching.setId(100L);
        matching.setEventCode("CONVERSATION_REPLY");
        matching.setEventFilters("{\"intent\":\"PRODUCT_A\"}");
        CanvasWaitSubscriptionDO skipped = waitRecord();
        skipped.setId(101L);
        skipped.setEventCode("CONVERSATION_REPLY");
        skipped.setEventFilters("{\"intent\":\"PRODUCT_B\"}");
        when(waitService.findActiveEventWaits("CONVERSATION_REPLY", "user-1")).thenReturn(List.of(matching, skipped));
        when(waitService.completeWait(eq(100L), any())).thenReturn(1);
        when(executionService.trigger(any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(Mono.just(Map.of()));

        int resumed = service.resumeEventWaits("CONVERSATION_REPLY", "user-1",
                Map.of("intent", "PRODUCT_A", "sessionId", 100L, "messageId", 200L), "evt-1");

        assertThat(resumed).isEqualTo(1);
        verify(waitService).completeWait(eq(100L), any());
        verify(waitService, never()).completeWait(eq(101L), any());
        verify(executionService).trigger(eq(10L), eq("user-1"), eq(TriggerType.WAIT_RESUME),
                eq(NodeType.WAIT), eq("wait-1"), any(), eq("exec-1:wait:100:COMPLETED"), eq(false));
    }

    @Test
    void duplicateResumeDoesNotTriggerWhenCasFails() {
        WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
        CanvasExecutionService executionService = mock(CanvasExecutionService.class);
        WaitResumeService service = new WaitResumeService(waitService, executionService, OBJECT_MAPPER);
        CanvasWaitSubscriptionDO wait = waitRecord();
        wait.setEventFilters("{\"amount\":{\"gt\":100}}");
        when(waitService.findActiveEventWaits("ORDER_PAID", "user-1")).thenReturn(List.of(wait));
        when(waitService.completeWait(eq(99L), any())).thenReturn(0);

        int resumed = service.resumeEventWaits("ORDER_PAID", "user-1", Map.of("amount", 150), "evt-1");

        assertThat(resumed).isZero();
        verify(executionService, never()).trigger(any(), any(), any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    void completedResumeRoutesToPersistedSuccessBranch() {
        WaitHandler handler = new WaitHandler(mock(WaitSubscriptionService.class), OBJECT_MAPPER);

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "wait-1",
                MapFieldKeys.WAIT_RESUME_STATUS, WaitSubscriptionService.STATUS_COMPLETED,
                MapFieldKeys.SUCCESS_NODE_ID, "done-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "done-1");
    }

    private static CanvasWaitSubscriptionDO waitRecord() {
        CanvasWaitSubscriptionDO wait = new CanvasWaitSubscriptionDO();
        wait.setId(99L);
        wait.setExecutionId("exec-1");
        wait.setCanvasId(10L);
        wait.setVersionId(20L);
        wait.setUserId("user-1");
        wait.setNodeId("wait-1");
        wait.setWaitType(WaitSubscriptionService.WAIT_TYPE_EVENT);
        wait.setEventCode("ORDER_PAID");
        wait.setResumePayload("{}");
        return wait;
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(20L);
        ctx.setUserId("user-1");
        return ctx;
    }
}
