package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;

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
