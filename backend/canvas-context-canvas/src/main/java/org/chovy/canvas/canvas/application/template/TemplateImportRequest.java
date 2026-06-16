package org.chovy.canvas.canvas.application.template;

import java.util.List;
import java.util.Map;

/**
 * 承载TemplateImportRequest的数据快照。
 */
public record TemplateImportRequest(
        /**
         * 记录租户标识。
         */
        Long tenantId,
        /**
         * 记录templateKey。
         */
        String templateKey,
        /**
         * 记录名称。
         */
        String name,
        /**
         * 记录graphJSON 内容。
         */
        String graphJson,
        /**
         * 记录sample payloadJSON 内容。
         */
        String samplePayloadJson,
        /**
         * 记录requiredPluginKeys。
         */
        List<String> requiredPluginKeys,
        /**
         * 记录pluginEnablement。
         */
        Map<String, Boolean> pluginEnablement,
        /**
         * 记录操作人。
         */
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
