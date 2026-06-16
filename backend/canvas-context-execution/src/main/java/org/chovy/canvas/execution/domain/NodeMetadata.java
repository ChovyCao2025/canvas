package org.chovy.canvas.execution.domain;

import java.util.List;

/**
 * 定义 NodeMetadata 的执行上下文数据结构或业务契约。
 * @param nodeType nodeType 对应的数据字段
 * @param displayName displayName 对应的数据字段
 * @param category category 对应的数据字段
 * @param configSchemaJson configSchemaJson 对应的数据字段
 * @param inputPorts inputPorts 对应的数据字段
 * @param outputPorts outputPorts 对应的数据字段
 * @param requiredPluginId requiredPluginId 对应的数据字段
 * @param enabled enabled 对应的数据字段
 * @param disabledReason disabledReason 对应的数据字段
 */
public record NodeMetadata(
        String nodeType,
        String displayName,
        String category,
        String configSchemaJson,
        List<String> inputPorts,
        List<String> outputPorts,
        String requiredPluginId,
        boolean enabled,
        String disabledReason) {

    public NodeMetadata {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("nodeType is required");
        }
        displayName = displayName == null ? nodeType : displayName;
        category = category == null ? "Runtime" : category;
        configSchemaJson = configSchemaJson == null ? "{}" : configSchemaJson;
        inputPorts = List.copyOf(inputPorts == null ? List.of() : inputPorts);
        outputPorts = List.copyOf(outputPorts == null ? List.of() : outputPorts);
        requiredPluginId = requiredPluginId == null ? "" : requiredPluginId;
        disabledReason = disabledReason == null ? "" : disabledReason;
    }
}
