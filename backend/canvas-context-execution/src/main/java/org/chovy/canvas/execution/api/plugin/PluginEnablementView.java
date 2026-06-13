package org.chovy.canvas.execution.api.plugin;

import java.util.List;

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
