package org.chovy.canvas.execution.adapter.plugin.official.approval;

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
@NodeHandlerType(OfficialApprovalNodeHandler.NODE_TYPE)
public class OfficialApprovalNodeHandler implements NodeHandler {

    static final String PLUGIN_ID = "canvas-plugin-approval";
    static final String NODE_TYPE = "approval.request";

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String approvalCode = stringConfig(context, "approvalCode");
        if (approvalCode.isBlank()) {
            return NodeExecutionResult.failure("approval code is required");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginId", PLUGIN_ID);
        output.put("nodeType", NODE_TYPE);
        output.put("approvalCode", approvalCode);
        output.put("requester", requester(context));
        output.put("payload", context.payload());
        output.put("context", context.contextData());
        output.put("request", "stub");
        output.put("status", "APPROVED");
        return NodeExecutionResult.success(output);
    }

    private static String requester(NodeExecutionContext context) {
        return userOrAnonymous(context);
    }
}
