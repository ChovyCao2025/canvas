package org.chovy.canvas.execution.adapter.plugin.official.webhook;

import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.stringConfig;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerType;
import org.springframework.stereotype.Component;

@Component
@NodeHandlerType(OfficialWebhookNodeHandler.NODE_TYPE)
public class OfficialWebhookNodeHandler implements NodeHandler {

    static final String PLUGIN_ID = "canvas-plugin-webhook";
    static final String NODE_TYPE = "webhook";

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String event = stringConfig(context, "event");
        if (event.isBlank()) {
            return NodeExecutionResult.failure("webhook event is required");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginId", PLUGIN_ID);
        output.put("nodeType", NODE_TYPE);
        output.put("event", event);
        output.put("source", source(context));
        output.put("payload", context.payload());
        output.put("context", context.contextData());
        output.put("received", true);
        return NodeExecutionResult.success(output);
    }

    private static String source(NodeExecutionContext context) {
        String configured = stringConfig(context, "source");
        return configured.isBlank() ? "webhook" : configured;
    }
}
