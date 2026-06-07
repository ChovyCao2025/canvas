package org.chovy.canvas.domain.bi.ai;

import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.chovy.canvas.domain.bi.query.BiQueryExecutionService;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.query.BiQueryResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BiAskDataAgentService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 10_000;

    private final BiDatasetSpecResolver datasetSpecResolver;
    private final BiAskDataPlanner planner;
    private final BiQueryExecutionService queryExecutionService;

    public BiAskDataAgentService(BiDatasetSpecResolver datasetSpecResolver,
                                 BiAskDataPlanner planner,
                                 BiQueryExecutionService queryExecutionService) {
        this.datasetSpecResolver = datasetSpecResolver == null
                ? BiDatasetSpecResolver.builtIn()
                : datasetSpecResolver;
        this.planner = planner;
        this.queryExecutionService = queryExecutionService;
    }

    public BiAskDataResponse ask(BiAskDataRequest request, BiQueryContext context) {
        if (request == null) {
            throw new IllegalArgumentException("ask-data request is required");
        }
        String question = requireQuestion(request.question());
        BiQueryContext scopedContext = context == null ? new BiQueryContext(0L, "system") : context;
        List<BiDatasetSpec> datasets = semanticCatalog(request.datasetKey(), scopedContext.tenantId());
        BiAskDataPlanningResult planning = planner.plan(new BiAskDataPlanningContext(
                scopedContext.tenantId(),
                scopedContext.username(),
                scopedContext.role(),
                question,
                request.datasetKey(),
                datasets,
                request));
        if (planning == null || planning.plan() == null) {
            throw new IllegalStateException("ask-data planner did not return a plan");
        }
        BiQueryRequest query = toQuery(planning.plan(), datasets, request.limit());
        BiQueryResult result = queryExecutionService.execute(query, scopedContext);
        return new BiAskDataResponse(
                question,
                planning.status(),
                planning.fallbackUsed(),
                normalizeExplanation(planning.plan().explanation()),
                query,
                result);
    }

    private List<BiDatasetSpec> semanticCatalog(String datasetKey, Long tenantId) {
        String scopedDatasetKey = trimToNull(datasetKey);
        if (scopedDatasetKey != null) {
            return List.of(datasetSpecResolver.dataset(scopedDatasetKey, tenantId));
        }
        return datasetSpecResolver.datasets(tenantId);
    }

    private BiQueryRequest toQuery(BiAskDataPlan plan, List<BiDatasetSpec> datasets, int requestedLimit) {
        String datasetKey = trimToNull(plan.datasetKey());
        if (datasetKey == null && datasets.size() == 1) {
            datasetKey = datasets.get(0).datasetKey();
        }
        if (datasetKey == null) {
            throw new IllegalArgumentException("planner datasetKey is required");
        }
        Set<String> allowedDatasetKeys = datasets.stream()
                .map(BiDatasetSpec::datasetKey)
                .collect(Collectors.toSet());
        if (!allowedDatasetKeys.contains(datasetKey)) {
            throw new IllegalArgumentException("planner selected dataset outside semantic catalog: " + datasetKey);
        }
        return new BiQueryRequest(
                datasetKey,
                plan.dimensions(),
                plan.metrics(),
                plan.filters(),
                plan.sorts(),
                boundedLimit(plan.limit(), requestedLimit));
    }

    private int boundedLimit(int plannerLimit, int requestedLimit) {
        int plannerBound = normalizeLimit(plannerLimit);
        int requestBound = normalizeLimit(requestedLimit);
        return Math.min(plannerBound, requestBound);
    }

    private int normalizeLimit(int value) {
        if (value <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(value, MAX_LIMIT);
    }

    private String requireQuestion(String question) {
        String value = trimToNull(question);
        if (value == null) {
            throw new IllegalArgumentException("question is required");
        }
        return value;
    }

    private String normalizeExplanation(String explanation) {
        String value = trimToNull(explanation);
        return value == null ? "Generated from BI semantic layer." : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
