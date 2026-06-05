package org.chovy.canvas.engine.template;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRenderServiceTest {

    @Test
    void rendersVariablesEscapesHtmlAndReportsMissingFields() {
        TemplateRenderService service = new TemplateRenderService(1000);

        TemplateRenderService.RenderResult result = service.render("Hi {{profile.name}}, {{missing}}", Map.of(
                "profile", Map.of("name", "<Ada>")));

        assertThat(result.output()).isEqualTo("Hi &lt;Ada&gt;, ");
        assertThat(result.errors())
                .extracting(TemplateRenderService.RenderError::code)
                .contains("MISSING_VARIABLE");
    }

    @Test
    void supportsDateFormattingConditionalsAndRepeatedLists() {
        TemplateRenderService service = new TemplateRenderService(1000);

        TemplateRenderService.RenderResult result = service.render(
                "{{#if paid}}paid{{/if}} {{formatDate createdAt 'yyyy-MM-dd'}} {{#each items}}{{name}};{{/each}}",
                Map.of("paid", true, "createdAt", "2026-06-03T10:00:00Z",
                        "items", List.of(Map.of("name", "A"), Map.of("name", "B"))));

        assertThat(result.output()).contains("paid").contains("2026-06-03").contains("A;B;");
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void limitsRenderedOutputLength() {
        TemplateRenderService service = new TemplateRenderService(4);

        TemplateRenderService.RenderResult result = service.render("abcdef", Map.of());

        assertThat(result.output()).isEqualTo("abcd");
        assertThat(result.errors())
                .extracting(TemplateRenderService.RenderError::code)
                .contains("MAX_RENDERED_LENGTH");
    }

    @Test
    void rejectsNonPositiveMaxLength() {
        assertThatThrownBy(() -> new TemplateRenderService(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRenderedLength");
    }
}
