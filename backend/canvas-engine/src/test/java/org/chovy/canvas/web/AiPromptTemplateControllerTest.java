package org.chovy.canvas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.ai.AiPromptEvaluationService;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptTemplateControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void renderAndEvaluatePromptThroughController() throws Exception {
        AiPromptTemplateService templateService = new AiPromptTemplateService(objectMapper);
        AiPromptEvaluationService evaluationService = new AiPromptEvaluationService(
                new AiProviderModelRegistryService(),
                templateService);
        AiPromptTemplateController controller = new AiPromptTemplateController(templateService, evaluationService);

        StepVerifier.create(controller.render(new AiPromptTemplateService.RenderRequest(
                        1L,
                        "Hello ${user.name} on {{channel}}",
                        objectMapper.readTree("""
                                {"user":{"name":"Mina"},"channel":"email"}
                                """))))
                .assertNext(response -> assertThat(response.getData().renderedPrompt()).isEqualTo("Hello Mina on email"))
                .verifyComplete();

        StepVerifier.create(controller.evaluate(new AiPromptEvaluationService.EvaluationRequest(
                        1L,
                        1L,
                        null,
                        null,
                        objectMapper.readTree("""
                                {"channelType":"email","userProfile":{"name":"Mina"},"productInfo":{"name":"coupon"}}
                                """),
                        objectMapper.readTree("""
                                {"text":"Coupon ready.","tone":"warm"}
                                """))))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().status()).isEqualTo("SUCCESS");
                    assertThat(response.getData().renderedPrompt()).contains("Mina");
                })
                .verifyComplete();

        StepVerifier.create(controller.audits())
                .assertNext(response -> assertThat(response.getData()).hasSize(1))
                .verifyComplete();
    }

    @Test
    void createTemplateReturnsTenantScopedTemplate() {
        AiPromptTemplateService templateService = new AiPromptTemplateService(objectMapper);
        AiPromptEvaluationService evaluationService = new AiPromptEvaluationService(
                new AiProviderModelRegistryService(),
                templateService);
        AiPromptTemplateController controller = new AiPromptTemplateController(templateService, evaluationService);

        StepVerifier.create(controller.create(new AiPromptTemplateService.TemplateCreateRequest(
                        "Subject Line",
                        "subject",
                        "Subject for ${product}",
                        objectMapper.createObjectNode().put("type", "object"),
                        objectMapper.createObjectNode().put("subject", "Default subject"),
                        true)))
                .assertNext(response -> {
                    assertThat(response.getCode()).isEqualTo(0);
                    assertThat(response.getData().tenantId()).isEqualTo(0L);
                    assertThat(response.getData().name()).isEqualTo("Subject Line");
                })
                .verifyComplete();
    }

    @Test
    void disableTemplateHidesTenantScopedTemplate() {
        AiPromptTemplateService templateService = new AiPromptTemplateService(objectMapper);
        AiPromptEvaluationService evaluationService = new AiPromptEvaluationService(
                new AiProviderModelRegistryService(),
                templateService);
        AiPromptTemplateController controller = new AiPromptTemplateController(templateService, evaluationService);
        AiPromptTemplateService.TemplateDetail created = templateService.createTemplate(0L,
                new AiPromptTemplateService.TemplateCreateRequest(
                        "Body Copy",
                        "copy",
                        "Copy for ${product}",
                        objectMapper.createObjectNode().put("type", "object"),
                        objectMapper.createObjectNode().put("text", "Default copy"),
                        true));

        StepVerifier.create(controller.disable(created.id()))
                .assertNext(response -> assertThat(response.getCode()).isEqualTo(0))
                .verifyComplete();

        assertThat(templateService.listTemplates(0L))
                .extracting(AiPromptTemplateService.TemplateSummary::id)
                .doesNotContain(created.id());
    }
}
