package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 定义 DirectCallNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType("DIRECT_CALL")
public class DirectCallNodeHandler implements NodeHandler {

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        for (Map<String, Object> inputParam : NodeHandlerSupport.listOfMaps(context.node().config().get("inputParams"))) {
            String name = NodeHandlerSupport.string(inputParam.get("name"), null);
            if (name != null && NodeHandlerSupport.bool(inputParam.get("required"))
                    && NodeHandlerSupport.resolve(context, name) == null) {
                return NodeExecutionResult.failure("DIRECT_CALL required input missing: " + name);
            }
        }

        Map<String, String> routes = branchRoutes(context.node().config());
        String nextNodeId = NodeHandlerSupport.string(context.node().config().get("nextNodeId"), null);
        if (routes.isEmpty() && nextNodeId != null) {
            routes = Map.of("success", nextNodeId);
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("directCallAccepted", true);
        output.put("inputCount", context.payload().size());
        return routes.isEmpty()
                ? NodeExecutionResult.success(output)
                : NodeExecutionResult.routed(output, routes);
    }

    /**
     * 执行 branchRoutes 对应的业务处理。
     * @param config config 参数
     * @return 处理后的结果
     */
    private Map<String, String> branchRoutes(Map<String, Object> config) {
        List<Map<String, Object>> branches = NodeHandlerSupport.listOfMaps(config.get("branches"));
        if (branches.isEmpty()) {
            return Map.of();
        }
        Map<String, String> routes = new LinkedHashMap<>();
        for (int i = 0; i < branches.size(); i++) {
            String target = NodeHandlerSupport.string(branches.get(i).get("nextNodeId"), null);
            if (target != null) {
                routes.put("branch-" + i, target);
            }
        }
        return Map.copyOf(routes);
    }
}
