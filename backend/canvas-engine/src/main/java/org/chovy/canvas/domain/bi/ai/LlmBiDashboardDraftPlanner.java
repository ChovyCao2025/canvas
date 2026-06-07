package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

@Service
public class LlmBiDashboardDraftPlanner implements BiDashboardDraftPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 6L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmBiDashboardDraftPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public BiDashboardDraftPlanningResult plan(BiDashboardDraftPlanningContext context) {
        BiDashboardDraftRequest request = context.request();
        AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                request.providerId(),
                request.templateId() == null ? DEFAULT_TEMPLATE_ID : request.templateId(),
                request.modelKey(),
                null,
                variables(context),
                request.params(),
                request.timeoutMs(),
                null,
                null,
                "bi-dashboard-draft-agent")).block();
        if (result == null) {
            throw new IllegalStateException("AI dashboard draft planner returned no result");
        }
        return new BiDashboardDraftPlanningResult(
                result.status(),
                result.fallbackUsed(),
                objectMapper.convertValue(result.output(), BiDashboardDraftPlan.class));
    }

    private com.fasterxml.jackson.databind.JsonNode variables(BiDashboardDraftPlanningContext context) {
        BiDashboardDraftRequest request = context.request();
        var variables = objectMapper.createObjectNode();
        variables.put("prompt", value(request.prompt()));
        variables.put("requestedDatasetKey", value(request.datasetKey()));
        variables.set("datasets", objectMapper.valueToTree(context.datasets()));
        return variables;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
