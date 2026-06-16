package org.chovy.canvas.execution.adapter.plugin.official.coupon;

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
 * 定义 OfficialCouponNodeHandler 的执行上下文数据结构或业务契约。
 */
@Component
@NodeHandlerType(OfficialCouponNodeHandler.NODE_TYPE)
public class OfficialCouponNodeHandler implements NodeHandler {

    /**
     * 保存 PLUGIN_ID 对应的状态或配置。
     */
    static final String PLUGIN_ID = "canvas-plugin-coupon";

    /**
     * 保存 NODE_TYPE 对应的状态或配置。
     */
    static final String NODE_TYPE = "coupon.grant";

    /**
     * 执行 execute 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String couponKey = stringConfig(context, "couponKey");
        if (couponKey.isBlank()) {
            return NodeExecutionResult.failure("coupon key is required");
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pluginId", PLUGIN_ID);
        output.put("nodeType", NODE_TYPE);
        output.put("couponKey", couponKey);
        output.put("recipient", recipient(context));
        output.put("payload", context.payload());
        output.put("context", context.contextData());
        output.put("grant", "stub");
        output.put("status", "SENT");
        return NodeExecutionResult.success(output);
    }

    /**
     * 执行 recipient 对应的业务处理。
     * @param context context 参数
     * @return 处理后的结果
     */
    private static String recipient(NodeExecutionContext context) {
        return userOrAnonymous(context);
    }
}
