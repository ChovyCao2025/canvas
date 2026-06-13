package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@NodeHandlerType("DIRECT_CALL")
public class DirectCallNodeHandler implements NodeHandler {

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
