package org.chovy.canvas.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.chovy.canvas.platform.domain.AiCatalog;
import org.junit.jupiter.api.Test;

/**
 * 覆盖 AI 应用服务的决策、预测、提示词和供应方流程。
 */
class AiApplicationServiceTest {

    /**
     * 验证决策和预测流程按租户隔离并遵守数量上限。
     */
    @Test
    void decisionAndPredictionFlowsAreTenantScopedAndLimitBounded() {
        AiApplicationService service = new AiApplicationService(new AiCatalog());

        Map<String, Object> tenantSevenRun = service.recomputeDecision(7L,
                Map.of("decisionScope", "DAILY_MARKETING"), "operator-1");
        service.recomputeDecision(8L, Map.of("decisionScope", "RETENTION"), "operator-2");

        assertThat(tenantSevenRun)
                .containsEntry("tenantId", 7L)
                .containsEntry("status", "COMPLETED")
                .containsEntry("triggeredBy", "operator-1");
        assertThat(service.latestDecisionRun(7L, "DAILY_MARKETING")).containsEntry("runId", "ai-decision-7-1");
        assertThat(service.latestDecisionRun(8L, "RETENTION")).containsEntry("runId", "ai-decision-8-1");

        List<Map<String, Object>> recommendations = service.decisionRecommendations(7L, null, null, null, 500);
        assertThat(recommendations).hasSize(2)
                .allSatisfy(recommendation -> assertThat(recommendation).containsEntry("tenantId", 7L));

        Map<String, Object> feedback = service.recordDecisionFeedback(7L, 7001L,
                Map.of("decision", "accepted"), "operator-1");
        assertThat(feedback)
                .containsEntry("recommendationId", 7001L)
                .containsEntry("feedbackStatus", "RECORDED")
                .containsEntry("updatedBy", "operator-1");

        Map<String, Object> predictionRun = service.recomputePrediction(7L, Map.of("force", true));
        assertThat(predictionRun)
                .containsEntry("runId", "ai-prediction-7-1")
                .containsEntry("status", "COMPLETED");
        assertThat(service.predictionReadiness(7L)).containsEntry("ready", true);
        assertThat(service.topRiskUsers(7L, 500)).hasSize(3);
    }

    /**
     * 验证提示词模板可渲染、评估、审计，并拒绝缺失名称。
     */
    @Test
    void promptTemplatesRenderEvaluateAuditAndRejectMissingNames() {
        AiApplicationService service = new AiApplicationService(new AiCatalog());

        Map<String, Object> template = service.createPromptTemplate(7L, Map.of(
                "name", "Retention prompt",
                "template", "Hello {{name}}",
                "scenario", "RETENTION"), "operator-1");

        assertThat(template)
                .containsEntry("id", 7001L)
                .containsEntry("status", "ENABLED")
                .containsEntry("createdBy", "operator-1");
        assertThat(service.promptTemplates(7L)).extracting(item -> item.get("id")).containsExactly(7001L);
        assertThat(service.promptTemplate(7L, 7001L)).containsEntry("name", "Retention prompt");

        Map<String, Object> updated = service.updatePromptTemplate(7L, 7001L,
                Map.of("name", "Retention prompt v2", "template", "Hi {{name}}"), "operator-2");
        assertThat(updated).containsEntry("updatedBy", "operator-2").containsEntry("name", "Retention prompt v2");
        assertThat(service.renderPromptTemplate(7L, Map.of("templateId", 7001L, "variables", Map.of("name", "Ada"))))
                .containsEntry("renderedPrompt", "Hi Ada");
        assertThat(service.evaluatePromptTemplate(7L, Map.of("templateId", 7001L, "sampleInput", "retention")))
                .containsEntry("status", "PASSED");
        assertThat(service.evaluationAudits(7L)).hasSize(1);
        assertThat(service.disablePromptTemplate(7L, 7001L, "operator-3")).containsEntry("status", "DISABLED");

        assertThatThrownBy(() -> service.createPromptTemplate(7L, Map.of("template", "missing name"), "operator-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    /**
     * 验证供应方按租户隔离、可更新禁用，并暴露模型列表。
     */
    @Test
    void providersAreTenantScopedMutableAndExposeModels() {
        AiApplicationService service = new AiApplicationService(new AiCatalog());

        Map<String, Object> provider = service.createProvider(7L,
                Map.of("name", "OpenAI", "providerKey", "openai"), "operator-1");

        assertThat(provider)
                .containsEntry("id", 7001L)
                .containsEntry("status", "ENABLED")
                .containsEntry("createdBy", "operator-1");
        assertThat(service.providers(7L)).extracting(item -> item.get("id")).containsExactly(7001L);
        assertThat(service.provider(7L, 7001L)).containsEntry("providerKey", "openai");

        Map<String, Object> updated = service.updateProvider(7L, 7001L,
                Map.of("displayName", "Primary AI"), "operator-2");
        assertThat(updated).containsEntry("displayName", "Primary AI").containsEntry("updatedBy", "operator-2");
        assertThat(service.providerModels(7L, 7001L)).extracting(item -> item.get("modelKey"))
                .containsExactly("gpt-4.1-mini", "gpt-4.1");
        assertThat(service.disableProvider(7L, 7001L, "operator-3")).containsEntry("status", "DISABLED");

        assertThatThrownBy(() -> service.provider(8L, 7001L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("provider not found");
    }
}
