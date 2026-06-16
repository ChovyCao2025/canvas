package org.chovy.canvas.canvas.application.template;

import java.util.List;

import org.chovy.canvas.canvas.api.template.TemplateValidationPort;

/**
 * 承载TemplateImportResult的数据快照。
 */
public record TemplateImportResult(
        /**
         * 记录imported。
         */
        boolean imported,
        /**
         * 记录画布标识。
         */
        Long canvasId,
        /**
         * 记录版本标识。
         */
        Long versionId,
        /**
         * 记录importMode。
         */
        ImportMode importMode,
        /**
         * 记录violations。
         */
        List<TemplateValidationPort.TemplateViolation> violations) {

    public TemplateImportResult {
        importMode = importMode == null ? ImportMode.NONE : importMode;
        violations = List.copyOf(violations == null ? List.of() : violations);
    }

    /**
     * 创建模板导入被校验阻断的结果。
     */
    static TemplateImportResult blocked(List<TemplateValidationPort.TemplateViolation> violations) {
        return new TemplateImportResult(false, null, null, ImportMode.NONE, violations);
    }

    /**
     * 创建模板导入成功的结果。
     */
    static TemplateImportResult imported(Long canvasId, Long versionId) {
        return new TemplateImportResult(true, canvasId, versionId, ImportMode.CLONE, List.of());
    }

    /**
     * 枚举ImportMode支持的取值。
     */
    public enum ImportMode {

        /**
         * 表示NONE状态。
         */
        NONE,
        /**
         * 表示CLONE状态。
         */
        CLONE
    }
}
