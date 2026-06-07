package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

@Service
public class LlmBiReportPlanner implements BiReportPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 5L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmBiReportPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public BiReportPlanningResult plan(BiReportPlanningContext context) {
        BiReportRequest request = context.request();
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
                "bi-report-agent")).block();
        if (result == null) {
            throw new IllegalStateException("AI report planner returned no result");
        }
        return new BiReportPlanningResult(
                result.status(),
                result.fallbackUsed(),
                objectMapper.convertValue(result.output(), BiReportPlan.class));
    }

    private com.fasterxml.jackson.databind.JsonNode variables(BiReportPlanningContext context) {
        BiReportRequest request = context.request();
        var variables = objectMapper.createObjectNode();
        variables.put("reportType", value(request.reportType()));
        variables.put("title", value(request.title()));
        variables.set("sections", objectMapper.valueToTree(context.sections()));
        variables.set("datasets", objectMapper.valueToTree(context.datasets()));
        return variables;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
