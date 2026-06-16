package org.chovy.canvas.execution.adapter.plugin.official.risk;

import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.stringConfig;
import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.userOrAnonymous;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerType;
import org.springframework.stereotype.Component;

@Component
@NodeHandlerType(OfficialRiskNodeHandler.NODE_TYPE)
public class OfficialRiskNodeHandler implements NodeHandler {

    static final String PLUGIN_ID = "canvas-plugin-risk";
    static final String NODE_TYPE = "risk.check";

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String policy = stringConfig(context, "policy");
        if (policy.isBlank()) {
            return NodeExecutionResult.failure("risk policy is required");
        }

        boolean allowed = isAllowed(policy);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginId", PLUGIN_ID);
        output.put("nodeType", NODE_TYPE);
        output.put("policy", policy);
        output.put("subject", subject(context));
        output.put("payload", context.payload());
        output.put("context", context.contextData());
        output.put("check", "stub");
        output.put("allowed", allowed);
        output.put("decision", allowed ? "allowed" : "blocked");
        output.put("status", allowed ? "MATCHED" : "BLOCKED");
        return NodeExecutionResult.success(output);
    }

    private static boolean isAllowed(String policy) {
        String normalized = policy.toUpperCase(Locale.ROOT);
        return !normalized.contains("BLOCK") && !normalized.contains("COMPLIANCE");
    }

    private static String subject(NodeExecutionContext context) {
        return userOrAnonymous(context);
    }
}
