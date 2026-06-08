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

/**
 * BiDatasourceOnboardingService 编排 domain.bi.datasource 场景的领域业务规则。
 */
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

    /**
     * 创建 BiDatasourceOnboardingService 实例并注入 domain.bi.datasource 场景依赖。
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceOnboardingService(DataSourceConfigMapper dataSourceConfigMapper) {
        this(dataSourceConfigMapper, null, null);
    }

    /**
     * 创建 BiDatasourceOnboardingService 实例并注入 domain.bi.datasource 场景依赖。
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasourceOnboardingService(DataSourceConfigMapper dataSourceConfigMapper,
                                         BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper) {
        this(dataSourceConfigMapper, schemaSnapshotMapper, null);
    }

    /**
     * 创建 BiDatasourceOnboardingService 实例并注入 domain.bi.datasource 场景依赖。
     * @param dataSourceConfigMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaSnapshotMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param credentialCipher credential cipher 参数，用于 BiDatasourceOnboardingService 流程中的校验、计算或对象转换。
     */
    @Autowired
    public BiDatasourceOnboardingService(DataSourceConfigMapper dataSourceConfigMapper,
                                         BiDatasourceSchemaSnapshotMapper schemaSnapshotMapper,
                                         DataSourceCredentialCipher credentialCipher) {
        this.dataSourceConfigMapper = dataSourceConfigMapper;
        this.schemaSnapshotMapper = schemaSnapshotMapper;
        this.credentialCipher = credentialCipher == null
                /**
                 * 执行 DataSourceCredentialCipher 流程，围绕 data source credential cipher 完成校验、计算或结果组装。
                 *
                 * @param DEFAULT_SECRET default secret 参数，用于 DataSourceCredentialCipher 流程中的校验、计算或对象转换。
                 * @return 返回 DataSourceCredentialCipher 流程生成的业务结果。
                 */
                ? new DataSourceCredentialCipher(DataSourceCredentialCipher.DEFAULT_SECRET)
                : credentialCipher;
    }

    /**
     * 返回 BI 数据源接入支持的连接器能力目录。
     *
     * <p>该方法只读取内存中的静态连接器目录，不访问数据库、不产生事务或外部副作用。返回内容用于前端展示
     * 支持状态、连接模式、驱动和能力开关。</p>
     *
     * @return 当前版本内置的连接器能力列表
     */
    public List<BiDatasourceConnectorCapability> connectorCatalog() {
        return connectorCatalogInternal();
    }

    /**
     * 查询租户已接入的 BI 数据源及其最新 schema 同步摘要。
     *
     * <p>该方法只读数据源配置表和 schema 快照表，不解密密码、不写缓存。租户 ID 为空时归一化为系统租户；
     * 返回视图会隐藏敏感连接信息，仅暴露接入配置和最近同步状态。</p>
     *
     * @param tenantId 租户 ID
     * @return 按数据源 ID 倒序排列的接入数据源视图
     */
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

    /**
     * 创建 BI 数据源接入配置。
     *
     * <p>该方法会校验连接器类型、连接模式、驱动和连接配置，并将需要保存的密码通过
     * {@link DataSourceCredentialCipher} 加密后写入数据源配置表；不会立即测试连接或同步 schema。</p>
     *
     * @param tenantId 租户 ID
     * @param operator 操作人，空值按 system 记录
     * @param command 数据源接入命令，包含连接器、URL、凭证和扩展配置
     * @return 创建后的数据源接入视图，敏感字段已脱敏
     */
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

    /**
     * 更新 BI 数据源接入配置。
     *
     * <p>该方法会在租户范围内查找已有配置，重新校验连接器相关字段并更新数据库。未提交新密码时保留原密文；
     * 不需要凭证的连接器会清空密码字段。方法不触发连接测试、schema 同步或缓存刷新。</p>
     *
     * @param tenantId 租户 ID
     * @param operator 操作人，用于补齐历史创建人
     * @param id 数据源配置 ID
     * @param command 更新命令
     * @return 更新后的数据源接入视图
     */
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
        if (!requiresCredentials(command, capability)) {
            row.setPassword("");
        // 根据前序判断结果进入后续条件分支。
        } else if (command == null || command.password() == null || command.password().isBlank()) {
            row.setPassword(existing.getPassword());
        } else {
            row.setPassword(credentialCipher.encrypt(command.password()));
        }
        dataSourceConfigMapper.updateById(row);
        return toView(row, null);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param sources sources 参数，用于 latestSnapshots 流程中的校验、计算或对象转换。
     * @return 返回 latestSnapshots 流程生成的业务结果。
     */
    private Map<Long, BiDatasourceSchemaSnapshotDO> latestSnapshots(Long tenantId, List<DataSourceConfigDO> sources) {
        // 最新快照按租户和数据源集合批量读取，只用于列表摘要，不暴露 schema 明细和凭证明文。
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

    /**
     * 应用请求中的业务字段或租户约束。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param existing existing 参数，用于 applyCommand 流程中的校验、计算或对象转换。
     * @return 返回 applyCommand 流程生成的业务结果。
     */
    private BiDatasourceConnectorCapability applyCommand(DataSourceConfigDO row,
                                                         BiDatasourceOnboardingCommand command,
                                                         DataSourceConfigDO existing) {
        // 接入命令在这里统一落库口径：连接器能力、连接模式、URL、凭证主体和扩展配置都必须先通过治理校验。
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

    /**
     * 执行 sourceType 流程，围绕 source type 完成校验、计算或结果组装。
     *
     * @param capability capability 参数，用于 sourceType 流程中的校验、计算或对象转换。
     * @return 返回 source type 生成的文本或业务键。
     */
    private String sourceType(BiDatasourceConnectorCapability capability) {
        if (isHttpJsonConnector(capability)) {
            return "API";
        }
        if (isFileConnector(capability)) {
            return "FILE";
        }
        return "JDBC";
    }

    /**
     * 执行 datasourceUrl 流程，围绕 datasource url 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param capability capability 参数，用于 datasourceUrl 流程中的校验、计算或对象转换。
     * @return 返回 datasource url 生成的文本或业务键。
     */
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

    /**
     * 执行 principal 流程，围绕 principal 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param capability capability 参数，用于 principal 流程中的校验、计算或对象转换。
     * @return 返回 principal 生成的文本或业务键。
     */
    private String principal(BiDatasourceOnboardingCommand command,
                             BiDatasourceConnectorCapability capability) {
        if (requiresCredentials(command, capability)) {
            return required(command.username(), "username");
        }
        if (isHttpJsonConnector(capability)) {
            return blankToDefault(command.username(), "anonymous");
        }
        return blankToDefault(command.username(), "file_upload");
    }

    /**
     * 处理安全、签名或敏感信息逻辑。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param capability capability 参数，用于 passwordForCreate 流程中的校验、计算或对象转换。
     * @return 返回 password for create 生成的文本或业务键。
     */
    private String passwordForCreate(BiDatasourceOnboardingCommand command,
                                     BiDatasourceConnectorCapability capability) {
        return requiresCredentials(command, capability) ? required(command.password(), "password") : "";
    }

    /**
     * 执行 driverClassName 流程，围绕 driver class name 完成校验、计算或结果组装。
     *
     * @param requested requested 参数，用于 driverClassName 流程中的校验、计算或对象转换。
     * @param capability capability 参数，用于 driverClassName 流程中的校验、计算或对象转换。
     * @param existing existing 参数，用于 driverClassName 流程中的校验、计算或对象转换。
     * @return 返回 driver class name 生成的文本或业务键。
     */
    private String driverClassName(String requested,
                                   BiDatasourceConnectorCapability capability,
                                   DataSourceConfigDO existing) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (requested != null && !requested.isBlank()) {
            return validateDriverClassName(requested.trim(), capability);
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "com.mysql.cj.jdbc.Driver";
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param requested requested 参数，用于 validateDriverClassName 流程中的校验、计算或对象转换。
     * @param capability capability 参数，用于 validateDriverClassName 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private String validateDriverClassName(String requested, BiDatasourceConnectorCapability capability) {
        // JDBC 驱动只允许连接器能力目录中的白名单，文件/API 的伪驱动不参与真实类加载。
        if (isHttpJsonConnector(capability) || isFileConnector(capability)) {
            return requested;
        }
        List<String> allowed = capability.driverClassNames();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(requested)) {
            throw new IllegalArgumentException("BI datasource driver is not supported: "
                    + requested + " for " + capability.connectorType());
        }
        return requested;
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param capability capability 参数，用于 connectorConfigJson 流程中的校验、计算或对象转换。
     * @param existing existing 参数，用于 connectorConfigJson 流程中的校验、计算或对象转换。
     * @return 返回 connector config json 生成的文本或业务键。
     */
    private String connectorConfigJson(BiDatasourceOnboardingCommand command,
                                       BiDatasourceConnectorCapability capability,
                                       DataSourceConfigDO existing) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (isHttpJsonConnector(capability)) {
            return serializeConnectorConfig(apiConnectorConfig(command.connectorConfig()));
        }
        if (isFileConnector(capability)) {
            return serializeConnectorConfig(fileConnectorConfig(command));
        }
        if (command.connectorConfig() != null && !command.connectorConfig().isEmpty()) {
            return serializeConnectorConfig(sanitizeConnectorConfig(command.connectorConfig()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return existing == null ? null : existing.getConnectorConfigJson();
    }

    /**
     * 执行 fileConnectorConfig 流程，围绕 file connector config 完成校验、计算或结果组装。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 fileConnectorConfig 流程生成的业务结果。
     */
    private Map<String, Object> fileConnectorConfig(BiDatasourceOnboardingCommand command) {
        // 文件连接器配置只保存解析所需元数据，不保存本地临时路径以外的敏感内容。
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

    /**
     * 执行 fileName 流程，围绕 file name 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 fileName 流程中的校验、计算或对象转换。
     * @param raw raw 参数，用于 fileName 流程中的校验、计算或对象转换。
     * @param url url 参数，用于 fileName 流程中的校验、计算或对象转换。
     * @return 返回 file name 生成的文本或业务键。
     */
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

    /**
     * 规范化输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fileName 名称文本，用于展示或唯一性校验。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeFileType(Object value, String fileName) {
        // 准备本次处理所需的上下文和中间变量。
        String requested = stringValue(value, "");
        String extension = "";
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return fileType;
    }

    /**
     * 执行 booleanValue 流程，围绕 boolean value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 booleanValue 流程中的校验、计算或对象转换。
     * @return 返回 boolean value 的布尔判断结果。
     */
    private boolean booleanValue(Object value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value).trim());
    }

    /**
     * 执行 apiConnectorConfig 流程，围绕 api connector config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 apiConnectorConfig 流程中的校验、计算或对象转换。
     * @param rawConfig 配置对象，用于控制运行参数和策略开关。
     * @return 返回 apiConnectorConfig 流程生成的业务结果。
     */
    private Map<String, Object> apiConnectorConfig(Map<String, Object> rawConfig) {
        // API 配置归一化为受支持的 HTTP 方法、认证方式和 JSON 响应路径，便于预览和 schema 推断复用。
        Map<String, Object> raw = rawConfig == null ? Map.of() : rawConfig;
        Map<String, Object> normalized = new LinkedHashMap<>();
        String requestMethod = stringValue(raw.get("requestMethod"), "GET").toUpperCase(Locale.ROOT);
        if (!List.of("GET", "POST").contains(requestMethod)) {
            throw new IllegalArgumentException("BI API datasource request method is not supported: " + requestMethod);
        }
        String authType = apiAuthType(raw);
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

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param capability capability 参数，用于 requiresCredentials 流程中的校验、计算或对象转换。
     * @return 返回 requires credentials 的布尔判断结果。
     */
    private boolean requiresCredentials(BiDatasourceOnboardingCommand command,
                                        BiDatasourceConnectorCapability capability) {
        if (!capability.supportsCredentials()) {
            return false;
        }
        if (isHttpJsonConnector(capability)) {
            return !"NONE".equals(apiAuthType(command.connectorConfig()));
        }
        return true;
    }

    /**
     * 执行 apiAuthType 流程，围绕 api auth type 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 apiAuthType 流程中的校验、计算或对象转换。
     * @param raw raw 参数，用于 apiAuthType 流程中的校验、计算或对象转换。
     * @return 返回 api auth type 生成的文本或业务键。
     */
    private String apiAuthType(Map<String, Object> raw) {
        return stringValue(raw == null ? null : raw.get("authType"), "NONE").toUpperCase(Locale.ROOT);
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param String string 参数，用于 serializeConnectorConfig 流程中的校验、计算或对象转换。
     * @param connectorConfig 配置对象，用于控制运行参数和策略开关。
     * @return 返回 serialize connector config 生成的文本或业务键。
     */
    private String serializeConnectorConfig(Map<String, Object> connectorConfig) {
        try {
            return OBJECT_MAPPER.writeValueAsString(connectorConfig == null ? Map.of() : connectorConfig);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("BI datasource connector config is not valid JSON", e);
        }
    }

    /**
     * 执行 sanitizeConnectorConfig 流程，围绕 sanitize connector config 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 sanitizeConnectorConfig 流程中的校验、计算或对象转换。
     * @param rawConfig 配置对象，用于控制运行参数和策略开关。
     * @return 返回 sanitizeConnectorConfig 流程生成的业务结果。
     */
    private Map<String, Object> sanitizeConnectorConfig(Map<String, Object> rawConfig) {
        // 扩展配置进入数据库前递归脱敏，避免 header、参数或自定义字段中的 token 被列表接口反向暴露。
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawConfig.entrySet()) {
            sanitized.put(entry.getKey(), SECRET_CONFIG_KEY.matcher(entry.getKey()).matches()
                    ? "***"
                    /**
                     * 执行 sanitizeConnectorValue 流程，围绕 sanitize connector value 完成校验、计算或结果组装。
                     *
                     * @return 返回 sanitizeConnectorValue 流程生成的业务结果。
                     */
                    : sanitizeConnectorValue(entry.getValue()));
        }
        return sanitized;
    }

    /**
     * 执行 sanitizeConnectorValue 流程，围绕 sanitize connector value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 sanitizeConnectorValue 流程生成的业务结果。
     */
    private Object sanitizeConnectorValue(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            boolean maskNamedValue = shouldMaskNamedConnectorValue(rawMap);
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, SECRET_CONFIG_KEY.matcher(key).matches()
                        || (maskNamedValue && isConnectorValueKey(key))
                        ? "***"
                        /**
                         * 执行 sanitizeConnectorValue 流程，围绕 sanitize connector value 完成校验、计算或结果组装。
                         *
                         * @return 返回 sanitizeConnectorValue 流程生成的业务结果。
                         */
                        : sanitizeConnectorValue(entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof List<?> rawList) {
            return rawList.stream().map(this::sanitizeConnectorValue).toList();
        }
        return value;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param rawMap raw map 参数，用于 shouldMaskNamedConnectorValue 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean shouldMaskNamedConnectorValue(Map<?, ?> rawMap) {
        // 形如 {name: apiKey, value: xxx} 的配置按 name/key 识别敏感语义，变量占位配置保留默认值。
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

    /**
     * 执行 booleanMapFlag 流程，围绕 boolean map flag 完成校验、计算或结果组装。
     *
     * @param rawMap raw map 参数，用于 booleanMapFlag 流程中的校验、计算或对象转换。
     * @param flag flag 参数，用于 booleanMapFlag 流程中的校验、计算或对象转换。
     * @return 返回 boolean map flag 的布尔判断结果。
     */
    private boolean booleanMapFlag(Map<?, ?> rawMap, String flag) {
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (flag.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                Object value = entry.getValue();
                return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
            }
        }
        return false;
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param key 业务键，用于在同一租户下定位资源。
     * @return 返回布尔判断结果。
     */
    private boolean isConnectorValueKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return "value".equals(normalized) || "defaultvalue".equals(normalized);
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 stringValue 流程中的校验、计算或对象转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value).trim();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 规范化输入值。
     *
     * @param connectorType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeConnector(String connectorType) {
        return required(connectorType, "connectorType").toUpperCase(Locale.ROOT);
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing BI datasource field: " + field);
        }
        return value.trim();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param latestSnapshot latest snapshot 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasourceOnboardingView toView(DataSourceConfigDO config, BiDatasourceSchemaSnapshotDO latestSnapshot) {
        // 准备本次处理所需的上下文和中间变量。
        String connectorType = connectorType(config);
        BiDatasourceConnectorCapability capability = CONNECTOR_INDEX.getOrDefault(connectorType, jdbcFallbackConnector(connectorType));
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 执行 sourceKey 流程，围绕 source key 完成校验、计算或结果组装。
     *
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @return 返回 source key 生成的文本或业务键。
     */
    private String sourceKey(DataSourceConfigDO config) {
        String connectorType = connectorType(config);
        String prefix = "API".equals(connectorType) || "API".equalsIgnoreCase(config.getType())
                ? "api"
                : ("CSV_EXCEL".equals(connectorType) || "FILE".equalsIgnoreCase(config.getType()) ? "file" : "jdbc");
        return prefix + "-" + (config.getId() == null ? "new" : config.getId());
    }

    /**
     * 执行 connectorType 流程，围绕 connector type 完成校验、计算或结果组装。
     *
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @return 返回 connector type 生成的文本或业务键。
     */
    private String connectorType(DataSourceConfigDO config) {
        if (config.getConnectorType() != null && !config.getConnectorType().isBlank()) {
            return normalizeConnector(config.getConnectorType());
        }
        String value = lower(config.getDriverClassName()) + " " + lower(config.getUrl()) + " "
                /**
                 * 执行 lower 流程，围绕 lower 完成校验、计算或结果组装。
                 *
                 * @return 返回 lower 流程生成的业务结果。
                 */
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

    /**
     * 解析业务依赖或上下文值。
     *
     * @param requested requested 参数，用于 resolveConnectionMode 流程中的校验、计算或对象转换。
     * @param capability capability 参数，用于 resolveConnectionMode 流程中的校验、计算或对象转换。
     * @param existing existing 参数，用于 resolveConnectionMode 流程中的校验、计算或对象转换。
     * @return 返回 resolve connection mode 生成的文本或业务键。
     */
    private String resolveConnectionMode(String requested,
                                         BiDatasourceConnectorCapability capability,
                                         DataSourceConfigDO existing) {
        // 更新时优先沿用仍被连接器支持的旧模式；否则回退默认模式，避免能力目录变化留下非法状态。
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

    /**
     * 执行 connectionMode 流程，围绕 connection mode 完成校验、计算或结果组装。
     *
     * @param config 配置对象，用于控制运行参数和策略开关。
     * @param capability capability 参数，用于 connectionMode 流程中的校验、计算或对象转换。
     * @return 返回 connection mode 生成的文本或业务键。
     */
    private String connectionMode(DataSourceConfigDO config, BiDatasourceConnectorCapability capability) {
        String mode = normalizeConnectionMode(config.getConnectionMode());
        return mode == null || !capability.supportedModes().contains(mode)
                /**
                 * 按默认值规则处理输入值。
                 *
                 * @param capability capability 参数，用于 defaultConnectionMode 流程中的校验、计算或对象转换。
                 * @return 返回 defaultConnectionMode 流程生成的业务结果。
                 */
                ? defaultConnectionMode(capability)
                : mode;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param capability capability 参数，用于 defaultConnectionMode 流程中的校验、计算或对象转换。
     * @return 返回 default connection mode 生成的文本或业务键。
     */
    private String defaultConnectionMode(BiDatasourceConnectorCapability capability) {
        return capability.supportedModes().contains("DIRECT_QUERY") ? "DIRECT_QUERY" : "EXTRACT";
    }

    /**
     * 规范化输入值。
     *
     * @param connectionMode connection mode 参数，用于 normalizeConnectionMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String normalizeConnectionMode(String connectionMode) {
        if (connectionMode == null || connectionMode.isBlank()) {
            return null;
        }
        return connectionMode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 capabilityList 流程，围绕 capability list 完成校验、计算或结果组装。
     *
     * @param capability capability 参数，用于 capabilityList 流程中的校验、计算或对象转换。
     * @return 返回 capability list 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String maskUrl(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return SECRET_URL_PARAMETER.matcher(value).replaceAll("$1$2***");
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String maskPrincipal(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        if (value.length() <= 4) {
            return value.charAt(0) + "***";
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    /**
     * 执行 lower 流程，围绕 lower 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 lower 生成的文本或业务键。
     */
    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 blankToDefault 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 执行 connectorCatalogInternal 流程，围绕 connector catalog internal 完成校验、计算或结果组装。
     *
     * @return 返回 connector catalog internal 汇总后的集合、分页或映射视图。
     */
    private static List<BiDatasourceConnectorCapability> connectorCatalogInternal() {
        // 汇总前面计算出的状态和明细，返回给调用方。
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

    /**
     * 执行 jdbc 流程，围绕 jdbc 完成校验、计算或结果组装。
     *
     * @param type 类型标识，用于选择对应处理分支。
     * @param label label 参数，用于 jdbc 流程中的校验、计算或对象转换。
     * @param drivers drivers 参数，用于 jdbc 流程中的校验、计算或对象转换。
     * @param note note 参数，用于 jdbc 流程中的校验、计算或对象转换。
     * @return 返回 jdbc 流程生成的业务结果。
     */
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

    /**
     * 执行 jdbcFallbackConnector 流程，围绕 jdbc fallback connector 完成校验、计算或结果组装。
     *
     * @param connectorType 类型标识，用于选择对应处理分支。
     * @return 返回 jdbcFallbackConnector 流程生成的业务结果。
     */
    private static BiDatasourceConnectorCapability jdbcFallbackConnector(String connectorType) {
        return jdbc(connectorType, connectorType, List.of(), "Generic JDBC connector");
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param capability capability 参数，用于 isFileConnector 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isFileConnector(BiDatasourceConnectorCapability capability) {
        return "FILE".equals(capability.sourceCategory()) || "CSV_EXCEL".equals(capability.connectorType());
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param capability capability 参数，用于 isHttpJsonConnector 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private static boolean isHttpJsonConnector(BiDatasourceConnectorCapability capability) {
        return "API".equals(capability.connectorType())
                || "HTTP".equals(capability.sourceCategory())
                || "APP".equals(capability.sourceCategory());
    }
}
