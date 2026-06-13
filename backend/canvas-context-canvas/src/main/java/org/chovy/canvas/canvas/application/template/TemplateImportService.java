package org.chovy.canvas.canvas.application.template;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.template.TemplateValidationPort;

public class TemplateImportService {

    private final TemplateValidationPort validationPort;
    private final DraftCreator draftCreator;

    public TemplateImportService(TemplateValidationPort validationPort, DraftCreator draftCreator) {
        this.validationPort = validationPort;
        this.draftCreator = draftCreator;
    }

    public TemplateImportResult importTemplate(TemplateImportRequest request) {
        List<TemplateValidationPort.TemplateViolation> pluginViolations = missingPluginViolations(request);
        if (!pluginViolations.isEmpty()) {
            return TemplateImportResult.blocked(pluginViolations);
        }

        TemplateValidationPort.TemplateValidationResult validation = validationPort.validateTemplate(
                new TemplateValidationPort.TemplateValidationCommand(
                        request.tenantId(),
                        request.templateKey(),
                        request.pluginEnablement(),
                        request.graphJson(),
                        request.samplePayloadJson()));
        if (!validation.valid()) {
            return TemplateImportResult.blocked(validation.violations());
        }

        DraftCreationResult draft = draftCreator.createDraft(new DraftCreationCommand(
                request.tenantId(),
                request.templateKey(),
                request.name(),
                request.graphJson(),
                request.operator()));
        return TemplateImportResult.imported(draft.canvasId(), draft.versionId());
    }

    private static List<TemplateValidationPort.TemplateViolation> missingPluginViolations(TemplateImportRequest request) {
        List<TemplateValidationPort.TemplateViolation> violations = new ArrayList<>();
        for (String pluginKey : request.requiredPluginKeys()) {
            if (!Boolean.TRUE.equals(request.pluginEnablement().get(pluginKey))) {
                violations.add(new TemplateValidationPort.TemplateViolation(
                        "MISSING_PLUGIN",
                        pluginKey + " is required"));
            }
        }
        return violations;
    }

    public interface DraftCreator {
        DraftCreationResult createDraft(DraftCreationCommand command);
    }

    public record DraftCreationCommand(
            Long tenantId,
            String templateKey,
            String name,
            String graphJson,
            String operator) {
    }

    public record DraftCreationResult(Long canvasId, Long versionId) {
    }
}
