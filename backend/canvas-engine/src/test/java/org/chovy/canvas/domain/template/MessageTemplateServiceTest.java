package org.chovy.canvas.domain.template;

import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageTemplateServiceTest {

    @Test
    void migrationCreatesTenantScopedTemplateTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V268__message_template_center.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS message_template")
                .contains("tenant_id BIGINT NOT NULL")
                .contains("template_code VARCHAR(128) NOT NULL")
                .contains("channel VARCHAR(64) NOT NULL")
                .contains("body TEXT NOT NULL")
                .contains("variable_schema_json JSON NOT NULL")
                .contains("status VARCHAR(32) NOT NULL")
                .contains("created_by VARCHAR(128) NOT NULL")
                .contains("UNIQUE KEY uk_message_template_code");
    }

    @Test
    void createExtractsVariablesAndPersistsTenantScopedTemplate() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);
        TenantContext tenant = new TenantContext(8L, RoleNames.OPERATOR, "operator-1");

        MessageTemplateService.Template saved = service.create(tenant, new MessageTemplateService.TemplateDraft(
                "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}, your level is {{tier}}."));

        assertThat(saved.variables()).containsExactly("firstName", "tier");
        assertThat(saved.createdBy()).isEqualTo("operator-1");
        verify(repository).insert(argThat(template ->
                template.tenantId().equals(8L)
                        && template.templateCode().equals("welcome_sms")
                        && template.variables().equals(List.of("firstName", "tier"))
                        && template.status().equals("DRAFT")));
    }

    @Test
    void searchDelegatesTenantAndNormalizedFilters() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);
        TenantContext tenant = new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
        when(repository.search(8L, "welcome", "SMS")).thenReturn(List.of(new MessageTemplateService.Template(
                8L, "welcome_sms", "Welcome SMS", "SMS", "Hi {{firstName}}", List.of("firstName"), "DRAFT", "operator-1")));

        List<MessageTemplateService.Template> result = service.search(tenant, " welcome ", "sms");

        assertThat(result).extracting(MessageTemplateService.Template::templateCode).containsExactly("welcome_sms");
    }

    @Test
    void previewReplacesKnownVariablesAndReportsMissingVariables() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);
        TenantContext tenant = new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
        when(repository.get(8L, "welcome_sms")).thenReturn(new MessageTemplateService.Template(
                8L,
                "welcome_sms",
                "Welcome SMS",
                "SMS",
                "Hi {{ firstName }}, use {{couponCode}}.",
                List.of("firstName", "couponCode"),
                "DRAFT",
                "operator-1"));

        MessageTemplateService.PreviewResult result = service.preview(tenant, "welcome_sms", Map.of("firstName", "Alice"));

        assertThat(result.renderedBody()).isEqualTo("Hi Alice, use {{couponCode}}.");
        assertThat(result.missingVariables()).containsExactly("couponCode");
    }

    @Test
    void createRejectsUnsupportedChannel() {
        MessageTemplateService.TemplateRepository repository = mock(MessageTemplateService.TemplateRepository.class);
        MessageTemplateService service = new MessageTemplateService(repository);

        assertThatThrownBy(() -> service.create(
                new TenantContext(8L, RoleNames.OPERATOR, "operator-1"),
                new MessageTemplateService.TemplateDraft("bad", "Bad", "FAX", "Hi")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported template channel FAX");
    }
}
