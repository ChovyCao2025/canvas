package com.photon.canvas.engine.handlers;

import com.photon.canvas.engine.context.ExecutionContext;
import com.photon.canvas.engine.handler.NodeHandler;
import com.photon.canvas.engine.handler.NodeHandlerType;
import com.photon.canvas.engine.handler.NodeResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直调返回节点：从上下文 / 自定义值构建同步返回给业务方的数据结构。
 */
@NodeHandlerType("DIRECT_RETURN")
public class DirectReturnHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) config.get("data");
        Map<String, Object> result = new HashMap<>();

        if (data != null) {
            for (Map<String, Object> item : data) {
                String name      = (String) item.get("name");
                String valueType = (String) item.get("valueType");
                String value     = (String) item.get("value");
                if ("CONTEXT".equals(valueType)) {
                    result.put(name, ctx.getContextValue(value));
                } else {
                    result.put(name, value);
                }
            }
        }

        // 终止节点，output 即为返回给调用方的数据
        return NodeResult.terminal(result);
    }

    @Override
    public boolean isBenefitNode() { return false; }
}
