package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;

import java.util.HashMap;
import java.util.Map;

/** Tagger 实时标签触发节点：标签事件已到达，直接通过 */
@NodeHandlerType("TAGGER_REALTIME")
public class TaggerRealtimeHandler implements NodeHandler {

    @Override
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        String nextNodeId = (String) config.get("nextNodeId");
        return NodeResult.ok(nextNodeId, new HashMap<>(ctx.getTriggerPayload()));
    }
}
