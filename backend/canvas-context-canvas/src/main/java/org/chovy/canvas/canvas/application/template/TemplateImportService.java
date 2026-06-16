package org.chovy.canvas.canvas.application.template;

import java.util.ArrayList;
import java.util.List;

import org.chovy.canvas.canvas.api.template.TemplateValidationPort;

/**
 * 封装TemplateImportService相关的业务逻辑。
 */
public class TemplateImportService {

    /**
     * 保存validationPort。
     */
    private final TemplateValidationPort validationPort;

    /**
     * 保存draftCreator。
     */
    private final DraftCreator draftCreator;

    /**
     * 创建当前对象实例。
     */
    public TemplateImportService(TemplateValidationPort validationPort, DraftCreator draftCreator) {
        this.validationPort = validationPort;
        this.draftCreator = draftCreator;
    }

    /**
     * 处理importTemplate。
     */
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

    /**
     * 处理missingPluginViolations。
     */
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

    /**
     * 定义DraftCreator对外提供的能力契约。
     */
    public interface DraftCreator {

        /**
         * 创建Draft。
         */
        DraftCreationResult createDraft(DraftCreationCommand command);
    }

    /**
     * 承载DraftCreationCommand的数据快照。
     */
    public record DraftCreationCommand(
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
             * 记录操作人。
             */
            String operator) {
    }

    /**
     * 承载DraftCreationResult的数据快照。
     */
    public record DraftCreationResult(Long canvasId, Long versionId) {
    }
}
