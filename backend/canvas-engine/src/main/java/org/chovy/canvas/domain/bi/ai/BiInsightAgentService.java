package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

@Service
public class BiInsightAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiInsightPlanner planner;

    public BiInsightAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                 BiInsightPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    public BiInsightResponse inspect(BiInsightRequest request, BiQueryContext context) {
        if (request == null) {
            throw new IllegalArgumentException("insight request is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        BiDatasetSpec dataset = semanticValidator.validateQuery(request.query(), scopedContext.tenantId());
        semanticValidator.validateResultDataset(request.currentResult(), dataset.datasetKey(), "current result");
        semanticValidator.validateResultDataset(request.baselineResult(), dataset.datasetKey(), "baseline result");
        BiInsightPlanningResult planning = planner.plan(new BiInsightPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                dataset,
                request.query(),
                request.currentResult(),
                request.baselineResult()));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("insight planner did not return a plan");
        }
        return new BiInsightResponse(
                planning.status(),
                planning.fallbackUsed(),
                planning.plan().trends(),
                planning.plan().anomalies(),
                planning.plan().opportunities());
    }
}
