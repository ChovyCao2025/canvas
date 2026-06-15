package org.chovy.canvas.marketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MessageTemplateFacade;
import org.junit.jupiter.api.Test;

class MessageTemplateApplicationServiceTest {

    @Test
    void createSearchAndPreviewAreTenantScopedAndNormalizeLegacyFields() {
        MessageTemplateFacade service = new MessageTemplateApplicationService();

        MessageTemplateFacade.TemplateView template = service.create(8L, "operator-1",
                new MessageTemplateFacade.TemplateDraft(
                        " Welcome_SMS ",
                        " Welcome SMS ",
                        "sms",
                        "Hi {{ firstName }}, use {{couponCode}}."));

        assertThat(template.tenantId()).isEqualTo(8L);
        assertThat(template.templateCode()).isEqualTo("welcome_sms");
        assertThat(template.displayName()).isEqualTo("Welcome SMS");
        assertThat(template.channel()).isEqualTo("SMS");
        assertThat(template.variables()).containsExactly("firstName", "couponCode");
        assertThat(template.status()).isEqualTo("DRAFT");
        assertThat(template.createdBy()).isEqualTo("operator-1");

        assertThat(service.search(8L, " welcome ", "sms"))
                .extracting(MessageTemplateFacade.TemplateView::templateCode)
                .containsExactly("welcome_sms");
        assertThat(service.search(9L, null, null)).isEmpty();

        MessageTemplateFacade.PreviewView preview = service.preview(8L, "WELCOME_SMS",
                Map.of("firstName", "Ada"));

        assertThat(preview.renderedBody()).isEqualTo("Hi Ada, use {{couponCode}}.");
        assertThat(preview.missingVariables()).containsExactly("couponCode");
    }

    @Test
    void rejectsUnsupportedChannelsAndMissingTemplatesWithLegacyMessages() {
        MessageTemplateFacade service = new MessageTemplateApplicationService();

        assertThatThrownBy(() -> service.create(8L, "operator-1",
                new MessageTemplateFacade.TemplateDraft("bad", "Bad", "fax", "Hi")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported template channel FAX");

        assertThatThrownBy(() -> service.preview(8L, "missing", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template not found: missing");
    }

    @Test
    void ignoresDuplicateVariablesInDeclarationOrder() {
        MessageTemplateFacade service = new MessageTemplateApplicationService();

        MessageTemplateFacade.TemplateView template = service.create(8L, null,
                new MessageTemplateFacade.TemplateDraft(
                        "otp_sms",
                        "OTP",
                        "SMS",
                        "{{code}} expires soon. Code: {{ code }} for {{name}}"));

        assertThat(template.variables()).containsExactly("code", "name");
        assertThat(template.createdBy()).isEqualTo("system");
    }
}
