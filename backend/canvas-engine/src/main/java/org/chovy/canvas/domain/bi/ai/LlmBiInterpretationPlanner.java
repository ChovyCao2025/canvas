package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

@Service
public class LlmBiInterpretationPlanner implements BiInterpretationPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 4L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmBiInterpretationPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public BiInterpretationPlanningResult plan(BiInterpretationPlanningContext context) {
        BiInterpretationRequest request = context.request();
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
                "bi-interpretation-agent")).block();
        if (result == null) {
            throw new IllegalStateException("AI interpretation planner returned no result");
        }
        return new BiInterpretationPlanningResult(
                result.status(),
                result.fallbackUsed(),
                objectMapper.convertValue(result.output(), BiInterpretationPlan.class));
    }

    private com.fasterxml.jackson.databind.JsonNode variables(BiInterpretationPlanningContext context) {
        BiInterpretationRequest request = context.request();
        var variables = objectMapper.createObjectNode();
        variables.put("question", value(request.question()));
        variables.put("subjectType", value(request.subjectType()));
        variables.put("subjectKey", value(request.subjectKey()));
        variables.set("query", objectMapper.valueToTree(context.query()));
        variables.set("result", objectMapper.valueToTree(context.result()));
        variables.set("datasets", objectMapper.valueToTree(context.datasets()));
        return variables;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
