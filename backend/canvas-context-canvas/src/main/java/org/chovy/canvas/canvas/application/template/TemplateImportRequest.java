package org.chovy.canvas.canvas.application.template;

import java.util.List;
import java.util.Map;

public record TemplateImportRequest(
        Long tenantId,
        String templateKey,
        String name,
        String graphJson,
        String samplePayloadJson,
        List<String> requiredPluginKeys,
        Map<String, Boolean> pluginEnablement,
        String operator) {

    public TemplateImportRequest {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (templateKey == null || templateKey.isBlank()) {
            throw new IllegalArgumentException("templateKey is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        graphJson = graphJson == null || graphJson.isBlank() ? "{}" : graphJson;
        samplePayloadJson = samplePayloadJson == null || samplePayloadJson.isBlank() ? "{}" : samplePayloadJson;
        requiredPluginKeys = List.copyOf(requiredPluginKeys == null ? List.of() : requiredPluginKeys);
        pluginEnablement = Map.copyOf(pluginEnablement == null ? Map.of() : pluginEnablement);
        operator = operator == null || operator.isBlank() ? "system" : operator;
    }
}
