package org.chovy.canvas.execution.api.plugin;

import java.util.List;

/**
 * 定义 PluginEnablementView 的执行上下文数据结构或业务契约。
 * @param pluginId pluginId 对应的数据字段
 * @param version version 对应的数据字段
 * @param enabled enabled 对应的数据字段
 * @param permissions permissions 对应的数据字段
 * @param nodeTypes nodeTypes 对应的数据字段
 * @param disabledReason disabledReason 对应的数据字段
 */
public record PluginEnablementView(
        String pluginId,
        String version,
        boolean enabled,
        List<String> permissions,
        List<String> nodeTypes,
        String disabledReason) {

    public PluginEnablementView {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId is required");
        }
        version = version == null ? "" : version;
        permissions = List.copyOf(permissions == null ? List.of() : permissions);
        nodeTypes = List.copyOf(nodeTypes == null ? List.of() : nodeTypes);
        disabledReason = disabledReason == null ? "" : disabledReason;
    }
}
