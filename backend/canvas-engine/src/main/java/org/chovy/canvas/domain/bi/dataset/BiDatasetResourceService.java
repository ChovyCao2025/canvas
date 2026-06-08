package org.chovy.canvas.domain.bi.dataset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiDatasetFieldDO;
import org.chovy.canvas.dal.dataobject.BiDatasetVersionDO;
import org.chovy.canvas.dal.dataobject.BiMetricDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDatasetFieldMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiDatasetVersionMapper;
import org.chovy.canvas.dal.mapper.BiMetricMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiSqlParameterSpec;
import org.chovy.canvas.domain.bi.query.MarketingBiDatasetRegistry;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns persisted BI dataset definitions and exposes them as executable query specs.
 *
 * <p>Built-in registry datasets remain the fallback for tenant 0 and missing rows; persisted datasets can be drafted,
 * published, versioned, restored, and converted into {@link BiDatasetSpec} for query execution.</p>
 */
@Service
public class BiDatasetResourceService implements BiDatasetSpecResolver {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Pattern TABLE_EXPRESSION = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+){0,2}");
    private static final Pattern COLUMN_EXPRESSION = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?");
    private static final Pattern METRIC_EXPRESSION = Pattern.compile("[A-Za-z0-9_\\s().,+\\-*/<>=]+");
    private static final Pattern SQL_FROM_TOKEN = Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORBIDDEN_SQL_TOKEN = Pattern.compile(
            "\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|MERGE|CALL|EXEC|GRANT|REVOKE|COPY|LOAD)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_PARAMETER_TEMPLATE = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_]*)\\s*}}");
    private static final String SQL_DERIVED_TABLE_ALIAS = "sql_dataset";
    private static final int MAX_TABLE_EXPRESSION_LENGTH = 500;
    private static final Set<String> DATASET_TYPES = Set.of("TABLE", "VIEW", "SQL");
    private static final Set<String> FIELD_ROLES = Set.of("DIMENSION", "MEASURE");
    private static final Set<String> DATA_TYPES = Set.of("STRING", "NUMBER", "DATE", "DATETIME", "BOOLEAN", "PERCENT");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDatasetFieldMapper fieldMapper;
    private final BiMetricMapper metricMapper;
    private final BiDatasetVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final BiResourcePermissionGuard permissionGuard;
    private final BiPublishApprovalService publishApprovalService;
    private final BiResourceCollaborationService collaborationService;

    /**
     * SqlDatasetNormalization 数据记录。
     */
    private record SqlDatasetNormalization(String tableExpression, Map<String, Object> model) {
    }

    /**
     * 执行 BiDatasetResourceService 流程，围绕 bi dataset resource service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fieldMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    ObjectMapper objectMapper) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, null, objectMapper);
    }

    /**
     * 执行 BiDatasetResourceService 流程，围绕 bi dataset resource service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fieldMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuardProvider permission guard provider 参数，用于 BiDatasetResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    ObjectProvider<BiResourcePermissionGuard> permissionGuardProvider,
                                    ObjectProvider<BiPublishApprovalService> publishApprovalServiceProvider,
                                    ObjectProvider<BiResourceCollaborationService> collaborationServiceProvider) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                permissionGuardProvider == null ? null : permissionGuardProvider.getIfAvailable(),
                publishApprovalServiceProvider == null ? null : publishApprovalServiceProvider.getIfAvailable(),
                collaborationServiceProvider == null ? null : collaborationServiceProvider.getIfAvailable());
    }

    /**
     * 执行 BiDatasetResourceService 流程，围绕 bi dataset resource service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fieldMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                (BiResourcePermissionGuard) null);
    }

    /**
     * 执行 BiDatasetResourceService 流程，围绕 bi dataset resource service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fieldMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiDatasetResourceService 流程中的校验、计算或对象转换。
     */
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    BiResourcePermissionGuard permissionGuard) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                permissionGuard, null, null);
    }

    /**
     * 执行 BiDatasetResourceService 流程，围绕 bi dataset resource service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fieldMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiDatasetResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    BiResourcePermissionGuard permissionGuard,
                                    BiPublishApprovalService publishApprovalService) {
        this(workspaceMapper, datasetMapper, fieldMapper, metricMapper, versionMapper, objectMapper,
                permissionGuard, publishApprovalService, null);
    }

    /**
     * 执行 BiDatasetResourceService 流程，围绕 bi dataset resource service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param fieldMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param metricMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiDatasetResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDatasetResourceService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDatasetFieldMapper fieldMapper,
                                    BiMetricMapper metricMapper,
                                    BiDatasetVersionMapper versionMapper,
                                    ObjectMapper objectMapper,
                                    BiResourcePermissionGuard permissionGuard,
                                    BiPublishApprovalService publishApprovalService,
                                    BiResourceCollaborationService collaborationService) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.fieldMapper = fieldMapper;
        this.metricMapper = metricMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.permissionGuard = permissionGuard;
        this.publishApprovalService = publishApprovalService;
        this.collaborationService = collaborationService;
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiDatasetResource> listResources(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                        .eq(BiDatasetDO::getTenantId, scopedTenantId)
                        .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                        .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiDatasetDO::getUpdatedAt)
                        .orderByAsc(BiDatasetDO::getDatasetKey)))
                .stream()
                .map(row -> toResource(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId())))
                .toList();
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 getResource 流程生成的业务结果。
     */
    public BiDatasetResource getResource(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            row = find(0L, workspaceId(0L), datasetKey);
        }
        if (row == null) {
            BiDatasetSpec builtIn = MarketingBiDatasetRegistry.dataset(datasetKey);
            return fromBuiltIn(builtIn);
        }
        return toResource(row, fields(row.getTenantId(), row.getId()), metrics(row.getTenantId(), row.getId()));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetResource saveDraft(Long tenantId, String username, BiDatasetResource resource) {
        return saveDraft(tenantId, username, null, resource);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetResource saveDraft(Long tenantId, String username, String role, BiDatasetResource resource) {
        return saveDraftInternal(tenantId, username, role, resource, null, false);
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetResource saveDraft(Long tenantId,
                                       String username,
                                       String role,
                                       BiDatasetResource resource,
                                       String lockToken) {
        return saveDraftInternal(tenantId, username, role, resource, lockToken, true);
    }

    /**
     * 规范化输入值。
     *
     * @param resource resource 参数，用于 normalizeDraft 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    public BiDatasetDraftNormalization normalizeDraft(BiDatasetResource resource) {
        BiDatasetResource normalized = validateResource(resource);
        return new BiDatasetDraftNormalization(normalized, toSpec(normalized));
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resource resource 参数，用于 saveDraftInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 saveDraftInternal 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private BiDatasetResource saveDraftInternal(Long tenantId,
                                                String username,
                                                String role,
                                                BiDatasetResource resource,
                                                String lockToken,
                                                boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetResource normalizedResource = validateResource(resource);
        BiDatasetDO existing = find(scopedTenantId, workspaceId, normalizedResource.datasetKey());
        requirePermission(scopedTenantId, workspaceId, "DATASET", existing == null ? null : existing.getId(),
                username, role, BiPermissionService.ACTION_EDIT);
        requireEditLock(scopedTenantId, workspaceId, "DATASET", normalizedResource.datasetKey(), username, role,
                lockToken, enforceEditLock && existing != null);

        BiDatasetDO row = new BiDatasetDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetKey(required(normalizedResource.datasetKey(), "datasetKey"));
        row.setName(required(normalizedResource.name(), "name"));
        row.setDatasetType(required(normalizedResource.datasetType(), "datasetType"));
        row.setTableExpression(required(normalizedResource.tableExpression(), "tableExpression"));
        row.setTenantColumn(required(normalizedResource.tenantColumn(), "tenantColumn"));
        row.setModelJson(json(normalizedResource.model()));
        row.setStatus(STATUS_DRAFT);
        row.setCreatedBy(username == null || username.isBlank() ? "system" : username);
        datasetMapper.upsert(row);

        BiDatasetDO persisted = find(scopedTenantId, workspaceId, normalizedResource.datasetKey());
        Long datasetId = persisted == null ? row.getId() : persisted.getId();
        if (datasetId == null) {
            throw new IllegalStateException("BI dataset was not persisted: " + normalizedResource.datasetKey());
        }
        // Replace child rows from the normalized draft so stale fields cannot survive a model rewrite.
        fieldMapper.deleteByDataset(scopedTenantId, datasetId);
        metricMapper.deleteByDataset(scopedTenantId, datasetId);
        for (BiDatasetFieldResource field : normalizedResource.fields()) {
            fieldMapper.insert(toField(scopedTenantId, datasetId, field));
        }
        for (BiMetricResource metric : normalizedResource.metrics()) {
            metricMapper.insert(toMetric(scopedTenantId, workspaceId, datasetId, metric));
        }
        return toResource(persisted == null ? row : persisted,
                normalizedResource.fields(),
                normalizedResource.metrics());
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetResource publish(Long tenantId, String datasetKey) {
        return publish(tenantId, null, datasetKey);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetResource publish(Long tenantId, String username, String datasetKey) {
        return publish(tenantId, username, null, datasetKey);
    }

    /**
     * 执行核心业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiDatasetResource publish(Long tenantId, String username, String role, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        requirePermission(scopedTenantId, workspaceId, "DATASET", row.getId(), username, role, BiPermissionService.ACTION_PUBLISH);
        // SQL datasets require approval because the stored query text controls the runtime source shape.
        requirePublishApproval(scopedTenantId, workspaceId, "DATASET", datasetKey, row.getUpdatedAt(), role,
                requiresSqlDatasetApproval(row));
        datasetMapper.publish(scopedTenantId, workspaceId, datasetKey);
        BiDatasetDO published = find(scopedTenantId, workspaceId, datasetKey);
        if (published == null) {
            row.setStatus(STATUS_PUBLISHED);
            BiDatasetResource resource = toResource(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId()));
            insertVersionSnapshot(scopedTenantId, workspaceId, row, resource, username);
            return resource;
        }
        BiDatasetResource resource =
                toResource(published, fields(scopedTenantId, published.getId()), metrics(scopedTenantId, published.getId()));
        insertVersionSnapshot(scopedTenantId, workspaceId, published, resource, username);
        return resource;
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 流程生成的业务结果。
     */
    public BiDatasetResource archive(Long tenantId, String datasetKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        datasetMapper.archive(scopedTenantId, workspaceId, datasetKey);
        BiDatasetDO archived = find(scopedTenantId, workspaceId, datasetKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId()));
        }
        return toResource(archived, fields(scopedTenantId, archived.getId()), metrics(scopedTenantId, archived.getId()));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiDatasetVersionView> listVersions(Long tenantId, String datasetKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiDatasetVersionDO>()
                        .eq(BiDatasetVersionDO::getTenantId, scopedTenantId)
                        .eq(BiDatasetVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiDatasetVersionDO::getDatasetId, row.getId())
                        .orderByDesc(BiDatasetVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    /**
     * 执行 restoreVersion 流程，围绕 restore version 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiDatasetResource restoreVersion(Long tenantId, String username, String datasetKey, int version) {
        return restoreVersion(tenantId, username, null, datasetKey, version);
    }

    /**
     * 执行 restoreVersion 流程，围绕 restore version 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiDatasetResource restoreVersion(Long tenantId, String username, String role, String datasetKey, int version) {
        return restoreVersionInternal(tenantId, username, role, datasetKey, version, null, false);
    }

    /**
     * 执行 restoreVersion 流程，围绕 restore version 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiDatasetResource restoreVersion(Long tenantId,
                                            String username,
                                            String role,
                                            String datasetKey,
                                            int version,
                                            String lockToken) {
        return restoreVersionInternal(tenantId, username, role, datasetKey, version, lockToken, true);
    }

    /**
     * 执行 restoreVersionInternal 流程，围绕 restore version internal 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersionInternal 流程生成的业务结果。
     */
    private BiDatasetResource restoreVersionInternal(Long tenantId,
                                                     String username,
                                                     String role,
                                                     String datasetKey,
                                                     int version,
                                                     String lockToken,
                                                     boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI dataset not found: " + datasetKey);
        }
        if (version <= 0) {
            throw new IllegalArgumentException("dataset version must be positive");
        }
        if (versionMapper == null) {
            throw new IllegalStateException("BI dataset version mapper is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiDatasetVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiDatasetVersionDO>()
                .eq(BiDatasetVersionDO::getTenantId, scopedTenantId)
                .eq(BiDatasetVersionDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetVersionDO::getDatasetId, row.getId())
                .eq(BiDatasetVersionDO::getVersion, version)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI dataset version not found: " + datasetKey + " v" + version);
        }
        if (enforceEditLock) {
            return saveDraft(scopedTenantId, username, role, resourceFromJson(snapshot.getResourceJson()), lockToken);
        }
        return saveDraft(scopedTenantId, username, role, resourceFromJson(snapshot.getResourceJson()));
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceId 业务对象 ID，用于定位具体记录。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param actionKey 业务键，用于在同一租户下定位资源。
     */
    private void requirePermission(Long tenantId,
                                   Long workspaceId,
                                   String resourceType,
                                   Long resourceId,
                                   String username,
                                   String role,
                                   String actionKey) {
        if (permissionGuard != null && resourceId != null) {
            permissionGuard.require(tenantId, workspaceId, resourceType, resourceId, username, role, actionKey);
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param resourceUpdatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param approvalAlwaysRequired approval always required 参数，用于 requirePublishApproval 流程中的校验、计算或对象转换。
     */
    private void requirePublishApproval(Long tenantId,
                                        Long workspaceId,
                                        String resourceType,
                                        String resourceKey,
                                        java.time.LocalDateTime resourceUpdatedAt,
                                        String role,
                                        boolean approvalAlwaysRequired) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (publishApprovalService != null && (approvalAlwaysRequired || !canBypassPublishApproval(role))) {
            publishApprovalService.requireApprovedApproval(
                    tenantId, workspaceId, resourceType, resourceKey, resourceUpdatedAt);
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 requires sql dataset approval 的布尔判断结果。
     */
    private boolean requiresSqlDatasetApproval(BiDatasetDO row) {
        return row != null && "SQL".equalsIgnoreCase(row.getDatasetType());
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param required required 参数，用于 requireEditLock 流程中的校验、计算或对象转换。
     */
    private void requireEditLock(Long tenantId,
                                 Long workspaceId,
                                 String resourceType,
                                 String resourceKey,
                                 String username,
                                 String role,
                                 String lockToken,
                                 boolean required) {
        if (required && collaborationService != null && !canBypassEditLock(role)) {
            collaborationService.requireCurrentLock(tenantId, workspaceId, resourceType, resourceKey, username, lockToken);
        }
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回布尔判断结果。
     */
    private boolean canBypassEditLock(String role) {
        return canBypassPublishApproval(role);
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回布尔判断结果。
     */
    private boolean canBypassPublishApproval(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return RoleNames.ADMIN.equals(normalized)
                || RoleNames.SUPER_ADMIN.equals(normalized)
                || RoleNames.TENANT_ADMIN.equals(normalized);
    }

    /**
     * 执行 dataset 流程，围绕 dataset 完成校验、计算或结果组装。
     *
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 dataset 流程生成的业务结果。
     */
    @Override
    public BiDatasetSpec dataset(String datasetKey, Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDatasetDO row = find(scopedTenantId, workspaceId, datasetKey);
        if (row == null) {
            row = find(0L, workspaceId(0L), datasetKey);
        }
        if (row == null || STATUS_ARCHIVED.equals(row.getStatus())) {
            return MarketingBiDatasetRegistry.dataset(datasetKey);
        }
        return toSpec(row, fields(row.getTenantId(), row.getId()), metrics(row.getTenantId(), row.getId()));
    }

    /**
     * 执行 datasets 流程，围绕 datasets 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 datasets 汇总后的集合、分页或映射视图。
     */
    @Override
    public List<BiDatasetSpec> datasets(Long tenantId) {
        Map<String, BiDatasetSpec> result = new LinkedHashMap<>();
        for (BiDatasetSpec builtIn : MarketingBiDatasetRegistry.datasets()) {
            result.put(builtIn.datasetKey(), builtIn);
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        // Tenant datasets override built-ins with the same key while preserving built-ins not customized by tenant.
        for (BiDatasetDO row : safeList(datasetMapper.selectList(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, scopedTenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .ne(BiDatasetDO::getStatus, STATUS_ARCHIVED)))) {
            result.put(row.getDatasetKey(), toSpec(row, fields(scopedTenantId, row.getId()), metrics(scopedTenantId, row.getId())));
        }
        return List.copyOf(result.values());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param resource resource 参数，用于 validateResource 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private BiDatasetResource validateResource(BiDatasetResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("dataset resource is required");
        }
        String datasetKey = required(resource.datasetKey(), "datasetKey");
        if (!RESOURCE_KEY.matcher(datasetKey).matches()) {
            throw new IllegalArgumentException("datasetKey contains unsafe characters");
        }
        String name = required(resource.name(), "name");
        String datasetType = required(resource.datasetType(), "datasetType").toUpperCase(Locale.ROOT);
        if (!DATASET_TYPES.contains(datasetType)) {
            throw new IllegalArgumentException("unsupported dataset type: " + datasetType);
        }
        String tenantColumn = required(resource.tenantColumn(), "tenantColumn");
        if (!COLUMN_EXPRESSION.matcher(tenantColumn).matches()) {
            throw new IllegalArgumentException("tenantColumn must be a column identifier");
        }
        String tableExpression = required(resource.tableExpression(), "tableExpression");
        Map<String, Object> model = resource.model();
        if ("SQL".equals(datasetType)) {
            // Persist SQL datasets as derived tables so executors can treat them like normal dataset specs.
            SqlDatasetNormalization normalizedSql = normalizeSqlDataset(tableExpression, tenantColumn, model);
            tableExpression = normalizedSql.tableExpression();
            model = normalizedSql.model();
        // 根据前序判断结果进入后续条件分支。
        } else if (!TABLE_EXPRESSION.matcher(tableExpression).matches()) {
            throw new IllegalArgumentException("tableExpression must be a qualified table name");
        }
        if (resource.fields().isEmpty()) {
            throw new IllegalArgumentException("dataset fields are required");
        }
        if (resource.metrics().isEmpty()) {
            throw new IllegalArgumentException("dataset metrics are required");
        }
        Set<String> fieldKeys = new LinkedHashSet<>();
        for (BiDatasetFieldResource field : resource.fields()) {
            validateField(field);
            if (!fieldKeys.add(field.fieldKey())) {
                throw new IllegalArgumentException("duplicate dataset field: " + field.fieldKey());
            }
        }
        Set<String> metricKeys = new LinkedHashSet<>();
        for (BiMetricResource metric : resource.metrics()) {
            validateMetric(metric, fieldKeys);
            if (!metricKeys.add(metric.metricKey())) {
                throw new IllegalArgumentException("duplicate metric: " + metric.metricKey());
            }
        }
        return new BiDatasetResource(
                datasetKey,
                name,
                datasetType,
                tableExpression,
                tenantColumn,
                model,
                resource.fields(),
                resource.metrics(),
                resource.status(),
                resource.source());
    }

    /**
     * 规范化输入值。
     *
     * @param sql sql 参数，用于 normalizeSqlDataset 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 normalizeSqlDataset 流程中的校验、计算或对象转换。
     * @param model model 参数，用于 normalizeSqlDataset 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private SqlDatasetNormalization normalizeSqlDataset(String sql,
                                                        String tenantColumn,
                                                        Map<String, Object> model) {
        String query = normalizedSqlQuery(sql, tenantColumn);
        List<String> parameterOrder = sqlParameterOrder(query);
        List<Map<String, Object>> parameterDefinitions = sqlParameterDefinitions(model, parameterOrder);
        String executableQuery = SQL_PARAMETER_TEMPLATE.matcher(query).replaceAll("?");
        String expression = "(" + executableQuery + ") " + SQL_DERIVED_TABLE_ALIAS;
        if (expression.length() > MAX_TABLE_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException("SQL dataset query is too long");
        }
        return new SqlDatasetNormalization(expression, sqlDatasetModel(model, query, parameterOrder, parameterDefinitions));
    }

    /**
     * 规范化输入值。
     *
     * @param sql sql 参数，用于 normalizedSqlQuery 流程中的校验、计算或对象转换。
     * @param tenantColumn tenant column 参数，用于 normalizedSqlQuery 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizedSqlQuery(String sql, String tenantColumn) {
        String query = sql.trim().replaceAll("\\s+", " ");
        // The SQL dataset surface is intentionally read-only and single-statement.
        if (!query.toUpperCase(Locale.ROOT).startsWith("SELECT ")
                || query.contains(";")
                || query.contains("--")
                || query.contains("/*")
                || query.contains("*/")
                || FORBIDDEN_SQL_TOKEN.matcher(query).find()) {
            throw new IllegalArgumentException("SQL dataset query must be a single read-only SELECT");
        }
        if (!Pattern.compile("\\b" + Pattern.quote(tenantColumn) + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(query)
                .find()) {
            throw new IllegalArgumentException("SQL dataset query must include tenant column: " + tenantColumn);
        }
        // Require an explicit source so parameter-only SELECTs cannot bypass lineage and permission review.
        if (!SQL_FROM_TOKEN.matcher(query).find()) {
            throw new IllegalArgumentException("SQL dataset query must include a FROM source");
        }
        return query;
    }

    /**
     * 执行 sqlDatasetModel 流程，围绕 sql dataset model 完成校验、计算或结果组装。
     *
     * @param model model 参数，用于 sqlDatasetModel 流程中的校验、计算或对象转换。
     * @param sqlTemplate sql template 参数，用于 sqlDatasetModel 流程中的校验、计算或对象转换。
     * @param parameterOrder parameter order 参数，用于 sqlDatasetModel 流程中的校验、计算或对象转换。
     * @param parameterDefinitions parameter definitions 参数，用于 sqlDatasetModel 流程中的校验、计算或对象转换。
     * @return 返回 sqlDatasetModel 流程生成的业务结果。
     */
    private Map<String, Object> sqlDatasetModel(Map<String, Object> model,
                                                String sqlTemplate,
                                                List<String> parameterOrder,
                                                List<Map<String, Object>> parameterDefinitions) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (model != null) {
            result.putAll(model);
        }
        result.put("sqlApprovalRequired", true);
        result.put("sqlTemplate", sqlTemplate);
        result.put("sqlParameterOrder", parameterOrder);
        result.put("sqlParameters", parameterDefinitions);
        return result;
    }

    /**
     * 执行 sqlParameterOrder 流程，围绕 sql parameter order 完成校验、计算或结果组装。
     *
     * @param query query 参数，用于 sqlParameterOrder 流程中的校验、计算或对象转换。
     * @return 返回 sql parameter order 汇总后的集合、分页或映射视图。
     */
    private List<String> sqlParameterOrder(String query) {
        List<String> order = new ArrayList<>();
        Matcher matcher = SQL_PARAMETER_TEMPLATE.matcher(query);
        while (matcher.find()) {
            order.add(matcher.group(1));
        }
        return order;
    }

    /**
     * 执行 sqlParameterDefinitions 流程，围绕 sql parameter definitions 完成校验、计算或结果组装。
     *
     * @param model model 参数，用于 sqlParameterDefinitions 流程中的校验、计算或对象转换。
     * @param parameterOrder parameter order 参数，用于 sqlParameterDefinitions 流程中的校验、计算或对象转换。
     * @return 返回 sqlParameterDefinitions 流程生成的业务结果。
     */
    private List<Map<String, Object>> sqlParameterDefinitions(Map<String, Object> model,
                                                              List<String> parameterOrder) {
        Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
        Object rawParameters = model == null ? null : model.get("sqlParameters");
        if (rawParameters instanceof List<?> rawList) {
            for (Object rawParameter : rawList) {
                Map<String, Object> definition = sqlParameterDefinition(rawParameter);
                definitions.put(String.valueOf(definition.get("key")), definition);
            }
        }
        Set<String> referenced = new LinkedHashSet<>(parameterOrder);
        for (String key : referenced) {
            if (!definitions.containsKey(key)) {
                throw new IllegalArgumentException("SQL parameter definition is required: " + key);
            }
        }
        // Reject unused definitions so approvals review the exact parameter contract that reaches execution.
        for (String key : definitions.keySet()) {
            if (!referenced.contains(key)) {
                throw new IllegalArgumentException("SQL parameter definition is not referenced: " + key);
            }
        }
        List<Map<String, Object>> orderedDefinitions = new ArrayList<>();
        for (String key : referenced) {
            orderedDefinitions.add(definitions.get(key));
        }
        return orderedDefinitions;
    }

    /**
     * 执行 sqlParameterDefinition 流程，围绕 sql parameter definition 完成校验、计算或结果组装。
     *
     * @param rawParameter raw parameter 参数，用于 sqlParameterDefinition 流程中的校验、计算或对象转换。
     * @return 返回 sqlParameterDefinition 流程生成的业务结果。
     */
    private Map<String, Object> sqlParameterDefinition(Object rawParameter) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(rawParameter instanceof Map<?, ?> rawMap)) {
            throw new IllegalArgumentException("SQL parameter definition must be an object");
        }
        String key = stringValue(rawMap.get("key"), "SQL parameter key");
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("SQL parameter key contains unsafe characters: " + key);
        }
        Object rawDataType = rawMap.containsKey("dataType") ? rawMap.get("dataType") : rawMap.get("type");
        String dataType = stringValue(rawDataType, "SQL parameter dataType")
                .toUpperCase(Locale.ROOT);
        if (!DATA_TYPES.contains(dataType)) {
            throw new IllegalArgumentException("unsupported SQL parameter data type: " + dataType);
        }
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("key", key);
        definition.put("dataType", dataType);
        definition.put("required", booleanValue(rawMap.get("required")));
        if (rawMap.containsKey("defaultValue")) {
            definition.put("defaultValue", rawMap.get("defaultValue"));
        }
        definition.put("allowedValues", listValues(rawMap.get("allowedValues")));
        // 汇总前面计算出的状态和明细，返回给调用方。
        return definition;
    }

    /**
     * 执行 sqlParameterSpecs 流程，围绕 sql parameter specs 完成校验、计算或结果组装。
     *
     * @param String string 参数，用于 sqlParameterSpecs 流程中的校验、计算或对象转换。
     * @param model model 参数，用于 sqlParameterSpecs 流程中的校验、计算或对象转换。
     * @return 返回 sql parameter specs 汇总后的集合、分页或映射视图。
     */
    private List<BiSqlParameterSpec> sqlParameterSpecs(Map<String, Object> model) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (model == null || model.isEmpty()) {
            return List.of();
        }
        Object rawParameters = model.get("sqlParameters");
        if (!(rawParameters instanceof List<?> rawList) || rawList.isEmpty()) {
            return List.of();
        }
        Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (Object rawParameter : rawList) {
            Map<String, Object> definition = sqlParameterDefinition(rawParameter);
            definitions.put(String.valueOf(definition.get("key")), definition);
        }
        List<String> order = stringListValues(model.get("sqlParameterOrder"));
        List<String> effectiveOrder = order.isEmpty() ? List.copyOf(definitions.keySet()) : order;
        List<BiSqlParameterSpec> result = new ArrayList<>();
        for (String key : effectiveOrder) {
            Map<String, Object> definition = definitions.get(key);
            if (definition == null) {
                throw new IllegalArgumentException("SQL parameter definition is required: " + key);
            }
            result.add(new BiSqlParameterSpec(
                    key,
                    stringValue(definition.get("dataType"), "SQL parameter dataType"),
                    booleanValue(definition.get("required")),
                    definition.containsKey("defaultValue") ? stringValue(definition.get("defaultValue")) : null,
                    stringListValues(definition.get("allowedValues"))));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return result;
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value, String fieldName) {
        String text = stringValue(value);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return text.trim();
    }

    /**
     * 执行 stringValue 流程，围绕 string value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string value 生成的文本或业务键。
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 booleanValue 流程，围绕 boolean value 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 boolean value 的布尔判断结果。
     */
    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 查询或读取业务数据。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<Object> listValues(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return List.copyOf(list);
    }

    /**
     * 执行 stringListValues 流程，围绕 string list values 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 string list values 汇总后的集合、分页或映射视图。
     */
    private List<String> stringListValues(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return list.stream()
                .map(String::valueOf)
                .toList();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     */
    private void validateField(BiDatasetFieldResource field) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (field == null) {
            throw new IllegalArgumentException("dataset field is required");
        }
        if (!RESOURCE_KEY.matcher(required(field.fieldKey(), "fieldKey")).matches()) {
            throw new IllegalArgumentException("fieldKey contains unsafe characters");
        }
        required(field.displayName(), "displayName");
        if (!COLUMN_EXPRESSION.matcher(required(field.columnExpression(), "columnExpression")).matches()) {
            throw new IllegalArgumentException("columnExpression must be a column identifier");
        }
        if (!FIELD_ROLES.contains(required(field.role(), "role"))) {
            throw new IllegalArgumentException("unsupported field role: " + field.role());
        }
        if (!DATA_TYPES.contains(required(field.dataType(), "dataType"))) {
            throw new IllegalArgumentException("unsupported field data type: " + field.dataType());
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param metric metric 参数，用于 validateMetric 流程中的校验、计算或对象转换。
     * @param fieldKeys field keys 参数，用于 validateMetric 流程中的校验、计算或对象转换。
     */
    private void validateMetric(BiMetricResource metric, Set<String> fieldKeys) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (metric == null) {
            throw new IllegalArgumentException("metric is required");
        }
        if (!RESOURCE_KEY.matcher(required(metric.metricKey(), "metricKey")).matches()) {
            throw new IllegalArgumentException("metricKey contains unsafe characters");
        }
        required(metric.displayName(), "displayName");
        String expression = required(metric.expression(), "expression");
        if (!METRIC_EXPRESSION.matcher(expression).matches()
                || expression.contains("--")
                || expression.contains("/*")
                || expression.contains(";")) {
            throw new IllegalArgumentException("metric expression contains unsafe characters");
        }
        required(metric.aggregation(), "aggregation");
        if (!DATA_TYPES.contains(required(metric.dataType(), "dataType"))) {
            throw new IllegalArgumentException("unsupported metric data type: " + metric.dataType());
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String dimension : metric.allowedDimensions()) {
            if (!fieldKeys.contains(dimension)) {
                throw new IllegalArgumentException("metric allowed dimension is not a dataset field: " + dimension);
            }
        }
    }

    /**
     * 查询或读取业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiDatasetDO find(Long tenantId, Long workspaceId, String datasetKey) {
        return datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .eq(BiDatasetDO::getTenantId, tenantId)
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .last("LIMIT 1"));
    }

    /**
     * 执行 workspaceId 流程，围绕 workspace id 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 workspace id 计算得到的数量、金额或指标值。
     */
    private Long workspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1"));
        return workspace == null || workspace.getId() == null ? 0L : workspace.getId();
    }

    /**
     * 执行 fields 流程，围绕 fields 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 fields 汇总后的集合、分页或映射视图。
     */
    private List<BiDatasetFieldResource> fields(Long tenantId, Long datasetId) {
        return safeList(fieldMapper.selectList(new LambdaQueryWrapper<BiDatasetFieldDO>()
                        .eq(BiDatasetFieldDO::getTenantId, tenantId)
                        .eq(BiDatasetFieldDO::getDatasetId, datasetId)
                        .orderByAsc(BiDatasetFieldDO::getSortOrder)
                        .orderByAsc(BiDatasetFieldDO::getFieldKey)))
                .stream()
                .map(this::toField)
                .toList();
    }

    /**
     * 执行 metrics 流程，围绕 metrics 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 metrics 汇总后的集合、分页或映射视图。
     */
    private List<BiMetricResource> metrics(Long tenantId, Long datasetId) {
        return safeList(metricMapper.selectList(new LambdaQueryWrapper<BiMetricDO>()
                        .eq(BiMetricDO::getTenantId, tenantId)
                        .eq(BiMetricDO::getDatasetId, datasetId)
                        .orderByAsc(BiMetricDO::getMetricKey)))
                .stream()
                .map(this::toMetric)
                .toList();
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param fields fields 参数，用于 toResource 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 toResource 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetResource toResource(BiDatasetDO row,
                                         List<BiDatasetFieldResource> fields,
                                         List<BiMetricResource> metrics) {
        return new BiDatasetResource(
                row.getDatasetKey(),
                row.getName(),
                row.getDatasetType(),
                row.getTableExpression(),
                row.getTenantColumn(),
                map(row.getModelJson()),
                fields,
                metrics,
                row.getStatus(),
                "PERSISTED");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param dataset dataset 参数，用于 fromBuiltIn 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetResource fromBuiltIn(BiDatasetSpec dataset) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<BiDatasetFieldResource> fields = dataset.fields().values().stream()
                .map(field -> new BiDatasetFieldResource(
                        field.fieldKey(),
                        field.fieldKey(),
                        field.columnExpression(),
                        field.role().name(),
                        field.valueType(),
                        null,
                        null,
                        null,
                        null,
                        true,
                        "NORMAL",
                        0))
                .toList();
        List<BiMetricResource> metrics = dataset.metrics().values().stream()
                .map(metric -> new BiMetricResource(
                        metric.metricKey(),
                        metric.metricKey(),
                        metric.expression(),
                        "CUSTOM",
                        metric.valueType(),
                        null,
                        null,
                        metric.allowedDimensions(),
                        "system",
                        null,
                        "ACTIVE"))
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDatasetResource(
                dataset.datasetKey(),
                dataset.datasetKey(),
                "TABLE",
                dataset.tableExpression(),
                dataset.tenantColumn(),
                Map.of("preset", true),
                fields,
                metrics,
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                STATUS_PUBLISHED,
                "PRESET");
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param fields fields 参数，用于 toSpec 流程中的校验、计算或对象转换。
     * @param metrics metrics 参数，用于 toSpec 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetSpec toSpec(BiDatasetDO row,
                                List<BiDatasetFieldResource> fields,
                                List<BiMetricResource> metrics) {
        Map<String, BiFieldSpec> fieldSpecs = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetFieldResource field : fields) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!field.visible()) {
                continue;
            }
            fieldSpecs.put(field.fieldKey(), new BiFieldSpec(
                    field.fieldKey(),
                    field.columnExpression(),
                    BiFieldSpec.Role.valueOf(field.role()),
                    field.dataType()));
        }
        Map<String, BiMetricSpec> metricSpecs = new LinkedHashMap<>();
        for (BiMetricResource metric : metrics) {
            if (STATUS_ARCHIVED.equals(metric.status())) {
                continue;
            }
            metricSpecs.put(metric.metricKey(), new BiMetricSpec(
                    metric.metricKey(),
                    metric.expression(),
                    metric.dataType(),
                    metric.allowedDimensions()));
        }
        Map<String, Object> model = map(row.getModelJson());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDatasetSpec(
                row.getDatasetKey(),
                row.getTableExpression(),
                row.getTenantColumn(),
                fieldSpecs,
                metricSpecs,
                sqlParameterSpecs(model),
                model);
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param resource resource 参数，用于 toSpec 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetSpec toSpec(BiDatasetResource resource) {
        Map<String, BiFieldSpec> fieldSpecs = new LinkedHashMap<>();
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDatasetFieldResource field : resource.fields()) {
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (!field.visible()) {
                continue;
            }
            fieldSpecs.put(field.fieldKey(), new BiFieldSpec(
                    field.fieldKey(),
                    field.columnExpression(),
                    BiFieldSpec.Role.valueOf(field.role()),
                    field.dataType()));
        }
        Map<String, BiMetricSpec> metricSpecs = new LinkedHashMap<>();
        for (BiMetricResource metric : resource.metrics()) {
            if (STATUS_ARCHIVED.equals(metric.status())) {
                continue;
            }
            metricSpecs.put(metric.metricKey(), new BiMetricSpec(
                    metric.metricKey(),
                    metric.expression(),
                    metric.dataType(),
                    metric.allowedDimensions()));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDatasetSpec(
                resource.datasetKey(),
                resource.tableExpression(),
                resource.tenantColumn(),
                fieldSpecs,
                metricSpecs,
                sqlParameterSpecs(resource.model()),
                resource.model());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetFieldDO toField(Long tenantId, Long datasetId, BiDatasetFieldResource field) {
        BiDatasetFieldDO row = new BiDatasetFieldDO();
        row.setTenantId(tenantId);
        row.setDatasetId(datasetId);
        row.setFieldKey(required(field.fieldKey(), "fieldKey"));
        row.setDisplayName(required(field.displayName(), "displayName"));
        row.setColumnExpression(required(field.columnExpression(), "columnExpression"));
        row.setRoleKey(required(field.role(), "role"));
        row.setDataType(required(field.dataType(), "dataType"));
        row.setSemanticType(field.semanticType());
        row.setDefaultAggregation(field.defaultAggregation());
        row.setFormatPattern(field.formatPattern());
        row.setUnit(field.unit());
        row.setVisible(field.visible());
        row.setSensitiveLevel(field.sensitiveLevel() == null || field.sensitiveLevel().isBlank() ? "NORMAL" : field.sensitiveLevel());
        row.setSortOrder(field.sortOrder());
        return row;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @param metric metric 参数，用于 toMetric 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiMetricDO toMetric(Long tenantId, Long workspaceId, Long datasetId, BiMetricResource metric) {
        BiMetricDO row = new BiMetricDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetId(datasetId);
        row.setMetricKey(required(metric.metricKey(), "metricKey"));
        row.setDisplayName(required(metric.displayName(), "displayName"));
        row.setExpression(required(metric.expression(), "expression"));
        row.setAggregation(required(metric.aggregation(), "aggregation"));
        row.setDataType(required(metric.dataType(), "dataType"));
        row.setUnit(metric.unit());
        row.setFormatPattern(metric.formatPattern());
        row.setAllowedDimensionsJson(json(metric.allowedDimensions()));
        row.setOwner(metric.owner());
        row.setDescription(metric.description());
        row.setStatus(metric.status() == null || metric.status().isBlank() ? "ACTIVE" : metric.status());
        return row;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetFieldResource toField(BiDatasetFieldDO row) {
        return new BiDatasetFieldResource(
                row.getFieldKey(),
                row.getDisplayName(),
                row.getColumnExpression(),
                row.getRoleKey(),
                row.getDataType(),
                row.getSemanticType(),
                row.getDefaultAggregation(),
                row.getFormatPattern(),
                row.getUnit(),
                Boolean.TRUE.equals(row.getVisible()),
                row.getSensitiveLevel(),
                row.getSortOrder() == null ? 0 : row.getSortOrder());
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiMetricResource toMetric(BiMetricDO row) {
        return new BiMetricResource(
                row.getMetricKey(),
                row.getDisplayName(),
                row.getExpression(),
                row.getAggregation(),
                row.getDataType(),
                row.getUnit(),
                row.getFormatPattern(),
                stringList(row.getAllowedDimensionsJson()),
                row.getOwner(),
                row.getDescription(),
                row.getStatus());
    }

    /**
     * 执行数据写入或状态变更。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param dataset dataset 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param resource resource 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     */
    private void insertVersionSnapshot(Long tenantId,
                                       Long workspaceId,
                                       BiDatasetDO dataset,
                                       BiDatasetResource resource,
                                       String username) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (versionMapper == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (dataset.getId() == null) {
            throw new IllegalStateException("BI dataset id is required for version snapshot: " + dataset.getDatasetKey());
        }
        BiDatasetVersionDO row = new BiDatasetVersionDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDatasetId(dataset.getId());
        row.setDatasetKey(dataset.getDatasetKey());
        row.setVersion(nextVersion(tenantId, workspaceId, dataset.getId()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setStatus(STATUS_PUBLISHED);
        row.setResourceJson(json(resource));
        row.setPublishedBy(defaultUser(username));
        versionMapper.insert(row);
    }

    /**
     * 执行 nextVersion 流程，围绕 next version 完成校验、计算或结果组装。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param datasetId 业务对象 ID，用于定位具体记录。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private int nextVersion(Long tenantId, Long workspaceId, Long datasetId) {
        if (versionMapper == null) {
            return 1;
        }
        BiDatasetVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<BiDatasetVersionDO>()
                .eq(BiDatasetVersionDO::getTenantId, tenantId)
                .eq(BiDatasetVersionDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetVersionDO::getDatasetId, datasetId)
                .orderByDesc(BiDatasetVersionDO::getVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDatasetVersionView toVersionView(BiDatasetVersionDO row) {
        return new BiDatasetVersionView(
                row.getId(),
                row.getDatasetKey(),
                row.getVersion(),
                row.getStatus(),
                resourceFromJson(row.getResourceJson()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 resourceFromJson 流程生成的业务结果。
     */
    private BiDatasetResource resourceFromJson(String json) {
        try {
            return objectMapper.readValue(json, BiDatasetResource.class);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI dataset version payload", e);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回组装或转换后的结果对象。
     */
    private Map<String, Object> map(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 执行 stringList 流程，围绕 string list 完成校验、计算或结果组装。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 string list 汇总后的集合、分页或映射视图。
     */
    private List<String> stringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 处理 JSON 序列化或反序列化。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI dataset payload", e);
        }
    }

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 按默认值规则处理输入值。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
