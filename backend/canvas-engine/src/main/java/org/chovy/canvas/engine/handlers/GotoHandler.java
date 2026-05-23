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
@NodeHandlerType(NodeType.GOTO)
public class GotoHandler implements NodeHandler {
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nodeId = string(config, "__nodeId", "goto");
        int maxJumps = number(config.get("maxJumps"), 1);
        if (maxJumps <= 0) {
            return Mono.just(NodeResult.fail("GOTO 必须配置 maxJumps > 0"));
        }
        int nextCount = ctx.getJumpCounts().merge(nodeId, 1, Integer::sum);
        if (nextCount > maxJumps) {
            return Mono.just(NodeResult.routed("max_exceeded", string(config, "maxExceededNodeId", null),
                    Map.of("jumpCount", nextCount, "jumpExceeded", true)));
        }
        return Mono.just(NodeResult.routed("goto", string(config, "targetNodeId", null),
                Map.of("jumpCount", nextCount)));
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }
}
