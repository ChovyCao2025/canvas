package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.meta.EventLog;
import org.chovy.canvas.domain.meta.EventLogMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.GOAL_CHECK)
public class GoalCheckHandler implements NodeHandler {

    private final EventLogMapper eventLogMapper;
    private final WaitSubscriptionService waitSubscriptionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GoalCheckHandler(
            EventLogMapper eventLogMapper,
            WaitSubscriptionService waitSubscriptionService,
            ObjectMapper objectMapper
    ) {
        this(eventLogMapper, waitSubscriptionService, objectMapper, Clock.systemDefaultZone());
    }

    GoalCheckHandler(
            EventLogMapper eventLogMapper,
            WaitSubscriptionService waitSubscriptionService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.eventLogMapper = eventLogMapper;
        this.waitSubscriptionService = waitSubscriptionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String resumeStatus = string(config, "__goalResumeStatus", "");
        if ("TIMEOUT".equalsIgnoreCase(resumeStatus) || "EXPIRED".equalsIgnoreCase(resumeStatus)) {
            return Mono.just(NodeResult.timeout(
                    string(config, "timeoutNodeId", null),
                    "GOAL_TIMEOUT",
                    "目标检测等待超时"
            ));
        }
        if ("COMPLETED".equalsIgnoreCase(resumeStatus)) {
            return Mono.just(goalMet(config));
        }

        String mode = string(config, "mode", "SYNC").toUpperCase();
        if ("ASYNC".equals(mode)) {
            return Mono.just(asyncWait(config, ctx));
        }
        return Mono.just(syncCheck(config, ctx));
    }

    private NodeResult syncCheck(Map<String, Object> config, ExecutionContext ctx) {
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return NodeResult.fail("GOAL_CHECK 必须配置 eventCode");
        }

        Long count = eventLogMapper.selectCount(new LambdaQueryWrapper<EventLog>()
                .eq(EventLog::getEventCode, eventCode)
                .eq(EventLog::getUserId, ctx.getUserId()));
        boolean met = count != null && count > 0;
        return met ? goalMet(config) : goalNotMet(config);
    }

    private NodeResult asyncWait(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", null);
        if (nodeId == null || nodeId.isBlank()) {
            return NodeResult.fail("GOAL_CHECK 节点未注入 __nodeId");
        }
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return NodeResult.fail("GOAL_CHECK 必须配置 eventCode");
        }
        if (!config.containsKey("maxWait")) {
            return NodeResult.fail("GOAL_CHECK 异步监听必须配置 maxWait");
        }
        Duration maxWait = durationFrom(config.get("maxWait"));
        if (maxWait.isZero() || maxWait.isNegative()) {
            return NodeResult.fail("GOAL_CHECK 的 maxWait 必须大于 0");
        }

        LocalDateTime expiresAt = now().plus(maxWait);
        waitSubscriptionService.createGoalWait(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                eventCode,
                toJson(resumePayload(nodeId, config)),
                expiresAt
        );
        return NodeResult.pending(
                expiresAt.atZone(zone()).toInstant().toEpochMilli(),
                "GOAL_PENDING",
                "目标检测等待事件"
        );
    }

    private NodeResult goalMet(Map<String, Object> config) {
        return NodeResult.routed("goal_met", string(config, "goalMetNodeId", null), Map.of("goalMet", true));
    }

    private NodeResult goalNotMet(Map<String, Object> config) {
        return NodeResult.routed("goal_not_met", string(config, "goalNotMetNodeId", null), Map.of("goalMet", false));
    }

    private Map<String, Object> resumePayload(String nodeId, Map<String, Object> config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceNodeId", nodeId);
        payload.put("goalMetNodeId", string(config, "goalMetNodeId", null));
        payload.put("goalNotMetNodeId", string(config, "goalNotMetNodeId", null));
        payload.put("timeoutNodeId", string(config, "timeoutNodeId", null));
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Duration durationFrom(Object value) {
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> duration = (Map<String, Object>) map;
            long amount = duration.get("value") instanceof Number number ? number.longValue() : 0L;
            String unit = string(duration, "unit", "SECONDS").toUpperCase();
            return switch (unit) {
                case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
                case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
                case "HOUR", "HOURS" -> Duration.ofHours(amount);
                case "DAY", "DAYS" -> Duration.ofDays(amount);
                default -> Duration.ofSeconds(amount);
            };
        }
        return Duration.ZERO;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("GOAL_CHECK 配置序列化失败", e);
        }
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private ZoneId zone() {
        return clock.getZone();
    }
}
