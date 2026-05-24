package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CanvasWaitSubscriptionDO;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WaitHandlerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-23T10:15:30Z"),
            ZoneId.of("Asia/Shanghai")
    );

    private final WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
    private final WaitHandler handler = new WaitHandler(waitService, new ObjectMapper(), CLOCK);

    @Test
    void duration_wait_returns_pending_resume_time() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "DURATION",
                "duration", Map.of("value", 3, "unit", "DAYS")
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.pending()).isTrue();
        assertThat(result.resumeAtEpochMs()).isEqualTo(
                LocalDateTime.of(2026, 5, 26, 18, 15, 30)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .toEpochMilli()
        );
    }

    @Test
    void timeWindow_continues_when_now_is_inside_window() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "TIME_WINDOW",
                "windowStart", "09:00",
                "windowEnd", "20:00",
                "nextNodeId", "next-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "next-1");
    }

    @Test
    void timeWindow_waits_until_next_start_when_now_is_outside_window() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "TIME_WINDOW",
                "windowStart", "20:00",
                "windowEnd", "22:00"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.resumeAtEpochMs()).isEqualTo(
                LocalDateTime.of(2026, 5, 23, 20, 0)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .toEpochMilli()
        );
    }

    @Test
    void untilDate_continues_immediately_when_target_is_past() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "UNTIL_DATE",
                "untilDate", "2026-05-23T17:00:00",
                "nextNodeId", "next-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("success", "next-1");
    }

    @Test
    void relativeTime_waits_until_tomorrow_when_time_already_passed_today() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "RELATIVE_TIME",
                "time", "09:30"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.resumeAtEpochMs()).isEqualTo(
                LocalDateTime.of(2026, 5, 24, 9, 30)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .toEpochMilli()
        );
    }

    @Test
    void untilEvent_creates_subscription_and_returns_pending() {
        ExecutionContext ctx = ctx();
        CanvasWaitSubscriptionDO wait = new CanvasWaitSubscriptionDO();
        wait.setId(99L);
        when(waitService.createEventWait(
                eq("exec-1"),
                eq(10L),
                eq(20L),
                eq("user-1"),
                eq("wait-1"),
                eq("ORDER_PAID"),
                eq("{\"amount\":{\"gt\":100}}"),
                eq("{\"sourceNodeId\":\"wait-1\",\"successNodeId\":\"next-1\",\"timeoutNodeId\":\"timeout-1\"}"),
                eq(LocalDateTime.of(2026, 5, 23, 18, 45, 30))
        )).thenReturn(wait);

        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "UNTIL_EVENT",
                "eventCode", "ORDER_PAID",
                "eventFilters", Map.of("amount", Map.of("gt", 100)),
                "maxWait", Map.of("value", 30, "unit", "MINUTES"),
                "nextNodeId", "next-1",
                "timeoutNodeId", "timeout-1"
        ), ctx).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.pending()).isTrue();
        assertThat(result.resumeAtEpochMs()).isEqualTo(
                LocalDateTime.of(2026, 5, 23, 18, 45, 30)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .toEpochMilli()
        );

        verify(waitService).createEventWait(
                eq("exec-1"),
                eq(10L),
                eq(20L),
                eq("user-1"),
                eq("wait-1"),
                eq("ORDER_PAID"),
                eq("{\"amount\":{\"gt\":100}}"),
                eq("{\"sourceNodeId\":\"wait-1\",\"successNodeId\":\"next-1\",\"timeoutNodeId\":\"timeout-1\"}"),
                eq(LocalDateTime.of(2026, 5, 23, 18, 45, 30))
        );
    }

    @Test
    void untilEvent_requires_max_wait() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "waitType", "UNTIL_EVENT",
                "eventCode", "ORDER_PAID"
        ), ctx()).block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("maxWait");
        verifyNoInteractions(waitService);
    }

    @Test
    void timeout_resume_routes_to_timeout_branch() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "wait-1",
                "__waitResumeStatus", "TIMEOUT",
                "timeoutNodeId", "timeout-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
        assertThat(result.routes()).containsEntry("timeout", "timeout-1");
        assertThat(result.reasonCode()).isEqualTo("WAIT_TIMEOUT");
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
