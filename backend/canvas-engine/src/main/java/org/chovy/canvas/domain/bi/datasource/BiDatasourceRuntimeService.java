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

    public BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                                      SecretCipher secretCipher,
                                      DataSourceCredentialCipher credentialCipher) {
        this(dataSourceConfigMapper, null, secretCipher, credentialCipher, new ObjectMapper(),
                BiDatasourceRuntimeService::openJdbcConnection, defaultApiHttpClient());
    }

    @Autowired
    public BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                                      BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                                      SecretCipher secretCipher,
                                      DataSourceCredentialCipher credentialCipher,
                                      ObjectMapper objectMapper) {
        this(dataSourceConfigMapper, schemaSnapshotMapper, secretCipher, credentialCipher, objectMapper,
                BiDatasourceRuntimeService::openJdbcConnection, defaultApiHttpClient());
    }

    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               JdbcConnectionFactory connectionFactory) {
        this(dataSourceConfigMapper, null, secretCipher, credentialCipher, new ObjectMapper(), connectionFactory,
                defaultApiHttpClient());
    }

    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               ApiHttpClient apiHttpClient) {
        this(dataSourceConfigMapper, null, secretCipher, credentialCipher, new ObjectMapper(),
                BiDatasourceRuntimeService::openJdbcConnection, apiHttpClient);
    }

    BiDatasourceRuntimeService(DataSourceConfigMapper dataSourceConfigMapper,
                               BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                               SecretCipher secretCipher,
                               DataSourceCredentialCipher credentialCipher,
                               ObjectMapper objectMapper,
                               JdbcConnectionFactory connectionFactory) {
        this(dataSourceConfigMapper, schemaSnapshotMapper, secretCipher, credentialCipher, objectMapper,
                connectionFactory, defaultApiHttpClient());
    }

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

    public BiDatasourceConnectionTestResult testConnection(Long sourceId, Long tenantId) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        long start = System.nanoTime();
        LocalDateTime checkedAt = LocalDateTime.now();
        if (isApiSource(source)) {
            String password = decryptPassword(source.getPassword());
            try {
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

    public BiDatasourceSchemaPreview previewSchema(Long sourceId, Long tenantId, int limit) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        String password = decryptPassword(source.getPassword());
        try {
            return readSchema(source, password, limit, Map.of());
        } catch (Exception e) {
            throw new IllegalStateException("BI datasource schema preview failed", e);
        }
    }

    public BiDatasourceApiPreview previewApiData(Long sourceId, Long tenantId, BiDatasourceApiPreviewRequest request) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        if (!isApiSource(source)) {
            throw new IllegalArgumentException("BI datasource is not an API connector: " + connectorType(source));
        }
        long start = System.nanoTime();
        LocalDateTime checkedAt = LocalDateTime.now();
        String password = decryptPassword(source.getPassword());
        try {
            Map<String, Object> config = connectorConfig(source);
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

    public BiDatasourceApiPreview previewFileData(Long sourceId, Long tenantId, int limit) {
        DataSourceConfigDO source = requireSource(sourceId, tenantId);
        if (!isFileSource(source)) {
            throw new IllegalArgumentException("BI datasource is not a file connector: " + connectorType(source));
        }
        long start = System.nanoTime();
        LocalDateTime checkedAt = LocalDateTime.now();
        try {
            FilePreviewRows fileRows = readFileRows(source, limit);
            List<Map<String, Object>> previewRows = fileRows.rows().stream()
                    .map(this::apiRow)
                    .map(row -> limitApiColumns(row, API_COLUMN_LIMIT))
                    .toList();
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

    public BiDatasourceSchemaSnapshotView syncSchema(Long sourceId, Long tenantId, String username, int limit) {
        return syncSchema(sourceId, tenantId, username, limit, null);
    }

    public BiDatasourceSchemaSnapshotView syncSchema(Long sourceId,
                                                     Long tenantId,
                                                     String username,
                                                     int limit,
                                                     BiDatasourceApiPreviewRequest request) {
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
            return toSnapshotView(row, source, List.of());
        }
    }

    public BiDatasourceSchemaSnapshotView latestSchemaSnapshot(Long sourceId, Long tenantId) {
        List<BiDatasourceSchemaSnapshotView> history = schemaSnapshotHistory(sourceId, tenantId, 1);
        return history.isEmpty() ? null : history.get(0);
    }

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

    private BiDatasourceSchemaPreview readSchema(DataSourceConfigDO source,
                                                 String password,
                                                 int limit,
                                                 Map<String, String> variables) throws Exception {
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
                while (tableRows.next() && tables.size() < boundedLimit(limit)) {
                    String tableName = tableRows.getString("TABLE_NAME");
                    tables.add(new BiDatasourceTablePreview(
                            tableName,
                            tableRows.getString("TABLE_TYPE"),
                            columns(metaData, catalog, tableName)));
                }
            }
            return new BiDatasourceSchemaPreview(
                    source.getId(),
                    sourceKey(source),
                    blankToDefault(source.getName(), sourceKey(source)),
                    connectorType(source),
                    tables,
                    LocalDateTime.now());
        }
    }

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
            while ((line = reader.readLine()) != null && rows.size() <= rowLimit) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(csvRow(headers, parseDelimitedLine(line, delimiter)));
            }
        }
        boolean truncated = rows.size() > rowLimit;
        return new FilePreviewRows(
                fileTableName(source, path),
                fileType,
                headers,
                truncated ? rows.stream().limit(rowLimit).toList() : rows,
                truncated);
    }

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
        return new FilePreviewRows(
                fileTableName(source, path),
                fileType,
                headers,
                truncated ? rows.stream().limit(rowLimit).toList() : rows,
                truncated);
    }

    private BiDatasourceSchemaPreview readApiSchema(DataSourceConfigDO source,
                                                    String password,
                                                    int limit,
                                                    Map<String, String> variables) throws Exception {
        ApiHttpResponse response = apiHttpClient.execute(apiRequest(source, password, variables == null ? Map.of() : variables));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " " + blankToDefault(response.statusText(), ""));
        }
        JsonNode root = objectMapper.readTree(blankToDefault(response.body(), "{}"));
        JsonNode rowsNode = selectJsonPath(root, stringConfig(connectorConfig(source), "responseRowsPath", "$"));
        List<JsonNode> rows = rows(rowsNode, boundedLimit(limit));
        return new BiDatasourceSchemaPreview(
                source.getId(),
                sourceKey(source),
                blankToDefault(source.getName(), sourceKey(source)),
                connectorType(source),
                List.of(new BiDatasourceTablePreview("api_response", "HTTP_JSON", inferColumns(rows))),
                LocalDateTime.now());
    }

    private ApiHttpRequest apiRequest(DataSourceConfigDO source, String password, Map<String, String> variables) {
        Map<String, Object> config = connectorConfig(source);
        String method = stringConfig(config, "requestMethod", "GET").toUpperCase(Locale.ROOT);
        String url = appendQueryParameters(source.getUrl(), listConfig(config.get("parameters")), variables);
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map<String, Object> header : listConfig(config.get("headers"))) {
            String name = stringValue(header.get("name"));
            String value = renderTemplate(stringValue(header.get("value")), variables);
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
        return new ApiHttpRequest(method, url, headers, body.isBlank() ? null : body);
    }

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

    private String appendQueryParameters(String url, List<Map<String, Object>> parameters, Map<String, String> variables) {
        if (parameters.isEmpty()) {
            return url;
        }
        StringBuilder builder = new StringBuilder(url == null ? "" : url);
        boolean hasQuery = builder.indexOf("?") >= 0;
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
        return builder.toString();
    }

    private List<Map<String, Object>> listConfig(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> rawMap) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                result.add(normalized);
            }
        }
        return result;
    }

    private JsonNode selectJsonPath(JsonNode root, String path) {
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
        for (String segment : normalized.split("\\.")) {
            if (segment.isBlank()) {
                continue;
            }
            current = current == null ? null : current.path(segment);
        }
        return current == null ? objectMapper.nullNode() : current;
    }

    private Path filePath(DataSourceConfigDO source) {
        String url = blankToDefault(source.getUrl(), "");
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
        return Path.of(uri);
    }

    private String delimiter(Map<String, Object> config) {
        String delimiter = stringConfig(config, "delimiter", ",");
        if ("\\t".equals(delimiter) || "TAB".equalsIgnoreCase(delimiter)) {
            return "\t";
        }
        return delimiter.isBlank() ? "," : delimiter.substring(0, 1);
    }

    private List<String> parseDelimitedLine(String line, String delimiter) {
        List<String> values = new ArrayList<>();
        char separator = delimiter == null || delimiter.isEmpty() ? ',' : delimiter.charAt(0);
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < (line == null ? 0 : line.length()); index++) {
            char value = line.charAt(index);
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
        return values;
    }

    private List<String> normalizeHeaders(List<String> rawHeaders, Path path) {
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < rawHeaders.size(); index++) {
            String header = rawHeaders.get(index);
            headers.add(header == null || header.isBlank() ? "column_" + (index + 1) : header.trim());
        }
        return headers.isEmpty() ? List.of(fileTableName(null, path) + "_value") : headers;
    }

    private List<String> generatedHeaders(int size) {
        int count = Math.max(1, size);
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            headers.add("column_" + (index + 1));
        }
        return headers;
    }

    private JsonNode csvRow(List<String> headers, List<String> values) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < values.size() ? values.get(index) : "";
            row.put(headers.get(index), csvScalar(value));
        }
        return objectMapper.valueToTree(row);
    }

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

    private List<BiDatasourceColumnPreview> varcharColumns(List<String> headers) {
        List<BiDatasourceColumnPreview> columns = new ArrayList<>();
        int ordinal = 1;
        for (String header : headers) {
            columns.add(new BiDatasourceColumnPreview(header, "VARCHAR", Types.VARCHAR, true, ordinal++));
        }
        return columns;
    }

    private String fileTableName(DataSourceConfigDO source, Path path) {
        String fallback = path == null ? "file_upload" : path.getFileName().toString();
        String fileName = source == null ? fallback : stringConfig(connectorConfig(source), "fileName", fallback);
        int dot = fileName.lastIndexOf('.');
        String tableName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return tableName.isBlank() ? "file_upload" : tableName;
    }

    private Sheet workbookSheet(Workbook workbook, Map<String, Object> config) {
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
        return workbook.getSheetAt(0);
    }

    private Row firstWorkbookRow(Sheet sheet) {
        if (sheet == null) {
            return null;
        }
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (workbookRowValues(row, new DataFormatter(Locale.ROOT)).stream().anyMatch(value -> !value.isBlank())) {
                return row;
            }
        }
        return null;
    }

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

    private List<JsonNode> rows(JsonNode rowsNode, int limit) {
        if (rowsNode == null || rowsNode.isMissingNode() || rowsNode.isNull()) {
            return List.of();
        }
        List<JsonNode> rows = new ArrayList<>();
        if (rowsNode.isArray()) {
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
        return rowsNode.isObject() ? List.of(rowsNode) : List.of();
    }

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

    private Object jsonValue(JsonNode value) {
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
        return value.toString();
    }

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

    private List<BiQueryColumn> inferApiPreviewColumns(List<Map<String, Object>> rows) {
        Map<String, String> columns = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                columns.putIfAbsent(entry.getKey(), apiPreviewType(entry.getValue()));
                if (columns.size() >= API_COLUMN_LIMIT) {
                    return apiPreviewColumns(columns);
                }
            }
        }
        return apiPreviewColumns(columns);
    }

    private List<BiQueryColumn> apiPreviewColumns(Map<String, String> columns) {
        return columns.entrySet()
                .stream()
                .map(entry -> new BiQueryColumn(entry.getKey(), "DIMENSION", entry.getValue()))
                .toList();
    }

    private String apiPreviewType(Object value) {
        if (value instanceof Number) {
            return "NUMBER";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        return "STRING";
    }

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

    private String serializeTables(List<BiDatasourceTablePreview> tables) {
        try {
            return objectMapper.writeValueAsString(tables == null ? List.of() : tables);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("BI datasource schema snapshot serialization failed", e);
        }
    }

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

    private int countColumns(List<BiDatasourceTablePreview> tables) {
        return tables == null ? 0 : tables.stream()
                .mapToInt(table -> table.columns() == null ? 0 : table.columns().size())
                .sum();
    }

    private BiDatasourceSchemaSnapshotMapper requireSchemaSnapshotMapper() {
        if (schemaSnapshotMapper == null) {
            throw new IllegalStateException("BI datasource schema snapshot mapper is not configured");
        }
        return schemaSnapshotMapper;
    }

    private String decryptPassword(String password) {
        if (password == null || password.isBlank()) {
            return password;
        }
        if (credentialCipher != null && credentialCipher.isEncrypted(password)) {
            return credentialCipher.decrypt(password);
        }
        return secretCipher == null ? password : secretCipher.decrypt(password);
    }

    private static String sanitizeErrorMessage(String message, DataSourceConfigDO source, String password) {
        String sanitized = message == null || message.isBlank() ? "schema sync failed" : message;
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
        return sanitized.length() > 1000 ? sanitized.substring(0, 1000) : sanitized;
    }

    private static Connection openJdbcConnection(DataSourceConfigDO source, String password) throws Exception {
        if (source.getDriverClassName() != null && !source.getDriverClassName().isBlank()) {
            Class.forName(source.getDriverClassName());
        }
        return DriverManager.getConnection(source.getUrl(), source.getUsername(), password);
    }

    private static String sourceKey(DataSourceConfigDO source) {
        if (isApiSource(source)) {
            return "api-" + (source.getId() == null ? "new" : source.getId());
        }
        if (isFileSource(source)) {
            return "file-" + (source.getId() == null ? "new" : source.getId());
        }
        return "jdbc-" + (source.getId() == null ? "new" : source.getId());
    }

    private static String connectorType(DataSourceConfigDO source) {
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
        return "JDBC";
    }

    private static boolean isApiSource(DataSourceConfigDO source) {
        return source != null
                && ("API".equalsIgnoreCase(source.getType())
                || "API".equalsIgnoreCase(source.getConnectorType())
                || "HTTP_JSON".equalsIgnoreCase(source.getDriverClassName()));
    }

    private static boolean isFileSource(DataSourceConfigDO source) {
        return source != null
                && ("FILE".equalsIgnoreCase(source.getType())
                || "CSV_EXCEL".equalsIgnoreCase(source.getConnectorType())
                || "FILE_UPLOAD".equalsIgnoreCase(source.getDriverClassName()));
    }

    private static int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 200);
    }

    private static Long durationMs(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String stringConfig(Map<String, Object> config, String key, String fallback) {
        Object value = config == null ? null : config.get(key);
        String stringValue = stringValue(value);
        return stringValue.isBlank() ? fallback : stringValue;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Map<String, String> requestVariables(BiDatasourceApiPreviewRequest request) {
        return request == null ? Map.of() : request.variables();
    }

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

    private static int apiRowLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return API_ROW_LIMIT;
        }
        return Math.min(limit, API_ROW_LIMIT);
    }

    private static String renderTemplate(String value, Map<String, String> variables) {
        if (value == null || value.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = TEMPLATE_PARAMETER.matcher(value);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            String replacement = variables.get(key);
            if (replacement == null) {
                throw new IllegalArgumentException("Missing BI API datasource variable: " + key);
            }
            matcher.appendReplacement(rendered, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ApiHttpClient defaultApiHttpClient() {
        HttpClient client = HttpClient.newHttpClient();
        return request -> {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()));
            request.headers().forEach(builder::header);
            if (request.body() == null || request.body().isBlank()) {
                builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(request.method(), HttpRequest.BodyPublishers.ofString(request.body()));
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new ApiHttpResponse(response.statusCode(), "", response.body());
        };
    }

    @FunctionalInterface
    public interface JdbcConnectionFactory {
        Connection open(DataSourceConfigDO source, String password) throws Exception;
    }

    @FunctionalInterface
    public interface ApiHttpClient {
        ApiHttpResponse execute(ApiHttpRequest request) throws Exception;
    }

    public record ApiHttpRequest(String method, String url, Map<String, String> headers, String body) {
        public ApiHttpRequest {
            method = method == null || method.isBlank() ? "GET" : method.trim().toUpperCase(Locale.ROOT);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }
    }

    public record ApiHttpResponse(int statusCode, String statusText, String body) {
    }

    private static class ColumnInference {
        private String typeName;
        private int dataType;
        private boolean nullable;

        void observe(JsonNode value) {
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
                return;
            } else if (isNumeric(typeName) && isNumeric(observedTypeName)) {
                typeName = "DOUBLE";
                dataType = Types.DOUBLE;
            } else {
                typeName = "VARCHAR";
                dataType = Types.VARCHAR;
            }
        }

        private boolean isNumeric(String value) {
            return "BIGINT".equals(value) || "DOUBLE".equals(value);
        }

        String typeName() {
            return typeName == null ? "VARCHAR" : typeName;
        }

        int dataType() {
            return typeName == null ? Types.VARCHAR : dataType;
        }

        boolean nullable() {
            return nullable;
        }
    }
}
