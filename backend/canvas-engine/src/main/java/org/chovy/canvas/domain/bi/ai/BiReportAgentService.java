package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BiReportAgentService {

    private final BiAiSemanticValidator semanticValidator;
    private final BiReportPlanner planner;

    public BiReportAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                BiReportPlanner planner) {
        this.semanticValidator = new BiAiSemanticValidator(datasetSpecResolver);
        this.planner = planner;
    }

    public BiReportResponse generate(BiReportRequest request, BiQueryContext context) {
        if (request == null) {
            throw new IllegalArgumentException("report request is required");
        }
        if (request.sections().isEmpty()) {
            throw new IllegalArgumentException("at least one report section is required");
        }
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        Map<String, BiDatasetSpec> datasets = new LinkedHashMap<>();
        for (BiReportSectionInput section : request.sections()) {
            if (section == null) {
                throw new IllegalArgumentException("report section is required");
            }
            BiDatasetSpec dataset = semanticValidator.validateQuery(section.query(), scopedContext.tenantId());
            semanticValidator.validateResultDataset(section.result(), dataset.datasetKey(), "section result");
            datasets.putIfAbsent(dataset.datasetKey(), dataset);
        }
        BiReportPlanningResult planning = planner.plan(new BiReportPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                request,
                List.copyOf(datasets.values()),
                request.sections()));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("report planner did not return a plan");
        }
        return new BiReportResponse(
                planning.status(),
                planning.fallbackUsed(),
                textOr(planning.plan().title(), textOr(request.title(), "BI Report")),
                textOr(planning.plan().executiveSummary(), "No executive summary generated."),
                planning.plan().sections(),
                planning.plan().nextActions());
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
