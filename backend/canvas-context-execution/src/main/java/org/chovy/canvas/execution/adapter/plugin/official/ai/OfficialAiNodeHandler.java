package org.chovy.canvas.execution.adapter.plugin.official.ai;

import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.stringConfig;
import static org.chovy.canvas.execution.adapter.plugin.official.OfficialPluginSupport.userOrAnonymous;

import java.util.LinkedHashMap;
import java.util.Map;

import org.chovy.canvas.execution.domain.NodeExecutionContext;
import org.chovy.canvas.execution.domain.NodeExecutionResult;
import org.chovy.canvas.execution.domain.NodeHandler;
import org.chovy.canvas.execution.domain.NodeHandlerType;
import org.springframework.stereotype.Component;

/**
 * 定义 OfficialAiNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType(OfficialAiNodeHandler.NODE_TYPE)
public class OfficialAiNodeHandler implements NodeHandler {

    /**
     * 保存 PLUGIN_ID 对应的状态或配置。
     */
    static final String PLUGIN_ID = "canvas-plugin-ai";

    /**
     * 保存 NODE_TYPE 对应的状态或配置。
     */
    static final String NODE_TYPE = "ai.generate-copy";

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String promptKey = stringConfig(context, "promptKey");
        if (promptKey.isBlank()) {
            return NodeExecutionResult.failure("AI prompt key is required");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginId", PLUGIN_ID);
        output.put("nodeType", NODE_TYPE);
        output.put("promptKey", promptKey);
        output.put("operator", operator(context));
        output.put("payload", context.payload());
        output.put("context", context.contextData());
        output.put("generation", "stub");
        output.put("status", "GENERATED");
        output.put("generatedCopy", generatedCopy(promptKey));
        return NodeExecutionResult.success(output);
    }

    /**
     * 执行 operator 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    private static String operator(NodeExecutionContext context) {
        return userOrAnonymous(context);
    }

    /**
     * 执行 generatedCopy 对应的业务处理。
     * @param promptKey promptKey 参数
     */
    private static String generatedCopy(String promptKey) {
        return "Generated copy for " + promptKey;
    }
}
