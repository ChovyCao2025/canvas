package org.chovy.canvas.canvas.api.template;

import java.util.List;
import java.util.Map;

/**
 * 定义TemplateValidationPort对外提供的能力契约。
 */
public interface TemplateValidationPort {

    /**
     * 处理validateTemplate。
     */
    TemplateValidationResult validateTemplate(TemplateValidationCommand command);

    /**
     * 承载TemplateValidationCommand的数据快照。
     */
    record TemplateValidationCommand(
            /**
             * 记录租户标识。
             */
            Long tenantId,
            /**
             * 记录templateKey。
             */
            String templateKey,
            /**
             * 记录pluginEnablement。
             */
            Map<String, Boolean> pluginEnablement,
            /**
             * 记录canvasJSON 内容。
             */
            String canvasJson,
            /**
             * 记录sample payloadJSON 内容。
             */
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

    /**
     * 承载TemplateValidationResult的数据快照。
     */
    record TemplateValidationResult(boolean valid, List<TemplateViolation> violations) {

        public TemplateValidationResult {
            violations = List.copyOf(violations == null ? List.of() : violations);
        }

        /**
         * 处理passed。
         */
        public static TemplateValidationResult passed() {
            return new TemplateValidationResult(true, List.of());
        }

        /**
         * 处理blocked。
         */
        public static TemplateValidationResult blocked(List<TemplateViolation> violations) {
            return new TemplateValidationResult(false, violations);
        }
    }

    /**
     * 承载TemplateViolation的数据快照。
     */
    record TemplateViolation(String code, String message) {

        public TemplateViolation {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }
}
