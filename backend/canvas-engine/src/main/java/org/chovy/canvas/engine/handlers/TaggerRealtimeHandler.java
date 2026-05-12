package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/** Tagger 实时标签触发节点：标签事件已到达，直接通过 */
@Component
@NodeHandlerType("TAGGER_REALTIME")
public class TaggerRealtimeHandler implements NodeHandler {

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String nextNodeId = (String) config.get("nextNodeId");
        return Mono.just(NodeResult.ok(nextNodeId, new HashMap<>(ctx.getTriggerPayload())));
    }
}
