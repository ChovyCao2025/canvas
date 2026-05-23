package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@NodeHandlerType(NodeType.AUDIENCE_TRIGGER)
public class AudienceTriggerHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String expectedSegment = string(config, "segmentId", null);
        String triggerOn = string(config, "triggerOn", "ENTER");
        Object actualSegment = ctx.getContextValue("segmentId");
        Object actualAction = ctx.getContextValue("audienceAction");
        if (expectedSegment != null && actualSegment != null && !expectedSegment.equals(actualSegment.toString())) {
            return Mono.just(NodeResult.terminal(Map.of("audienceMatched", false)));
        }
        if (actualAction != null && !triggerOn.equalsIgnoreCase(actualAction.toString())) {
            return Mono.just(NodeResult.terminal(Map.of("audienceMatched", false)));
        }
        return Mono.just(NodeResult.ok(string(config, "nextNodeId", null), Map.of("audienceMatched", true)));
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
