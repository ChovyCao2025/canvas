package org.chovy.canvas.domain.bi.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.BiDatasourceSchemaSnapshotDO;
import org.chovy.canvas.dal.dataobject.DataSourceConfigDO;
import org.chovy.canvas.dal.mapper.BiDatasourceSchemaSnapshotMapper;
import org.chovy.canvas.dal.mapper.DataSourceConfigMapper;
import org.chovy.canvas.domain.datasource.DataSourceCredentialCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BiDatasourceOnboardingService {

    private static final Pattern SECRET_URL_PARAMETER = Pattern.compile(
            "(?i)(^|[?&;])((?:password|pwd|token|authorization|api[-_]?key|access[-_]?key(?:[-_]?id|[-_]?secret)?|secret)=)([^&;]+)");
    private static final Pattern SECRET_CONFIG_KEY = Pattern.compile("(?i).*(password|pwd|secret|token|authorization|api[-_]?key|access[-_]?key).*");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> JDBC_MODES = List.of("DIRECT_QUERY", "CACHE");
    private static final List<String> EXTRACT_MODES = List.of("EXTRACT");
    private static final List<String> JDBC_CAPABILITIES =
            List.of("CONNECTION_TEST", "SCHEMA_SYNC", "SQL_DATASET", "TABLE_DATASET");
    private static final Map<String, BiDatasourceConnectorCapability> CONNECTOR_INDEX = connectorCatalogInternal()
            .stream()
            .collect(Collectors.toUnmodifiableMap(BiDatasourceConnectorCapability::connectorType, connector -> connector));

    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper;
    private final DataSourceCredentialCipher credentialCipher;

    public BiDatasourceOnboardingService(DataSourceConfigMapper dataSourceConfigMapper) {
        this(dataSourceConfigMapper, null, null);
    }

    public BiDatasourceOnboardingService(DataSourceConfigMapper dataSourceConfigMapper,
                                         BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper) {
        this(dataSourceConfigMapper, schemaSnapshotMapper, null);
    }

    @Autowired
    public BiDatasourceOnboardingService(DataSourceConfigMapper dataSourceConfigMapper,
                                         BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                                         DataSourceCredentialCipher credentialCipher) {
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.schemaSnapshotMapper = schemaSnapshotMapper;
        this.credentialCipher = credentialCipher == null
                ? new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET)
                : credentialCipher;
    }

    public List<BiDatasourceConnectorCapability> connectorCatalog() {
        return connectorCatalogInternal();
    }

    public List<BiDatasourceOnboardingView> listOnboardingSources(Long tenantId) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        LambdaQueryWrapper<DataSourceConfigDO> wrapper = new LambdaQueryWrapper<DataSourceConfigDO>()
                .eq(DataSourceConfigDO::getTenantId, scopedTenantId)
                .orderByDesc(DataSourceConfigDO::getId);
        List<DataSourceConfigDO> sources = dataSourceConfigMapper.selectList(wrapper);
        Map<Long, BiDatasourceSchemaSnapshotDO> latestSnapshots = latestSnapshots(scopedTenantId, sources);
        return sources
                .stream()
                .map(config -> toView(config, latestSnapshots.get(config.getId())))
                .toList();
    }

    public BiDatasourceOnboardingView createOnboardingSource(Long tenantId,
                                                             String operator,
                                                             BiDatasourceOnboardingCommand command) {
        DataSourceConfigDO row = new DataSourceConfigDO();
        BiDatasourceConnectorCapability capability = applyCommand(row, command, null);
        row.setTenantId(normalizeTenant(tenantId));
        row.setCreatedBy(blankToDefault(operator, "system"));
        row.setPassword(credentialCipher.encrypt(passwordForCreate(command, capability)));
        dataSourceConfigMapper.insert(row);
        return toView(row, null);
    }

    public BiDatasourceOnboardingView updateOnboardingSource(Long tenantId,
                                                             String operator,
                                                             Long id,
                                                             BiDatasourceOnboardingCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        DataSourceConfigDO existing = dataSourceConfigMapper.selectOne(
                new LambdaQueryWrapper<DataSourceConfigDO>()
                        .eq(DataSourceConfigDO::getTenantId, scopedTenantId)
                        .eq(DataSourceConfigDO::getId, id)
                        .last("LIMIT 1"));
        if (existing == null) {
            throw new IllegalArgumentException("BI datasource not found: " + id);
        }
        DataSourceConfigDO row = new DataSourceConfigDO();
        row.setId(id);
        row.setTenantId(scopedTenantId);
        row.setCreatedBy(existing.getCreatedBy() == null ? blankToDefault(operator, "system") : existing.getCreatedBy());
        BiDatasourceConnectorCapability capability = applyCommand(row, command, existing);
        if (!capability.supportsCredentials()) {
            row.setPassword("");
        } else if (command == null || command.password() == null || command.password().isBlank()) {
            row.setPassword(existing.getPassword());
        } else {
            row.setPassword(credentialCipher.encrypt(command.password()));
        }
        dataSourceConfigMapper.updateById(row);
        return toView(row, null);
    }

    private Map<Long, BiDatasourceSchemaSnapshotDO> latestSnapshots(Long tenantId, List<DataSourceConfigDO> sources) {
        if (schemaSnapshotMapper == null || sources == null || sources.isEmpty()) {
            return Map.of();
        }
        List<Long> sourceIds = sources.stream()
                .map(DataSourceConfigDO::getId)
                .filter(id -> id != null)
                .toList();
        if (sourceIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<BiDatasourceSchemaSnapshotDO> wrapper = new LambdaQueryWrapper<BiDatasourceSchemaSnapshotDO>()
                .eq(BiDatasourceSchemaSnapshotDO::getTenantId, tenantId)
                .in(BiDatasourceSchemaSnapshotDO::getDataSourceConfigId, sourceIds)
                .orderByDesc(BiDatasourceSchemaSnapshotDO::getSyncedAt);
        return schemaSnapshotMapper.selectList(wrapper)
                .stream()
                .collect(Collectors.toMap(
                        BiDatasourceSchemaSnapshotDO::getDataSourceConfigId,
                        snapshot -> snapshot,
                        (latest, ignored) -> latest));
    }

    private BiDatasourceConnectorCapability applyCommand(DataSourceConfigDO row,
                                                         BiDatasourceOnboardingCommand command,
                                                         DataSourceConfigDO existing) {
        if (command == null) {
            throw new IllegalArgumentException("BI datasource onboarding command is required");
        }
        String connectorType = normalizeConnector(command.connectorType());
        BiDatasourceConnectorCapability capability = CONNECTOR_INDEX.get(connectorType);
        if (capability == null || !"AVAILABLE".equals(capability.supportStatus())) {
            throw new IllegalArgumentException("BI datasource connector is not available: " + connectorType);
        }
        row.setName(required(command.name(), "name"));
        row.setType(sourceType(capability));
        row.setConnectorType(connectorType);
        row.setConnectionMode(resolveConnectionMode(command.connectionMode(), capability, existing));
        row.setUrl(datasourceUrl(command, capability));
        row.setUsername(principal(command, capability));
        row.setDriverClassName(driverClassName(command.driverClassName(), capability, existing));
        row.setConnectorConfigJson(connectorConfigJson(command, capability, existing));
        row.setDescription(command.description());
        row.setEnabled(command.enabled() == null || command.enabled() ? 1 : 0);
        return capability;
    }

    private String sourceType(BiDatasourceConnectorCapability capability) {
        if (isHttpJsonConnector(capability)) {
            return "API";
        }
        if (isFileConnector(capability)) {
            return "FILE";
        }
        return "JDBC";
    }

    private String datasourceUrl(BiDatasourceOnboardingCommand command,
                                 BiDatasourceConnectorCapability capability) {
        if (!isFileConnector(capability)) {
            return required(command.url(), "url");
        }
        String requested = stringValue(command.url(), "");
        if (!requested.isBlank()) {
            return requested;
        }
        return "file://" + required(fileName(command.connectorConfig(), ""), "fileName");
    }

    private String principal(BiDatasourceOnboardingCommand command,
                             BiDatasourceConnectorCapability capability) {
        if (capability.supportsCredentials()) {
            return required(command.username(), "username");
        }
        return blankToDefault(command.username(), "file_upload");
    }

    private String passwordForCreate(BiDatasourceOnboardingCommand command,
                                     BiDatasourceConnectorCapability capability) {
        return capability.supportsCredentials() ? required(command.password(), "password") : "";
    }

    private String driverClassName(String requested,
                                   BiDatasourceConnectorCapability capability,
                                   DataSourceConfigDO existing) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        if (isHttpJsonConnector(capability)) {
            return "HTTP_JSON";
        }
        if (isFileConnector(capability)) {
            return "FILE_UPLOAD";
        }
        if (capability.driverClassNames() != null && !capability.driverClassNames().isEmpty()) {
            return capability.driverClassNames().get(0);
        }
        if (existing != null && existing.getDriverClassName() != null && !existing.getDriverClassName().isBlank()) {
            return existing.getDriverClassName();
        }
        return "com.mysql.cj.jdbc.Driver";
    }

    private String connectorConfigJson(BiDatasourceOnboardingCommand command,
                                       BiDatasourceConnectorCapability capability,
                                       DataSourceConfigDO existing) {
        if (isHttpJsonConnector(capability)) {
            return serializeConnectorConfig(apiConnectorConfig(command.connectorConfig()));
        }
        if (isFileConnector(capability)) {
            return serializeConnectorConfig(fileConnectorConfig(command));
        }
        if (command.connectorConfig() != null && !command.connectorConfig().isEmpty()) {
            return serializeConnectorConfig(sanitizeConnectorConfig(command.connectorConfig()));
        }
        return existing == null ? null : existing.getConnectorConfigJson();
    }

    private Map<String, Object> fileConnectorConfig(BiDatasourceOnboardingCommand command) {
        Map<String, Object> raw = command.connectorConfig() == null ? Map.of() : command.connectorConfig();
        Map<String, Object> normalized = new LinkedHashMap<>();
        String fileName = required(fileName(raw, command.url()), "fileName");
        String fileType = normalizeFileType(raw.get("fileType"), fileName);
        normalized.put("fileName", fileName);
        normalized.put("fileType", fileType);
        String sheetName = stringValue(raw.get("sheetName"), "");
        if (!sheetName.isBlank()) {
            normalized.put("sheetName", sheetName);
        }
        String delimiter = stringValue(raw.get("delimiter"), "");
        if (!delimiter.isBlank()) {
            normalized.put("delimiter", delimiter);
        }
        normalized.put("headerRow", booleanValue(raw.get("headerRow"), true));
        normalized.put("encoding", stringValue(raw.get("encoding"), "UTF-8").toUpperCase(Locale.ROOT));
        return normalized;
    }

    private String fileName(Map<String, Object> raw, String url) {
        String requested = stringValue(raw == null ? null : raw.get("fileName"), "");
        if (!requested.isBlank()) {
            return requested;
        }
        String sourceUrl = stringValue(url, "");
        if (sourceUrl.isBlank()) {
            return "";
        }
        int slash = sourceUrl.lastIndexOf('/');
        return slash >= 0 ? sourceUrl.substring(slash + 1).trim() : sourceUrl;
    }

    private String normalizeFileType(Object value, String fileName) {
        String requested = stringValue(value, "");
        String extension = "";
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            extension = fileName.substring(dot + 1);
        }
        String fileType = (requested.isBlank() ? extension : requested).toUpperCase(Locale.ROOT);
        if (fileType.isBlank()) {
            fileType = "CSV";
        }
        if (!List.of("CSV", "XLS", "XLSX").contains(fileType)) {
            throw new IllegalArgumentException("BI file datasource type is not supported: " + fileType);
        }
        return fileType;
    }

    private boolean booleanValue(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private Map<String, Object> apiConnectorConfig(Map<String, Object> rawConfig) {
        Map<String, Object> raw = rawConfig == null ? Map.of() : rawConfig;
        Map<String, Object> normalized = new LinkedHashMap<>();
        String requestMethod = stringValue(raw.get("requestMethod"), "GET").toUpperCase(Locale.ROOT);
        if (!List.of("GET", "POST").contains(requestMethod)) {
            throw new IllegalArgumentException("BI API datasource request method is not supported: " + requestMethod);
        }
        String authType = stringValue(raw.get("authType"), "NONE").toUpperCase(Locale.ROOT);
        if (!List.of("NONE", "BASIC", "BEARER", "API_KEY").contains(authType)) {
            throw new IllegalArgumentException("BI API datasource auth type is not supported: " + authType);
        }
        String responseFormat = stringValue(raw.get("responseFormat"), "JSON").toUpperCase(Locale.ROOT);
        if (!"JSON".equals(responseFormat)) {
            throw new IllegalArgumentException("BI API datasource response format must be JSON");
        }
        normalized.put("requestMethod", requestMethod);
        normalized.put("authType", authType);
        normalized.put("headers", sanitizeConnectorValue(raw.getOrDefault("headers", List.of())));
        normalized.put("parameters", sanitizeConnectorValue(raw.getOrDefault("parameters", List.of())));
        String bodyTemplate = stringValue(raw.get("bodyTemplate"), "");
        if (!bodyTemplate.isBlank()) {
            normalized.put("bodyTemplate", bodyTemplate);
        }
        normalized.put("responseRowsPath", stringValue(raw.get("responseRowsPath"), "$"));
        normalized.put("responseFormat", responseFormat);
        return normalized;
    }

    private String serializeConnectorConfig(Map<String, Object> connectorConfig) {
        try {
            return OBJECT_MAPPER.writeValueAsString(connectorConfig == null ? Map.of() : connectorConfig);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("BI datasource connector config is not valid JSON", e);
        }
    }

    private Map<String, Object> sanitizeConnectorConfig(Map<String, Object> rawConfig) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawConfig.entrySet()) {
            sanitized.put(entry.getKey(), SECRET_CONFIG_KEY.matcher(entry.getKey()).matches()
                    ? "***"
                    : sanitizeConnectorValue(entry.getValue()));
        }
        return sanitized;
    }

    private Object sanitizeConnectorValue(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            boolean maskNamedValue = shouldMaskNamedConnectorValue(rawMap);
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, SECRET_CONFIG_KEY.matcher(key).matches()
                        || (maskNamedValue && isConnectorValueKey(key))
                        ? "***"
                        : sanitizeConnectorValue(entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof List<?> rawList) {
            return rawList.stream().map(this::sanitizeConnectorValue).toList();
        }
        return value;
    }

    private boolean shouldMaskNamedConnectorValue(Map<?, ?> rawMap) {
        if (booleanMapFlag(rawMap, "variable")) {
            return false;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
            if (!List.of("name", "key", "header", "parameter").contains(key)) {
                continue;
            }
            Object value = entry.getValue();
            if (value != null && SECRET_CONFIG_KEY.matcher(String.valueOf(value)).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean booleanMapFlag(Map<?, ?> rawMap, String flag) {
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (flag.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                Object value = entry.getValue();
                return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
            }
        }
        return false;
    }

    private boolean isConnectorValueKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return "value".equals(normalized) || "defaultvalue".equals(normalized);
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String normalizeConnector(String connectorType) {
        return required(connectorType, "connectorType").toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing BI datasource field: " + field);
        }
        return value.trim();
    }

    private BiDatasourceOnboardingView toView(DataSourceConfigDO config, BiDatasourceSchemaSnapshotDO latestSnapshot) {
        String connectorType = connectorType(config);
        BiDatasourceConnectorCapability capability = CONNECTOR_INDEX.getOrDefault(connectorType, jdbcFallbackConnector(connectorType));
        return new BiDatasourceOnboardingView(
                config.getId(),
                sourceKey(config),
                blankToDefault(config.getName(), sourceKey(config)),
                blankToDefault(config.getType(), "JDBC"),
                connectorType,
                config.getEnabled() == null || config.getEnabled() != 0,
                config.getDriverClassName(),
                maskUrl(config.getUrl()),
                maskPrincipal(config.getUsername()),
                connectionMode(config, capability),
                latestSnapshot == null ? "NOT_SYNCED" : latestSnapshot.getSyncStatus(),
                latestSnapshot == null || latestSnapshot.getTableCount() == null ? 0 : latestSnapshot.getTableCount(),
                latestSnapshot == null ? null : latestSnapshot.getSyncedAt(),
                capability.supportedModes(),
                capability.supportStatus(),
                capabilityList(capability));
    }

    private String sourceKey(DataSourceConfigDO config) {
        String connectorType = connectorType(config);
        String prefix = "API".equals(connectorType) || "API".equalsIgnoreCase(config.getType())
                ? "api"
                : ("CSV_EXCEL".equals(connectorType) || "FILE".equalsIgnoreCase(config.getType()) ? "file" : "jdbc");
        return prefix + "-" + (config.getId() == null ? "new" : config.getId());
    }

    private String connectorType(DataSourceConfigDO config) {
        if (config.getConnectorType() != null && !config.getConnectorType().isBlank()) {
            return normalizeConnector(config.getConnectorType());
        }
        String value = lower(config.getDriverClassName()) + " " + lower(config.getUrl()) + " "
                + lower(config.getName()) + " " + lower(config.getDescription());
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

    private String resolveConnectionMode(String requested,
                                         BiDatasourceConnectorCapability capability,
                                         DataSourceConfigDO existing) {
        String normalized = normalizeConnectionMode(requested);
        if (normalized == null && existing != null) {
            normalized = normalizeConnectionMode(existing.getConnectionMode());
            if (normalized != null && !capability.supportedModes().contains(normalized)) {
                normalized = null;
            }
        }
        String connectionMode = normalized == null ? defaultConnectionMode(capability) : normalized;
        if (!capability.supportedModes().contains(connectionMode)) {
            throw new IllegalArgumentException("BI datasource connection mode is not supported: "
                    + connectionMode + " for " + capability.connectorType());
        }
        return connectionMode;
    }

    private String connectionMode(DataSourceConfigDO config, BiDatasourceConnectorCapability capability) {
        String mode = normalizeConnectionMode(config.getConnectionMode());
        return mode == null || !capability.supportedModes().contains(mode)
                ? defaultConnectionMode(capability)
                : mode;
    }

    private String defaultConnectionMode(BiDatasourceConnectorCapability capability) {
        return capability.supportedModes().contains("DIRECT_QUERY") ? "DIRECT_QUERY" : "EXTRACT";
    }

    private static String normalizeConnectionMode(String connectionMode) {
        if (connectionMode == null || connectionMode.isBlank()) {
            return null;
        }
        return connectionMode.trim().toUpperCase(Locale.ROOT);
    }

    private List<String> capabilityList(BiDatasourceConnectorCapability capability) {
        return List.of(
                        capability.supportsConnectionTest() ? "CONNECTION_TEST" : "",
                        capability.supportsSchemaSync() ? "SCHEMA_SYNC" : "",
                        capability.supportsSqlDataset() ? "SQL_DATASET" : "",
                        capability.supportsTableDataset() ? "TABLE_DATASET" : "",
                        capability.supportsCredentials() ? "CREDENTIALS" : "")
                .stream()
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static String maskUrl(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return SECRET_URL_PARAMETER.matcher(value).replaceAll("$1$2***");
    }

    private static String maskPrincipal(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        if (value.length() <= 4) {
            return value.charAt(0) + "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<BiDatasourceConnectorCapability> connectorCatalogInternal() {
        return List.of(
                jdbc("MYSQL", "MySQL", List.of("com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver"),
                        "JDBC connector is available for table and SQL datasets"),
                jdbc("DORIS", "Apache Doris", List.of("com.mysql.cj.jdbc.Driver"),
                        "Doris is onboarded through the MySQL wire protocol"),
                jdbc("POSTGRESQL", "PostgreSQL", List.of("org.postgresql.Driver"),
                        "JDBC connector is available for table and SQL datasets"),
                jdbc("CLICKHOUSE", "ClickHouse", List.of("com.clickhouse.jdbc.ClickHouseDriver", "ru.yandex.clickhouse.ClickHouseDriver"),
                        "JDBC connector is available when the driver is provided"),
                jdbc("HOLOGRES", "Hologres", List.of("org.postgresql.Driver"),
                        "Hologres is onboarded through the PostgreSQL wire protocol"),
                jdbc("ANALYTICDB_MYSQL", "AnalyticDB for MySQL", List.of("com.mysql.cj.jdbc.Driver"),
                        "AnalyticDB MySQL-compatible connector shares JDBC onboarding"),
                jdbc("ANALYTICDB_POSTGRESQL", "AnalyticDB for PostgreSQL", List.of("org.postgresql.Driver"),
                        "AnalyticDB PostgreSQL-compatible connector shares JDBC onboarding"),
                jdbc("ORACLE", "Oracle", List.of("oracle.jdbc.OracleDriver"),
                        "JDBC connector is available when the driver is provided"),
                jdbc("SQLSERVER", "SQL Server", List.of("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
                        "JDBC connector is available when the driver is provided"),
                new BiDatasourceConnectorCapability(
                        "MAXCOMPUTE",
                        "MaxCompute",
                        "ALIBABA_CLOUD",
                        EXTRACT_MODES,
                        "PLANNED",
                        "WAREHOUSE_EXTRACT",
                        "Warehouse-scale extract capacity; native connector pending",
                        false,
                        false,
                        false,
                        false,
                        true,
                        List.of(),
                        "Native MaxCompute connector is planned after the JDBC onboarding path"),
                new BiDatasourceConnectorCapability(
                        "CSV_EXCEL",
                        "CSV / Excel",
                        "FILE",
                        EXTRACT_MODES,
                        "AVAILABLE",
                        "FILE_EXTRACT_SMALL",
                        "Uploaded-file extract capacity for analyst-managed files",
                        false,
                        false,
                        false,
                        true,
                        false,
                        List.of(),
                        "File upload onboarding stores file metadata for extract datasets; parsing runtime follows separately"),
                new BiDatasourceConnectorCapability(
                        "API",
                        "API",
                        "HTTP",
                        EXTRACT_MODES,
                        "AVAILABLE",
                        "HTTP_EXTRACT_SMALL",
                        "HTTP JSON extract capacity with bounded preview and schema inference",
                        false,
                        false,
                        false,
                        true,
                        true,
                        List.of(),
                        "HTTP API extract connector supports JSON extract mode with request parameters"),
                new BiDatasourceConnectorCapability(
                        "APP_ANALYTICS",
                        "Application Analytics",
                        "APP",
                        EXTRACT_MODES,
                        "AVAILABLE",
                        "APP_EXTRACT_SMALL",
                        "Application SaaS/API extract capacity for campaign and operations apps",
                        false,
                        false,
                        false,
                        true,
                        true,
                        List.of(),
                        "Application datasource uses HTTP JSON extract onboarding with governed app capacity"));
    }

    private static BiDatasourceConnectorCapability jdbc(String type,
                                                        String label,
                                                        List<String> drivers,
                                                        String note) {
        return new BiDatasourceConnectorCapability(
                type,
                label,
                "JDBC",
                JDBC_MODES,
                "AVAILABLE",
                "INTERACTIVE_QUERY",
                "Interactive JDBC capacity supports direct query and cached query modes",
                true,
                true,
                true,
                true,
                true,
                drivers,
                note);
    }

    private static BiDatasourceConnectorCapability jdbcFallbackConnector(String connectorType) {
        return jdbc(connectorType, connectorType, List.of(), "Generic JDBC connector");
    }

    private static boolean isFileConnector(BiDatasourceConnectorCapability capability) {
        return "FILE".equals(capability.sourceCategory()) || "CSV_EXCEL".equals(capability.connectorType());
    }

    private static boolean isHttpJsonConnector(BiDatasourceConnectorCapability capability) {
        return "API".equals(capability.connectorType())
                || "HTTP".equals(capability.sourceCategory())
                || "APP".equals(capability.sourceCategory());
    }
}
