package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.domain.canvas.UserInputService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeOutcome;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.USER_INPUT)
public class UserInputHandler implements NodeHandler {

    private static final String FORM_SCHEMA = "formSchema";
    private static final String INPUT_STATUS = "inputStatus";
    private static final String INPUT_RESPONSE_ID = "inputResponseId";
    private static final String INPUT_RESPONSE = "inputResponse";

    private final UserInputService service;

    public UserInputHandler(UserInputService service) {
        this.service = service;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String resumeStatus = resumeStatus(config, ctx);
        if (UserInputService.STATUS_COMPLETED.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(completed(config, ctx));
        }
        if (UserInputService.STATUS_EXPIRED.equalsIgnoreCase(resumeStatus)
                || MapFieldKeys.TIMEOUT.equalsIgnoreCase(resumeStatus)) {
            return Mono.just(timeout(config, ctx));
        }

        Object schema = config.get(FORM_SCHEMA);
        if (schema == null || (schema instanceof String text && text.isBlank())) {
            return Mono.just(NodeResult.fail("USER_INPUT: formSchema is required"));
        }
        String nodeId = string(config.get(MapFieldKeys.NODE_ID_INTERNAL), null);
        String completedNodeId = string(config.get("completedNodeId"), string(config.get(MapFieldKeys.NEXT_NODE_ID), null));
        String timeoutNodeId = string(config.get(MapFieldKeys.TIMEOUT_NODE_ID), null);
        LocalDateTime expiresAt = expiresAt(config);
        UserInputService.PendingInput pending = service.createPending(ctx, nodeId, schema,
                completedNodeId, timeoutNodeId, expiresAt);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(INPUT_STATUS, pending.status());
        output.put(INPUT_RESPONSE_ID, pending.responseId());
        output.put(MapFieldKeys.TIMEOUT_NODE_ID, pending.timeoutNodeId());
        Long resumeAt = pending.expiresAt() == null
                ? null
                : pending.expiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return Mono.just(new NodeResult(null, null, null, null, null, output, true, null,
                true, NodeOutcome.PENDING, Map.of(), "USER_INPUT_PENDING",
                "waiting for user input", resumeAt));
    }

    private NodeResult completed(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = new LinkedHashMap<>();
        Map<String, Object> payload = ctx.getTriggerPayload();
        output.put(INPUT_STATUS, UserInputService.STATUS_COMPLETED);
        output.put(INPUT_RESPONSE_ID, payload.get(INPUT_RESPONSE_ID));
        output.put(INPUT_RESPONSE, payload.getOrDefault(INPUT_RESPONSE, Map.of()));
        String next = string(payload.get("completedNodeId"),
                string(config.get("completedNodeId"), string(config.get(MapFieldKeys.NEXT_NODE_ID), null)));
        return NodeResult.ok(next, output);
    }

    private NodeResult timeout(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(INPUT_STATUS, UserInputService.STATUS_EXPIRED);
        output.put(INPUT_RESPONSE_ID, ctx.getTriggerPayload().get(INPUT_RESPONSE_ID));
        String timeoutNodeId = string(ctx.getTriggerPayload().get(MapFieldKeys.TIMEOUT_NODE_ID),
                string(config.get(MapFieldKeys.TIMEOUT_NODE_ID), null));
        return new NodeResult(null, null, null, null, null, output, true, null, false,
                NodeOutcome.TIMEOUT,
                timeoutNodeId == null ? Map.of() : Map.of("timeout", timeoutNodeId),
                "USER_INPUT_TIMEOUT",
                "user input timed out",
                null);
    }

    private String resumeStatus(Map<String, Object> config, ExecutionContext ctx) {
        String value = string(config.get(MapFieldKeys.WAIT_RESUME_STATUS), null);
        if (value != null) {
            return value;
        }
        return string(ctx.getTriggerPayload().get(MapFieldKeys.WAIT_RESUME_STATUS), null);
    }

    private LocalDateTime expiresAt(Map<String, Object> config) {
        Object raw = config.get(MapFieldKeys.MAX_WAIT);
        Duration duration = duration(raw);
        return duration == null ? null : LocalDateTime.now().plus(duration);
    }

    @SuppressWarnings("unchecked")
    private Duration duration(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        Object value = ((Map<Object, Object>) map).getOrDefault("value", map.get("durationValue"));
        Object unit = ((Map<Object, Object>) map).getOrDefault("unit", map.get("durationUnit"));
        Number number;
        if (value instanceof Number parsed) {
            number = parsed;
        } else {
            if (value == null || String.valueOf(value).isBlank()) {
                return null;
            }
            number = Double.valueOf(String.valueOf(value));
        }
        long amount = number.longValue();
        String normalizedUnit = unit == null ? "MINUTES" : String.valueOf(unit).toUpperCase();
        return switch (normalizedUnit) {
            case "SECOND", "SECONDS" -> Duration.ofSeconds(amount);
            case "HOUR", "HOURS" -> Duration.ofHours(amount);
            case "DAY", "DAYS" -> Duration.ofDays(amount);
            default -> Duration.ofMinutes(amount);
        };
    }

    private String string(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
