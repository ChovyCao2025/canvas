package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;

import java.util.List;
import java.util.Map;

/**
 * 业务直调触发节点：校验必填入参，将 inputParams 写入上下文。
 */
@NodeHandlerType("DIRECT_CALL")
public class DirectCallHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> inputParams = (List<Map<String, Object>>) config.get("inputParams");
        String nextNodeId = (String) config.get("nextNodeId");

        if (inputParams != null) {
            for (Map<String, Object> param : inputParams) {
                Boolean required = (Boolean) param.get("required");
                String name      = (String) param.get("name");
                if (Boolean.TRUE.equals(required) && ctx.getContextValue(name) == null) {
                    return NodeResult.fail("业务直调必填参数缺失: " + name);
                }
            }
        }

        return NodeResult.ok(nextNodeId, Map.of());
    }
}
