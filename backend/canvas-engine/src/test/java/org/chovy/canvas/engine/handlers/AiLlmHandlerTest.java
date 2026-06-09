package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.chovy.canvas.engine.llm.AiUsageAuditService;
import org.chovy.canvas.engine.llm.LlmClient;
import org.chovy.canvas.engine.llm.LlmProviderType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiLlmHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void executesMockProviderAndWritesPrefixedOutputs() {
        AiUsageAuditService auditService = new AiUsageAuditService();
        AiLlmHandler handler = handler(auditService);

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "ai-1",
                MapFieldKeys.TEMPLATE_ID, 1L,
                MapFieldKeys.NEXT_NODE_ID, "next-1",
                "outputPrefix", "ai",
                MapFieldKeys.VARIABLES, Map.of("channelType", "email")
        ), ctx()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.routes()).containsEntry("success", "next-1");
        assertThat(result.output()).containsEntry("ai.ai_status", AiLlmGateway.STATUS_SUCCESS);
        assertThat(result.output()).containsEntry("ai.ai_fallback_used", false);
        assertThat(result.output()).containsEntry("ai.text", "Your exclusive benefit is ready.");
        assertThat(auditService.recent()).hasSize(1);
        assertThat(auditService.recent().get(0).executionId()).isEqualTo("exec-1");
    }

    @Test
    void rejectsMissingTemplateId() {
        NodeResult result = handler(new AiUsageAuditService())
                .executeAsync(Map.of(MapFieldKeys.NODE_ID_INTERNAL, "ai-1"), ctx())
                .block();

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("templateId");
    }

    @Test
    void fatalTemplateErrorsFailNodeWithoutFailBranchRouting() {
        NodeResult result = handler(new AiUsageAuditService())
                .executeAsync(Map.of(
                        MapFieldKeys.NODE_ID_INTERNAL, "ai-1",
                        MapFieldKeys.TEMPLATE_ID, 999L,
                        MapFieldKeys.FAIL_NODE_ID, "fail-1"
                ), ctx())
                .block();

        assertThat(result.success()).isFalse();
        assertThat(result.routes()).isEmpty();
        assertThat(result.output()).containsEntry("ai_status", "FAILED");
    }

    @Test
    void forwardsBoundedMaxTokensAndSchemaOverrideToGatewayClient() {
        AiProviderModelRegistryService registry = new AiProviderModelRegistryService();
        AiProviderModelRegistryService.ProviderView provider = registry.createProvider(7L,
                new AiProviderModelRegistryService.ProviderCreateRequest(
                        "openai",
                        "OpenAI",
                        LlmProviderType.OPENAI_COMPATIBLE,
                        "https://api.example.test/v1",
                        "sk-test",
                        true,
                        List.of(new AiProviderModelRegistryService.ModelCreateRequest(
                                "gpt-test", "GPT Test", "TEXT_JSON", 128000, true))));
        CapturingClient client = new CapturingClient();
        AiLlmHandler handler = new AiLlmHandler(new AiLlmGateway(
                registry,
                new AiPromptTemplateService(objectMapper),
                new AiUsageAuditService(),
                objectMapper,
                List.of(client)), objectMapper);

        NodeResult result = handler.executeAsync(Map.of(
                MapFieldKeys.NODE_ID_INTERNAL, "ai-1",
                MapFieldKeys.TEMPLATE_ID, 1L,
                "providerId", provider.id(),
                "modelKey", "gpt-test",
                "maxTokens", 333,
                "schemaOverride", "{\"type\":\"object\",\"required\":[\"summary\"]}"
        ), ctx()).block();

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsEntry("summary", "schema override accepted");
        assertThat(client.request.get().params())
                .containsEntry("max_tokens", 333)
                .doesNotContainKey("maxTokens");
        assertThat(client.request.get().outputSchema().path("required").path(0).asText()).isEqualTo("summary");
    }

    @Test
    void explicitProviderConfigErrorFailsNodeWithoutSuccessRoute() {
        NodeResult result = handler(new AiUsageAuditService())
                .executeAsync(Map.of(
                        MapFieldKeys.NODE_ID_INTERNAL, "ai-1",
                        MapFieldKeys.TEMPLATE_ID, 1L,
                        "providerId", 404L,
                        MapFieldKeys.NEXT_NODE_ID, "next-1"
                ), ctx())
                .block();

        assertThat(result.success()).isFalse();
        assertThat(result.routes()).isEmpty();
        assertThat(result.output()).containsEntry("ai_status", AiLlmGateway.STATUS_CONFIG_ERROR);
    }

    private AiLlmHandler handler(AiUsageAuditService auditService) {
        return new AiLlmHandler(new AiLlmGateway(
                new AiProviderModelRegistryService(),
                new AiPromptTemplateService(objectMapper),
                auditService,
                objectMapper,
                List.of()), objectMapper);
    }

    private static ExecutionContext ctx() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setTenantId(7L);
        ctx.setExecutionId("exec-1");
        ctx.setCanvasId(10L);
        ctx.setVersionId(20L);
        ctx.setUserId("user-1");
        ctx.setTriggerType("DIRECT_CALL");
        ctx.setTriggerPayload(Map.of(
                "userProfile", Map.of("name", "Ada"),
                "productInfo", Map.of("name", "Canvas")));
        return ctx;
    }

    private class CapturingClient implements LlmClient {
        private final AtomicReference<LlmRequest> request = new AtomicReference<>();

        @Override
        public boolean supports(String providerType) {
            return LlmProviderType.OPENAI_COMPATIBLE.equals(providerType);
        }

        @Override
        public Mono<LlmResponse> complete(LlmRequest request) {
            this.request.set(request);
            return Mono.just(new LlmResponse(
                    "{\"summary\":\"schema override accepted\"}",
                    objectMapper.createObjectNode().put("summary", "schema override accepted"),
                    10,
                    5));
        }
    }
}
