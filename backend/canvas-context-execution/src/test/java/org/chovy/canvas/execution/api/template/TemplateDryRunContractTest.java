package org.chovy.canvas.execution.api.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateDryRunContractTest {

    @Test
    void dryRunsTemplatePackSamplesAgainstExpectedTraceWithoutPublishing() {
        RecordingTemplateDryRunFacade facade = new RecordingTemplateDryRunFacade();

        List<TemplateDryRunFacade.TemplateDryRunCommand> commands = List.of(
                command("new-user-welcome", List.of(
                        expected("segment", "condition", "MATCHED"),
                        expected("message", "message.send", "SENT"))),
                command("dormant-user-winback", List.of(
                        expected("risk", "risk.check", "MATCHED"),
                        expected("coupon", "coupon.grant", "SENT"))),
                command("coupon-approval-release", List.of(
                        expected("approval", "approval.request", "APPROVED"),
                        expected("message", "message.send", "SENT"))));

        List<TemplateDryRunFacade.TemplateDryRunResultView> results = commands.stream()
                .map(facade::dryRun)
                .toList();

        assertThat(results).allSatisfy(result -> {
            assertThat(result.valid()).isTrue();
            assertThat(result.published()).isFalse();
            assertThat(result.violations()).isEmpty();
        });
        assertThat(results.get(0).matchedNodeIds()).containsExactly("segment", "message");
        assertThat(facade.publishCalls).isZero();
        assertThat(facade.commands)
                .extracting(TemplateDryRunFacade.TemplateDryRunCommand::templateKey)
                .containsExactly("new-user-welcome", "dormant-user-winback", "coupon-approval-release");
    }

    @Test
    void commandAndResultDefensivelyCopyMutableTemplatePackCollections() {
        List<String> requiredPlugins = new ArrayList<>(List.of("canvas-plugin-message"));
        Map<String, Boolean> pluginEnablement = new HashMap<>(Map.of("canvas-plugin-message", true));
        List<TemplateDryRunFacade.ExpectedTraceStep> expectedTrace = new ArrayList<>(List.of(
                expected("message", "message.send", "SENT")));

        TemplateDryRunFacade.TemplateDryRunCommand command = new TemplateDryRunFacade.TemplateDryRunCommand(
                7L,
                "new-user-welcome",
                "{\"nodes\":[]}",
                "{\"user\":{\"id\":\"u1\"}}",
                requiredPlugins,
                pluginEnablement,
                expectedTrace,
                true);

        requiredPlugins.add("mutated");
        pluginEnablement.put("mutated", true);
        expectedTrace.clear();

        assertThat(command.requiredPluginKeys()).containsExactly("canvas-plugin-message");
        assertThat(command.pluginEnablement()).containsOnly(Map.entry("canvas-plugin-message", true));
        assertThat(command.expectedTrace()).extracting(TemplateDryRunFacade.ExpectedTraceStep::nodeId)
                .containsExactly("message");
        assertThatThrownBy(() -> command.expectedTrace().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reportsDependencyAndExpectedTraceViolationsInPublicResult() {
        RecordingTemplateDryRunFacade facade = new RecordingTemplateDryRunFacade();
        TemplateDryRunFacade.TemplateDryRunCommand command = new TemplateDryRunFacade.TemplateDryRunCommand(
                7L,
                "new-user-welcome",
                "{\"nodes\":[]}",
                "{\"user\":{\"id\":\"u1\"}}",
                List.of("canvas-plugin-message", "canvas-plugin-coupon"),
                Map.of("canvas-plugin-message", true),
                List.of(expected("missing", "coupon.grant", "SENT")),
                true);

        TemplateDryRunFacade.TemplateDryRunResultView result = facade.dryRun(command);

        assertThat(result.valid()).isFalse();
        assertThat(result.published()).isFalse();
        assertThat(result.violations()).extracting(TemplateDryRunFacade.TemplateDryRunViolation::code)
                .containsExactly("MISSING_PLUGIN", "EXPECTED_TRACE_MISMATCH");
        assertThat(result.trace()).isEmpty();
        assertThat(facade.publishCalls).isZero();
    }

    private static TemplateDryRunFacade.TemplateDryRunCommand command(
            String templateKey,
            List<TemplateDryRunFacade.ExpectedTraceStep> expectedTrace) {
        return new TemplateDryRunFacade.TemplateDryRunCommand(
                7L,
                templateKey,
                "{\"nodes\":[]}",
                "{\"user\":{\"id\":\"u1\"}}",
                List.of("canvas-plugin-message"),
                Map.of("canvas-plugin-message", true),
                expectedTrace,
                true);
    }

    private static TemplateDryRunFacade.ExpectedTraceStep expected(String nodeId, String nodeType, String outcome) {
        return new TemplateDryRunFacade.ExpectedTraceStep(nodeId, nodeType, outcome, nodeId + " summary");
    }

    private static final class RecordingTemplateDryRunFacade implements TemplateDryRunFacade {
        private int publishCalls;
        private final List<TemplateDryRunCommand> commands = new ArrayList<>();

        @Override
        public TemplateDryRunResultView dryRun(TemplateDryRunCommand command) {
            this.commands.add(command);
            List<TemplateDryRunViolation> violations = new ArrayList<>();
            for (String pluginKey : command.requiredPluginKeys()) {
                if (!Boolean.TRUE.equals(command.pluginEnablement().get(pluginKey))) {
                    violations.add(new TemplateDryRunViolation("MISSING_PLUGIN", pluginKey + " is required"));
                }
            }
            for (ExpectedTraceStep step : command.expectedTrace()) {
                if ("missing".equals(step.nodeId())) {
                    violations.add(new TemplateDryRunViolation(
                            "EXPECTED_TRACE_MISMATCH",
                            step.nodeId() + " did not appear in dry-run trace"));
                }
            }
            if (!violations.isEmpty()) {
                return TemplateDryRunResultView.blocked(violations);
            }

            List<TemplateTraceStepView> trace = command.expectedTrace().stream()
                    .map(step -> new TemplateTraceStepView(
                            step.nodeId(),
                            step.nodeType(),
                            step.outcome(),
                            step.summary(),
                            Map.of("templateKey", command.templateKey())))
                    .toList();
            return TemplateDryRunResultView.passed(
                    "template-dry-run-" + commands.size(),
                    trace,
                    trace.stream().map(TemplateTraceStepView::nodeId).toList());
        }
    }
}
