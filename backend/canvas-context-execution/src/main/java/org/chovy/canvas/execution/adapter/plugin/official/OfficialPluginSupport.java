package org.chovy.canvas.execution.adapter.plugin.official;

import org.chovy.canvas.execution.domain.NodeExecutionContext;

public final class OfficialPluginSupport {

    private OfficialPluginSupport() {
    }

    public static String stringConfig(NodeExecutionContext context, String key) {
        Object value = context.node().config().get(key);
        return value instanceof String string ? string.trim() : "";
    }

    public static String userOrAnonymous(NodeExecutionContext context) {
        return context.userId().isBlank() ? "anonymous" : context.userId();
    }
}
