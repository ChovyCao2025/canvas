package org.chovy.canvas.canvas.application.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.canvas.api.template.TemplateValidationPort;
import org.junit.jupiter.api.Test;

/**
 * 封装TemplateImportServiceTest相关的业务逻辑。
 */
class TemplateImportServiceTest {

    /**
     * 处理missingRequiredPluginBlocksImportBeforeDraftCreation。
     */
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

    /**
     * 处理enabledTemplateCreatesDraftThroughPublicDraftCreator。
     */
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

    /**
     * 处理validationPortBlocksSamplePayloadBeforeDraftCreation。
     */
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

    /**
     * 处理repeatedImportsCreateExplicitClonesWithoutReusingPreviousDraft。
     */
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

    /**
     * 封装BlockingValidationPort相关的业务逻辑。
     */
    private static final class BlockingValidationPort implements TemplateValidationPort {

        /**
         * 处理validateTemplate。
         */
        @Override
        public TemplateValidationResult validateTemplate(TemplateValidationCommand command) {
            return TemplateValidationResult.blocked(List.of(
                    new TemplateViolation("MISSING_PLUGIN", "canvas-plugin-coupon is required")));
        }
    }

    /**
     * 封装CapturingValidationPort相关的业务逻辑。
     */
    private static final class CapturingValidationPort implements TemplateValidationPort {

        /**
         * 保存结果。
         */
        private final TemplateValidationResult result;

        /**
         * 保存lastCommand。
         */
        private TemplateValidationCommand lastCommand;

        /**
         * 创建当前对象实例。
         */
        private CapturingValidationPort(TemplateValidationResult result) {
            this.result = result;
        }

        /**
         * 处理validateTemplate。
         */
        @Override
        public TemplateValidationResult validateTemplate(TemplateValidationCommand command) {
            this.lastCommand = command;
            return result;
        }
    }

    /**
     * 封装RecordingDraftCreator相关的业务逻辑。
     */
    private static final class RecordingDraftCreator implements TemplateImportService.DraftCreator {

        /**
         * 保存calls。
         */
        private int calls;

        /**
         * 保存lastCommand。
         */
        private TemplateImportService.DraftCreationCommand lastCommand;

        /**
         * 保存测试或内存实现使用的commands列表。
         */
        private final List<TemplateImportService.DraftCreationCommand> commands = new ArrayList<>();

        /**
         * 创建Draft。
         */
        @Override
        public TemplateImportService.DraftCreationResult createDraft(TemplateImportService.DraftCreationCommand command) {
            this.calls++;
            this.lastCommand = command;
            this.commands.add(command);
            return new TemplateImportService.DraftCreationResult(41L + calls, 99L + calls);
        }
    }
}
