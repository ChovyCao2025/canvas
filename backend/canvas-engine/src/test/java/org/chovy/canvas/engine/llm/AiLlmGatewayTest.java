package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiLlmGatewayTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mockProviderReturnsTemplateDefaultsAndAuditsUsage() {
        AiUsageAuditService auditService = new AiUsageAuditService();
        AiLlmGateway gateway = gateway(auditService, List.of());

        AiLlmGateway.AiLlmResult result = gateway.evaluate(0L, request(null, 1L, null)).block();

        assertThat(result.status()).isEqualTo(AiLlmGateway.STATUS_SUCCESS);
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.output().path("text").asText()).isEqualTo("Your exclusive benefit is ready.");
        assertThat(auditService.recent()).hasSize(1);
        assertThat(auditService.recent().get(0).nodeId()).isEqualTo("ai-1");
    }

    @Test
    void compatibleProviderAcceptsSchemaMatchedJson() {
        AiUsageAuditService auditService = new AiUsageAuditService();
        AiProviderModelRegistryService registry = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView provider = registry.createProvider(0L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "test-openai",
                        "Test OpenAI",
                        LlmProviderType.OPENAI_COMPATIBLE,
                        "https://ai.example.test/v1",
                        "secret",
                        true,
                        List.of(new AiProviderModelRegistryService.ModelCreateRequest(
                                "gpt-test", "GPT Test", "TEXT_JSON", 128000, true))));
        AiLlmGateway gateway = gateway(registry, auditService, List.of(new StaticClient(
                objectMapper.createObjectNode().put("text", "hello").put("tone", "direct"))));

        AiLlmGateway.AiLlmResult result = gateway.evaluate(0L, request(provider.id(), 1L, "gpt-test")).block();

        assertThat(result.status()).isEqualTo(AiLlmGateway.STATUS_SUCCESS);
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.output().path("text").asText()).isEqualTo("hello");
        assertThat(auditService.recent().get(0).modelKey()).isEqualTo("gpt-test");
    }

    @Test
    void invalidProviderOutputFallsBackToTemplateDefaults() {
        AiUsageAuditService auditService = new AiUsageAuditService();
        AiProviderModelRegistryService registry = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView provider = registry.createProvider(0L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "bad-openai",
                        "Bad OpenAI",
                        LlmProviderType.OPENAI_COMPATIBLE,
                        "https://ai.example.test/v1",
                        "secret",
                        true,
                        List.of(new AiProviderModelRegistryService.ModelCreateRequest(
                                "gpt-test", "GPT Test", "TEXT_JSON", 128000, true))));
        AiLlmGateway gateway = gateway(registry, auditService,
                List.of(new StaticClient(objectMapper.createObjectNode().put("text", "missing tone"))));

        AiLlmGateway.AiLlmResult result = gateway.evaluate(0L, request(provider.id(), 1L, "gpt-test")).block();

        assertThat(result.status()).isEqualTo(AiLlmGateway.STATUS_SCHEMA_MISMATCH);
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.output().path("tone").asText()).isEqualTo("warm");
        assertThat(auditService.recent().get(0).fallbackUsed()).isTrue();
    }

    @Test
    void disabledProviderFallsBackWithoutCallingClient() {
        AiUsageAuditService auditService = new AiUsageAuditService();
        AiProviderModelRegistryService registry = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView provider = registry.createProvider(0L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "disabled-openai",
                        "Disabled OpenAI",
                        LlmProviderType.OPENAI_COMPATIBLE,
                        "https://ai.example.test/v1",
                        "secret",
                        false,
                        List.of()));
        AiLlmGateway gateway = gateway(registry, auditService,
                List.of(new FailingClient()));

        AiLlmGateway.AiLlmResult result = gateway.evaluate(0L, request(provider.id(), 1L, null)).block();

        assertThat(result.status()).isEqualTo(AiLlmGateway.STATUS_PROVIDER_DISABLED);
        assertThat(result.fallbackUsed()).isTrue();
        assertThat(result.output().path("text").asText()).isEqualTo("Your exclusive benefit is ready.");
    }

    private AiLlmGateway gateway(AiUsageAuditService auditService, List<LlmClient> clients) {
        return gateway(new AiProviderModelRegistryService(), auditService, clients);
    }

    private AiLlmGateway gateway(AiProviderModelRegistryService registry,
                                 AiUsageAuditService auditService,
                                 List<LlmClient> clients) {
        return new AiLlmGateway(
                registry,
                new AiPromptTemplateService(objectMapper),
                auditService,
                objectMapper,
                clients);
    }

    private AiLlmGateway.AiLlmRequest request(Long providerId, Long templateId, String modelKey) {
        return new AiLlmGateway.AiLlmRequest(
                providerId,
                templateId,
                modelKey,
                null,
                objectMapper.valueToTree(Map.of(
                        "userProfile", Map.of("name", "Ada"),
                        "productInfo", Map.of("name", "Canvas"),
                        "channelType", "email")),
                Map.of("temperature", 0.2),
                1000,
                10L,
                "exec-1",
                "ai-1");
    }

    private record StaticClient(JsonNode output) implements LlmClient {
        @Override
        public boolean supports(String providerType) {
            return LlmProviderType.OPENAI_COMPATIBLE.equals(providerType);
        }

        @Override
        public Mono<LlmResponse> complete(LlmRequest request) {
            return Mono.just(new LlmResponse(output.toString(), output, 10, 5));
        }
    }

    private static class FailingClient implements LlmClient {
        @Override
        public boolean supports(String providerType) {
            return true;
        }

        @Override
        public Mono<LlmResponse> complete(LlmRequest request) {
            return Mono.error(new AssertionError("client should not be called"));
        }
    }
}
