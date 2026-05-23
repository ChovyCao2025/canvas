package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.meta.EventLogMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalCheckHandlerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-23T10:15:30Z"),
            ZoneId.of("Asia/Shanghai")
    );

    private final EventLogMapper eventLogMapper = mock(EventLogMapper.class);
    private final WaitSubscriptionService waitService = mock(WaitSubscriptionService.class);
    private final GoalCheckHandler handler = new GoalCheckHandler(eventLogMapper, waitService, new ObjectMapper(), CLOCK);

    @Test
    void sync_goal_met_routes_to_goalMet_branch() {
        when(eventLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "goal-1",
                "eventCode", "ORDER_PAID",
                "goalMetNodeId", "met-1",
                "goalNotMetNodeId", "not-met-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("goal_met", "met-1");
        assertThat(result.output()).containsEntry("goalMet", true);
    }

    @Test
    void sync_goal_not_met_routes_to_goalNotMet_branch() {
        when(eventLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "goal-1",
                "eventCode", "ORDER_PAID",
                "goalMetNodeId", "met-1",
                "goalNotMetNodeId", "not-met-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.SUCCESS);
        assertThat(result.routes()).containsEntry("goal_not_met", "not-met-1");
        assertThat(result.output()).containsEntry("goalMet", false);
    }

    @Test
    void async_goal_wait_creates_subscription_and_returns_pending() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "goal-1",
                "mode", "ASYNC",
                "eventCode", "ORDER_PAID",
                "maxWait", Map.of("value", 30, "unit", "MINUTES"),
                "goalMetNodeId", "met-1",
                "goalNotMetNodeId", "not-met-1",
                "timeoutNodeId", "timeout-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.PENDING);
        assertThat(result.pending()).isTrue();
        assertThat(result.resumeAtEpochMs()).isEqualTo(
                LocalDateTime.of(2026, 5, 23, 18, 45, 30)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toInstant()
                        .toEpochMilli()
        );

        verify(waitService).createGoalWait(
                eq("exec-1"),
                eq(10L),
                eq(20L),
                eq("user-1"),
                eq("goal-1"),
                eq("ORDER_PAID"),
                eq("{\"sourceNodeId\":\"goal-1\",\"goalMetNodeId\":\"met-1\",\"goalNotMetNodeId\":\"not-met-1\",\"timeoutNodeId\":\"timeout-1\"}"),
                eq(LocalDateTime.of(2026, 5, 23, 18, 45, 30))
        );
    }

    @Test
    void timeout_resume_routes_to_timeout_branch() {
        NodeResult result = handler.executeAsync(Map.of(
                "__nodeId", "goal-1",
                "__goalResumeStatus", "TIMEOUT",
                "timeoutNodeId", "timeout-1"
        ), ctx()).block();

        assertThat(result.outcome()).isEqualTo(NodeOutcome.TIMEOUT);
        assertThat(result.routes()).containsEntry("timeout", "timeout-1");
        assertThat(result.reasonCode()).isEqualTo("GOAL_TIMEOUT");
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
