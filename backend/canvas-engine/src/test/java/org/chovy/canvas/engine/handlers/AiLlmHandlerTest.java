package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.domain.ai.AiPromptTemplateService;
import org.chovy.canvas.domain.ai.AiProviderModelRegistryService;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.chovy.canvas.engine.llm.AiUsageAuditService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    void routesFatalTemplateErrorsToFailBranch() {
        NodeResult result = handler(new AiUsageAuditService())
                .executeAsync(Map.of(
                        MapFieldKeys.NODE_ID_INTERNAL, "ai-1",
                        MapFieldKeys.TEMPLATE_ID, 999L,
                        MapFieldKeys.FAIL_NODE_ID, "fail-1"
                ), ctx())
                .block();

        assertThat(result.routes()).containsEntry("fail", "fail-1");
        assertThat(result.output()).containsEntry("ai_status", "FAILED");
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
}
