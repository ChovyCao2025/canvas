package org.chovy.canvas.canvas.api.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * 封装TemplateValidationContractTest相关的业务逻辑。
 */
class TemplateValidationContractTest {

    /**
     * 处理validationBlocksDraftCreationWhenRequiredPluginIsMissing。
     */
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

    /**
     * 封装BlockingTemplateValidationPort相关的业务逻辑。
     */
    private static final class BlockingTemplateValidationPort implements TemplateValidationPort {

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
     * 封装DraftCreator相关的业务逻辑。
     */
    private static final class DraftCreator {

        /**
         * 保存created。
         */
        private boolean created;

        /**
         * 创建Draft。
         */
        private void createDraft() {
            this.created = true;
        }
    }
}
