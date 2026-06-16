package org.chovy.canvas.execution.api.template;

import java.util.List;
import java.util.Map;

/**
 * 定义 TemplateDryRunFacade 的执行上下文数据结构或业务契约。
 */
public interface TemplateDryRunFacade {

    /**
     * 执行 dryRun 对应的业务处理。
     * @param command command 参数
     */
    TemplateDryRunResultView dryRun(TemplateDryRunCommand command);

    /**
     * 定义 TemplateDryRunCommand 的执行上下文数据结构或业务契约。
     * @param tenantId tenantId 对应的数据字段
     * @param templateKey templateKey 对应的数据字段
     * @param canvasJson canvasJson 对应的数据字段
     * @param samplePayloadJson samplePayloadJson 对应的数据字段
     * @param requiredPluginKeys requiredPluginKeys 对应的数据字段
     * @param pluginEnablement pluginEnablement 对应的数据字段
     * @param expectedTrace expectedTrace 对应的数据字段
     * @param mockMode mockMode 对应的数据字段
     */
    record TemplateDryRunCommand(
            Long tenantId,
            String templateKey,
            String canvasJson,
            String samplePayloadJson,
            List<String> requiredPluginKeys,
            Map<String, Boolean> pluginEnablement,
            List<ExpectedTraceStep> expectedTrace,
            boolean mockMode) {

        public TemplateDryRunCommand {
            if (tenantId == null || tenantId <= 0) {
                throw new IllegalArgumentException("tenantId is required");
            }
            if (templateKey == null || templateKey.isBlank()) {
                throw new IllegalArgumentException("templateKey is required");
            }
            if (canvasJson == null || canvasJson.isBlank()) {
                throw new IllegalArgumentException("canvasJson is required");
            }
            samplePayloadJson = samplePayloadJson == null || samplePayloadJson.isBlank() ? "{}" : samplePayloadJson;
            requiredPluginKeys = List.copyOf(requiredPluginKeys == null ? List.of() : requiredPluginKeys);
            pluginEnablement = Map.copyOf(pluginEnablement == null ? Map.of() : pluginEnablement);
            expectedTrace = List.copyOf(expectedTrace == null ? List.of() : expectedTrace);
        }
    }

    /**
     * 定义 ExpectedTraceStep 的执行上下文数据结构或业务契约。
     * @param nodeId nodeId 对应的数据字段
     * @param nodeType nodeType 对应的数据字段
     * @param outcome outcome 对应的数据字段
     * @param summary summary 对应的数据字段
     */
    record ExpectedTraceStep(
            String nodeId,
            String nodeType,
            String outcome,
            String summary) {

        public ExpectedTraceStep {
            if (nodeId == null || nodeId.isBlank()) {
                throw new IllegalArgumentException("nodeId is required");
            }
            nodeType = nodeType == null ? "" : nodeType;
            if (outcome == null || outcome.isBlank()) {
                throw new IllegalArgumentException("outcome is required");
            }
            summary = summary == null ? "" : summary;
        }
    }

    /**
     * 定义 TemplateTraceStepView 的执行上下文数据结构或业务契约。
     * @param nodeId nodeId 对应的数据字段
     * @param nodeType nodeType 对应的数据字段
     * @param outcome outcome 对应的数据字段
     * @param summary summary 对应的数据字段
     * @param outputData outputData 对应的数据字段
     */
    record TemplateTraceStepView(
            String nodeId,
            String nodeType,
            String outcome,
            String summary,
            Map<String, Object> outputData) {

        public TemplateTraceStepView {
            if (nodeId == null || nodeId.isBlank()) {
                throw new IllegalArgumentException("nodeId is required");
            }
            nodeType = nodeType == null ? "" : nodeType;
            outcome = outcome == null || outcome.isBlank() ? "UNKNOWN" : outcome;
            summary = summary == null ? "" : summary;
            outputData = Map.copyOf(outputData == null ? Map.of() : outputData);
        }
    }

    /**
     * 定义 TemplateDryRunViolation 的执行上下文数据结构或业务契约。
     * @param code code 对应的数据字段
     * @param message message 对应的数据字段
     */
    record TemplateDryRunViolation(String code, String message) {

        public TemplateDryRunViolation {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }

    /**
     * 定义 TemplateDryRunResultView 的执行上下文数据结构或业务契约。
     * @param valid valid 对应的数据字段
     * @param executionId executionId 对应的数据字段
     * @param published published 对应的数据字段
     * @param trace trace 对应的数据字段
     * @param matchedNodeIds matchedNodeIds 对应的数据字段
     * @param violations violations 对应的数据字段
     */
    record TemplateDryRunResultView(
            boolean valid,
            String executionId,
            boolean published,
            List<TemplateTraceStepView> trace,
            List<String> matchedNodeIds,
            List<TemplateDryRunViolation> violations) {

        public TemplateDryRunResultView {
            if (valid && (executionId == null || executionId.isBlank())) {
                throw new IllegalArgumentException("executionId is required for successful dry-run");
            }
            executionId = executionId == null ? "" : executionId;
            trace = List.copyOf(trace == null ? List.of() : trace);
            matchedNodeIds = List.copyOf(matchedNodeIds == null ? List.of() : matchedNodeIds);
            violations = List.copyOf(violations == null ? List.of() : violations);
        }

        public static TemplateDryRunResultView passed(
                String executionId,
                List<TemplateTraceStepView> trace,
                List<String> matchedNodeIds) {
            return new TemplateDryRunResultView(true, executionId, false, trace, matchedNodeIds, List.of());
        }

        public static TemplateDryRunResultView blocked(List<TemplateDryRunViolation> violations) {
            return new TemplateDryRunResultView(false, "", false, List.of(), List.of(), violations);
        }
    }
}
