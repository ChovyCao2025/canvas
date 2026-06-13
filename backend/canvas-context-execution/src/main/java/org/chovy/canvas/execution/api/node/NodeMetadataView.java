package org.chovy.canvas.execution.api.node;

import java.util.List;

public record NodeMetadataView(
        String nodeType,
        String displayName,
        String category,
        String configSchemaJson,
        List<String> inputPorts,
        List<String> outputPorts,
        String requiredPluginId,
        boolean enabled,
        String disabledReason) {

    public NodeMetadataView {
        if (nodeType == null || nodeType.isBlank()) {
            throw new IllegalArgumentException("nodeType is required");
        }
        displayName = displayName == null ? "" : displayName;
        category = category == null ? "" : category;
        configSchemaJson = configSchemaJson == null ? "{}" : configSchemaJson;
        inputPorts = List.copyOf(inputPorts == null ? List.of() : inputPorts);
        outputPorts = List.copyOf(outputPorts == null ? List.of() : outputPorts);
        requiredPluginId = requiredPluginId == null ? "" : requiredPluginId;
        disabledReason = disabledReason == null ? "" : disabledReason;
    }
}
