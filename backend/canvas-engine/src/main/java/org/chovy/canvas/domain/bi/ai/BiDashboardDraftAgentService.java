package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BiDashboardDraftAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiDashboardDraftPlanner planner;

    public BiDashboardDraftAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                        BiDashboardDraftPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    public BiDashboardDraftResponse generate(BiDashboardDraftRequest request, BiQueryContext context) {
        if (request == null) {
            throw new IllegalArgumentException("dashboard draft request is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        List<BiDatasetSpec> datasets = semanticValidator.catalog(request.datasetKey(), scopedContext.tenantId());
        BiDashboardDraftPlanningResult planning = planner.plan(new BiDashboardDraftPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                datasets));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("dashboard draft planner did not return a plan");
        }
        semanticValidator.validateDashboardDraft(planning.plan(), scopedContext.tenantId());
        return new BiDashboardDraftResponse(
                planning.status(),
                planning.fallbackUsed(),
                planning.plan().dashboard(),
                planning.plan().charts(),
                textOr(planning.plan().explanation(), "Generated dashboard draft from BI semantic layer."));
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
