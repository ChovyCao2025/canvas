package org.chovy.canvas.canvas.application.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.template.TemplateValidationPort;
import org.junit.jupiter.api.Test;

class TemplateImportServiceTest {

    @Test
    void missingRequiredPluginBlocksImportBeforeDraftCreation() {
        RecordingDraftCreator draftCreator = new RecordingDraftCreator();
        TemplateImportService service = new TemplateImportService(new BlockingValidationPort(), draftCreator);

        TemplateImportResult result = service.importTemplate(new TemplateImportRequest(
                7L,
                "new-user-welcome",
                "New user welcome",
                "{\"nodes\":[]}",
                "{\"user\":{\"id\":\"u1\"}}",
                List.of("canvas-plugin-message", "canvas-plugin-coupon"),
                Map.of("canvas-plugin-message", true),
                "operator"));

        assertThat(result.imported()).isFalse();
        assertThat(result.violations()).extracting(TemplateValidationPort.TemplateViolation::code)
                .containsExactly("MISSING_PLUGIN");
        assertThat(draftCreator.calls).isZero();
    }

    @Test
    void enabledTemplateCreatesDraftThroughPublicDraftCreator() {
        RecordingDraftCreator draftCreator = new RecordingDraftCreator();
        TemplateImportService service = new TemplateImportService(command -> TemplateValidationPort.TemplateValidationResult.passed(),
                draftCreator);

        TemplateImportResult result = service.importTemplate(new TemplateImportRequest(
                7L,
                "new-user-welcome",
                "New user welcome",
                "{\"nodes\":[]}",
                "{\"user\":{\"id\":\"u1\"}}",
                List.of("canvas-plugin-message"),
                Map.of("canvas-plugin-message", true),
                "operator"));

        assertThat(result.imported()).isTrue();
        assertThat(result.canvasId()).isEqualTo(42L);
        assertThat(draftCreator.calls).isOne();
        assertThat(draftCreator.lastCommand.templateKey()).isEqualTo("new-user-welcome");
        assertThat(draftCreator.lastCommand.graphJson()).isEqualTo("{\"nodes\":[]}");
    }

    @Test
    void validationPortBlocksSamplePayloadBeforeDraftCreation() {
        RecordingDraftCreator draftCreator = new RecordingDraftCreator();
        CapturingValidationPort validationPort = new CapturingValidationPort(
                TemplateValidationPort.TemplateValidationResult.blocked(List.of(
                        new TemplateValidationPort.TemplateViolation(
                                "EXPECTED_TRACE_MISMATCH",
                                "sample payload did not produce expected message node"))));
        TemplateImportService service = new TemplateImportService(validationPort, draftCreator);

        TemplateImportResult result = service.importTemplate(new TemplateImportRequest(
                7L,
                "new-user-welcome",
                "New user welcome",
                "{\"nodes\":[{\"id\":\"message\"}]}",
                "{\"user\":{\"id\":\"u1\"}}",
                List.of("canvas-plugin-message"),
                Map.of("canvas-plugin-message", true),
                "operator"));

        assertThat(result.imported()).isFalse();
        assertThat(result.violations()).extracting(TemplateValidationPort.TemplateViolation::code)
                .containsExactly("EXPECTED_TRACE_MISMATCH");
        assertThat(validationPort.lastCommand.samplePayloadJson()).isEqualTo("{\"user\":{\"id\":\"u1\"}}");
        assertThat(validationPort.lastCommand.canvasJson()).contains("\"message\"");
        assertThat(draftCreator.calls).isZero();
    }

    @Test
    void repeatedImportsCreateExplicitClonesWithoutReusingPreviousDraft() {
        RecordingDraftCreator draftCreator = new RecordingDraftCreator();
        TemplateImportService service = new TemplateImportService(
                command -> TemplateValidationPort.TemplateValidationResult.passed(),
                draftCreator);
        TemplateImportRequest request = new TemplateImportRequest(
                7L,
                "new-user-welcome",
                "New user welcome",
                "{\"nodes\":[]}",
                "{\"user\":{\"id\":\"u1\"}}",
                List.of("canvas-plugin-message"),
                Map.of("canvas-plugin-message", true),
                "operator");

        TemplateImportResult first = service.importTemplate(request);
        TemplateImportResult second = service.importTemplate(request);

        assertThat(first.importMode()).isEqualTo(TemplateImportResult.ImportMode.CLONE);
        assertThat(second.importMode()).isEqualTo(TemplateImportResult.ImportMode.CLONE);
        assertThat(first.canvasId()).isEqualTo(42L);
        assertThat(second.canvasId()).isEqualTo(43L);
        assertThat(draftCreator.commands)
                .extracting(TemplateImportService.DraftCreationCommand::templateKey)
                .containsExactly("new-user-welcome", "new-user-welcome");
    }

    private static final class BlockingValidationPort implements TemplateValidationPort {
        @Override
        public TemplateValidationResult validateTemplate(TemplateValidationCommand command) {
            return TemplateValidationResult.blocked(List.of(
                    new TemplateViolation("MISSING_PLUGIN", "canvas-plugin-coupon is required")));
        }
    }

    private static final class CapturingValidationPort implements TemplateValidationPort {
        private final TemplateValidationResult result;
        private TemplateValidationCommand lastCommand;

        private CapturingValidationPort(TemplateValidationResult result) {
            this.result = result;
        }

        @Override
        public TemplateValidationResult validateTemplate(TemplateValidationCommand command) {
            this.lastCommand = command;
            return result;
        }
    }

    private static final class RecordingDraftCreator implements TemplateImportService.DraftCreator {
        private int calls;
        private TemplateImportService.DraftCreationCommand lastCommand;
        private final List<TemplateImportService.DraftCreationCommand> commands = new ArrayList<>();

        @Override
        public TemplateImportService.DraftCreationResult createDraft(TemplateImportService.DraftCreationCommand command) {
            this.calls++;
            this.lastCommand = command;
            this.commands.add(command);
            return new TemplateImportService.DraftCreationResult(41L + calls, 99L + calls);
        }
    }
}
