package org.chovy.canvas.domain.bi.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiPortalMenuDO;
import org.chovy.canvas.dal.dataobject.BiPortalVersionDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiPortalMenuMapper;
import org.chovy.canvas.dal.mapper.BiPortalVersionMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
/**
 * BiPortalResourceService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiPortalResourceService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Set<String> RESOURCE_TYPES = Set.of(
            "DASHBOARD",
            "CHART",
            "SELF_SERVICE",
            "SPREADSHEET",
            "BIG_SCREEN",
            "EXTERNAL_LINK");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiPortalMapper portalMapper;
    private final BiPortalMenuMapper menuMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final BiResourcePermissionGuard permissionGuard;
    private final BiPublishApprovalService publishApprovalService;
    private final BiResourceCollaborationService collaborationService;

    /**
     * 初始化 BiPortalResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param menuMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPortalResourceService(BiWorkspaceMapper workspaceMapper,
                                   BiPortalMapper portalMapper,
                                   BiPortalMenuMapper menuMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   ObjectMapper objectMapper) {
        this(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper, null, objectMapper, null);
    }

    @Autowired
    /**
     * 初始化 BiPortalResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param menuMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiPortalResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPortalResourceService(BiWorkspaceMapper workspaceMapper,
                                   BiPortalMapper portalMapper,
                                   BiPortalMenuMapper menuMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   BiPortalVersionMapper versionMapper,
                                   ObjectMapper objectMapper,
                                   BiResourcePermissionGuard permissionGuard,
                                   BiPublishApprovalService publishApprovalService,
                                   BiResourceCollaborationService collaborationService) {
        this.workspaceMapper = workspaceMapper;
        this.portalMapper = portalMapper;
        this.menuMapper = menuMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.permissionGuard = permissionGuard;
        this.publishApprovalService = publishApprovalService;
        this.collaborationService = collaborationService;
    }

    /**
     * 初始化 BiPortalResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param menuMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPortalResourceService(BiWorkspaceMapper workspaceMapper,
                                   BiPortalMapper portalMapper,
                                   BiPortalMenuMapper menuMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   BiPortalVersionMapper versionMapper,
                                   ObjectMapper objectMapper) {
        this(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper, versionMapper, objectMapper, null);
    }

    /**
     * 初始化 BiPortalResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param menuMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiPortalResourceService 流程中的校验、计算或对象转换。
     */
    public BiPortalResourceService(BiWorkspaceMapper workspaceMapper,
                                   BiPortalMapper portalMapper,
                                   BiPortalMenuMapper menuMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   BiPortalVersionMapper versionMapper,
                                   ObjectMapper objectMapper,
                                   BiResourcePermissionGuard permissionGuard) {
        this(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper, versionMapper, objectMapper,
                permissionGuard, null, null);
    }

    /**
     * 初始化 BiPortalResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param menuMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiPortalResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPortalResourceService(BiWorkspaceMapper workspaceMapper,
                                   BiPortalMapper portalMapper,
                                   BiPortalMenuMapper menuMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   BiPortalVersionMapper versionMapper,
                                   ObjectMapper objectMapper,
                                   BiResourcePermissionGuard permissionGuard,
                                   BiPublishApprovalService publishApprovalService) {
        this(workspaceMapper, portalMapper, menuMapper, dashboardMapper, chartMapper, versionMapper, objectMapper,
                permissionGuard, publishApprovalService, null);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiPortalResource> list(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(portalMapper.selectList(new LambdaQueryWrapper<BiPortalDO>()
                        .eq(BiPortalDO::getTenantId, scopedTenantId)
                        .eq(BiPortalDO::getWorkspaceId, workspaceId)
                        .ne(BiPortalDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiPortalDO::getUpdatedAt)
                        .orderByAsc(BiPortalDO::getPortalKey)))
                .stream()
                .map(row -> toResource(row, menus(scopedTenantId, row.getId())))
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回 get 流程生成的业务结果。
     */
    public BiPortalResource get(Long tenantId, String portalKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiPortalDO row = find(scopedTenantId, workspaceId, portalKey);
        if (row == null) {
            throw new IllegalArgumentException("BI portal not found: " + portalKey);
        }
        return toResource(row, menus(scopedTenantId, row.getId()));
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param resource resource 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiPortalResource saveDraft(Long tenantId, String username, BiPortalResource resource) {
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
    public BiPortalResource saveDraft(Long tenantId, String username, String role, BiPortalResource resource) {
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
    public BiPortalResource saveDraft(Long tenantId,
                                      String username,
                                      String role,
                                      BiPortalResource resource,
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
    private BiPortalResource saveDraftInternal(Long tenantId,
                                               String username,
                                               String role,
                                               BiPortalResource resource,
                                               String lockToken,
                                               boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        validateResource(resource);
        BiPortalDO existing = find(scopedTenantId, workspaceId, resource.portalKey());
        requirePermission(scopedTenantId, workspaceId, "PORTAL", existing == null ? null : existing.getId(),
                username, role, BiPermissionService.ACTION_EDIT);
        requireEditLock(scopedTenantId, workspaceId, "PORTAL", resource.portalKey(), username, role,
                lockToken, enforceEditLock && existing != null);

        BiPortalDO row = new BiPortalDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setPortalKey(required(resource.portalKey(), "portalKey"));
        row.setName(required(resource.name(), "name"));
        row.setThemeJson(json(resource.theme()));
        row.setStatus(STATUS_DRAFT);
        row.setCreatedBy(username == null || username.isBlank() ? "system" : username);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        portalMapper.upsert(row);

        BiPortalDO persisted = find(scopedTenantId, workspaceId, resource.portalKey());
        Long portalId = persisted == null ? row.getId() : persisted.getId();
        if (portalId == null) {
            throw new IllegalStateException("BI portal was not persisted: " + resource.portalKey());
        }
        menuMapper.deleteByPortal(scopedTenantId, portalId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiPortalMenuResource menu : resource.menus()) {
            menuMapper.insert(toMenu(scopedTenantId, workspaceId, portalId, menu));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiPortalResource(
                resource.portalKey(),
                resource.name(),
                resource.theme(),
                resource.menus(),
                STATUS_DRAFT,
                "PERSISTED");
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiPortalResource publish(Long tenantId, String portalKey) {
        return publish(tenantId, null, portalKey);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiPortalResource publish(Long tenantId, String username, String portalKey) {
        return publish(tenantId, username, null, portalKey);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiPortalResource publish(Long tenantId, String username, String role, String portalKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiPortalDO row = find(scopedTenantId, workspaceId, portalKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            throw new IllegalArgumentException("BI portal not found: " + portalKey);
        }
        requirePermission(scopedTenantId, workspaceId, "PORTAL", row.getId(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                username, role, BiPermissionService.ACTION_PUBLISH);
        requirePublishApproval(scopedTenantId, workspaceId, "PORTAL", portalKey, row.getUpdatedAt(), role);
        portalMapper.publish(scopedTenantId, workspaceId, portalKey);
        BiPortalDO published = find(scopedTenantId, workspaceId, portalKey);
        if (published == null) {
            row.setStatus(STATUS_PUBLISHED);
            BiPortalResource resource = toResource(row, menus(scopedTenantId, row.getId()));
            insertVersionSnapshot(scopedTenantId, workspaceId, row, resource, username);
            return resource;
        }
        BiPortalResource resource = toResource(published, menus(scopedTenantId, published.getId()));
        insertVersionSnapshot(scopedTenantId, workspaceId, published, resource, username);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return resource;
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 流程生成的业务结果。
     */
    public BiPortalResource archive(Long tenantId, String portalKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiPortalDO row = find(scopedTenantId, workspaceId, portalKey);
        if (row == null) {
            throw new IllegalArgumentException("BI portal not found: " + portalKey);
        }
        portalMapper.archive(scopedTenantId, workspaceId, portalKey);
        BiPortalDO archived = find(scopedTenantId, workspaceId, portalKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return toResource(row, menus(scopedTenantId, row.getId()));
        }
        return toResource(archived, menus(scopedTenantId, archived.getId()));
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiPortalVersionView> listVersions(Long tenantId, String portalKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiPortalDO row = find(scopedTenantId, workspaceId, portalKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiPortalVersionDO>()
                        .eq(BiPortalVersionDO::getTenantId, scopedTenantId)
                        .eq(BiPortalVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiPortalVersionDO::getPortalId, row.getId())
                        .orderByDesc(BiPortalVersionDO::getVersion)
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
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiPortalResource restoreVersion(Long tenantId, String username, String portalKey, int version) {
        return restoreVersion(tenantId, username, null, portalKey, version);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiPortalResource restoreVersion(Long tenantId, String username, String role, String portalKey, int version) {
        return restoreVersionInternal(tenantId, username, role, portalKey, version, null, false);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiPortalResource restoreVersion(Long tenantId,
                                           String username,
                                           String role,
                                           String portalKey,
                                           int version,
                                           String lockToken) {
        return restoreVersionInternal(tenantId, username, role, portalKey, version, lockToken, true);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersionInternal 流程生成的业务结果。
     */
    private BiPortalResource restoreVersionInternal(Long tenantId,
                                                    String username,
                                                    String role,
                                                    String portalKey,
                                                    int version,
                                                    String lockToken,
                                                    boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiPortalDO row = find(scopedTenantId, workspaceId, portalKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI portal not found: " + portalKey);
        }
        if (version <= 0) {
            throw new IllegalArgumentException("portal version must be positive");
        }
        if (versionMapper == null) {
            throw new IllegalStateException("BI portal version mapper is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiPortalVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiPortalVersionDO>()
                .eq(BiPortalVersionDO::getTenantId, scopedTenantId)
                .eq(BiPortalVersionDO::getWorkspaceId, workspaceId)
                .eq(BiPortalVersionDO::getPortalId, row.getId())
                .eq(BiPortalVersionDO::getVersion, version)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI portal version not found: " + portalKey + " v" + version);
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
     * @param resource resource 参数，用于 validateResource 流程中的校验、计算或对象转换。
     */
    private void validateResource(BiPortalResource resource) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (resource == null) {
            throw new IllegalArgumentException("portal resource is required");
        }
        if (!RESOURCE_KEY.matcher(required(resource.portalKey(), "portalKey")).matches()) {
            throw new IllegalArgumentException("portalKey contains unsafe characters");
        }
        required(resource.name(), "name");
        if (resource.menus().isEmpty()) {
            throw new IllegalArgumentException("portal menus are required");
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiPortalMenuResource menu : resource.menus()) {
            validateMenu(menu);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param menu menu 参数，用于 validateMenu 流程中的校验、计算或对象转换。
     */
    private void validateMenu(BiPortalMenuResource menu) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (menu == null) {
            throw new IllegalArgumentException("portal menu is required");
        }
        if (!RESOURCE_KEY.matcher(required(menu.menuKey(), "menuKey")).matches()) {
            throw new IllegalArgumentException("menuKey contains unsafe characters");
        }
        if (menu.parentMenuKey() != null
                && !menu.parentMenuKey().isBlank()
                && !RESOURCE_KEY.matcher(menu.parentMenuKey()).matches()) {
            throw new IllegalArgumentException("parentMenuKey contains unsafe characters");
        }
        required(menu.title(), "title");
        String resourceType = required(menu.resourceType(), "resourceType");
        if (!RESOURCE_TYPES.contains(resourceType)) {
            throw new IllegalArgumentException("unsupported portal resource type: " + resourceType);
        }
        if ("EXTERNAL_LINK".equals(resourceType)) {
            String url = required(menu.externalUrl(), "externalUrl");
            if (!url.startsWith("https://") && !url.startsWith("http://") && !url.startsWith("/")) {
                throw new IllegalArgumentException("externalUrl must be http(s) or an internal path");
            }
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        required(menu.resourceKey(), "resourceKey");
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param portalKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiPortalDO find(Long tenantId, Long workspaceId, String portalKey) {
        return portalMapper.selectOne(new LambdaQueryWrapper<BiPortalDO>()
                .eq(BiPortalDO::getTenantId, tenantId)
                .eq(BiPortalDO::getWorkspaceId, workspaceId)
                .eq(BiPortalDO::getPortalKey, required(portalKey, "portalKey"))
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
     * @param portalId 业务对象 ID，用于定位具体记录。
     * @return 返回 menus 汇总后的集合、分页或映射视图。
     */
    private List<BiPortalMenuResource> menus(Long tenantId, Long portalId) {
        return safeList(menuMapper.selectList(new LambdaQueryWrapper<BiPortalMenuDO>()
                        .eq(BiPortalMenuDO::getTenantId, tenantId)
                        .eq(BiPortalMenuDO::getPortalId, portalId)))
                .stream()
                .sorted(Comparator.comparingInt(row -> row.getSortOrder() == null ? 0 : row.getSortOrder()))
                .map(row -> toMenu(tenantId, row))
                .toList();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param menus menus 参数，用于 toResource 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiPortalResource toResource(BiPortalDO row, List<BiPortalMenuResource> menus) {
        return new BiPortalResource(
                row.getPortalKey(),
                row.getName(),
                map(row.getThemeJson()),
                menus,
                row.getStatus(),
                "PERSISTED");
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param portalId 业务对象 ID，用于定位具体记录。
     * @param menu menu 参数，用于 toMenu 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiPortalMenuDO toMenu(Long tenantId, Long workspaceId, Long portalId, BiPortalMenuResource menu) {
        BiPortalMenuDO row = new BiPortalMenuDO();
        row.setTenantId(tenantId);
        row.setPortalId(portalId);
        row.setMenuKey(required(menu.menuKey(), "menuKey"));
        row.setParentMenuKey(menu.parentMenuKey());
        row.setTitle(required(menu.title(), "title"));
        row.setResourceType(required(menu.resourceType(), "resourceType"));
        row.setResourceId(resourceId(tenantId, workspaceId, menu));
        row.setExternalUrl(menu.externalUrl());
        row.setVisibilityJson(json(menu.visibility()));
        row.setSortOrder(menu.sortOrder());
        return row;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiPortalMenuResource toMenu(Long tenantId, BiPortalMenuDO row) {
        return new BiPortalMenuResource(
                row.getMenuKey(),
                row.getParentMenuKey(),
                row.getTitle(),
                row.getResourceType(),
                resourceKey(tenantId, row),
                row.getResourceId(),
                row.getExternalUrl(),
                map(row.getVisibilityJson()),
                row.getSortOrder() == null ? 0 : row.getSortOrder());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param menu menu 参数，用于 resourceId 流程中的校验、计算或对象转换。
     * @return 返回 resource id 计算得到的数量、金额或指标值。
     */
    private Long resourceId(Long tenantId, Long workspaceId, BiPortalMenuResource menu) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if ("EXTERNAL_LINK".equals(menu.resourceType())) {
            return null;
        }
        if ("DASHBOARD".equals(menu.resourceType())) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            BiDashboardDO dashboard = dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                    .in(BiDashboardDO::getTenantId, List.of(tenantId, 0L))
                    .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                    .eq(BiDashboardDO::getDashboardKey, required(menu.resourceKey(), "resourceKey"))
                    .orderByDesc(BiDashboardDO::getTenantId)
                    .last("LIMIT 1"));
            if (dashboard == null || dashboard.getId() == null) {
                throw new IllegalArgumentException("portal dashboard resource not found: " + menu.resourceKey());
            }
            return dashboard.getId();
        }
        if ("CHART".equals(menu.resourceType())) {
            BiChartDO chart = chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                    .in(BiChartDO::getTenantId, List.of(tenantId, 0L))
                    .eq(BiChartDO::getWorkspaceId, workspaceId)
                    .eq(BiChartDO::getChartKey, required(menu.resourceKey(), "resourceKey"))
                    .orderByDesc(BiChartDO::getTenantId)
                    .last("LIMIT 1"));
            if (chart == null || chart.getId() == null) {
                throw new IllegalArgumentException("portal chart resource not found: " + menu.resourceKey());
            }
            return chart.getId();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return menu.resourceId();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 resource key 生成的文本或业务键。
     */
    private String resourceKey(Long tenantId, BiPortalMenuDO row) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row.getResourceId() == null) {
            return null;
        }
        if ("DASHBOARD".equals(row.getResourceType())) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            BiDashboardDO dashboard = dashboardMapper.selectById(row.getResourceId());
            return dashboard == null ? null : dashboard.getDashboardKey();
        }
        if ("CHART".equals(row.getResourceType())) {
            BiChartDO chart = chartMapper.selectById(row.getResourceId());
            return chart == null ? null : chart.getChartKey();
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return null;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param portal portal 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param resource resource 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     */
    private void insertVersionSnapshot(Long tenantId,
                                       Long workspaceId,
                                       BiPortalDO portal,
                                       BiPortalResource resource,
                                       String username) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (versionMapper == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (portal.getId() == null) {
            throw new IllegalStateException("BI portal id is required for version snapshot: " + portal.getPortalKey());
        }
        BiPortalVersionDO row = new BiPortalVersionDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setPortalId(portal.getId());
        row.setPortalKey(portal.getPortalKey());
        row.setVersion(nextVersion(tenantId, workspaceId, portal.getId()));
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
     * @param portalId 业务对象 ID，用于定位具体记录。
     * @return 返回 next version 计算得到的数量、金额或指标值。
     */
    private int nextVersion(Long tenantId, Long workspaceId, Long portalId) {
        if (versionMapper == null) {
            return 1;
        }
        BiPortalVersionDO latest = versionMapper.selectOne(new LambdaQueryWrapper<BiPortalVersionDO>()
                .eq(BiPortalVersionDO::getTenantId, tenantId)
                .eq(BiPortalVersionDO::getWorkspaceId, workspaceId)
                .eq(BiPortalVersionDO::getPortalId, portalId)
                .orderByDesc(BiPortalVersionDO::getVersion)
                .last("LIMIT 1"));
        return latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiPortalVersionView toVersionView(BiPortalVersionDO row) {
        return new BiPortalVersionView(
                row.getId(),
                row.getPortalKey(),
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
    private BiPortalResource resourceFromJson(String json) {
        try {
            return objectMapper.readValue(json, BiPortalResource.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI portal version payload", e);
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
            throw new IllegalArgumentException("invalid BI portal payload", e);
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
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
