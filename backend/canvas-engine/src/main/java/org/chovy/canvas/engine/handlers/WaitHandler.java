package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.wait.WaitSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.WAIT)
public class WaitHandler implements NodeHandler {

    private static final String DEFAULT_WAIT_TYPE = "DURATION";

    private final WaitSubscriptionService waitSubscriptionService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WaitHandler(WaitSubscriptionService waitSubscriptionService, ObjectMapper objectMapper) {
        this(waitSubscriptionService, objectMapper, Clock.systemDefaultZone());
    }

    WaitHandler(WaitSubscriptionService waitSubscriptionService, ObjectMapper objectMapper, Clock clock) {
        this.waitSubscriptionService = waitSubscriptionService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String resumeStatus = string(config, "__waitResumeStatus", "");
        if ("TIMEOUT".equalsIgnoreCase(resumeStatus) || "EXPIRED".equalsIgnoreCase(resumeStatus)) {
            return Mono.just(NodeResult.timeout(
                    string(config, "timeoutNodeId", null),
                    "WAIT_TIMEOUT",
                    "等待节点超时"
            ));
        }
        if ("COMPLETED".equalsIgnoreCase(resumeStatus)) {
            return Mono.just(success(config));
        }

        String waitType = waitType(config);
        return switch (waitType) {
            case "UNTIL_EVENT" -> Mono.just(waitUntilEvent(config, ctx));
            case "UNTIL_DATE" -> Mono.just(waitUntilDate(config, ctx));
            case "RELATIVE_TIME" -> Mono.just(waitUntilRelativeTime(config, ctx));
            case "TIME_WINDOW" -> Mono.just(waitForTimeWindow(config, ctx));
            case "DURATION" -> Mono.just(waitForDuration(config, ctx));
            default -> Mono.just(NodeResult.fail("未知 WAIT 类型: " + waitType));
        };
    }

    private NodeResult waitForDuration(Map<String, Object> config, ExecutionContext ctx) {
        Duration duration = durationFrom(config.get("duration"), config);
        if (duration.isZero() || duration.isNegative()) {
            return NodeResult.fail("WAIT DURATION 的等待时长必须大于 0");
        }
        return pendingAt(config, ctx, "DURATION", now().plus(duration));
    }

    private NodeResult waitUntilDate(Map<String, Object> config, ExecutionContext ctx) {
        LocalDateTime until;
        try {
            until = parseDateTime(string(config, "untilDate", null));
        } catch (DateTimeParseException e) {
            return NodeResult.fail("WAIT UNTIL_DATE 的 untilDate 格式不正确");
        }
        if (until == null || !until.isAfter(now())) {
            return success(config);
        }
        return pendingAt(config, ctx, "UNTIL_DATE", until);
    }

    private NodeResult waitUntilRelativeTime(Map<String, Object> config, ExecutionContext ctx) {
        LocalTime targetTime;
        try {
            targetTime = parseTime(string(config, "time", null), LocalTime.of(9, 0));
        } catch (DateTimeParseException e) {
            return NodeResult.fail("WAIT RELATIVE_TIME 的 time 格式不正确");
        }
        LocalDateTime candidate = LocalDateTime.of(LocalDate.now(clock), targetTime);
        if (!candidate.isAfter(now())) {
            candidate = candidate.plusDays(1);
        }
        return pendingAt(config, ctx, "RELATIVE_TIME", candidate);
    }

    @SuppressWarnings("unchecked")
    private NodeResult waitForTimeWindow(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> window = config.get("timeWindow") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        LocalTime start;
        LocalTime end;
        try {
            start = parseTime(string(window, "start", string(config, "windowStart", null)), LocalTime.of(9, 0));
            end = parseTime(string(window, "end", string(config, "windowEnd", null)), LocalTime.of(20, 0));
        } catch (DateTimeParseException e) {
            return NodeResult.fail("WAIT TIME_WINDOW 的时间窗口格式不正确");
        }
        LocalTime current = LocalTime.now(clock);

        if (insideWindow(current, start, end)) {
            return success(config);
        }

        LocalDate date = LocalDate.now(clock);
        LocalDateTime nextStart = LocalDateTime.of(date, start);
        if (!nextStart.isAfter(now())) {
            nextStart = nextStart.plusDays(1);
        }
        return pendingAt(config, ctx, "TIME_WINDOW", nextStart);
    }

    private NodeResult waitUntilEvent(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", null);
        if (nodeId == null || nodeId.isBlank()) {
            return NodeResult.fail("WAIT 节点未注入 __nodeId");
        }
        String eventCode = string(config, "eventCode", null);
        if (eventCode == null || eventCode.isBlank()) {
            return NodeResult.fail("WAIT UNTIL_EVENT 必须配置 eventCode");
        }

        if (!config.containsKey("maxWait")) {
            return NodeResult.fail("WAIT UNTIL_EVENT 必须配置 maxWait");
        }
        Duration maxWait = durationFrom(config.get("maxWait"), Map.of());
        if (maxWait.isZero() || maxWait.isNegative()) {
            return NodeResult.fail("WAIT UNTIL_EVENT 的 maxWait 必须大于 0");
        }

        LocalDateTime expiresAt = now().plus(maxWait);
        String eventFiltersJson = toJsonOrNull(config.get("eventFilters"));
        String resumePayload = toJson(resumePayload(nodeId, config));

        waitSubscriptionService.createEventWait(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                eventCode,
                eventFiltersJson,
                resumePayload,
                expiresAt
        );
        return pending(expiresAt);
    }

    private NodeResult success(Map<String, Object> config) {
        return NodeResult.ok(string(config, "nextNodeId", null), Map.of("waitStatus", "COMPLETED"));
    }

    private NodeResult pendingAt(
            Map<String, Object> config,
            ExecutionContext ctx,
            String waitType,
            LocalDateTime resumeAt
    ) {
        String nodeId = string(config, "__nodeId", null);
        if (nodeId == null || nodeId.isBlank()) {
            return NodeResult.fail("WAIT 节点未注入 __nodeId");
        }
        waitSubscriptionService.createTimeWait(
                ctx.getExecutionId(),
                ctx.getCanvasId(),
                ctx.getVersionId(),
                ctx.getUserId(),
                nodeId,
                waitType,
                toJson(resumePayload(nodeId, config)),
                resumeAt
        );
        return pending(resumeAt);
    }

    private NodeResult pending(LocalDateTime resumeAt) {
        return NodeResult.pending(
                resumeAt.atZone(zone()).toInstant().toEpochMilli(),
                "WAIT_PENDING",
                "等待节点挂起"
        );
    }

    private Map<String, Object> resumePayload(String nodeId, Map<String, Object> config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourceNodeId", nodeId);
        payload.put("successNodeId", string(config, "nextNodeId", null));
        payload.put("timeoutNodeId", string(config, "timeoutNodeId", null));
        return payload;
    }

    private String waitType(Map<String, Object> config) {
        return string(config, "waitType", string(config, "wait_type", DEFAULT_WAIT_TYPE)).toUpperCase();
    }

    @SuppressWarnings("unchecked")
    private Duration durationFrom(Object value, Map<String, Object> fallback) {
        if (value instanceof Number number) {
            return duration(number.longValue(), string(fallback, "unit", "SECONDS"));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> duration = (Map<String, Object>) map;
            long amount = duration.get("value") instanceof Number number ? number.longValue() : 0L;
            return duration(amount, string(duration, "unit", "SECONDS"));
        }
        long amount = fallback.get("durationValue") instanceof Number number ? number.longValue() : 0L;
        return duration(amount, string(fallback, "durationUnit", string(fallback, "unit", "SECONDS")));
    }

    private Duration duration(long amount, String unit) {
        return switch (unit == null ? "SECONDS" : unit.toUpperCase()) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
            case "MINUTE", "MINUTES" -> Duration.ofMinutes(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            case "DAY", "DAYS" -> Duration.ofDays(amount);
            default -> Duration.ofSeconds(amount);
        };
    }

    private boolean insideWindow(LocalTime current, LocalTime start, LocalTime end) {
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !current.isBefore(start) && current.isBefore(end);
        }
        return !current.isBefore(start) || current.isBefore(end);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDateTime.parse(value);
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        if (value == null || value.isBlank()) return fallback;
        return LocalTime.parse(value);
    }

    private String toJsonOrNull(Object value) {
        return value == null ? null : toJson(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("WAIT 配置序列化失败", e);
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
