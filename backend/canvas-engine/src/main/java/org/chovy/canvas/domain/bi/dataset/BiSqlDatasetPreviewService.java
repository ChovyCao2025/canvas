package org.chovy.canvas.domain.bi.dataset;

import org.chovy.canvas.domain.bi.query.BiCompiledQuery;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryExecutor;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BiSqlDatasetPreviewService {

    private static final int DEFAULT_SAMPLE_LIMIT = 20;
    private static final int MAX_SAMPLE_LIMIT = 100;
    private static final Pattern SOURCE_TABLE = Pattern.compile(
            "\\b(?:FROM|JOIN)\\s+([A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+){0,2})",
            Pattern.CASE_INSENSITIVE);

    private final BiDatasetResourceService datasetResourceService;
    private final BiQueryCompiler compiler;
    private final BiQueryExecutor executor;

    public BiSqlDatasetPreviewService(BiDatasetResourceService datasetResourceService,
                                      BiQueryExecutor executor) {
        this.datasetResourceService = datasetResourceService;
        this.compiler = new BiQueryCompiler();
        this.executor = executor;
    }

    @Autowired
    public BiSqlDatasetPreviewService(BiDatasetResourceService datasetResourceService,
                                      ObjectProvider<BiQueryExecutor> executorProvider) {
        this(datasetResourceService, executorProvider == null ? null : executorProvider.getIfAvailable());
    }

    public BiSqlDatasetPreviewResult preview(Long tenantId, BiSqlDatasetPreviewCommand command) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (command == null || command.resource() == null) {
            throw new IllegalArgumentException("SQL dataset preview resource is required");
        }
        BiDatasetDraftNormalization normalization = datasetResourceService.normalizeDraft(command.resource());
        BiDatasetResource resource = normalization.resource();
        if (!"SQL".equalsIgnoreCase(resource.datasetType())) {
            throw new IllegalArgumentException("SQL dataset preview only supports SQL dataset resources");
        }
        BiDatasetSpec dataset = normalization.spec();
        int sampleLimit = sampleLimit(command.limit());
        BiQueryRequest request = sampleRequest(resource, dataset, command.sqlParameters(), sampleLimit);
        BiCompiledQuery compiled = compiler.compile(dataset, request, tenantId);
        List<BiQueryColumn> columns = columns(dataset, request);

        List<String> warnings = new ArrayList<>();
        List<Map<String, Object>> rows = List.of();
        boolean sampleExecuted = false;
        String executionError = null;
        if (Boolean.FALSE.equals(command.executeSample())) {
            warnings.add("SAMPLE_EXECUTION_DISABLED");
        } else if (executor == null) {
            warnings.add("SAMPLE_EXECUTOR_UNAVAILABLE");
        } else {
            try {
                rows = executor.execute(compiled, dataset);
                sampleExecuted = true;
            } catch (RuntimeException e) {
                executionError = e.getMessage();
                warnings.add("SAMPLE_EXECUTION_FAILED");
            }
        }

        BiSqlDatasetLineageView lineage = lineage(resource);
        BiSqlDatasetImpactView impact = impact(resource, warnings);
        return new BiSqlDatasetPreviewResult(
                resource.datasetKey(),
                stringValue(resource.model().get("sqlTemplate")),
                compiled.sql(),
                compiled.parameters().size(),
                columns,
                rows,
                rows.size(),
                sampleLimit,
                sampleExecuted,
                executionError,
                lineage,
                impact);
    }

    private BiQueryRequest sampleRequest(BiDatasetResource resource,
                                         BiDatasetSpec dataset,
                                         Map<String, String> sqlParameters,
                                         int sampleLimit) {
        List<String> dimensions = resource.fields().stream()
                .filter(BiDatasetFieldResource::visible)
                .filter(field -> "DIMENSION".equals(field.role()))
                .map(BiDatasetFieldResource::fieldKey)
                .filter(dataset.fields()::containsKey)
                .limit(10)
                .toList();
        List<String> metrics = resource.metrics().stream()
                .filter(metric -> !"ARCHIVED".equals(metric.status()))
                .filter(metric -> dataset.metrics().containsKey(metric.metricKey()))
                .filter(metric -> metricAllowsDimensions(dataset.metrics().get(metric.metricKey()), dimensions))
                .map(BiMetricResource::metricKey)
                .limit(3)
                .toList();
        if (dimensions.isEmpty() && metrics.isEmpty() && !dataset.metrics().isEmpty()) {
            metrics = dataset.metrics().keySet().stream().limit(1).toList();
        }
        return new BiQueryRequest(
                dataset.datasetKey(),
                null,
                dimensions,
                metrics,
                List.of(),
                List.of(),
                sampleLimit,
                0,
                sqlParameters);
    }

    private boolean metricAllowsDimensions(BiMetricSpec metric, List<String> dimensions) {
        return metric.allowedDimensions().isEmpty() || metric.allowedDimensions().containsAll(dimensions);
    }

    private List<BiQueryColumn> columns(BiDatasetSpec dataset, BiQueryRequest request) {
        return java.util.stream.Stream.concat(
                        request.dimensions().stream()
                                .map(key -> {
                                    BiFieldSpec field = dataset.fields().get(key);
                                    return new BiQueryColumn(key, field.role().name(), field.valueType());
                                }),
                        request.metrics().stream()
                                .map(key -> new BiQueryColumn(key, "METRIC", dataset.metrics().get(key).valueType())))
                .toList();
    }

    private BiSqlDatasetLineageView lineage(BiDatasetResource resource) {
        Map<String, Object> model = resource.model();
        return new BiSqlDatasetLineageView(
                longValue(model.get("dataSourceConfigId")),
                sourceTables(stringValue(model.get("sqlTemplate"))),
                stringList(model.get("sqlParameterOrder")),
                resource.tenantColumn(),
                resource.fields().stream()
                        .map(BiDatasetFieldResource::columnExpression)
                        .distinct()
                        .toList(),
                resource.metrics().stream()
                        .filter(metric -> !"ARCHIVED".equals(metric.status()))
                        .map(BiMetricResource::expression)
                        .distinct()
                        .toList(),
                Boolean.TRUE.equals(model.get("sqlApprovalRequired")));
    }

    private BiSqlDatasetImpactView impact(BiDatasetResource resource, List<String> warnings) {
        List<String> impactedAssetTypes = List.of(
                "DATASET_DRAFT",
                "PUBLISH_APPROVAL",
                "QUERY_CACHE",
                "DOWNSTREAM_REPORTS");
        List<String> governanceGates = List.of(
                "READ_ONLY_SQL_LINT",
                "TENANT_COLUMN_REQUIRED",
                "SQL_PARAMETER_BINDING",
                "PUBLISH_APPROVAL_REQUIRED");
        List<String> resultWarnings = new ArrayList<>(warnings);
        if (longValue(resource.model().get("dataSourceConfigId")) == null) {
            resultWarnings.add("DATASOURCE_NOT_BOUND");
        }
        if (sourceTables(stringValue(resource.model().get("sqlTemplate"))).isEmpty()) {
            resultWarnings.add("SOURCE_TABLE_LINEAGE_EMPTY");
        }
        return new BiSqlDatasetImpactView(impactedAssetTypes, governanceGates, resultWarnings);
    }

    private int sampleLimit(Integer rawLimit) {
        int limit = rawLimit == null ? DEFAULT_SAMPLE_LIMIT : rawLimit;
        if (limit <= 0) {
            return DEFAULT_SAMPLE_LIMIT;
        }
        return Math.min(limit, MAX_SAMPLE_LIMIT);
    }

    private List<String> sourceTables(String sqlTemplate) {
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            return List.of();
        }
        Set<String> tables = new LinkedHashSet<>();
        Matcher matcher = SOURCE_TABLE.matcher(sqlTemplate);
        while (matcher.find()) {
            String table = matcher.group(1);
            if (!table.equalsIgnoreCase("SELECT")) {
                tables.add(table);
            }
        }
        return List.copyOf(tables);
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim().replaceAll("\\s+", " ");
    }
}
