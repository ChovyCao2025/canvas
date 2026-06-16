package org.chovy.canvas.execution.domain;

import java.util.List;

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
