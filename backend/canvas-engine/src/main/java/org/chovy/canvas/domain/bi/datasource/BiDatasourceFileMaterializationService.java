package org.chovy.canvas.domain.bi.datasource;

import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationPolicyCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetAccelerationService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceCommand;
import org.chovy.canvas.domain.bi.dataset.BiDatasetFromDatasourceService;
import org.chovy.canvas.domain.bi.dataset.BiDatasetResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * BiDatasourceFileMaterializationService 编排 domain.bi.datasource 场景的领域业务规则。
 */
@Service
public class BiDatasourceFileMaterializationService {

    private static final int DEFAULT_SCHEMA_LIMIT = 200;
    private static final long DEFAULT_MAX_ROWS = 100_000L;

    private final BiDatasourceFileUploadService uploadService;
    private final BiDatasourceRuntimeService runtimeService;
    private final BiDatasetFromDatasourceService datasetService;
    private final BiDatasetAccelerationService accelerationService;

    /**
     * 创建 BiDatasourceFileMaterializationService 实例并注入 domain.bi.datasource 场景依赖。
     * @param uploadService 依赖组件，用于完成数据访问或外部能力调用。
     * @param runtimeService 时间参数，用于计算窗口、过期或审计时间。
     * @param datasetService 依赖组件，用于完成数据访问或外部能力调用。
     * @param accelerationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiDatasourceFileMaterializationService(BiDatasourceFileUploadService uploadService,
                                                  BiDatasourceRuntimeService runtimeService,
                                                  BiDatasetFromDatasourceService datasetService,
                                                  BiDatasetAccelerationService accelerationService) {
        this.uploadService = uploadService;
        this.runtimeService = runtimeService;
        this.datasetService = datasetService;
        this.accelerationService = accelerationService;
    }

    /**
     * 上传文件数据源并立即物化为可建模数据资产，串联接入、存储和预览生命周期。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param role 当前操作人的角色，用于判断管理、发布和审批豁免权限
     * @param originalFileName 上传文件的原始名称，用于推断文件类型并保留导入来源
     * @param content 上传或导入的文件内容字节
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 文件数据源接入和物化后的结果
     */
    public BiDatasourceFileMaterializationResult uploadAndMaterialize(Long tenantId,
                                                                      String username,
                                                                      String role,
                                                                      String originalFileName,
                                                                      byte[] content,
                                                                      BiDatasourceFileMaterializationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI datasource file materialization command is required");
        }
        BiDatasourceOnboardingView source = uploadService.upload(
                tenantId,
                username,
                originalFileName,
                content,
                new BiDatasourceFileUploadCommand(
                        command.name(),
                        command.description(),
                        command.sheetName(),
                        command.delimiter(),
                        command.headerRow(),
                        command.encoding()));
        Long sourceId = required(source == null ? null : source.id(), "sourceId");
        int schemaLimit = boundedSchemaLimit(command.schemaLimit());
        BiDatasourceSchemaSnapshotView snapshot = runtimeService.syncSchema(
                sourceId,
                normalizeTenant(tenantId),
                actor(username),
                schemaLimit,
                null);
        if (snapshot == null || !"SUCCESS".equalsIgnoreCase(defaultString(snapshot.syncStatus()))) {
            throw new IllegalStateException("BI uploaded file schema sync must succeed before materialization");
        }
        // 文件物化必须基于刚同步成功的 schema snapshot，保证字段列表、表名和后续数据集口径一致。
        BiDatasourceTablePreview table = firstTable(snapshot);
        String datasetKey = defaultString(command.datasetKey());
        if (datasetKey.isBlank()) {
            datasetKey = defaultDatasetKey(sourceId, table.name());
        }
        String datasetName = defaultString(command.datasetName());
        if (datasetName.isBlank()) {
            datasetName = defaultString(source.name());
            datasetName = (datasetName.isBlank() ? "Uploaded File" : datasetName) + " " + table.name();
        }
        String tenantColumn = defaultString(command.tenantColumn());
        if (tenantColumn.isBlank()) {
            tenantColumn = "tenant_id";
        }
        // 文件来源允许在抽取物化阶段注入默认租户列，统一后续查询的租户隔离条件。
        BiDatasetResource dataset = datasetService.createTableDataset(
                normalizeTenant(tenantId),
                actor(username),
                role,
                new BiDatasetFromDatasourceCommand(
                        sourceId,
                        table.name(),
                        datasetKey,
                        datasetName,
                        tenantColumn,
                        selectedColumns(table)));
        Long maxRows = positive(command.maxRows(), DEFAULT_MAX_ROWS);
        // 先写 EXTRACT 策略再立即刷新，确保物化表、刷新状态和容量治理记录在同一策略口径下沉淀。
        var policy = accelerationService.upsertPolicy(
                normalizeTenant(tenantId),
                dataset.datasetKey(),
                new BiDatasetAccelerationPolicyCommand(
                        true,
                        BiDatasetAccelerationService.MODE_EXTRACT,
                        BiDatasetAccelerationService.REFRESH_MANUAL,
                        60L,
                        300L,
                        maxRows,
                        null),
                actor(username));
        var refreshRun = accelerationService.refreshNow(normalizeTenant(tenantId), dataset.datasetKey(), actor(username));
        return new BiDatasourceFileMaterializationResult(source, snapshot, dataset, policy, refreshRun);
    }

    /**
     * 执行 firstTable 流程，围绕 first table 完成校验、计算或结果组装。
     *
     * @param snapshot snapshot 参数，用于 firstTable 流程中的校验、计算或对象转换。
     * @return 返回 firstTable 流程生成的业务结果。
     */
    private static BiDatasourceTablePreview firstTable(BiDatasourceSchemaSnapshotView snapshot) {
        if (snapshot.tables() == null || snapshot.tables().isEmpty()) {
            throw new IllegalStateException("BI uploaded file schema snapshot has no tables");
        }
        return snapshot.tables().get(0);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param table table 参数，用于 selectedColumns 流程中的校验、计算或对象转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private static List<String> selectedColumns(BiDatasourceTablePreview table) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return table.columns() == null
                ? List.of()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                : table.columns().stream()
                .map(BiDatasourceColumnPreview::name)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param sourceId 业务对象 ID，用于定位具体记录。
     * @param tableName 名称文本，用于展示或唯一性校验。
     * @return 返回 default dataset key 生成的文本或业务键。
     */
    private static String defaultDatasetKey(Long sourceId, String tableName) {
        // 默认 key 绑定数据源 ID，降低同名上传文件在同租户工作区内冲突的概率。
        String safeTable = safeIdentifier(tableName == null || tableName.isBlank() ? "file_upload" : tableName);
        return "file_" + sourceId + "_" + safeTable;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe identifier 生成的文本或业务键。
     */
    private static String safeIdentifier(String value) {
        // 上传文件名进入 datasetKey 时只保留安全字符，避免路径、空格或扩展字符污染资源 key。
        String normalized = defaultString(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? "file_upload" : normalized;
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 计算得到的数量、金额或指标值。
     */
    private static Long required(Long value, String field) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("BI uploaded file " + field + " is required");
        }
        return value;
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
     * 解析操作人标识。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 actor 生成的文本或业务键。
     */
    private static String actor(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private static int boundedSchemaLimit(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_SCHEMA_LIMIT;
        }
        return Math.min(value, 1_000);
    }

    /**
     * 执行 positive 流程，围绕 positive 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 positive 流程中的校验、计算或对象转换。
     * @return 返回 positive 计算得到的数量、金额或指标值。
     */
    private static Long positive(Long value, long fallback) {
        return value == null || value <= 0L ? fallback : value;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }
}
