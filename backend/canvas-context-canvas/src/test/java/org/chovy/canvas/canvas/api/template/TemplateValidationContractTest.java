package org.chovy.canvas.canvas.api.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TemplateValidationContractTest {

    @Test
    void validationBlocksDraftCreationWhenRequiredPluginIsMissing() {
        BlockingTemplateValidationPort port = new BlockingTemplateValidationPort();
        TemplateValidationPort.TemplateValidationCommand command =
                new TemplateValidationPort.TemplateValidationCommand(
                        7L,
                        "new-user-welcome",
                        Map.of("canvas-plugin-message", true),
                        "{\"nodes\":[]}",
                        "{\"user\":{\"id\":\"u1\"}}");
        DraftCreator draftCreator = new DraftCreator();

        TemplateValidationPort.TemplateValidationResult result = port.validateTemplate(command);
        if (result.valid()) {
            draftCreator.createDraft();
        }

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).extracting(TemplateValidationPort.TemplateViolation::code)
                .containsExactly("MISSING_PLUGIN");
        assertThat(draftCreator.created).isFalse();
    }

    private static final class BlockingTemplateValidationPort implements TemplateValidationPort {
        @Override
        public TemplateValidationResult validateTemplate(TemplateValidationCommand command) {
            return TemplateValidationResult.blocked(List.of(
                    new TemplateViolation("MISSING_PLUGIN", "canvas-plugin-coupon is required")));
        }
    }

    private static final class DraftCreator {
        private boolean created;

        private void createDraft() {
            this.created = true;
        }
    }
}
