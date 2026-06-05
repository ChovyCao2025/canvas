package org.chovy.canvas.domain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptEvaluationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void renderPromptUsesNestedVariables() throws Exception {
        AiPromptTemplateService templateService = new AiPromptTemplateService(objectMapper);

        AiPromptTemplateService.RenderResult result = templateService.render(0L,
                new AiPromptTemplateService.RenderRequest(
                        1L,
                        null,
                        objectMapper.readTree("""
                                {
                                  "channelType": "email",
                                  "userProfile": {"name": "Ada"},
                                  "productInfo": {"name": "VIP coupon"}
                                }
                                """)));

        assertThat(result.renderedPrompt())
                .isEqualTo("Create a email message for Ada about VIP coupon.");
    }

    @Test
    void evaluateAcceptsValidMockOutputAndWritesAudit() throws Exception {
        AiProviderModelRegistryService providerRegistry = new AiProviderModelRegistryService();
        AiPromptTemplateService templateService = new AiPromptTemplateService(objectMapper);
        AiPromptEvaluationService evaluationService = new AiPromptEvaluationService(providerRegistry, templateService);

        AiPromptEvaluationService.EvaluationResult result = evaluationService.evaluate(0L,
                new AiPromptEvaluationService.EvaluationRequest(
                        1L,
                        1L,
                        null,
                        null,
                        objectMapper.readTree("""
                                {"channelType":"sms","userProfile":{"name":"Lin"},"productInfo":{"name":"points"}}
                                """),
                        objectMapper.readTree("""
                                {"text":"Use your points today.","tone":"direct"}
                                """)));

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.modelKey()).isEqualTo("mock-marketing-v1");
        assertThat(result.output().path("text").asText()).isEqualTo("Use your points today.");
        assertThat(evaluationService.recentAudits())
                .extracting(AiPromptEvaluationService.EvaluationAuditEvent::status)
                .containsExactly("SUCCESS");
    }

    @Test
    void evaluateFallsBackToTemplateDefaultsWhenOutputIsInvalid() throws Exception {
        AiPromptEvaluationService evaluationService = new AiPromptEvaluationService(
                new AiProviderModelRegistryService(),
                new AiPromptTemplateService(objectMapper));

        AiPromptEvaluationService.EvaluationResult result = evaluationService.evaluate(0L,
                new AiPromptEvaluationService.EvaluationRequest(
                        1L,
                        1L,
                        null,
                        null,
                        objectMapper.createObjectNode(),
                        objectMapper.readTree("""
                                {"text":"missing tone"}
                                """)));

        assertThat(result.status()).isEqualTo("INVALID_JSON");
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.output().path("text").asText()).isEqualTo("Your exclusive benefit is ready.");
    }

    @Test
    void evaluateFallsBackWhenProviderIsDisabled() {
        AiProviderModelRegistryService providerRegistry = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView provider = providerRegistry.createProvider(0L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "disabled-ai",
                        "Disabled AI",
                        "OPENAI_COMPATIBLE",
                        "https://disabled.example.test",
                        "disabled-secret",
                        true,
                        List.of()));
        providerRegistry.disableProvider(0L, provider.id());
        AiPromptEvaluationService evaluationService = new AiPromptEvaluationService(
                providerRegistry,
                new AiPromptTemplateService(objectMapper));

        AiPromptEvaluationService.EvaluationResult result = evaluationService.evaluate(0L,
                new AiPromptEvaluationService.EvaluationRequest(
                        provider.id(),
                        1L,
                        null,
                        null,
                        objectMapper.createObjectNode(),
                        null));

        assertThat(result.status()).isEqualTo("PROVIDER_DISABLED");
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.output().path("tone").asText()).isEqualTo("warm");
    }
}
