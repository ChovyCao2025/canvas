package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@NodeHandlerType("DIRECT_RETURN")
public class DirectReturnNodeHandler implements NodeHandler {

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        Object rawMappings = context.node().config().getOrDefault("data", context.node().config().get("bizData"));
        List<Map<String, Object>> mappings = NodeHandlerSupport.listOfMaps(rawMappings);
        Map<String, Object> output = new LinkedHashMap<>();
        for (Map<String, Object> mapping : mappings) {
            String name = NodeHandlerSupport.string(mapping.getOrDefault("name", mapping.get("key")), null);
            if (name == null) {
                continue;
            }
            Object resolved = value(mapping, context);
            if (resolved != null) {
                output.put(name, resolved);
            }
        }
        return NodeExecutionResult.success(output);
    }

    private Object value(Map<String, Object> mapping, NodeExecutionContext context) {
        Object rawValue = mapping.get("value");
        String valueText = NodeHandlerSupport.string(rawValue, null);
        String valueType = NodeHandlerSupport.upper(mapping.get("valueType"), "LITERAL");
        if ("CONTEXT".equals(valueType) || (valueText != null && valueText.startsWith("${") && valueText.endsWith("}"))) {
            return NodeHandlerSupport.resolve(context, NodeHandlerSupport.normalizeTemplate(valueText));
        }
        if ("PAYLOAD".equals(valueType)) {
            return NodeHandlerSupport.nestedValue(context.payload(), valueText);
        }
        return rawValue;
    }
}
