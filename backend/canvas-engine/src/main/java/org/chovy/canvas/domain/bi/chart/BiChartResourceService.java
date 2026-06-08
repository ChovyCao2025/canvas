package org.chovy.canvas.domain.bi.chart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiChartVersionDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiChartVersionMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiDatasetSpecResolver;
import org.chovy.canvas.domain.bi.query.BiQueryCompiler;
import org.chovy.canvas.domain.bi.query.BiQueryRequest;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
/**
 * BiChartResourceService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiChartResourceService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Set<String> CHART_TYPES = Set.of(
            "KPI_CARD",
            "TABLE",
            "CROSS_TABLE",
            "LINE",
            "AREA",
            "BAR",
            "STACKED_BAR",
            "PIE",
            "FUNNEL",
            "SCATTER",
            "HEATMAP");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiChartMapper chartMapper;
    private final BiChartVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final BiDatasetSpecResolver datasetSpecResolver;
    private final BiResourcePermissionGuard permissionGuard;
    private final BiPublishApprovalService publishApprovalService;
    private final BiResourceCollaborationService collaborationService;
    private final BiQueryCompiler queryCompiler = new BiQueryCompiler();

    /**
     * 初始化 BiChartResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiChartResourceService(BiWorkspaceMapper workspaceMapper,
                                  BiDatasetMapper datasetMapper,
                                  BiChartMapper chartMapper,
                                  ObjectMapper objectMapper) {
        this(workspaceMapper, datasetMapper, chartMapper, null, objectMapper, BiDatasetSpecResolver.builtIn(), null);
    }

    @Autowired
    /**
     * 初始化 BiChartResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolverProvider 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param permissionGuardProvider permission guard provider 参数，用于 BiChartResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiChartResourceService(BiWorkspaceMapper workspaceMapper,
                                  BiDatasetMapper datasetMapper,
                                  BiChartMapper chartMapper,
                                  BiChartVersionMapper versionMapper,
                                  ObjectMapper objectMapper,
                                  ObjectProvider<BiDatasetSpecResolver> datasetSpecResolverProvider,
                                  ObjectProvider<BiResourcePermissionGuard> permissionGuardProvider,
                                  ObjectProvider<BiPublishApprovalService> publishApprovalServiceProvider,
                                  ObjectProvider<BiResourceCollaborationService> collaborationServiceProvider) {
        this(workspaceMapper, datasetMapper, chartMapper, versionMapper, objectMapper,
                datasetSpecResolverProvider.getIfAvailable(BiDatasetSpecResolver::builtIn),
                permissionGuardProvider == null ? null : permissionGuardProvider.getIfAvailable(),
                publishApprovalServiceProvider == null ? null : publishApprovalServiceProvider.getIfAvailable(),
                collaborationServiceProvider == null ? null : collaborationServiceProvider.getIfAvailable());
    }

    /**
     * 初始化 BiChartResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public BiChartResourceService(BiWorkspaceMapper workspaceMapper,
                                  BiDatasetMapper datasetMapper,
                                  BiChartMapper chartMapper,
                                  BiChartVersionMapper versionMapper,
                                  ObjectMapper objectMapper,
                                  BiDatasetSpecResolver datasetSpecResolver) {
        this(workspaceMapper, datasetMapper, chartMapper, versionMapper, objectMapper, datasetSpecResolver, null);
    }

    /**
     * 初始化 BiChartResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiChartResourceService 流程中的校验、计算或对象转换。
     */
    public BiChartResourceService(BiWorkspaceMapper workspaceMapper,
                                  BiDatasetMapper datasetMapper,
                                  BiChartMapper chartMapper,
                                  BiChartVersionMapper versionMapper,
                                  ObjectMapper objectMapper,
                                  BiDatasetSpecResolver datasetSpecResolver,
                                  BiResourcePermissionGuard permissionGuard) {
        this(workspaceMapper, datasetMapper, chartMapper, versionMapper, objectMapper, datasetSpecResolver,
                permissionGuard, null, null);
    }

    /**
     * 初始化 BiChartResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiChartResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiChartResourceService(BiWorkspaceMapper workspaceMapper,
                                  BiDatasetMapper datasetMapper,
                                  BiChartMapper chartMapper,
                                  BiChartVersionMapper versionMapper,
                                  ObjectMapper objectMapper,
                                  BiDatasetSpecResolver datasetSpecResolver,
                                  BiResourcePermissionGuard permissionGuard,
                                  BiPublishApprovalService publishApprovalService) {
        this(workspaceMapper, datasetMapper, chartMapper, versionMapper, objectMapper, datasetSpecResolver,
                permissionGuard, publishApprovalService, null);
    }

    /**
     * 初始化 BiChartResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetSpecResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiChartResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiChartResourceService(BiWorkspaceMapper workspaceMapper,
                                  BiDatasetMapper datasetMapper,
                                  BiChartMapper chartMapper,
                                  BiChartVersionMapper versionMapper,
                                  ObjectMapper objectMapper,
                                  BiDatasetSpecResolver datasetSpecResolver,
                                  BiResourcePermissionGuard permissionGuard,
                                  BiPublishApprovalService publishApprovalService,
                                  BiResourceCollaborationService collaborationService) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.chartMapper = chartMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.datasetSpecResolver = datasetSpecResolver == null ? BiDatasetSpecResolver.builtIn() : datasetSpecResolver;
        this.permissionGuard = permissionGuard;
        this.publishApprovalService = publishApprovalService;
        this.collaborationService = collaborationService;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiChartResource> list(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(chartMapper.selectList(new LambdaQueryWrapper<BiChartDO>()
                        .eq(BiChartDO::getTenantId, scopedTenantId)
                        .eq(BiChartDO::getWorkspaceId, workspaceId)
                        .ne(BiChartDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiChartDO::getUpdatedAt)
                        .orderByAsc(BiChartDO::getChartKey)))
                .stream()
                .map(this::toResource)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @return 返回 get 流程生成的业务结果。
     */
    public BiChartResource get(Long tenantId, String chartKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiChartDO row = find(scopedTenantId, workspaceId, chartKey);
        if (row == null) {
            throw new IllegalArgumentException("BI chart not found: " + chartKey);
        }
        return toResource(row);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiChartResource saveDraft(Long tenantId, String username, BiChartResource resource) {
        return saveDraft(tenantId, username, null, resource);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiChartResource saveDraft(Long tenantId, String username, String role, BiChartResource resource) {
        return saveDraftInternal(tenantId, username, role, resource, null, false);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回流程执行后的业务结果。
     */
    public BiChartResource saveDraft(Long tenantId,
                                     String username,
                                     String role,
                                     BiChartResource resource,
                                     String lockToken) {
        return saveDraftInternal(tenantId, username, role, resource, lockToken, true);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param resource resource 参数，用于 saveDraftInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 saveDraftInternal 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private BiChartResource saveDraftInternal(Long tenantId,
                                              String username,
                                              String role,
                                              BiChartResource resource,
                                              String lockToken,
                                              boolean enforceEditLock) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        validateResource(resource, scopedTenantId);
        BiChartDO existing = find(scopedTenantId, workspaceId, resource.chartKey());
        requirePermission(scopedTenantId, workspaceId, "CHART", existing == null ? null : existing.getId(),
                username, role, BiPermissionService.ACTION_EDIT);
        requireEditLock(scopedTenantId, workspaceId, "CHART", resource.chartKey(), username, role,
                lockToken, enforceEditLock && existing != null);

        BiChartDO row = new BiChartDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setChartKey(required(resource.chartKey(), "chartKey"));
        row.setName(required(resource.name(), "name"));
        row.setChartType(required(resource.chartType(), "chartType"));
        row.setDatasetId(datasetId(scopedTenantId, workspaceId, resource.datasetKey()));
        row.setQueryJson(json(resource.query()));
        row.setStyleJson(json(resource.style()));
        row.setInteractionJson(json(resource.interaction()));
        row.setStatus(STATUS_DRAFT);
        row.setCreatedBy(username == null || username.isBlank() ? "system" : username);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        chartMapper.upsert(row);

        BiChartDO persisted = find(scopedTenantId, workspaceId, resource.chartKey());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return persisted == null ? new BiChartResource(
                resource.chartKey(),
                resource.name(),
                resource.chartType(),
                resource.datasetKey(),
                resource.query(),
                resource.style(),
                resource.interaction(),
                STATUS_DRAFT,
                "PERSISTED") : toResource(persisted);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiChartResource publish(Long tenantId, String chartKey) {
        return publish(tenantId, null, chartKey);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiChartResource publish(Long tenantId, String username, String chartKey) {
        return publish(tenantId, username, null, chartKey);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiChartResource publish(Long tenantId, String username, String role, String chartKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiChartDO row = find(scopedTenantId, workspaceId, chartKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            throw new IllegalArgumentException("BI chart not found: " + chartKey);
        }
        requirePermission(scopedTenantId, workspaceId, "CHART", row.getId(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                username, role, BiPermissionService.ACTION_PUBLISH);
        requirePublishApproval(scopedTenantId, workspaceId, "CHART", chartKey, row.getUpdatedAt(), role);
        chartMapper.publish(scopedTenantId, workspaceId, chartKey);
        BiChartDO published = find(scopedTenantId, workspaceId, chartKey);
        if (published == null) {
            row.setStatus(STATUS_PUBLISHED);
            BiChartResource resource = toResource(row);
            insertVersionSnapshot(scopedTenantId, workspaceId, row, resource, username);
            return resource;
        }
        BiChartResource resource = toResource(published);
        insertVersionSnapshot(scopedTenantId, workspaceId, published, resource, username);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return resource;
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 流程生成的业务结果。
     */
    public BiChartResource archive(Long tenantId, String chartKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiChartDO row = find(scopedTenantId, workspaceId, chartKey);
        if (row == null) {
            throw new IllegalArgumentException("BI chart not found: " + chartKey);
        }
        chartMapper.archive(scopedTenantId, workspaceId, chartKey);
        BiChartDO archived = find(scopedTenantId, workspaceId, chartKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row);
        }
        return toResource(archived);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiChartVersionView> listVersions(Long tenantId, String chartKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiChartDO row = find(scopedTenantId, workspaceId, chartKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiChartVersionDO>()
                        .eq(BiChartVersionDO::getTenantId, scopedTenantId)
                        .eq(BiChartVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiChartVersionDO::getChartId, row.getId())
                        .orderByDesc(BiChartVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiChartResource restoreVersion(Long tenantId, String username, String chartKey, int version) {
        return restoreVersion(tenantId, username, null, chartKey, version);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiChartResource restoreVersion(Long tenantId, String username, String role, String chartKey, int version) {
        return restoreVersionInternal(tenantId, username, role, chartKey, version, null, false);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiChartResource restoreVersion(Long tenantId,
                                          String username,
                                          String role,
                                          String chartKey,
                                          int version,
                                          String lockToken) {
        return restoreVersionInternal(tenantId, username, role, chartKey, version, lockToken, true);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersionInternal 流程生成的业务结果。
     */
    private BiChartResource restoreVersionInternal(Long tenantId,
                                                   String username,
                                                   String role,
                                                   String chartKey,
                                                   int version,
                                                   String lockToken,
                                                   boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiChartDO row = find(scopedTenantId, workspaceId, chartKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI chart not found: " + chartKey);
        }
        if (version <= 0) {
            throw new IllegalArgumentException("chart version must be positive");
        }
        if (versionMapper == null) {
            throw new IllegalStateException("BI chart version mapper is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiChartVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiChartVersionDO>()
                .eq(BiChartVersionDO::getTenantId, scopedTenantId)
                .eq(BiChartVersionDO::getWorkspaceId, workspaceId)
                .eq(BiChartVersionDO::getChartId, row.getId())
                .eq(BiChartVersionDO::getVersion, version)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI chart version not found: " + chartKey + " v" + version);
        }
        if (enforceEditLock) {
            return saveDraft(scopedTenantId, username, role, resourceFromJson(snapshot.getResourceJson()), lockToken);
        }
        return saveDraft(scopedTenantId, username, role, resourceFromJson(snapshot.getResourceJson()));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     * @param resourceUpdatedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param role 角色标识，用于权限校验和访问范围判断。
     */
    private void requirePublishApproval(Long tenantId,
                                        Long workspaceId,
                                        String resourceType,
                                        String resourceKey,
                                        java.time.LocalDateTime resourceUpdatedAt,
                                        String role) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (publishApprovalService != null && !canBypassPublishApproval(role)) {
            publishApprovalService.requireApprovedApproval(
                    tenantId, workspaceId, resourceType, resourceKey, resourceUpdatedAt);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
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
     * 校验输入、权限或业务前置条件。
     *
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @return 返回布尔判断结果。
     */
    private boolean canBypassEditLock(String role) {
        return canBypassPublishApproval(role);
    }

    /**
     * 校验输入、权限或业务前置条件。
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
     * 校验输入、权限或业务前置条件。
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
     * 校验输入、权限或业务前置条件。
     *
     * @param resource resource 参数，用于 validateResource 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     */
    private void validateResource(BiChartResource resource, Long tenantId) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (resource == null) {
            throw new IllegalArgumentException("chart resource is required");
        }
        if (!RESOURCE_KEY.matcher(required(resource.chartKey(), "chartKey")).matches()) {
            throw new IllegalArgumentException("chartKey contains unsafe characters");
        }
        required(resource.name(), "name");
        String chartType = required(resource.chartType(), "chartType");
        if (!CHART_TYPES.contains(chartType)) {
            throw new IllegalArgumentException("unsupported chart type: " + chartType);
        }
        String datasetKey = required(resource.datasetKey(), "datasetKey");
        if (resource.query() == null) {
            throw new IllegalArgumentException("query is required");
        }
        if (!datasetKey.equals(resource.query().datasetKey())) {
            throw new IllegalArgumentException("dataset does not match query");
        }
        BiDatasetSpec dataset = datasetSpecResolver.dataset(datasetKey, tenantId);
        queryCompiler.compile(dataset, resource.query(), tenantId);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param chartKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiChartDO find(Long tenantId, Long workspaceId, String chartKey) {
        return chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                .eq(BiChartDO::getTenantId, tenantId)
                .eq(BiChartDO::getWorkspaceId, workspaceId)
                .eq(BiChartDO::getChartKey, required(chartKey, "chartKey"))
                .last("LIMIT 1"));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param datasetKey 业务键，用于在同一租户下定位资源。
     * @return 返回 dataset id 计算得到的数量、金额或指标值。
     */
    private Long datasetId(Long tenantId, Long workspaceId, String datasetKey) {
        BiDatasetDO dataset = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                .in(BiDatasetDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                .orderByDesc(BiDatasetDO::getTenantId)
                .last("LIMIT 1"));
        if (dataset == null) {
            dataset = datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                    .eq(BiDatasetDO::getTenantId, 0L)
                    .eq(BiDatasetDO::getDatasetKey, required(datasetKey, "datasetKey"))
                    .last("LIMIT 1"));
        }
        if (dataset == null || dataset.getId() == null) {
            throw new IllegalArgumentException("BI dataset metadata not found: " + datasetKey);
        }
        return dataset.getId();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiChartResource toResource(BiChartDO row) {
        BiQueryRequest query = query(row.getQueryJson());
        return new BiChartResource(
                row.getChartKey(),
                row.getName(),
                row.getChartType(),
                query.datasetKey(),
                query,
                map(row.getStyleJson()),
                map(row.getInteractionJson()),
                row.getStatus(),
                "PERSISTED");
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param chart chart 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param resource resource 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     */
    private void insertVersionSnapshot(Long tenantId,
                                       Long workspaceId,
                                       BiChartDO chart,
                                       BiChartResource resource,
                                       String username) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (versionMapper == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (chart.getId() == null) {
            throw new IllegalStateException("BI chart id is required for version snapshot: " + chart.getChartKey());
        }
        BiChartVersionDO row = new BiChartVersionDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setChartId(chart.getId());
        row.setChartKey(chart.getChartKey());
        row.setVersion(nextVersion(tenantId, workspaceId, chart.getId()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setStatus(STATUS_PUBLISHED);
        row.setResourceJson(json(resource));
        row.setPublishedBy(defaultUser(username));
        versionMapper.insert(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param chartId 业务对象 ID，用于定位具体记录。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private int nextVersion(Long tenantId, Long workspaceId, Long chartId) {
        if (versionMapper == null) {
            return 1;
        }
        BiChartVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<BiChartVersionDO>()
                .eq(BiChartVersionDO::getTenantId, tenantId)
                .eq(BiChartVersionDO::getWorkspaceId, workspaceId)
                .eq(BiChartVersionDO::getChartId, chartId)
                .orderByDesc(BiChartVersionDO::getVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiChartVersionView toVersionView(BiChartVersionDO row) {
        return new BiChartVersionView(
                row.getId(),
                row.getChartKey(),
                row.getVersion(),
                row.getStatus(),
                resourceFromJson(row.getResourceJson()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 resourceFromJson 流程生成的业务结果。
     */
    private BiChartResource resourceFromJson(String json) {
        try {
            return objectMapper.readValue(json, BiChartResource.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI chart version payload", e);
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiQueryRequest query(String json) {
        try {
            return objectMapper.readValue(json, BiQueryRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI chart query payload", e);
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
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 json 生成的文本或业务键。
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI chart payload", e);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
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
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param username 操作人标识，用于审计和权限判断。
     * @return 返回 default user 生成的文本或业务键。
     */
    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
