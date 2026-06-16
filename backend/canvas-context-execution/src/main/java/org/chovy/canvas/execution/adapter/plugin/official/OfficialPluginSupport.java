package org.chovy.canvas.execution.adapter.plugin.official;

import org.chovy.canvas.execution.domain.NodeExecutionContext;

/**
 * 定义 OfficialPluginSupport 的执行上下文数据结构或业务契约。
 */
public final class OfficialPluginSupport {

    /**
     * 执行 OfficialPluginSupport 对应的业务处理。
     */
    private OfficialPluginSupport() {
    }

    /**
     * 执行 stringConfig 对应的业务处理。
     * @param context context 参数
     * @param key key 参数
     */
    public static String stringConfig(NodeExecutionContext context, String key) {
        Object value = context.node().config().get(key);
        return value instanceof String string ? string.trim() : "";
    }

    /**
     * 执行 userOrAnonymous 对应的业务处理。
     * @param context context 参数
     */
    public static String userOrAnonymous(NodeExecutionContext context) {
        return context.userId().isBlank() ? "anonymous" : context.userId();
    }
}
