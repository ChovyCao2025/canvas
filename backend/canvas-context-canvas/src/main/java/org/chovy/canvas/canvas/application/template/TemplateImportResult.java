package org.chovy.canvas.canvas.application.template;

import java.util.List;

import org.chovy.canvas.canvas.api.template.TemplateValidationPort;

public record TemplateImportResult(
        boolean imported,
        Long canvasId,
        Long versionId,
        ImportMode importMode,
        List<TemplateValidationPort.TemplateViolation> violations) {

    public TemplateImportResult {
        importMode = importMode == null ? ImportMode.NONE : importMode;
        violations = List.copyOf(violations == null ? List.of() : violations);
    }

    static TemplateImportResult blocked(List<TemplateValidationPort.TemplateViolation> violations) {
        return new TemplateImportResult(false, null, null, ImportMode.NONE, violations);
    }

    static TemplateImportResult imported(Long canvasId, Long versionId) {
        return new TemplateImportResult(true, canvasId, versionId, ImportMode.CLONE, List.of());
    }

    public enum ImportMode {
        NONE,
        CLONE
    }
}
