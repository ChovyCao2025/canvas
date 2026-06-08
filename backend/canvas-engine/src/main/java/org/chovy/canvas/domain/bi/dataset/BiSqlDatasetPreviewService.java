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

/**
 * BiSqlDatasetPreviewService 编排 domain.bi.dataset 场景的领域业务规则。
 */
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

    /**
     * 创建 BiSqlDatasetPreviewService 实例并注入 domain.bi.dataset 场景依赖。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param executor 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public BiSqlDatasetPreviewService(BiDatasetResourceService datasetResourceService,
                                      BiQueryExecutor executor) {
        this.datasetResourceService = datasetResourceService;
        this.compiler = new BiQueryCompiler();
        this.executor = executor;
    }

    /**
     * 创建 BiSqlDatasetPreviewService 实例并注入 domain.bi.dataset 场景依赖。
     * @param datasetResourceService 依赖组件，用于完成数据访问或外部能力调用。
     * @param executorProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    @Autowired
    public BiSqlDatasetPreviewService(BiDatasetResourceService datasetResourceService,
                                      ObjectProvider<BiQueryExecutor> executorProvider) {
        this(datasetResourceService, executorProvider == null ? null : executorProvider.getIfAvailable());
    }

    /**
     * 执行自助预览或 SQL 数据集预览，在不创建导出任务的情况下返回受治理的样本查询结果。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 本次操作的业务处理结果
     */
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
        // 根据前序判断结果进入后续条件分支。
        } else if (executor == null) {
            warnings.add("SAMPLE_EXECUTOR_UNAVAILABLE");
        } else {
            try {
                rows = executor.execute(compiled, dataset);
                sampleExecuted = true;
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
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

    /**
     * 执行 sampleRequest 流程，围绕 sample request 完成校验、计算或结果组装。
     *
     * @param resource resource 参数，用于 sampleRequest 流程中的校验、计算或对象转换。
     * @param dataset dataset 参数，用于 sampleRequest 流程中的校验、计算或对象转换。
     * @param sqlParameters sql parameters 参数，用于 sampleRequest 流程中的校验、计算或对象转换。
     * @param sampleLimit sample limit 参数，用于 sampleRequest 流程中的校验、计算或对象转换。
     * @return 返回 sampleRequest 流程生成的业务结果。
     */
    private BiQueryRequest sampleRequest(BiDatasetResource resource,
                                         BiDatasetSpec dataset,
                                         Map<String, String> sqlParameters,
                                         int sampleLimit) {
        // 预览查询优先选择可见维度和兼容维度的少量指标，避免样本查询扩大扫描面或触发不兼容指标。
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

    /**
     * 执行 metricAllowsDimensions 流程，围绕 metric allows dimensions 完成校验、计算或结果组装。
     *
     * @param metric metric 参数，用于 metricAllowsDimensions 流程中的校验、计算或对象转换。
     * @param dimensions dimensions 参数，用于 metricAllowsDimensions 流程中的校验、计算或对象转换。
     * @return 返回 metric allows dimensions 的布尔判断结果。
     */
    private boolean metricAllowsDimensions(BiMetricSpec metric, List<String> dimensions) {
        return metric.allowedDimensions().isEmpty() || metric.allowedDimensions().containsAll(dimensions);
    }

    /**
     * 执行 columns 流程，围绕 columns 完成校验、计算或结果组装。
     *
     * @param dataset dataset 参数，用于 columns 流程中的校验、计算或对象转换。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 columns 汇总后的集合、分页或映射视图。
     */
    private List<BiQueryColumn> columns(BiDatasetSpec dataset, BiQueryRequest request) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return java.util.stream.Stream.concat(
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        request.dimensions().stream()
                                .map(key -> {
                                    BiFieldSpec field = dataset.fields().get(key);
                                    return new BiQueryColumn(key, field.role().name(), field.valueType());
                                }),
                        request.metrics().stream()
                                .map(key -> new BiQueryColumn(key, "METRIC", dataset.metrics().get(key).valueType())))
                .toList();
    }

    /**
     * 执行 lineage 流程，围绕 lineage 完成校验、计算或结果组装。
     *
     * @param resource resource 参数，用于 lineage 流程中的校验、计算或对象转换。
     * @return 返回 lineage 流程生成的业务结果。
     */
    private BiSqlDatasetLineageView lineage(BiDatasetResource resource) {
        // 血缘视图来自规范化后的 SQL 模板和字段/指标定义，用于审批和发布前影响分析。
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

    /**
     * 执行 impact 流程，围绕 impact 完成校验、计算或结果组装。
     *
     * @param resource resource 参数，用于 impact 流程中的校验、计算或对象转换。
     * @param warnings warnings 参数，用于 impact 流程中的校验、计算或对象转换。
     * @return 返回 impact 流程生成的业务结果。
     */
    private BiSqlDatasetImpactView impact(BiDatasetResource resource, List<String> warnings) {
        // SQL 草稿预览显式暴露治理关卡，让调用方知道缓存、审批和下游报表都可能受影响。
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

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rawLimit raw limit 参数，用于 sampleLimit 流程中的校验、计算或对象转换。
     * @return 返回 sample limit 计算得到的数量、金额或指标值。
     */
    private int sampleLimit(Integer rawLimit) {
        // 样本行数有硬上限，预览不能成为绕过查询治理的大批量导出入口。
        int limit = rawLimit == null ? DEFAULT_SAMPLE_LIMIT : rawLimit;
        if (limit <= 0) {
            return DEFAULT_SAMPLE_LIMIT;
        }
        return Math.min(limit, MAX_SAMPLE_LIMIT);
    }

    /**
     * 执行 sourceTables 流程，围绕 source tables 完成校验、计算或结果组装。
     *
     * @param sqlTemplate sql template 参数，用于 sourceTables 流程中的校验、计算或对象转换。
     * @return 返回 source tables 汇总后的集合、分页或映射视图。
     */
    private List<String> sourceTables(String sqlTemplate) {
        // 仅抽取 FROM/JOIN 后的受控表名用于展示血缘，不把它作为授权或 SQL 安全判断依据。
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

    /**
     * 执行 stringList 流程，围绕 string list 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
    private List<String> stringList(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return list.stream()
                .map(String::valueOf)
                .filter(item -> !item.isBlank())
                .toList();
    }

    /**
     * 执行 longValue 流程，围绕 long value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value).trim().replaceAll("\\s+", " ");
    }
}
