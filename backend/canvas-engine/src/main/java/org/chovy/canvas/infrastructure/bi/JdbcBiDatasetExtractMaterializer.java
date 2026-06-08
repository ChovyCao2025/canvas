package org.chovy.canvas.infrastructure.bi;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyView;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractMaterializationResult;
import org.chovy.canvas.domain.bi.dataset.BiDatasetExtractMaterializer;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreview;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceApiPreviewRequest;
import org.chovy.canvas.domain.bi.datasource.BiDatasourceRuntimeService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
/**
 * JdbcBiDatasetExtractMaterializer 封装本模块的核心职责、输入输出结构和协作边界。
 */
public class JdbcBiDatasetExtractMaterializer implements BiDatasetExtractMaterializer {

    private static final Pattern QUALIFIED_IDENTIFIER =
            Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+){0,2}");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");
    private static final DateTimeFormatter SUFFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate dorisJdbcTemplate;
    private final String extractSchema;
    private final Clock clock;
    private final BiDatasourceRuntimeService datasourceRuntimeService;

    @Autowired
    /**
     * 初始化 JdbcBiDatasetExtractMaterializer 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param extractSchema extract schema 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param datasourceRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    public JdbcBiDatasetExtractMaterializer(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.extract.schema:bi_extract}") String extractSchema,
            ObjectProvider<BiDatasourceRuntimeService> datasourceRuntimeService) {
        this(primaryJdbcTemplate == null ? null : primaryJdbcTemplate.getIfAvailable(),
                dorisJdbcTemplate == null ? null : dorisJdbcTemplate.getIfAvailable(),
                extractSchema,
                Clock.systemUTC(),
                datasourceRuntimeService == null ? null : datasourceRuntimeService.getIfAvailable());
    }

    /**
     * 初始化 JdbcBiDatasetExtractMaterializer 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param extractSchema extract schema 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     */
    public JdbcBiDatasetExtractMaterializer(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.extract.schema:bi_extract}") String extractSchema) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, extractSchema, null);
    }

    /**
     * 初始化 JdbcBiDatasetExtractMaterializer 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param extractSchema extract schema 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    public JdbcBiDatasetExtractMaterializer(JdbcTemplate primaryJdbcTemplate,
                                            JdbcTemplate dorisJdbcTemplate,
                                            String extractSchema,
                                            Clock clock) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, extractSchema, clock, null);
    }

    /**
     * 初始化 JdbcBiDatasetExtractMaterializer 实例。
     *
     * @param primaryJdbcTemplate primary jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param dorisJdbcTemplate doris jdbc template 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param extractSchema extract schema 参数，用于 JdbcBiDatasetExtractMaterializer 流程中的校验、计算或对象转换。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param datasourceRuntimeService 时间参数，用于计算窗口、过期或审计时间。
     */
    public JdbcBiDatasetExtractMaterializer(JdbcTemplate primaryJdbcTemplate,
                                            JdbcTemplate dorisJdbcTemplate,
                                            String extractSchema,
                                            Clock clock,
                                            BiDatasourceRuntimeService datasourceRuntimeService) {
        this.primaryJdbcTemplate = primaryJdbcTemplate;
        this.dorisJdbcTemplate = dorisJdbcTemplate;
        this.extractSchema = safeIdentifier(extractSchema == null || extractSchema.isBlank()
                ? "bi_extract"
                : extractSchema.trim());
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.datasourceRuntimeService = datasourceRuntimeService;
    }

    @Override
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 materialize 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 materialize 流程中的校验、计算或对象转换。
     * @return 返回 materialize 流程生成的业务结果。
     */
    public BiDatasetExtractMaterializationResult materialize(Long tenantId,
                                                             BiDatasetSpec dataset,
                                                             BiDatasetAccelerationPolicyView policy) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (dataset == null) {
            throw new IllegalArgumentException("dataset is required");
        }
        if (isApiDataset(dataset)) {
            return materializeApiDataset(tenantId, dataset, policy);
        }
        if (isFileDataset(dataset)) {
            return materializeFileDataset(tenantId, dataset, policy);
        }
        String source = safeQualifiedIdentifier(dataset.tableExpression());
        String tenantColumn = safeQualifiedIdentifier(dataset.tenantColumn());
        JdbcTemplate jdbcTemplate = jdbcTemplate(dataset);
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI dataset EXTRACT datasource is not available for " + dataset.datasetKey());
        }
        long startedAt = clock.instant().toEpochMilli();
        long maxRows = policy == null || policy.maxRows() == null ? 100_000L : Math.max(1L, policy.maxRows());
        String target = materializedTableName(tenantId, dataset.datasetKey());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + target);
        jdbcTemplate.execute("CREATE TABLE " + target + " AS SELECT * FROM " + source + " WHERE 1 = 0");
        long rowCount = jdbcTemplate.update(
                "INSERT INTO " + target + " SELECT * FROM " + source
                        + " WHERE " + tenantColumn + " = ? LIMIT " + maxRows,
                tenantId == null ? 0L : tenantId);
        long durationMs = Math.max(0L, clock.instant().toEpochMilli() - startedAt);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDatasetExtractMaterializationResult(target, rowCount, durationMs);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 materializeFileDataset 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 materializeFileDataset 流程中的校验、计算或对象转换。
     * @return 返回 materializeFileDataset 流程生成的业务结果。
     */
    private BiDatasetExtractMaterializationResult materializeFileDataset(Long tenantId,
                                                                         BiDatasetSpec dataset,
                                                                         BiDatasetAccelerationPolicyView policy) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasourceRuntimeService == null) {
            throw new IllegalStateException("BI file datasource runtime is not available for " + dataset.datasetKey());
        }
        JdbcTemplate jdbcTemplate = primaryJdbcTemplate == null ? dorisJdbcTemplate : primaryJdbcTemplate;
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI dataset EXTRACT storage is not available for " + dataset.datasetKey());
        }
        long startedAt = clock.instant().toEpochMilli();
        long maxRows = policy == null || policy.maxRows() == null ? 100_000L : Math.max(1L, policy.maxRows());
        Long sourceId = fileDatasourceId(dataset);
        BiDatasourceApiPreview preview = datasourceRuntimeService.previewFileData(
                sourceId,
                tenantId == null ? 0L : tenantId,
                apiLimit(maxRows));

        List<ExtractColumn> columns = extractColumns(dataset);
        String target = materializedTableName(tenantId, dataset.datasetKey());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + target);
        jdbcTemplate.execute("CREATE TABLE " + target + " (" + createColumnsSql(columns) + ")");

        long rowCount = 0L;
        String insertSql = insertSql(target, columns);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map<String, Object> row : preview.rows()) {
            if (!isVisibleToTenant(tenantId, dataset.tenantColumn(), row)) {
                continue;
            }
            jdbcTemplate.update(insertSql, values(tenantId, dataset.tenantColumn(), row, columns));
            rowCount++;
            if (rowCount >= maxRows) {
                break;
            }
        }
        long durationMs = Math.max(0L, clock.instant().toEpochMilli() - startedAt);
        return new BiDatasetExtractMaterializationResult(target, rowCount, durationMs);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 materializeApiDataset 流程中的校验、计算或对象转换。
     * @param policy policy 参数，用于 materializeApiDataset 流程中的校验、计算或对象转换。
     * @return 返回 materializeApiDataset 流程生成的业务结果。
     */
    private BiDatasetExtractMaterializationResult materializeApiDataset(Long tenantId,
                                                                        BiDatasetSpec dataset,
                                                                        BiDatasetAccelerationPolicyView policy) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (datasourceRuntimeService == null) {
            throw new IllegalStateException("BI API datasource runtime is not available for " + dataset.datasetKey());
        }
        JdbcTemplate jdbcTemplate = primaryJdbcTemplate == null ? dorisJdbcTemplate : primaryJdbcTemplate;
        if (jdbcTemplate == null) {
            throw new IllegalStateException("BI dataset EXTRACT storage is not available for " + dataset.datasetKey());
        }
        long startedAt = clock.instant().toEpochMilli();
        long maxRows = policy == null || policy.maxRows() == null ? 100_000L : Math.max(1L, policy.maxRows());
        Long sourceId = apiDatasourceId(dataset);
        BiDatasourceApiPreview preview = datasourceRuntimeService.previewApiData(
                sourceId,
                tenantId == null ? 0L : tenantId,
                new BiDatasourceApiPreviewRequest(apiVariables(tenantId, dataset), apiLimit(maxRows)));

        List<ExtractColumn> columns = extractColumns(dataset);
        String target = materializedTableName(tenantId, dataset.datasetKey());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + target);
        jdbcTemplate.execute("CREATE TABLE " + target + " (" + createColumnsSql(columns) + ")");

        long rowCount = 0L;
        String insertSql = insertSql(target, columns);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map<String, Object> row : preview.rows()) {
            if (!isVisibleToTenant(tenantId, dataset.tenantColumn(), row)) {
                continue;
            }
            jdbcTemplate.update(insertSql, values(tenantId, dataset.tenantColumn(), row, columns));
            rowCount++;
            if (rowCount >= maxRows) {
                break;
            }
        }
        long durationMs = Math.max(0L, clock.instant().toEpochMilli() - startedAt);
        return new BiDatasetExtractMaterializationResult(target, rowCount, durationMs);
    }

    @Override
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param materializedTable materialized table 参数，用于 dropMaterializedTable 流程中的校验、计算或对象转换。
     * @return 返回 drop materialized table 的布尔判断结果。
     */
    public boolean dropMaterializedTable(String materializedTable) {
        String safeTable = safeQualifiedIdentifier(materializedTable);
        boolean dropped = false;
        RuntimeException lastFailure = null;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (JdbcTemplate jdbcTemplate : new JdbcTemplate[]{dorisJdbcTemplate, primaryJdbcTemplate}) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (jdbcTemplate == null) {
                continue;
            }
            try {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + safeTable);
                dropped = true;
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        if (!dropped && lastFailure != null) {
            throw lastFailure;
        }
        return dropped;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 jdbcTemplate 流程中的校验、计算或对象转换。
     * @return 返回 jdbcTemplate 流程生成的业务结果。
     */
    private JdbcTemplate jdbcTemplate(BiDatasetSpec dataset) {
        String tableExpression = dataset.tableExpression().toLowerCase(Locale.ROOT);
        if (tableExpression.startsWith("canvas_dws.") || tableExpression.startsWith("canvas_ods.")) {
            return dorisJdbcTemplate == null ? primaryJdbcTemplate : dorisJdbcTemplate;
        }
        return primaryJdbcTemplate;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 materialized table name 生成的文本或业务键。
     */
    private String materializedTableName(Long tenantId, String datasetKey) {
        String safeDataset = safeIdentifier(datasetKey).toLowerCase(Locale.ROOT);
        String suffix = LocalDateTime.now(clock).format(SUFFIX_FORMATTER);
        return extractSchema + ".t" + (tenantId == null ? 0L : tenantId) + "_" + safeDataset + "_" + suffix;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param dataset dataset 参数，用于 isApiDataset 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isApiDataset(BiDatasetSpec dataset) {
        Map<String, Object> model = dataset.model();
        Object connectorType = model.get("connectorType");
        Object sourceKey = model.get("sourceKey");
        return "API".equalsIgnoreCase(string(connectorType))
                || string(sourceKey).toLowerCase(Locale.ROOT).startsWith("api-");
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param dataset dataset 参数，用于 isFileDataset 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isFileDataset(BiDatasetSpec dataset) {
        Map<String, Object> model = dataset.model();
        Object connectorType = model.get("connectorType");
        Object sourceKey = model.get("sourceKey");
        return "CSV_EXCEL".equalsIgnoreCase(string(connectorType))
                || "FILE".equalsIgnoreCase(string(connectorType))
                || "FILE_UPLOAD".equalsIgnoreCase(string(connectorType))
                || string(sourceKey).toLowerCase(Locale.ROOT).startsWith("file-");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 apiDatasourceId 流程中的校验、计算或对象转换。
     * @return 返回 api datasource id 计算得到的数量、金额或指标值。
     */
    private Long apiDatasourceId(BiDatasetSpec dataset) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> model = dataset.model();
        Long sourceId = longValue(model.get("dataSourceConfigId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sourceId != null) {
            return sourceId;
        }
        String sourceKey = string(model.get("sourceKey"));
        if (sourceKey.toLowerCase(Locale.ROOT).startsWith("api-")) {
            sourceId = longValue(sourceKey.substring(4));
        }
        if (sourceId == null || sourceId <= 0L) {
            throw new IllegalArgumentException("BI API datasource id is required for " + dataset.datasetKey());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sourceId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 fileDatasourceId 流程中的校验、计算或对象转换。
     * @return 返回 file datasource id 计算得到的数量、金额或指标值。
     */
    private Long fileDatasourceId(BiDatasetSpec dataset) {
        // 准备本次处理所需的上下文和中间变量。
        Map<String, Object> model = dataset.model();
        Long sourceId = longValue(model.get("dataSourceConfigId"));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sourceId != null && sourceId > 0L) {
            return sourceId;
        }
        String sourceKey = string(model.get("sourceKey"));
        if (sourceKey.toLowerCase(Locale.ROOT).startsWith("file-")) {
            sourceId = longValue(sourceKey.substring(5));
        }
        if (sourceId == null || sourceId <= 0L) {
            throw new IllegalArgumentException("BI file datasource id is required for " + dataset.datasetKey());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sourceId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dataset dataset 参数，用于 apiVariables 流程中的校验、计算或对象转换。
     * @return 返回 api variables 生成的文本或业务键。
     */
    private Map<String, String> apiVariables(Long tenantId, BiDatasetSpec dataset) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("tenantId", String.valueOf(tenantId == null ? 0L : tenantId));
        appendApiVariables(variables, dataset.model().get("apiVariables"));
        appendApiVariables(variables, dataset.model().get("apiResponseVariables"));
        return variables;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 appendApiVariables 流程中的校验、计算或对象转换。
     * @param variables variables 参数，用于 appendApiVariables 流程中的校验、计算或对象转换。
     * @param rawVariables raw variables 参数，用于 appendApiVariables 流程中的校验、计算或对象转换。
     */
    private void appendApiVariables(Map<String, String> variables, Object rawVariables) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(rawVariables instanceof Map<?, ?> rawMap)) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                variables.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param maxRows max rows 参数，用于 apiLimit 流程中的校验、计算或对象转换。
     * @return 返回 api limit 计算得到的数量、金额或指标值。
     */
    private int apiLimit(long maxRows) {
        return (int) Math.max(1L, Math.min(maxRows, Integer.MAX_VALUE));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataset dataset 参数，用于 extractColumns 流程中的校验、计算或对象转换。
     * @return 返回 extract columns 汇总后的集合、分页或映射视图。
     */
    private List<ExtractColumn> extractColumns(BiDatasetSpec dataset) {
        List<ExtractColumn> columns = new ArrayList<>();
        String tenantColumn = safeIdentifier(dataset.tenantColumn());
        boolean hasTenantColumn = false;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiFieldSpec field : dataset.fields().values()) {
            String columnName = safeIdentifier(field.columnExpression());
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (tenantColumn.equals(columnName)) {
                hasTenantColumn = true;
            }
            columns.add(new ExtractColumn(field.fieldKey(), columnName, sqlType(field.valueType())));
        }
        if (!hasTenantColumn) {
            columns.add(0, new ExtractColumn(tenantColumn, tenantColumn, "BIGINT"));
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("BI dataset extract fields are required: " + dataset.datasetKey());
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return List.copyOf(columns);
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param columns columns 参数，用于 createColumnsSql 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private String createColumnsSql(List<ExtractColumn> columns) {
        List<String> definitions = new ArrayList<>();
        for (ExtractColumn column : columns) {
            definitions.add(column.columnName() + " " + column.sqlType());
        }
        return String.join(", ", definitions);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param target target 参数，用于 insertSql 流程中的校验、计算或对象转换。
     * @param columns columns 参数，用于 insertSql 流程中的校验、计算或对象转换。
     * @return 返回 insert sql 生成的文本或业务键。
     */
    private String insertSql(String target, List<ExtractColumn> columns) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        String columnList = columns.stream()
                .map(ExtractColumn::columnName)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String placeholders = "?";
        for (int index = 1; index < columns.size(); index++) {
            placeholders += ", ?";
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "INSERT INTO " + target + " (" + columnList + ") VALUES (" + placeholders + ")";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tenantColumn tenant column 参数，用于 values 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param columns columns 参数，用于 values 流程中的校验、计算或对象转换。
     * @return 返回 values 流程生成的业务结果。
     */
    private Object[] values(Long tenantId,
                            String tenantColumn,
                            Map<String, Object> row,
                            List<ExtractColumn> columns) {
        Object[] values = new Object[columns.size()];
        for (int index = 0; index < columns.size(); index++) {
            ExtractColumn column = columns.get(index);
            Object value = rowValue(row, column, tenantColumn);
            if (value == null && column.columnName().equals(tenantColumn)) {
                value = tenantId == null ? 0L : tenantId;
            }
            values[index] = sqlValue(value);
        }
        return values;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param MapString map string 参数，用于 rowValue 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param column column 参数，用于 rowValue 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 rowValue 流程中的校验、计算或对象转换。
     * @return 返回 rowValue 流程生成的业务结果。
     */
    private Object rowValue(Map<String, Object> row, ExtractColumn column, String tenantColumn) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row.containsKey(column.columnName())) {
            return row.get(column.columnName());
        }
        if (row.containsKey(column.fieldKey())) {
            return row.get(column.fieldKey());
        }
        if (tenantColumn != null && tenantColumn.equals(column.columnName())) {
            return row.get(tenantColumn);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param tenantColumn tenant column 参数，用于 isVisibleToTenant 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 isVisibleToTenant 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回布尔判断结果。
     */
    private boolean isVisibleToTenant(Long tenantId, String tenantColumn, Map<String, Object> row) {
        String safeTenantColumn = safeIdentifier(tenantColumn);
        if (!row.containsKey(safeTenantColumn)) {
            return true;
        }
        Object value = row.get(safeTenantColumn);
        return value == null || tenantMatches(value, tenantId == null ? 0L : tenantId);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant matches 的布尔判断结果。
     */
    private boolean tenantMatches(Object value, Long tenantId) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).compareTo(BigDecimal.valueOf(tenantId)) == 0;
        }
        String text = string(value);
        try {
            return new BigDecimal(text).compareTo(BigDecimal.valueOf(tenantId)) == 0;
        } catch (NumberFormatException ignored) {
            return text.equals(String.valueOf(tenantId));
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sqlValue 流程生成的业务结果。
     */
    private Object sqlValue(Object value) {
        if (value == null
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof java.sql.Date
                || value instanceof java.sql.Timestamp
                || value instanceof LocalDate
                || value instanceof LocalDateTime) {
            return value;
        }
        return String.valueOf(value);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param dataType 类型标识，用于选择对应处理分支。
     * @return 返回 sql type 生成的文本或业务键。
     */
    private String sqlType(String dataType) {
        String normalized = dataType == null ? "STRING" : dataType.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NUMBER", "PERCENT" -> "DOUBLE";
            case "BOOLEAN" -> "BOOLEAN";
            default -> "VARCHAR(512)";
        };
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 long value 计算得到的数量、金额或指标值。
     */
    private Long longValue(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return null;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string 生成的文本或业务键。
     */
    private String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe qualified identifier 生成的文本或业务键。
     */
    private String safeQualifiedIdentifier(String value) {
        if (value == null || !QUALIFIED_IDENTIFIER.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("unsafe BI dataset identifier: " + value);
        }
        return value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe identifier 生成的文本或业务键。
     */
    private String safeIdentifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("unsafe BI dataset identifier: " + value);
        }
        return value.trim();
    }

    /**
     * ExtractColumn 封装本模块的核心职责、输入输出结构和协作边界。
     */
    private record ExtractColumn(String fieldKey, String columnName, String sqlType) {
    }
}
