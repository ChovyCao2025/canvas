package org.chovy.canvas.execution.adapter.plugin.official.message;

import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.stringConfig;
import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.userOrAnonymous;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerType;
import org.springframework.stereotype.Component;

@Component
@NodeHandlerType(OfficialMessageNodeHandler.NODE_TYPE)
public class OfficialMessageNodeHandler implements NodeHandler {

    static final String PLUGIN_ID = "canvas-plugin-message";
    static final String NODE_TYPE = "message.send";

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String template = stringConfig(context, "template");
        if (template.isBlank()) {
            return NodeExecutionResult.failure("message template is required");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginId", PLUGIN_ID);
        output.put("nodeType", NODE_TYPE);
        output.put("channel", channel(context));
        output.put("template", template);
        output.put("recipient", recipient(context));
        output.put("payload", context.payload());
        output.put("context", context.contextData());
        output.put("delivery", "stub");
        output.put("status", "SENT");
        return NodeExecutionResult.success(output);
    }

    private static String channel(NodeExecutionContext context) {
        String configured = stringConfig(context, "channel");
        return configured.isBlank() ? "sms" : configured;
    }

    private static String recipient(NodeExecutionContext context) {
        String configured = stringConfig(context, "recipient");
        if (configured.isBlank()) {
            return userOrAnonymous(context);
        }
        Object resolved = resolve(context, configured);
        if (resolved != null) {
            String text = String.valueOf(resolved).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        if (isReference(configured)) {
            return userOrAnonymous(context);
        }
        return configured;
    }

    private static Object resolve(NodeExecutionContext context, String key) {
        String normalized = normalizeTemplate(key);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("payload.")) {
            return nestedValue(context.payload(), normalized.substring("payload.".length()));
        }
        if (normalized.startsWith("context.")) {
            return nestedValue(context.contextData(), normalized.substring("context.".length()));
        }
        if (context.contextData().containsKey(normalized)) {
            return context.contextData().get(normalized);
        }
        if (context.payload().containsKey(normalized)) {
            return context.payload().get(normalized);
        }
        Object contextValue = nestedValue(context.contextData(), normalized);
        return contextValue == null ? nestedValue(context.payload(), normalized) : contextValue;
    }

    private static String normalizeTemplate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.startsWith("${") && text.endsWith("}") && text.length() > 3) {
            return text.substring(2, text.length() - 1).trim();
        }
        return text;
    }

    private static boolean isReference(String value) {
        String text = value == null ? "" : value.trim();
        return (text.startsWith("${") && text.endsWith("}") && text.length() > 3)
                || text.startsWith("payload.")
                || text.startsWith("context.");
    }

    private static Object nestedValue(Map<String, Object> source, String path) {
        if (source == null || path == null || path.isBlank()) {
            return null;
        }
        if (source.containsKey(path)) {
            return source.get(path);
        }
        Object current = source;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }
}
