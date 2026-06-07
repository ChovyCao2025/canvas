package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.trigger.CanvasExecutionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@NodeHandlerType(NodeType.TRANSFER_JOURNEY)
public class TransferJourneyHandler implements NodeHandler {

    private final CanvasExecutionService executionService;

    public TransferJourneyHandler(@Lazy CanvasExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Long targetJourneyId = longValue(config == null ? null : config.get("targetJourneyId"));
        if (targetJourneyId == null) {
            return Mono.just(NodeResult.fail("TRANSFER_JOURNEY: targetJourneyId is required"));
        }
        String userId = ctx == null ? null : ctx.getUserId();
        String sourceExecutionId = ctx == null ? null : ctx.getExecutionId();
        Map<String, Object> payload = Boolean.TRUE.equals(config == null ? null : config.get("carryContext"))
                ? carriedPayload(ctx)
                : new LinkedHashMap<>();
        if (sourceExecutionId != null) {
            payload.put(MapFieldKeys.SOURCE_EXECUTION_ID, sourceExecutionId);
        }
        String msgId = sourceExecutionId + ":transfer:" + targetJourneyId;
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("targetJourneyId", targetJourneyId);
        if (sourceExecutionId != null) {
            output.put(MapFieldKeys.SOURCE_EXECUTION_ID, sourceExecutionId);
        }
        return executionService.trigger(targetJourneyId, userId, TriggerType.TRANSFER_JOURNEY,
                        NodeType.DIRECT_CALL, null, payload, msgId, false)
                .thenReturn(NodeResult.ok(null, output));
    }

    private Map<String, Object> carriedPayload(ExecutionContext ctx) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (ctx == null) {
            return payload;
        }
        if (ctx.getTriggerPayload() != null) {
            payload.putAll(ctx.getTriggerPayload());
        }
        ctx.getNodeOutputs().forEach((nodeId, output) -> output.forEach((key, value) -> {
            if (key != null && value != null) {
                payload.put(key, value);
                payload.put(nodeId + "." + key, value);
            }
        }));
        return payload;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.parseLong(value.toString());
    }
}
