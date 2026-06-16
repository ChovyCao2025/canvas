package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 定义 UserInputNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType("USER_INPUT")
public class UserInputNodeHandler implements NodeHandler {

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String resumeStatus = resumeStatus(context);
        if ("COMPLETED".equals(resumeStatus)) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("inputStatus", "COMPLETED");
            putIfPresent(output, "inputResponseId", context.payload().get("inputResponseId"));
            putIfPresent(output, "inputResponse", context.payload().get("inputResponse"));
            String completedNodeId = completedNodeId(context);
            return NodeExecutionResult.routed(
                    output,
                    completedNodeId == null ? Map.of() : Map.of("completed", completedNodeId));
        }
        if ("TIMEOUT".equals(resumeStatus) || "EXPIRED".equals(resumeStatus)) {
            String timeoutNodeId = NodeHandlerSupport.string(context.node().config().get("timeoutNodeId"), null);
            return NodeExecutionResult.routed(
                    Map.of("inputStatus", "EXPIRED"),
                    timeoutNodeId == null ? Map.of() : Map.of("timeout", timeoutNodeId));
        }

        Object formSchema = context.node().config().get("formSchema");
        if (formSchema == null || String.valueOf(formSchema).isBlank()) {
            return NodeExecutionResult.failure("USER_INPUT: formSchema is required");
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("inputStatus", "PENDING");
        output.put("sourceNodeId", context.node().nodeId());
        output.put("formSchema", formSchema);
        putIfPresent(output, "completedNodeId", completedNodeId(context));
        putIfPresent(output, "timeoutNodeId", context.node().config().get("timeoutNodeId"));
        putIfPresent(output, "maxWait", context.node().config().get("maxWait"));
        return NodeExecutionResult.pending(output);
    }

    /**
     * 执行 resumeStatus 对应的业务处理。
     * @param context context 参数
     */
    private String resumeStatus(NodeExecutionContext context) {
        Object configured = context.node().config().getOrDefault("waitResumeStatus", context.node().config().get("resumeStatus"));
        Object payload = context.payload().getOrDefault("waitResumeStatus", context.payload().get("resumeStatus"));
        Object contextValue = context.contextData().getOrDefault("waitResumeStatus", context.contextData().get("resumeStatus"));
        return NodeHandlerSupport.upper(configured != null ? configured : payload != null ? payload : contextValue, "");
    }

    /**
     * 执行 completedNodeId 对应的业务处理。
     * @param context context 参数
     */
    private String completedNodeId(NodeExecutionContext context) {
        String fromPayload = NodeHandlerSupport.string(context.payload().get("completedNodeId"), null);
        if (fromPayload != null) {
            return fromPayload;
        }
        String configured = NodeHandlerSupport.string(context.node().config().get("completedNodeId"), null);
        return configured == null ? NodeHandlerSupport.string(context.node().config().get("nextNodeId"), null) : configured;
    }

    /**
     * 执行 putIfPresent 对应的业务处理。
     * @param output output 参数
     * @param key key 参数
     * @param value value 参数
     */
    private void putIfPresent(Map<String, Object> output, String key, Object value) {
        if (value != null) {
            output.put(key, value);
        }
    }
}
