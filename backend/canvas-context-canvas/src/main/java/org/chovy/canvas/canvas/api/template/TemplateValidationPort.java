package org.chovy.canvas.canvas.api.template;

import java.util.List;
import java.util.Map;

public interface TemplateValidationPort {

    TemplateValidationResult validateTemplate(TemplateValidationCommand command);

    record TemplateValidationCommand(
            Long tenantId,
            String templateKey,
            Map<String, Boolean> pluginEnablement,
            String canvasJson,
            String samplePayloadJson) {

        public TemplateValidationCommand {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (templateKey == null || templateKey.isBlank()) {
                throw new IllegalArgumentException("templateKey is required");
            }
            pluginEnablement = Map.copyOf(pluginEnablement == null ? Map.of() : pluginEnablement);
            canvasJson = canvasJson == null ? "{}" : canvasJson;
            samplePayloadJson = samplePayloadJson == null ? "{}" : samplePayloadJson;
        }
    }

    record TemplateValidationResult(boolean valid, List<TemplateViolation> violations) {

        public TemplateValidationResult {
            violations = List.copyOf(violations == null ? List.of() : violations);
        }

        public static TemplateValidationResult passed() {
            return new TemplateValidationResult(true, List.of());
        }

        public static TemplateValidationResult blocked(List<TemplateViolation> violations) {
            return new TemplateValidationResult(false, violations);
        }
    }

    record TemplateViolation(String code, String message) {

        public TemplateViolation {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }
}
