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

    public JdbcBiDatasetExtractMaterializer(
            @Qualifier("jdbcTemplate") ObjectProvider<JdbcTemplate> primaryJdbcTemplate,
            @Qualifier("dorisJdbcTemplate") ObjectProvider<JdbcTemplate> dorisJdbcTemplate,
            @Value("${canvas.bi.extract.schema:bi_extract}") String extractSchema) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, extractSchema, null);
    }

    public JdbcBiDatasetExtractMaterializer(JdbcTemplate primaryJdbcTemplate,
                                            JdbcTemplate dorisJdbcTemplate,
                                            String extractSchema,
                                            Clock clock) {
        this(primaryJdbcTemplate, dorisJdbcTemplate, extractSchema, clock, null);
    }

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
    public BiDatasetExtractMaterializationResult materialize(Long tenantId,
                                                             BiDatasetSpec dataset,
                                                             BiDatasetAccelerationPolicyView policy) {
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
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + target);
        jdbcTemplate.execute("CREATE TABLE " + target + " AS SELECT * FROM " + source + " WHERE 1 = 0");
        long rowCount = jdbcTemplate.update(
                "INSERT INTO " + target + " SELECT * FROM " + source
                        + " WHERE " + tenantColumn + " = ? LIMIT " + maxRows,
                tenantId == null ? 0L : tenantId);
        long durationMs = Math.max(0L, clock.instant().toEpochMilli() - startedAt);
        return new BiDatasetExtractMaterializationResult(target, rowCount, durationMs);
    }

    private BiDatasetExtractMaterializationResult materializeFileDataset(Long tenantId,
                                                                         BiDatasetSpec dataset,
                                                                         BiDatasetAccelerationPolicyView policy) {
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
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + target);
        jdbcTemplate.execute("CREATE TABLE " + target + " (" + createColumnsSql(columns) + ")");

        long rowCount = 0L;
        String insertSql = insertSql(target, columns);
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

    private BiDatasetExtractMaterializationResult materializeApiDataset(Long tenantId,
                                                                        BiDatasetSpec dataset,
                                                                        BiDatasetAccelerationPolicyView policy) {
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
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + target);
        jdbcTemplate.execute("CREATE TABLE " + target + " (" + createColumnsSql(columns) + ")");

        long rowCount = 0L;
        String insertSql = insertSql(target, columns);
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
    public boolean dropMaterializedTable(String materializedTable) {
        String safeTable = safeQualifiedIdentifier(materializedTable);
        boolean dropped = false;
        RuntimeException lastFailure = null;
        for (JdbcTemplate jdbcTemplate : new JdbcTemplate[]{dorisJdbcTemplate, primaryJdbcTemplate}) {
            if (jdbcTemplate == null) {
                continue;
            }
            try {
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

    private JdbcTemplate jdbcTemplate(BiDatasetSpec dataset) {
        String tableExpression = dataset.tableExpression().toLowerCase(Locale.ROOT);
        if (tableExpression.startsWith("canvas_dws.") || tableExpression.startsWith("canvas_ods.")) {
            return dorisJdbcTemplate == null ? primaryJdbcTemplate : dorisJdbcTemplate;
        }
        return primaryJdbcTemplate;
    }

    private String materializedTableName(Long tenantId, String datasetKey) {
        String safeDataset = safeIdentifier(datasetKey).toLowerCase(Locale.ROOT);
        String suffix = LocalDateTime.now(clock).format(SUFFIX_FORMATTER);
        return extractSchema + ".t" + (tenantId == null ? 0L : tenantId) + "_" + safeDataset + "_" + suffix;
    }

    private boolean isApiDataset(BiDatasetSpec dataset) {
        Map<String, Object> model = dataset.model();
        Object connectorType = model.get("connectorType");
        Object sourceKey = model.get("sourceKey");
        return "API".equalsIgnoreCase(string(connectorType))
                || string(sourceKey).toLowerCase(Locale.ROOT).startsWith("api-");
    }

    private boolean isFileDataset(BiDatasetSpec dataset) {
        Map<String, Object> model = dataset.model();
        Object connectorType = model.get("connectorType");
        Object sourceKey = model.get("sourceKey");
        return "CSV_EXCEL".equalsIgnoreCase(string(connectorType))
                || "FILE".equalsIgnoreCase(string(connectorType))
                || "FILE_UPLOAD".equalsIgnoreCase(string(connectorType))
                || string(sourceKey).toLowerCase(Locale.ROOT).startsWith("file-");
    }

    private Long apiDatasourceId(BiDatasetSpec dataset) {
        Map<String, Object> model = dataset.model();
        Long sourceId = longValue(model.get("dataSourceConfigId"));
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
        return sourceId;
    }

    private Long fileDatasourceId(BiDatasetSpec dataset) {
        Map<String, Object> model = dataset.model();
        Long sourceId = longValue(model.get("dataSourceConfigId"));
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
        return sourceId;
    }

    private Map<String, String> apiVariables(Long tenantId, BiDatasetSpec dataset) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("tenantId", String.valueOf(tenantId == null ? 0L : tenantId));
        appendApiVariables(variables, dataset.model().get("apiVariables"));
        appendApiVariables(variables, dataset.model().get("apiResponseVariables"));
        return variables;
    }

    private void appendApiVariables(Map<String, String> variables, Object rawVariables) {
        if (!(rawVariables instanceof Map<?, ?> rawMap)) {
            return;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                variables.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    private int apiLimit(long maxRows) {
        return (int) Math.max(1L, Math.min(maxRows, Integer.MAX_VALUE));
    }

    private List<ExtractColumn> extractColumns(BiDatasetSpec dataset) {
        List<ExtractColumn> columns = new ArrayList<>();
        String tenantColumn = safeIdentifier(dataset.tenantColumn());
        boolean hasTenantColumn = false;
        for (BiFieldSpec field : dataset.fields().values()) {
            String columnName = safeIdentifier(field.columnExpression());
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
        return List.copyOf(columns);
    }

    private String createColumnsSql(List<ExtractColumn> columns) {
        List<String> definitions = new ArrayList<>();
        for (ExtractColumn column : columns) {
            definitions.add(column.columnName() + " " + column.sqlType());
        }
        return String.join(", ", definitions);
    }

    private String insertSql(String target, List<ExtractColumn> columns) {
        String columnList = columns.stream()
                .map(ExtractColumn::columnName)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String placeholders = "?";
        for (int index = 1; index < columns.size(); index++) {
            placeholders += ", ?";
        }
        return "INSERT INTO " + target + " (" + columnList + ") VALUES (" + placeholders + ")";
    }

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

    private Object rowValue(Map<String, Object> row, ExtractColumn column, String tenantColumn) {
        if (row.containsKey(column.columnName())) {
            return row.get(column.columnName());
        }
        if (row.containsKey(column.fieldKey())) {
            return row.get(column.fieldKey());
        }
        if (tenantColumn != null && tenantColumn.equals(column.columnName())) {
            return row.get(tenantColumn);
        }
        return null;
    }

    private boolean isVisibleToTenant(Long tenantId, String tenantColumn, Map<String, Object> row) {
        String safeTenantColumn = safeIdentifier(tenantColumn);
        if (!row.containsKey(safeTenantColumn)) {
            return true;
        }
        Object value = row.get(safeTenantColumn);
        return value == null || tenantMatches(value, tenantId == null ? 0L : tenantId);
    }

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

    private String sqlType(String dataType) {
        String normalized = dataType == null ? "STRING" : dataType.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NUMBER", "PERCENT" -> "DOUBLE";
            case "BOOLEAN" -> "BOOLEAN";
            default -> "VARCHAR(512)";
        };
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String safeQualifiedIdentifier(String value) {
        if (value == null || !QUALIFIED_IDENTIFIER.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("unsafe BI dataset identifier: " + value);
        }
        return value.trim();
    }

    private String safeIdentifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("unsafe BI dataset identifier: " + value);
        }
        return value.trim();
    }

    private record ExtractColumn(String fieldKey, String columnName, String sqlType) {
    }
}
