package org.chovy.canvas.execution.api.template;

import java.util.List;
import java.util.Map;

public interface TemplateDryRunFacade {

    TemplateDryRunResultView dryRun(TemplateDryRunCommand command);

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

    record TemplateDryRunViolation(String code, String message) {

        public TemplateDryRunViolation {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("code is required");
            }
            message = message == null ? "" : message;
        }
    }

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
