package org.chovy.canvas.domain.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.dal.dataobject.MarketingContentTemplateDO;
import org.chovy.canvas.dal.dataobject.MarketingContentTemplateVersionDO;
import org.chovy.canvas.dal.mapper.MarketingContentTemplateMapper;
import org.chovy.canvas.dal.mapper.MarketingContentTemplateVersionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentTemplateServiceTest {

    @Test
    void saveExtractsVariablesAndWritesVersion() {
        MarketingContentTemplateMapper templateMapper = mock(MarketingContentTemplateMapper.class);
        MarketingContentTemplateVersionMapper versionMapper = mock(MarketingContentTemplateVersionMapper.class);
        ContentTemplateService service = new ContentTemplateService(templateMapper, versionMapper, new ObjectMapper());

        ContentTemplateService.TemplateView view = service.save(operator(), new ContentTemplateService.TemplateCommand(
                " Promo Email ",
                "Promo Email",
                "email",
                "Hi {{firstName}}",
                "<h1>{{headline}}</h1><p>{{offer}}</p>",
                "{\"blocks\":[{\"type\":\"hero\"}]}",
                "[\"hero-video\"]",
                "draft",
                "initial",
                "operator-1"));

        assertThat(view.templateKey()).isEqualTo("promo-email");
        assertThat(view.channel()).isEqualTo("EMAIL");
        assertThat(view.variables()).containsExactly("firstName", "headline", "offer");
        verify(templateMapper).insert(argThat((MarketingContentTemplateDO row) ->
                row.getTenantId().equals(8L)
                        && row.getTemplateKey().equals("promo-email")
                        && row.getVariablesJson().equals("[\"firstName\",\"headline\",\"offer\"]")
                        && row.getStatus().equals("DRAFT")));
        verify(versionMapper).insert(argThat((MarketingContentTemplateVersionDO row) ->
                row.getTenantId().equals(8L)
                        && row.getTemplateKey().equals("promo-email")
                        && row.getVersionNo().equals(1)
                        && row.getVariablesJson().equals("[\"firstName\",\"headline\",\"offer\"]")));
    }

    @Test
    void previewRendersKnownVariablesAndReportsMissing() {
        MarketingContentTemplateMapper templateMapper = mock(MarketingContentTemplateMapper.class);
        ContentTemplateService service = new ContentTemplateService(
                templateMapper, mock(MarketingContentTemplateVersionMapper.class), new ObjectMapper());
        when(templateMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(
                ContentTemplateServiceTestRows.template("welcome_email", "EMAIL", "Hi {{ firstName }}", "Use {{couponCode}}"));

        ContentTemplateService.PreviewResult result = service.preview(
                operator(), "welcome_email", Map.of("firstName", "Alice"));

        assertThat(result.renderedSubject()).isEqualTo("Hi Alice");
        assertThat(result.renderedBody()).isEqualTo("Use {{couponCode}}");
        assertThat(result.missingVariables()).containsExactly("couponCode");
    }

    @Test
    void saveRejectsUnsupportedChannelAndInvalidJson() {
        ContentTemplateService service = new ContentTemplateService(
                mock(MarketingContentTemplateMapper.class),
                mock(MarketingContentTemplateVersionMapper.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.save(operator(), new ContentTemplateService.TemplateCommand(
                "bad", "Bad", "FAX", null, "Hi", "{}", "[]", "DRAFT", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported template channel FAX");

        assertThatThrownBy(() -> service.save(operator(), new ContentTemplateService.TemplateCommand(
                "bad", "Bad", "EMAIL", null, "Hi", "{bad", "[]", "DRAFT", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("designJson must be valid JSON");

        assertThatThrownBy(() -> service.save(operator(), new ContentTemplateService.TemplateCommand(
                "bad", "Bad", "EMAIL", null, "Hi", "[]", "[]", "DRAFT", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("designJson must be a JSON object");

        assertThatThrownBy(() -> service.save(operator(), new ContentTemplateService.TemplateCommand(
                "bad", "Bad", "EMAIL", null, "Hi", "{}", "{}", "DRAFT", null, "operator-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assetRefsJson must be a JSON array");
    }

    private TenantContext operator() {
        return new TenantContext(8L, RoleNames.OPERATOR, "operator-1");
    }
}
