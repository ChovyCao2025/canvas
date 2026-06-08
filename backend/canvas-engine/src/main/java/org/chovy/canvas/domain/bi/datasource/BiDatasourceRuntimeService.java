package org.chovy.canvas.domain.bi.datasource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.chovy.canvas.dal.dataobject.BiDatasourceSchemaSnapshotDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.BiDatasourceSchemaSnapshotMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.bi.query.BiQueryColumn;
import org.chovy.canvas.domain.datasource.DataSourceCredentialCipher;
import org.chovy.canvas.security.SecretCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Types;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
/**
 * BiDatasourceRuntimeService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiDatasourceRuntimeService {

    private static final Pattern SECRET_URL_PARAMETER = Pattern.compile("(?i)(password|pwd|token|authorization|api[-_]?key|access[-_]?key(?:[-_]?id|[-_]?secret)?|secret)=([^&;\\s]+)");
    private static final Pattern TEMPLATE_PARAMETER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}|\\$\\{\\s*([A-Za-z0-9_.-]+)\\s*}");
    private static final int API_RESPONSE_BYTES_LIMIT = 10 * 1024 * 1024;
    private static final int API_ROW_LIMIT = 1000;
    private static final int API_COLUMN_LIMIT = 100;
    private static final TypeReference<List<BiDatasourceTablePreview>> TABLES_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> CONNECTOR_CONFIG_TYPE = new TypeReference<>() {
    };

    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper;
    private final SecretCipher secretCipher;
    private final DataSourceCredentialCipher credentialCipher;
    private final ObjectMapper objectMapper;
    private final JdbcConnectionFactory connectionFactory;
    private final ApiHttpClient apiHttpClient;

    /**
     * 初始化 BiDatasourceRuntimeService 实例。
     *
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     */
    public BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                                      SecretCipher secretCipher,
                                      DataSourceCredentialCipher credentialCipher) {
        this(dataSourceConfigMapper, null, secretCipher, credentialCipher, new ObjectMapper(),
                BiDatasourceRuntimeService::openJdbcConnection, defaultApiHttpClient());
    }

    @Autowired
    /**
     * 初始化 BiDatasourceRuntimeService 实例。
     *
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                                      BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                                      SecretCipher secretCipher,
                                      DataSourceCredentialCipher credentialCipher,
                                      ObjectMapper objectMapper) {
        this(dataSourceConfigMapper, schemaSnapshotMapper, secretCipher, credentialCipher, objectMapper,
                BiDatasourceRuntimeService::openJdbcConnection, defaultApiHttpClient());
    }

    /**
     * 初始化 BiDatasourceRuntimeService 实例。
     *
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param connectionFactory 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               JdbcConnectionFactory connectionFactory) {
        this(dataSourceConfigMapper, null, secretCipher, credentialCipher, new ObjectMapper(), connectionFactory,
                defaultApiHttpClient());
    }

    /**
     * 初始化 BiDatasourceRuntimeService 实例。
     *
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param apiHttpClient 依赖组件，用于完成数据访问或外部能力调用。
     */
    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               ApiHttpClient apiHttpClient) {
        this(dataSourceConfigMapper, null, secretCipher, credentialCipher, new ObjectMapper(),
                BiDatasourceRuntimeService::openJdbcConnection, apiHttpClient);
    }

    /**
     * 初始化 BiDatasourceRuntimeService 实例。
     *
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param connectionFactory 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               ObjectMapper objectMapper,
                               JdbcConnectionFactory connectionFactory) {
        this(dataSourceConfigMapper, schemaSnapshotMapper, secretCipher, credentialCipher, objectMapper,
                connectionFactory, defaultApiHttpClient());
    }

    /**
     * 初始化 BiDatasourceRuntimeService 实例。
     *
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param secretCipher secret cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceRuntimeService 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param connectionFactory 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param apiHttpClient 依赖组件，用于完成数据访问或外部能力调用。
     */
    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               ObjectMapper objectMapper,
                               JdbcConnectionFactory connectionFactory,
                               ApiHttpClient apiHttpClient) {
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.schemaSnapshotMapper = schemaSnapshotMapper;
        this.secretCipher = secretCipher;
        this.credentialCipher = credentialCipher;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.connectionFactory = connectionFactory;
        this.apiHttpClient = apiHttpClient == null ? defaultApiHttpClient() : apiHttpClient;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 testConnection 流程生成的业务结果。
     */
    public BiDatasourceConnectionTestResult testConnection(Long sourceId, Long tenantId) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        long start = System.nanoTime();
        LocalDateTime checkedAt = LocalDateTime.now();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (isApiSource(source)) {
            String password = decryptPassword(source.getPassword());
            try {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                ApiHttpResponse response = apiHttpClient.execute(apiRequest(source, password, Map.of()));
                boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                return new BiDatasourceConnectionTestResult(
                        source.getId(),
                        sourceKey(source),
                        connectorType(source),
                        success,
                        success ? "connection ok" : "connection failed: HTTP " + response.statusCode(),
                        "HTTP JSON",
                        response.statusCode() + " " + blankToDefault(response.statusText(), ""),
                        checkedAt,
                        durationMs(start));
            } catch (Exception e) {
                return new BiDatasourceConnectionTestResult(
                        source.getId(),
                        sourceKey(source),
                        connectorType(source),
                        false,
                        "connection failed: " + sanitizeErrorMessage(e.getMessage(), source, password),
                        "HTTP JSON",
                        null,
                        checkedAt,
                        durationMs(start));
            }
        }
        if (isFileSource(source)) {
            try {
                Path path = filePath(source);
                boolean success = Files.isRegularFile(path) && Files.isReadable(path);
                return new BiDatasourceConnectionTestResult(
                        source.getId(),
                        sourceKey(source),
                        connectorType(source),
                        success,
                        success ? "connection ok" : "connection failed: file is not readable",
                        "CSV/Excel File",
                        stringConfig(connectorConfig(source), "fileType", "CSV"),
                        checkedAt,
                        durationMs(start));
            } catch (Exception e) {
                return new BiDatasourceConnectionTestResult(
                        source.getId(),
                        sourceKey(source),
                        connectorType(source),
                        false,
                        "connection failed: " + sanitizeErrorMessage(e.getMessage(), source, null),
                        "CSV/Excel File",
                        null,
                        checkedAt,
                        durationMs(start));
            }
        }
        try (Connection connection = connectionFactory.open(source, decryptPassword(source.getPassword()))) {
            DatabaseMetaData metaData = connection.getMetaData();
            return new BiDatasourceConnectionTestResult(
                    source.getId(),
                    sourceKey(source),
                    connectorType(source),
                    true,
                    "connection ok",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    checkedAt,
                    durationMs(start));
        } catch (Exception e) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new BiDatasourceConnectionTestResult(
                    source.getId(),
                    sourceKey(source),
                    connectorType(source),
                    false,
                    "connection failed: " + e.getMessage(),
                    null,
                    null,
                    checkedAt,
                    durationMs(start));
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 previewSchema 流程生成的业务结果。
     */
    public BiDatasourceSchemaPreview previewSchema(Long sourceId, Long tenantId, int limit) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        String password = decryptPassword(source.getPassword());
        try {
            return readSchema(source, password, limit, Map.of());
        } catch (Exception e) {
            throw new IllegalStateException("BI datasource schema preview failed", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 previewApiData 流程生成的业务结果。
     */
    public BiDatasourceApiPreview previewApiData(Long sourceId, Long tenantId, BiDatasourceApiPreviewRequest request) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!isApiSource(source)) {
            throw new IllegalArgumentException("BI datasource is not an API connector: " + connectorType(source));
        }
        long start = System.nanoTime();
        LocalDateTime checkedAt = LocalDateTime.now();
        String password = decryptPassword(source.getPassword());
        try {
            Map<String, Object> config = connectorConfig(source);
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            ApiHttpResponse response = apiHttpClient.execute(apiRequest(source, password, requestVariables(request)));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " " + blankToDefault(response.statusText(), ""));
            }
            String body = blankToDefault(response.body(), "[]");
            if (body.getBytes(StandardCharsets.UTF_8).length > API_RESPONSE_BYTES_LIMIT) {
                throw new IllegalStateException("response exceeds 10MB direct preview limit");
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode rowsNode = selectJsonPath(root, stringConfig(config, "responseRowsPath", "$"));
            int rowLimit = Math.min(apiRowLimit(request == null ? null : request.limit()), API_ROW_LIMIT);
            List<JsonNode> apiRows = rows(rowsNode, rowLimit + 1);
            boolean rowTruncated = apiRows.size() > rowLimit;
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            List<Map<String, Object>> previewRows = apiRows.stream()
                    .limit(rowLimit)
                    .map(this::apiRow)
                    .map(row -> limitApiColumns(row, API_COLUMN_LIMIT))
                    .toList();
            List<BiQueryColumn> columns = inferApiPreviewColumns(previewRows);
            boolean columnTruncated = apiRows.stream()
                    .limit(rowLimit)
                    .map(this::apiRow)
                    .anyMatch(row -> row.size() > API_COLUMN_LIMIT);
            return new BiDatasourceApiPreview(
                    source.getId(),
                    sourceKey(source),
                    blankToDefault(source.getName(), sourceKey(source)),
                    connectorType(source),
                    columns,
                    previewRows,
                    previewRows.size(),
                    rowTruncated || columnTruncated,
                    durationMs(start),
                    checkedAt);
        } catch (Exception e) {
            throw new IllegalStateException(
                    sanitizeErrorMessage("BI API datasource preview failed: " + e.getMessage(), source, password),
                    e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 previewFileData 流程生成的业务结果。
     */
    public BiDatasourceApiPreview previewFileData(Long sourceId, Long tenantId, int limit) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!isFileSource(source)) {
            throw new IllegalArgumentException("BI datasource is not a file connector: " + connectorType(source));
        }
        long start = System.nanoTime();
        LocalDateTime checkedAt = LocalDateTime.now();
        try {
            FilePreviewRows fileRows = readFileRows(source, limit);
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            List<Map<String, Object>> previewRows = fileRows.rows().stream()
                    .map(this::apiRow)
                    .map(row -> limitApiColumns(row, API_COLUMN_LIMIT))
                    .toList();
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new BiDatasourceApiPreview(
                    source.getId(),
                    sourceKey(source),
                    blankToDefault(source.getName(), sourceKey(source)),
                    connectorType(source),
                    inferApiPreviewColumns(previewRows),
                    previewRows,
                    previewRows.size(),
                    fileRows.truncated(),
                    durationMs(start),
                    checkedAt);
        } catch (Exception e) {
            throw new IllegalStateException(
                    sanitizeErrorMessage("BI file datasource preview failed: " + e.getMessage(), source, null),
                    e);
        }
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasourceSchemaSnapshotView syncSchema(Long sourceId, Long tenantId, String username, int limit) {
        return syncSchema(sourceId, tenantId, username, limit, null);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasourceSchemaSnapshotView syncSchema(Long sourceId,
                                                     Long tenantId,
                                                     String username,
                                                     int limit,
                                                     BiDatasourceApiPreviewRequest request) {
        // 准备本次处理所需的上下文和中间变量。
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        String password = decryptPassword(source.getPassword());
        LocalDateTime syncedAt = LocalDateTime.now();
        try {
            BiDatasourceSchemaPreview preview = readSchema(source, password, limit, requestVariables(request));
            List<BiDatasourceTablePreview> tables = preview.tables();
            BiDatasourceSchemaSnapshotDO row = snapshotRow(
                    source,
                    scopedTenantId,
                    "SUCCESS",
                    null,
                    tables,
                    username,
                    syncedAt);
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            requireSchemaSnapshotMapper().insert(row);
            return toSnapshotView(row, source, tables);
        } catch (Exception e) {
            BiDatasourceSchemaSnapshotDO row = snapshotRow(
                    source,
                    scopedTenantId,
                    "FAILED",
                    sanitizeErrorMessage("schema sync failed: " + e.getMessage(), source, password),
                    List.of(),
                    username,
                    syncedAt);
            requireSchemaSnapshotMapper().insert(row);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return toSnapshotView(row, source, List.of());
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 latestSchemaSnapshot 流程生成的业务结果。
     */
    public BiDatasourceSchemaSnapshotView latestSchemaSnapshot(Long sourceId, Long tenantId) {
        List<BiDatasourceSchemaSnapshotView> history = schemaSnapshotHistory(sourceId, tenantId, 1);
        return history.isEmpty() ? null : history.get(0);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 schema snapshot history 汇总后的集合、分页或映射视图。
     */
    public List<BiDatasourceSchemaSnapshotView> schemaSnapshotHistory(Long sourceId, Long tenantId, int limit) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        QueryWrapper<BiDatasourceSchemaSnapshotDO> wrapper = new QueryWrapper<BiDatasourceSchemaSnapshotDO>()
                .eq("tenant_id", scopedTenantId)
                .eq("data_source_config_id", source.getId())
                .orderByDesc("synced_at")
                .last("LIMIT " + boundedLimit(limit));
        return requireSchemaSnapshotMapper().selectList(wrapper)
                .stream()
                .map(row -> toSnapshotView(row, source, deserializeTables(row.getSchemaJson())))
                .toList();
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param source source 参数，用于 readSchema 流程中的校验、计算或对象转换。
     * @param password password 参数，用于 readSchema 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param variables variables 参数，用于 readSchema 流程中的校验、计算或对象转换。
     * @return 返回 readSchema 流程生成的业务结果。
     */
    private BiDatasourceSchemaPreview readSchema(DataSourceConfigDO source,
                                                 String password,
                                                 int limit,
                                                 Map<String, String> variables) throws Exception {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (isApiSource(source)) {
            return readApiSchema(source, password, limit, variables);
        }
        if (isFileSource(source)) {
            return readFileSchema(source, limit);
        }
        try (Connection connection = connectionFactory.open(source, password)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            List<BiDatasourceTablePreview> tables = new ArrayList<>();
            try (ResultSet tableRows = metaData.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                while (tableRows.next() && tables.size() < boundedLimit(limit)) {
                    String tableName = tableRows.getString("TABLE_NAME");
                    tables.add(new BiDatasourceTablePreview(
                            tableName,
                            tableRows.getString("TABLE_TYPE"),
                            columns(metaData, catalog, tableName)));
                }
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new BiDatasourceSchemaPreview(
                    source.getId(),
                    sourceKey(source),
                    blankToDefault(source.getName(), sourceKey(source)),
                    connectorType(source),
                    tables,
                    LocalDateTime.now());
        }
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param source source 参数，用于 readFileSchema 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 readFileSchema 流程生成的业务结果。
     */
    private BiDatasourceSchemaPreview readFileSchema(DataSourceConfigDO source, int limit) throws Exception {
        FilePreviewRows previewRows = readFileRows(source, limit);
        List<BiDatasourceColumnPreview> columns = previewRows.rows().isEmpty()
                ? varcharColumns(previewRows.headers())
                : inferColumns(previewRows.rows());
        return new BiDatasourceSchemaPreview(
                source.getId(),
                sourceKey(source),
                blankToDefault(source.getName(), sourceKey(source)),
                connectorType(source),
                List.of(new BiDatasourceTablePreview(previewRows.tableName(), previewRows.fileType(), columns)),
                LocalDateTime.now());
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param source source 参数，用于 readFileRows 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 readFileRows 流程生成的业务结果。
     */
    private FilePreviewRows readFileRows(DataSourceConfigDO source, int limit) throws Exception {
        Map<String, Object> config = connectorConfig(source);
        String fileType = stringConfig(config, "fileType", "CSV").toUpperCase(Locale.ROOT);
        Path path = filePath(source);
        if ("CSV".equals(fileType)) {
            return readDelimitedFileRows(source, path, config, fileType, limit);
        }
        if ("XLSX".equals(fileType) || "XLS".equals(fileType)) {
            return readWorkbookFileRows(source, path, config, fileType, limit);
        }
        throw new IllegalArgumentException("BI file datasource preview supports CSV, XLSX, and XLS only: " + fileType);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param source source 参数，用于 readDelimitedFileRows 流程中的校验、计算或对象转换。
     * @param path path 参数，用于 readDelimitedFileRows 流程中的校验、计算或对象转换。
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param fileType 类型标识，用于选择对应处理分支。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 readDelimitedFileRows 流程生成的业务结果。
     */
    private FilePreviewRows readDelimitedFileRows(DataSourceConfigDO source,
                                                  Path path,
                                                  Map<String, Object> config,
                                                  String fileType,
                                                  int limit) throws Exception {
        Charset charset = Charset.forName(stringConfig(config, "encoding", "UTF-8"));
        String delimiter = delimiter(config);
        boolean headerRow = !config.containsKey("headerRow") || booleanValue(config.get("headerRow"));
        List<String> headers = new ArrayList<>();
        List<JsonNode> rows = new ArrayList<>();
        int rowLimit = boundedLimit(limit);
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            String firstLine = reader.readLine();
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (firstLine == null) {
                headers = List.of("value");
            } else if (headerRow) {
                headers = normalizeHeaders(parseDelimitedLine(firstLine, delimiter), path);
            } else {
                List<String> firstValues = parseDelimitedLine(firstLine, delimiter);
                headers = generatedHeaders(firstValues.size());
                rows.add(csvRow(headers, firstValues));
            }
            String line;
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            while ((line = reader.readLine()) != null && rows.size() <= rowLimit) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(csvRow(headers, parseDelimitedLine(line, delimiter)));
            }
        }
        boolean truncated = rows.size() > rowLimit;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new FilePreviewRows(
                fileTableName(source, path),
                fileType,
                headers,
                truncated ? rows.stream().limit(rowLimit).toList() : rows,
                truncated);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param source source 参数，用于 readWorkbookFileRows 流程中的校验、计算或对象转换。
     * @param path path 参数，用于 readWorkbookFileRows 流程中的校验、计算或对象转换。
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param fileType 类型标识，用于选择对应处理分支。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 readWorkbookFileRows 流程生成的业务结果。
     */
    private FilePreviewRows readWorkbookFileRows(DataSourceConfigDO source,
                                                 Path path,
                                                 Map<String, Object> config,
                                                 String fileType,
                                                 int limit) throws Exception {
        boolean headerRow = !config.containsKey("headerRow") || booleanValue(config.get("headerRow"));
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        List<String> headers = new ArrayList<>();
        List<JsonNode> rows = new ArrayList<>();
        int rowLimit = boundedLimit(limit);
        try (Workbook workbook = WorkbookFactory.create(path.toFile())) {
            Sheet sheet = workbookSheet(workbook, config);
            Row firstRow = firstWorkbookRow(sheet);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (firstRow == null) {
                headers = List.of("value");
            } else if (headerRow) {
                headers = normalizeHeaders(workbookRowValues(firstRow, formatter), path);
            } else {
                List<String> firstValues = workbookRowValues(firstRow, formatter);
                headers = generatedHeaders(firstValues.size());
                rows.add(csvRow(headers, firstValues));
            }
            int startRow = firstRow == null ? sheet.getFirstRowNum() : firstRow.getRowNum() + 1;
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum() && rows.size() <= rowLimit; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<String> values = workbookRowValues(row, formatter);
                if (values.stream().allMatch(String::isBlank)) {
                    continue;
                }
                rows.add(csvRow(headers, values));
            }
        }
        boolean truncated = rows.size() > rowLimit;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new FilePreviewRows(
                fileTableName(source, path),
                fileType,
                headers,
                truncated ? rows.stream().limit(rowLimit).toList() : rows,
                truncated);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param source source 参数，用于 readApiSchema 流程中的校验、计算或对象转换。
     * @param password password 参数，用于 readApiSchema 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @param variables variables 参数，用于 readApiSchema 流程中的校验、计算或对象转换。
     * @return 返回 readApiSchema 流程生成的业务结果。
     */
    private BiDatasourceSchemaPreview readApiSchema(DataSourceConfigDO source,
                                                    String password,
                                                    int limit,
                                                    Map<String, String> variables) throws Exception {
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        ApiHttpResponse response = apiHttpClient.execute(apiRequest(source, password, variables == null ? Map.of() : variables));
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + blankToDefault(response.statusText(), ""));
        }
        JsonNode root = objectMapper.readTree(blankToDefault(response.body(), "{}"));
        JsonNode rowsNode = selectJsonPath(root, stringConfig(connectorConfig(source), "responseRowsPath", "$"));
        List<JsonNode> rows = rows(rowsNode, boundedLimit(limit));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDatasourceSchemaPreview(
                source.getId(),
                sourceKey(source),
                blankToDefault(source.getName(), sourceKey(source)),
                connectorType(source),
                List.of(new BiDatasourceTablePreview("api_response", "HTTP_JSON", inferColumns(rows))),
                LocalDateTime.now());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param source source 参数，用于 apiRequest 流程中的校验、计算或对象转换。
     * @param password password 参数，用于 apiRequest 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 apiRequest 流程中的校验、计算或对象转换。
     * @param variables variables 参数，用于 apiRequest 流程中的校验、计算或对象转换。
     * @return 返回 apiRequest 流程生成的业务结果。
     */
    private ApiHttpRequest apiRequest(DataSourceConfigDO source, String password, Map<String, String> variables) {
        Map<String, Object> config = connectorConfig(source);
        String method = stringConfig(config, "requestMethod", "GET").toUpperCase(Locale.ROOT);
        String url = appendQueryParameters(source.getUrl(), listConfig(config.get("parameters")), variables);
        Map<String, String> headers = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map<String, Object> header : listConfig(config.get("headers"))) {
            String name = stringValue(header.get("name"));
            String value = renderTemplate(stringValue(header.get("value")), variables);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!name.isBlank() && !value.isBlank() && !"***".equals(value)) {
                headers.put(name, value);
            }
        }
        String authType = stringConfig(config, "authType", "NONE").toUpperCase(Locale.ROOT);
        if ("BEARER".equals(authType) && password != null && !password.isBlank()) {
            headers.put("Authorization", "Bearer " + password);
        } else if ("BASIC".equals(authType) && password != null && !password.isBlank()) {
            String principal = blankToDefault(source.getUsername(), "");
            String token = Base64.getEncoder().encodeToString((principal + ":" + password).getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + token);
        } else if ("API_KEY".equals(authType) && source.getUsername() != null && !source.getUsername().isBlank()
                && password != null && !password.isBlank()) {
            headers.put(source.getUsername().trim(), password);
        }
        String body = renderTemplate(stringConfig(config, "bodyTemplate", ""), variables);
        if (!body.isBlank()) {
            headers.putIfAbsent("Content-Type", "application/json");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new ApiHttpRequest(method, url, headers, body.isBlank() ? null : body);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param source source 参数，用于 connectorConfig 流程中的校验、计算或对象转换。
     * @return 返回 connectorConfig 流程生成的业务结果。
     */
    private Map<String, Object> connectorConfig(DataSourceConfigDO source) {
        String json = source.getConnectorConfigJson();
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, CONNECTOR_CONFIG_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param url url 参数，用于 appendQueryParameters 流程中的校验、计算或对象转换。
     * @param ListMapString list map string 参数，用于 appendQueryParameters 流程中的校验、计算或对象转换。
     * @param parameters parameters 参数，用于 appendQueryParameters 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 appendQueryParameters 流程中的校验、计算或对象转换。
     * @param variables variables 参数，用于 appendQueryParameters 流程中的校验、计算或对象转换。
     * @return 返回 append query parameters 生成的文本或业务键。
     */
    private String appendQueryParameters(String url, List<Map<String, Object>> parameters, Map<String, String> variables) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (parameters.isEmpty()) {
            return url;
        }
        StringBuilder builder = new StringBuilder(url == null ? "" : url);
        boolean hasQuery = builder.indexOf("?") >= 0;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map<String, Object> parameter : parameters) {
            String name = stringValue(parameter.get("name"));
            String value = renderTemplate(stringValue(parameter.get("value")), variables);
            if (name.isBlank() || value.isBlank() || "***".equals(value)) {
                continue;
            }
            builder.append(hasQuery ? "&" : "?");
            hasQuery = true;
            builder.append(urlEncode(name)).append("=").append(urlEncode(value));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return builder.toString();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<Map<String, Object>> listConfig(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(normalized);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param root root 参数，用于 selectJsonPath 流程中的校验、计算或对象转换。
     * @param path path 参数，用于 selectJsonPath 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private JsonNode selectJsonPath(JsonNode root, String path) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (root == null || path == null || path.isBlank() || "$".equals(path.trim())) {
            return root;
        }
        JsonNode current = root;
        String normalized = path.trim();
        if (normalized.startsWith("$.")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("$")) {
            normalized = normalized.substring(1);
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String segment : normalized.split("\\.")) {
            if (segment.isBlank()) {
                continue;
            }
            current = current == null ? null : current.path(segment);
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return current == null ? objectMapper.nullNode() : current;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param source source 参数，用于 filePath 流程中的校验、计算或对象转换。
     * @return 返回 filePath 流程生成的业务结果。
     */
    private Path filePath(DataSourceConfigDO source) {
        // 准备本次处理所需的上下文和中间变量。
        String url = blankToDefault(source.getUrl(), "");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (url.isBlank()) {
            throw new IllegalArgumentException("BI file datasource URL is required");
        }
        URI uri = URI.create(url);
        if (uri.getScheme() == null || uri.getScheme().isBlank()) {
            return Path.of(url);
        }
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("BI file datasource only supports file:// URLs");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return Path.of(uri);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 delimiter 流程中的校验、计算或对象转换。
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @return 返回 delimiter 生成的文本或业务键。
     */
    private String delimiter(Map<String, Object> config) {
        String delimiter = stringConfig(config, "delimiter", ",");
        if ("\\t".equals(delimiter) || "TAB".equalsIgnoreCase(delimiter)) {
            return "\t";
        }
        return delimiter.isBlank() ? "," : delimiter.substring(0, 1);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param line line 参数，用于 parseDelimitedLine 流程中的校验、计算或对象转换。
     * @param delimiter delimiter 参数，用于 parseDelimitedLine 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> parseDelimitedLine(String line, String delimiter) {
        List<String> values = new ArrayList<>();
        char separator = delimiter == null || delimiter.isEmpty() ? ',' : delimiter.charAt(0);
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int index = 0; index < (line == null ? 0 : line.length()); index++) {
            char value = line.charAt(index);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (value == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (value == separator && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        values.add(current.toString().trim());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return values;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param rawHeaders raw headers 参数，用于 normalizeHeaders 流程中的校验、计算或对象转换。
     * @param path path 参数，用于 normalizeHeaders 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private List<String> normalizeHeaders(List<String> rawHeaders, Path path) {
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < rawHeaders.size(); index++) {
            String header = rawHeaders.get(index);
            headers.add(header == null || header.isBlank() ? "column_" + (index + 1) : header.trim());
        }
        return headers.isEmpty() ? List.of(fileTableName(null, path) + "_value") : headers;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param size 分页或数量限制，避免一次处理过多数据。
     * @return 返回 generated headers 汇总后的集合、分页或映射视图。
     */
    private List<String> generatedHeaders(int size) {
        int count = Math.max(1, size);
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            headers.add("column_" + (index + 1));
        }
        return headers;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param headers 待处理业务值，用于规则计算、转换或外部调用。
     * @param values values 参数，用于 csvRow 流程中的校验、计算或对象转换。
     * @return 返回 csvRow 流程生成的业务结果。
     */
    private JsonNode csvRow(List<String> headers, List<String> values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < values.size() ? values.get(index) : "";
            row.put(headers.get(index), csvScalar(value));
        }
        return objectMapper.valueToTree(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 csvScalar 流程生成的业务结果。
     */
    private Object csvScalar(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            // Try floating point below.
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            return trimmed;
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param headers 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 varchar columns 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasourceColumnPreview> varcharColumns(List<String> headers) {
        List<BiDatasourceColumnPreview> columns = new ArrayList<>();
        int ordinal = 1;
        for (String header : headers) {
            columns.add(new BiDatasourceColumnPreview(header, "VARCHAR", Types.VARCHAR, true, ordinal++));
        }
        return columns;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param source source 参数，用于 fileTableName 流程中的校验、计算或对象转换。
     * @param path path 参数，用于 fileTableName 流程中的校验、计算或对象转换。
     * @return 返回 file table name 生成的文本或业务键。
     */
    private String fileTableName(DataSourceConfigDO source, Path path) {
        String fallback = path == null ? "file_upload" : path.getFileName().toString();
        String fileName = source == null ? fallback : stringConfig(connectorConfig(source), "fileName", fallback);
        int dot = fileName.lastIndexOf('.');
        String tableName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return tableName.isBlank() ? "file_upload" : tableName;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param workbook workbook 参数，用于 workbookSheet 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 workbookSheet 流程中的校验、计算或对象转换。
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @return 返回 workbookSheet 流程生成的业务结果。
     */
    private Sheet workbookSheet(Workbook workbook, Map<String, Object> config) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (workbook == null || workbook.getNumberOfSheets() < 1) {
            throw new IllegalArgumentException("BI file datasource workbook has no sheets");
        }
        String sheetName = stringConfig(config, "sheetName", "");
        if (!sheetName.isBlank()) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("BI file datasource workbook sheet not found: " + sheetName);
            }
            return sheet;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return workbook.getSheetAt(0);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param sheet sheet 参数，用于 firstWorkbookRow 流程中的校验、计算或对象转换。
     * @return 返回 firstWorkbookRow 流程生成的业务结果。
     */
    private Row firstWorkbookRow(Sheet sheet) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (sheet == null) {
            return null;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (workbookRowValues(row, new DataFormatter(Locale.ROOT)).stream().anyMatch(value -> !value.isBlank())) {
                return row;
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param formatter formatter 参数，用于 workbookRowValues 流程中的校验、计算或对象转换。
     * @return 返回 workbook row values 汇总后的集合、分页或映射视图。
     */
    private List<String> workbookRowValues(Row row, DataFormatter formatter) {
        if (row == null || row.getLastCellNum() < 0) {
            return List.of();
        }
        DataFormatter safeFormatter = formatter == null ? new DataFormatter(Locale.ROOT) : formatter;
        List<String> values = new ArrayList<>();
        for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell == null ? "" : safeFormatter.formatCellValue(cell).trim());
        }
        return values;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param rowsNode rows node 参数，用于 rows 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 rows 汇总后的集合、分页或映射视图。
     */
    private List<JsonNode> rows(JsonNode rowsNode, int limit) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (rowsNode == null || rowsNode.isMissingNode() || rowsNode.isNull()) {
            return List.of();
        }
        List<JsonNode> rows = new ArrayList<>();
        if (rowsNode.isArray()) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (JsonNode row : rowsNode) {
                if (rows.size() >= limit) {
                    break;
                }
                if (row.isObject()) {
                    rows.add(row);
                }
            }
            return rows;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return rowsNode.isObject() ? List.of(rowsNode) : List.of();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rowNode row node 参数，用于 apiRow 流程中的校验、计算或对象转换。
     * @return 返回 apiRow 流程生成的业务结果。
     */
    private Map<String, Object> apiRow(JsonNode rowNode) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (rowNode == null || rowNode.isNull()) {
            return row;
        }
        if (!rowNode.isObject()) {
            row.put("value", jsonValue(rowNode));
            return row;
        }
        rowNode.fields().forEachRemaining(entry -> row.put(entry.getKey(), jsonValue(entry.getValue())));
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 jsonValue 流程生成的业务结果。
     */
    private Object jsonValue(JsonNode value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return value.toString();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param MapString map string 参数，用于 limitApiColumns 流程中的校验、计算或对象转换。
     * @param row 持久化行数据，承载数据库记录内容。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Map<String, Object> limitApiColumns(Map<String, Object> row, int limit) {
        Map<String, Object> limited = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (count++ >= limit) {
                break;
            }
            limited.put(entry.getKey(), entry.getValue());
        }
        return limited;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ListMapString list map string 参数，用于 inferApiPreviewColumns 流程中的校验、计算或对象转换。
     * @param rows rows 参数，用于 inferApiPreviewColumns 流程中的校验、计算或对象转换。
     * @return 返回 infer api preview columns 汇总后的集合、分页或映射视图。
     */
    private List<BiQueryColumn> inferApiPreviewColumns(List<Map<String, Object>> rows) {
        Map<String, String> columns = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                columns.putIfAbsent(entry.getKey(), apiPreviewType(entry.getValue()));
                // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                if (columns.size() >= API_COLUMN_LIMIT) {
                    return apiPreviewColumns(columns);
                }
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return apiPreviewColumns(columns);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 apiPreviewColumns 流程中的校验、计算或对象转换。
     * @param columns columns 参数，用于 apiPreviewColumns 流程中的校验、计算或对象转换。
     * @return 返回 api preview columns 汇总后的集合、分页或映射视图。
     */
    private List<BiQueryColumn> apiPreviewColumns(Map<String, String> columns) {
        return columns.entrySet()
                .stream()
                .map(entry -> new BiQueryColumn(entry.getKey(), "DIMENSION", entry.getValue()))
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 api preview type 生成的文本或业务键。
     */
    private String apiPreviewType(Object value) {
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        return "STRING";
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 inferColumns 流程中的校验、计算或对象转换。
     * @return 返回 infer columns 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasourceColumnPreview> inferColumns(List<JsonNode> rows) {
        Map<String, ColumnInference> columns = new LinkedHashMap<>();
        for (JsonNode row : rows == null ? List.<JsonNode>of() : rows) {
            row.fields().forEachRemaining(entry -> columns
                    .computeIfAbsent(entry.getKey(), ignored -> new ColumnInference())
                    .observe(entry.getValue()));
        }
        List<BiDatasourceColumnPreview> previews = new ArrayList<>();
        int ordinal = 1;
        for (Map.Entry<String, ColumnInference> entry : columns.entrySet()) {
            ColumnInference inference = entry.getValue();
            previews.add(new BiDatasourceColumnPreview(
                    entry.getKey(),
                    inference.typeName(),
                    inference.dataType(),
                    inference.nullable(),
                    ordinal++));
        }
        return previews;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param metaData meta data 参数，用于 columns 流程中的校验、计算或对象转换。
     * @param catalog catalog 参数，用于 columns 流程中的校验、计算或对象转换。
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @return 返回 columns 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasourceColumnPreview> columns(DatabaseMetaData metaData, String catalog, String tableName)
            throws Exception {
        List<BiDatasourceColumnPreview> columns = new ArrayList<>();
        try (ResultSet columnRows = metaData.getColumns(catalog, null, tableName, "%")) {
            while (columnRows.next()) {
                columns.add(new BiDatasourceColumnPreview(
                        columnRows.getString("COLUMN_NAME"),
                        columnRows.getString("TYPE_NAME"),
                        columnRows.getInt("DATA_TYPE"),
                        columnRows.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                        columnRows.getInt("ORDINAL_POSITION")));
            }
        }
        return columns;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 requireSource 流程生成的业务结果。
     */
    private DataSourceConfigDO requireSource(Long sourceId, Long tenantId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("BI data source id is required");
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        DataSourceConfigDO source = dataSourceConfigMapper.selectById(sourceId);
        if (source == null || !scopedTenantId.equals(source.getTenantId())) {
            throw new AccessDeniedException("BI data source access denied");
        }
        return source;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param source source 参数，用于 snapshotRow 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param errorMessage error message 参数，用于 snapshotRow 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 snapshotRow 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     * @param syncedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 snapshotRow 流程生成的业务结果。
     */
    private BiDatasourceSchemaSnapshotDO snapshotRow(DataSourceConfigDO source,
                                                     Long tenantId,
                                                     String status,
                                                     String errorMessage,
                                                     List<BiDatasourceTablePreview> tables,
                                                     String username,
                                                     LocalDateTime syncedAt) {
        BiDatasourceSchemaSnapshotDO row = new BiDatasourceSchemaSnapshotDO();
        row.setTenantId(tenantId);
        row.setDataSourceConfigId(source.getId());
        row.setSourceKey(sourceKey(source));
        row.setConnectorType(connectorType(source));
        row.setSchemaJson(serializeTables(tables));
        row.setSyncStatus(status);
        row.setErrorMessage(errorMessage);
        row.setTableCount(tables == null ? 0 : tables.size());
        row.setColumnCount(countColumns(tables));
        row.setSyncedBy(blankToDefault(username, "system"));
        row.setSyncedAt(syncedAt == null ? LocalDateTime.now() : syncedAt);
        return row;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param source source 参数，用于 toSnapshotView 流程中的校验、计算或对象转换。
     * @param tables tables 参数，用于 toSnapshotView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasourceSchemaSnapshotView toSnapshotView(BiDatasourceSchemaSnapshotDO row,
                                                          DataSourceConfigDO source,
                                                          List<BiDatasourceTablePreview> tables) {
        return new BiDatasourceSchemaSnapshotView(
                row.getId(),
                row.getDataSourceConfigId(),
                row.getSourceKey(),
                blankToDefault(source.getName(), row.getSourceKey()),
                row.getConnectorType(),
                row.getSyncStatus(),
                row.getErrorMessage(),
                row.getTableCount(),
                row.getColumnCount(),
                tables == null ? List.of() : tables,
                row.getSyncedAt(),
                row.getSyncedBy());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tables tables 参数，用于 serializeTables 流程中的校验、计算或对象转换。
     * @return 返回 serialize tables 生成的文本或业务键。
     */
    private String serializeTables(List<BiDatasourceTablePreview> tables) {
        try {
            return objectMapper.writeValueAsString(tables == null ? List.of() : tables);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("BI datasource schema snapshot serialization failed", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param schemaJson JSON 字符串，承载结构化配置或明细。
     * @return 返回 deserialize tables 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasourceTablePreview> deserializeTables(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(schemaJson, TABLES_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 统计符合条件的数据规模或状态数量。
     *
     * @param tables tables 参数，用于 countColumns 流程中的校验、计算或对象转换。
     * @return 返回统计数量。
     */
    private int countColumns(List<BiDatasourceTablePreview> tables) {
        return tables == null ? 0 : tables.stream()
                .mapToInt(table -> table.columns() == null ? 0 : table.columns().size())
                .sum();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回 require schema snapshot mapper 汇总后的集合、分页或映射视图。
     */
    private BiDatasourceSchemaSnapshotMapper requireSchemaSnapshotMapper() {
        if (schemaSnapshotMapper == null) {
            throw new IllegalStateException("BI datasource schema snapshot mapper is not configured");
        }
        return schemaSnapshotMapper;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param password password 参数，用于 decryptPassword 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String decryptPassword(String password) {
        if (password == null || password.isBlank()) {
            return password;
        }
        if (credentialCipher != null && credentialCipher.isEncrypted(password)) {
            return credentialCipher.decrypt(password);
        }
        return secretCipher == null ? password : secretCipher.decrypt(password);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @param source source 参数，用于 sanitizeErrorMessage 流程中的校验、计算或对象转换。
     * @param password password 参数，用于 sanitizeErrorMessage 流程中的校验、计算或对象转换。
     * @return 返回 sanitize error message 生成的文本或业务键。
     */
    private static String sanitizeErrorMessage(String message, DataSourceConfigDO source, String password) {
        // 准备本次处理所需的上下文和中间变量。
        String sanitized = message == null || message.isBlank() ? "schema sync failed" : message;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (password != null && !password.isBlank()) {
            sanitized = sanitized.replace(password, "***");
        }
        if (source.getPassword() != null && !source.getPassword().isBlank()) {
            sanitized = sanitized.replace(source.getPassword(), "***");
        }
        if (source.getUrl() != null && !source.getUrl().isBlank()) {
            sanitized = sanitized.replace(source.getUrl(), "jdbc-url");
        }
        sanitized = SECRET_URL_PARAMETER.matcher(sanitized).replaceAll("$1=***");
        // 汇总前面计算出的状态和明细，返回给调用方。
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param source source 参数，用于 openJdbcConnection 流程中的校验、计算或对象转换。
     * @param password password 参数，用于 openJdbcConnection 流程中的校验、计算或对象转换。
     * @return 返回 openJdbcConnection 流程生成的业务结果。
     */
    private static Connection openJdbcConnection(DataSourceConfigDO source, String password) throws Exception {
        if (source.getDriverClassName() != null && !source.getDriverClassName().isBlank()) {
            Class.forName(source.getDriverClassName());
        }
        return DriverManager.getConnection(source.getUrl(), source.getUsername(), password);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param source source 参数，用于 sourceKey 流程中的校验、计算或对象转换。
     * @return 返回 source key 生成的文本或业务键。
     */
    private static String sourceKey(DataSourceConfigDO source) {
        if (isApiSource(source)) {
            return "api-" + (source.getId() == null ? "new" : source.getId());
        }
        if (isFileSource(source)) {
            return "file-" + (source.getId() == null ? "new" : source.getId());
        }
        return "jdbc-" + (source.getId() == null ? "new" : source.getId());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param source source 参数，用于 connectorType 流程中的校验、计算或对象转换。
     * @return 返回 connector type 生成的文本或业务键。
     */
    private static String connectorType(DataSourceConfigDO source) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (isApiSource(source)) {
            return source.getConnectorType() == null || source.getConnectorType().isBlank()
                    ? "API"
                    : source.getConnectorType().trim().toUpperCase();
        }
        if (isFileSource(source)) {
            return "CSV_EXCEL";
        }
        String value = lower(source.getDriverClassName()) + " " + lower(source.getUrl()) + " "
                + lower(source.getName()) + " " + lower(source.getDescription());
        if (value.contains("doris")) {
            return "DORIS";
        }
        if (value.contains("postgresql") || value.contains(":postgres:")) {
            return "POSTGRESQL";
        }
        if (value.contains("clickhouse")) {
            return "CLICKHOUSE";
        }
        if (value.contains("oracle")) {
            return "ORACLE";
        }
        if (value.contains("sqlserver") || value.contains("microsoft")) {
            return "SQLSERVER";
        }
        if (value.contains("mysql")) {
            return "MYSQL";
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "JDBC";
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param source source 参数，用于 isApiSource 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isApiSource(DataSourceConfigDO source) {
        return source != null
                && ("API".equalsIgnoreCase(source.getType())
                || "API".equalsIgnoreCase(source.getConnectorType())
                || "HTTP_JSON".equalsIgnoreCase(source.getDriverClassName()));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param source source 参数，用于 isFileSource 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isFileSource(DataSourceConfigDO source) {
        return source != null
                && ("FILE".equalsIgnoreCase(source.getType())
                || "CSV_EXCEL".equalsIgnoreCase(source.getConnectorType())
                || "FILE_UPLOAD".equalsIgnoreCase(source.getDriverClassName()));
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 200);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param startNanos start nanos 参数，用于 durationMs 流程中的校验、计算或对象转换。
     * @return 返回 duration ms 计算得到的数量、金额或指标值。
     */
    private static Long durationMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 lower 生成的文本或业务键。
     */
    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 blankToDefault 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param MapString map string 参数，用于 stringConfig 流程中的校验、计算或对象转换。
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param key 业务键，用于在同一租户下定位资源。
     * @param fallback fallback 参数，用于 stringConfig 流程中的校验、计算或对象转换。
     * @return 返回 string config 生成的文本或业务键。
     */
    private static String stringConfig(Map<String, Object> config, String key, String fallback) {
        Object value = config == null ? null : config.get(key);
        String stringValue = stringValue(value);
        return stringValue.isBlank() ? fallback : stringValue;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 request variables 生成的文本或业务键。
     */
    private static Map<String, String> requestVariables(BiDatasourceApiPreviewRequest request) {
        return request == null ? Map.of() : request.variables();
    }

    /**
     * FilePreviewRows 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record FilePreviewRows(String tableName,
                                   String fileType,
                                   List<String> headers,
                                   List<JsonNode> rows,
                                   boolean truncated) {
        private FilePreviewRows {
            headers = headers == null ? List.of() : List.copyOf(headers);
            rows = rows == null ? List.of() : List.copyOf(rows);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 api row limit 计算得到的数量、金额或指标值。
     */
    private static int apiRowLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return API_ROW_LIMIT;
        }
        return Math.min(limit, API_ROW_LIMIT);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param MapString map string 参数，用于 renderTemplate 流程中的校验、计算或对象转换。
     * @param variables variables 参数，用于 renderTemplate 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private static String renderTemplate(String value, Map<String, String> variables) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null || value.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = TEMPLATE_PARAMETER.matcher(value);
        StringBuffer rendered = new StringBuffer();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        while (matcher.find()) {
            String key = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            String replacement = variables.get(key);
            if (replacement == null) {
                throw new IllegalArgumentException("Missing BI API datasource variable: " + key);
            }
            matcher.appendReplacement(rendered, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return rendered.toString();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 boolean value 的布尔判断结果。
     */
    private static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 url encode 生成的文本或业务键。
     */
    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @return 返回 defaultApiHttpClient 流程生成的业务结果。
     */
    private static ApiHttpClient defaultApiHttpClient() {
        HttpClient client = HttpClient.newHttpClient();
        return request -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()));
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            request.headers().forEach(builder::header);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (request.body() == null || request.body().isBlank()) {
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(request.method(), HttpRequest.BodyPublishers.ofString(request.body()));
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new ApiHttpResponse(response.statusCode(), "", response.body());
        };
    }

    @FunctionalInterface
    /**
     * JdbcConnectionFactory 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface JdbcConnectionFactory {
        /**
         * 创建业务对象并完成必要的初始化。
         *
         * @param source source 参数，用于 open 流程中的校验、计算或对象转换。
         * @param password password 参数，用于 open 流程中的校验、计算或对象转换。
         * @return 返回 open 流程生成的业务结果。
         */
        Connection open(DataSourceConfigDO source, String password) throws Exception;
    }

    @FunctionalInterface
    /**
     * ApiHttpClient 承载对应领域的业务规则、流程编排和结果转换。
     */
    public interface ApiHttpClient {
        /**
         * 执行核心业务流程，并协调依赖组件完成处理。
         *
         * @param request 请求对象，承载本次操作的输入参数。
         * @return 返回流程执行后的业务结果。
         */
        ApiHttpResponse execute(ApiHttpRequest request) throws Exception;
    }

    /**
     * ApiHttpRequest 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ApiHttpRequest(String method, String url, Map<String, String> headers, String body) {
        public ApiHttpRequest {
            method = method == null || method.isBlank() ? "GET" : method.trim().toUpperCase(Locale.ROOT);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }

    /**
     * ApiHttpResponse 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ApiHttpResponse(int statusCode, String statusText, String body) {
    }

    /**
     * ColumnInference 承载对应领域的业务规则、流程编排和结果转换。
     */
    private static class ColumnInference {
        private String typeName;
        private int dataType;
        private boolean nullable;

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @param value 待处理值，用于规则计算或转换。
         */
        void observe(JsonNode value) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (value == null || value.isNull() || value.isMissingNode()) {
                nullable = true;
                return;
            }
            String observedTypeName;
            int observedDataType;
            if (value.isIntegralNumber()) {
                observedTypeName = "BIGINT";
                observedDataType = Types.BIGINT;
            } else if (value.isFloatingPointNumber() || value.isBigDecimal()) {
                observedTypeName = "DOUBLE";
                observedDataType = Types.DOUBLE;
            } else if (value.isBoolean()) {
                observedTypeName = "BOOLEAN";
                observedDataType = Types.BOOLEAN;
            } else {
                observedTypeName = "VARCHAR";
                observedDataType = Types.VARCHAR;
            }
            if (typeName == null) {
                typeName = observedTypeName;
                dataType = observedDataType;
            } else if (typeName.equals(observedTypeName)) {
                // 汇总前面计算出的状态和明细，返回给调用方。
                return;
            } else if (isNumeric(typeName) && isNumeric(observedTypeName)) {
                typeName = "DOUBLE";
                dataType = Types.DOUBLE;
            } else {
                typeName = "VARCHAR";
                dataType = Types.VARCHAR;
            }
        }

        /**
         * 校验输入、权限或业务前置条件。
         *
         * @param value 待处理值，用于规则计算或转换。
         * @return 返回布尔判断结果。
         */
        private boolean isNumeric(String value) {
            return "BIGINT".equals(value) || "DOUBLE".equals(value);
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 type name 生成的文本或业务键。
         */
        String typeName() {
            return typeName == null ? "VARCHAR" : typeName;
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 data type 计算得到的数量、金额或指标值。
         */
        int dataType() {
            return typeName == null ? Types.VARCHAR : dataType;
        }

        /**
         * 根据方法职责完成对应的业务处理流程。
         *
         * @return 返回 nullable 的布尔判断结果。
         */
        boolean nullable() {
            return nullable;
        }
    }
}
