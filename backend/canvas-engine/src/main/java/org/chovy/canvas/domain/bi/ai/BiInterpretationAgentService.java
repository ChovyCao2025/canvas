package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BiInterpretationAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiInterpretationPlanner planner;

    public BiInterpretationAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                        BiInterpretationPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    public BiInterpretationResponse interpret(BiInterpretationRequest request, BiQueryContext context) {
        if (request == null) {
            throw new IllegalArgumentException("interpretation request is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        BiDatasetSpec dataset = semanticValidator.validateQuery(request.query(), scopedContext.tenantId());
        semanticValidator.validateResultDataset(request.result(), dataset.datasetKey(), "result");
        BiInterpretationPlanningResult planning = planner.plan(new BiInterpretationPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                List.of(dataset),
                request.query(),
                request.result()));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("interpretation planner did not return a plan");
        }
        return new BiInterpretationResponse(
                planning.status(),
                planning.fallbackUsed(),
                textOr(planning.plan().summary(), "No interpretation generated."),
                planning.plan().keyFindings(),
                planning.plan().recommendations());
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
