package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

@Service
public class LlmBiInsightPlanner implements BiInsightPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 7L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmBiInsightPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public BiInsightPlanningResult plan(BiInsightPlanningContext context) {
        BiInsightRequest request = context.request();
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
                "bi-insight-agent")).block();
        if (result == null) {
            throw new IllegalStateException("AI insight planner returned no result");
        }
        return new BiInsightPlanningResult(
                result.status(),
                result.fallbackUsed(),
                objectMapper.convertValue(result.output(), BiInsightPlan.class));
    }

    private com.fasterxml.jackson.databind.JsonNode variables(BiInsightPlanningContext context) {
        BiInsightRequest request = context.request();
        var variables = objectMapper.createObjectNode();
        variables.put("question", value(request.question()));
        variables.set("dataset", objectMapper.valueToTree(context.dataset()));
        variables.set("query", objectMapper.valueToTree(context.query()));
        variables.set("currentResult", objectMapper.valueToTree(context.currentResult()));
        variables.set("baselineResult", objectMapper.valueToTree(context.baselineResult()));
        return variables;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
