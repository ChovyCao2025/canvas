package org.chovy.canvas.domain.bi.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.tenant.RoleNames;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDashboardVersionDO;
import org.chovy.canvas.dal.dataobject.BiDashboardWidgetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDashboardVersionMapper;
import org.chovy.canvas.dal.mapper.BiDashboardWidgetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.chovy.canvas.domain.bi.permission.BiPermissionService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourcePermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
/**
 * BiDashboardResourceService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class BiDashboardResourceService {

    private static final String WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiDashboardWidgetMapper widgetMapper;
    private final BiDashboardVersionMapper versionMapper;
    private final ObjectMapper objectMapper;
    private final BiResourcePermissionGuard permissionGuard;
    private final BiPublishApprovalService publishApprovalService;
    private final BiResourceCollaborationService collaborationService;

    /**
     * 初始化 BiDashboardResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param widgetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      ObjectMapper objectMapper) {
        this(workspaceMapper, dashboardMapper, widgetMapper, null, objectMapper, null);
    }

    @Autowired
    /**
     * 初始化 BiDashboardResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param widgetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiDashboardResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     * @param collaborationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      BiDashboardVersionMapper versionMapper,
                                      ObjectMapper objectMapper,
                                      BiResourcePermissionGuard permissionGuard,
                                      BiPublishApprovalService publishApprovalService,
                                      BiResourceCollaborationService collaborationService) {
        this.workspaceMapper = workspaceMapper;
        this.dashboardMapper = dashboardMapper;
        this.widgetMapper = widgetMapper;
        this.versionMapper = versionMapper;
        this.objectMapper = objectMapper;
        this.permissionGuard = permissionGuard;
        this.publishApprovalService = publishApprovalService;
        this.collaborationService = collaborationService;
    }

    /**
     * 初始化 BiDashboardResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param widgetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      BiDashboardVersionMapper versionMapper,
                                      ObjectMapper objectMapper) {
        this(workspaceMapper, dashboardMapper, widgetMapper, versionMapper, objectMapper, null);
    }

    /**
     * 初始化 BiDashboardResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param widgetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiDashboardResourceService 流程中的校验、计算或对象转换。
     */
    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      BiDashboardVersionMapper versionMapper,
                                      ObjectMapper objectMapper,
                                      BiResourcePermissionGuard permissionGuard) {
        this(workspaceMapper, dashboardMapper, widgetMapper, versionMapper, objectMapper,
                permissionGuard, null, null);
    }

    /**
     * 初始化 BiDashboardResourceService 实例。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param widgetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param versionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionGuard permission guard 参数，用于 BiDashboardResourceService 流程中的校验、计算或对象转换。
     * @param publishApprovalService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      BiDashboardVersionMapper versionMapper,
                                      ObjectMapper objectMapper,
                                      BiResourcePermissionGuard permissionGuard,
                                      BiPublishApprovalService publishApprovalService) {
        this(workspaceMapper, dashboardMapper, widgetMapper, versionMapper, objectMapper,
                permissionGuard, publishApprovalService, null);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiDashboardResource> list(Long tenantId) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        return safeList(dashboardMapper.selectList(new LambdaQueryWrapper<BiDashboardDO>()
                        .eq(BiDashboardDO::getTenantId, scopedTenantId)
                        .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                        .ne(BiDashboardDO::getStatus, STATUS_ARCHIVED)
                        .orderByDesc(BiDashboardDO::getUpdatedAt)
                        .orderByAsc(BiDashboardDO::getDashboardKey)))
                .stream()
                .map(row -> new BiDashboardResource(toPreset(row, widgets(scopedTenantId, row.getId())),
                        row.getStatus(), value(row.getVersion(), 1), "PERSISTED"))
                .toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 get 流程生成的业务结果。
     */
    public BiDashboardResource get(Long tenantId, String dashboardKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        if (row == null) {
            return new BiDashboardResource(MarketingBiDashboardPresetRegistry.preset(dashboardKey),
                    "PRESET", 1, "PRESET");
        }
        return new BiDashboardResource(toPreset(row, widgets(scopedTenantId, row.getId())),
                row.getStatus(), value(row.getVersion(), 1), "PERSISTED");
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param preset preset 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiDashboardResource saveDraft(Long tenantId, String username, BiDashboardPreset preset) {
        return saveDraft(tenantId, username, null, preset);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param preset preset 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public BiDashboardResource saveDraft(Long tenantId, String username, String role, BiDashboardPreset preset) {
        return saveDraftInternal(tenantId, username, role, preset, null, false);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param preset preset 参数，用于 saveDraft 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回流程执行后的业务结果。
     */
    public BiDashboardResource saveDraft(Long tenantId,
                                         String username,
                                         String role,
                                         BiDashboardPreset preset,
                                         String lockToken) {
        return saveDraftInternal(tenantId, username, role, preset, lockToken, true);
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param preset preset 参数，用于 saveDraftInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 saveDraftInternal 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private BiDashboardResource saveDraftInternal(Long tenantId,
                                                  String username,
                                                  String role,
                                                  BiDashboardPreset preset,
                                                  String lockToken,
                                                  boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        validatePreset(preset);
        BiDashboardDO existing = find(scopedTenantId, workspaceId, preset.dashboardKey());
        requirePermission(scopedTenantId, workspaceId, "DASHBOARD", existing == null ? null : existing.getId(),
                username, role, BiPermissionService.ACTION_EDIT);
        requireEditLock(scopedTenantId, workspaceId, "DASHBOARD", preset.dashboardKey(), username, role,
                lockToken, enforceEditLock && existing != null);
        int version = existing == null ? 1 : value(existing.getVersion(), 1);
        BiDashboardDO row = new BiDashboardDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspaceId);
        row.setDashboardKey(preset.dashboardKey());
        row.setName(preset.title());
        row.setDescription(preset.description());
        row.setThemeJson(json(Map.of("datasetKey", preset.datasetKey(), "embedScopes", preset.embedScopes())));
        row.setFilterJson(json(Map.of("filters", preset.filters(), "interactions", preset.interactions(),
                "subscriptionChannels", preset.subscriptionChannels())));
        row.setStatus(STATUS_DRAFT);
        row.setVersion(version);
        row.setCreatedBy(username == null || username.isBlank() ? "system" : username);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        dashboardMapper.upsert(row);

        BiDashboardDO persisted = find(scopedTenantId, workspaceId, preset.dashboardKey());
        Long dashboardId = persisted == null ? row.getId() : persisted.getId();
        if (dashboardId == null) {
            throw new IllegalStateException("BI dashboard was not persisted: " + preset.dashboardKey());
        }
        widgetMapper.deleteByDashboard(scopedTenantId, dashboardId);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (BiDashboardWidget widget : preset.widgets()) {
            widgetMapper.insert(toWidget(scopedTenantId, dashboardId, widget));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new BiDashboardResource(preset, STATUS_DRAFT, version, "PERSISTED");
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiDashboardResource publish(Long tenantId, String dashboardKey) {
        return publish(tenantId, null, dashboardKey);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiDashboardResource publish(Long tenantId, String username, String dashboardKey) {
        return publish(tenantId, username, null, dashboardKey);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回流程执行后的业务结果。
     */
    public BiDashboardResource publish(Long tenantId, String username, String role, String dashboardKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null) {
            throw new IllegalArgumentException("BI dashboard not found: " + dashboardKey);
        }
        requirePermission(scopedTenantId, workspaceId, "DASHBOARD", row.getId(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                username, role, BiPermissionService.ACTION_PUBLISH);
        requirePublishApproval(scopedTenantId, workspaceId, "DASHBOARD", dashboardKey, row.getUpdatedAt(), role);
        dashboardMapper.publish(scopedTenantId, workspaceId, dashboardKey);
        BiDashboardDO published = find(scopedTenantId, workspaceId, dashboardKey);
        BiDashboardDO effective = published == null ? row : published;
        if (published == null) {
            effective.setStatus(STATUS_PUBLISHED);
            effective.setVersion(value(row.getVersion(), 1) + 1);
        }
        BiDashboardPreset preset = toPreset(effective, widgets(scopedTenantId, effective.getId()));
        insertVersionSnapshot(scopedTenantId, workspaceId, effective, preset, username);
        return new BiDashboardResource(preset, STATUS_PUBLISHED, value(effective.getVersion(), 1), "PERSISTED");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param sourceDashboardKey 业务键，用于在同一租户下定位资源。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 cloneResource 流程生成的业务结果。
     */
    public BiDashboardResource cloneResource(Long tenantId,
                                             String username,
                                             String sourceDashboardKey,
                                             BiDashboardCloneCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        String targetKey = resourceKey(command == null ? null : command.dashboardKey(), "dashboardKey");
        String sourceKey = required(sourceDashboardKey, "sourceDashboardKey");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (targetKey.equals(sourceKey)) {
            throw new IllegalArgumentException("target dashboard key must differ from source");
        }
        if (find(scopedTenantId, workspaceId, targetKey) != null) {
            throw new IllegalArgumentException("BI dashboard already exists: " + targetKey);
        }
        BiDashboardDO source = find(scopedTenantId, workspaceId, sourceKey);
        BiDashboardPreset sourcePreset = source == null
                ? MarketingBiDashboardPresetRegistry.preset(sourceKey)
                : toPreset(source, widgets(scopedTenantId, source.getId()));
        BiDashboardPreset cloned = new BiDashboardPreset(
                targetKey,
                textOr(command == null ? null : command.title(), sourcePreset.title() + " 副本"),
                textOr(command == null ? null : command.description(), sourcePreset.description()),
                sourcePreset.datasetKey(),
                sourcePreset.widgets(),
                sourcePreset.filters(),
                sourcePreset.interactions(),
                sourcePreset.subscriptionChannels(),
                sourcePreset.embedScopes());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return saveDraft(scopedTenantId, username, null, cloned);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 exportResource 流程生成的业务结果。
     */
    public BiDashboardExportPackage exportResource(Long tenantId, String username, String dashboardKey) {
        BiDashboardResource resource = get(tenantId, dashboardKey);
        if (!STATUS_PUBLISHED.equals(resource.status())) {
            throw new IllegalStateException("BI dashboard must be published before export: " + dashboardKey);
        }
        return new BiDashboardExportPackage(
                "DASHBOARD",
                1,
                resource.preset().dashboardKey(),
                resource.version(),
                resource.preset(),
                defaultUser(username),
                LocalDateTime.now());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 exportResourceFile 流程生成的业务结果。
     */
    public DashboardPackageFile exportResourceFile(Long tenantId, String username, String dashboardKey) {
        BiDashboardExportPackage packagePayload = exportResource(tenantId, username, dashboardKey);
        String filename = resourceKey(packagePayload.sourceDashboardKey(), "sourceDashboardKey")
                + "-v" + value(packagePayload.sourceVersion(), 1)
                + ".bi-dashboard.json";
        try {
            return new DashboardPackageFile(
                    filename,
                    "application/json",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(packagePayload));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize BI dashboard package", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 importResource 流程生成的业务结果。
     */
    public BiDashboardResource importResource(Long tenantId, String username, BiDashboardImportCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null || command.packagePayload() == null || command.packagePayload().preset() == null) {
            throw new IllegalArgumentException("dashboard import package is required");
        }
        BiDashboardExportPackage packagePayload = command.packagePayload();
        if (!"DASHBOARD".equals(packagePayload.resourceType())) {
            throw new IllegalArgumentException("unsupported BI package resource type: " + packagePayload.resourceType());
        }
        int schemaVersion = value(packagePayload.schemaVersion(), 1);
        if (schemaVersion > 1) {
            throw new IllegalArgumentException("unsupported BI dashboard package schema version: " + schemaVersion);
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardPreset source = packagePayload.preset();
        String targetKey = resourceKey(command.dashboardKey() == null || command.dashboardKey().isBlank()
                ? source.dashboardKey()
                : command.dashboardKey(), "dashboardKey");
        boolean overwrite = Boolean.TRUE.equals(command.overwrite());
        if (!overwrite && find(scopedTenantId, workspaceId, targetKey) != null) {
            throw new IllegalArgumentException("BI dashboard already exists: " + targetKey);
        }
        BiDashboardPreset imported = new BiDashboardPreset(
                targetKey,
                textOr(command.title(), source.title()),
                source.description(),
                source.datasetKey(),
                source.widgets(),
                source.filters(),
                source.interactions(),
                source.subscriptionChannels(),
                source.embedScopes());
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return saveDraft(scopedTenantId, username, null, imported);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param content content 参数，用于 importResourceFile 流程中的校验、计算或对象转换。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param title title 参数，用于 importResourceFile 流程中的校验、计算或对象转换。
     * @param overwrite overwrite 参数，用于 importResourceFile 流程中的校验、计算或对象转换。
     * @return 返回 importResourceFile 流程生成的业务结果。
     */
    public BiDashboardResource importResourceFile(Long tenantId,
                                                  String username,
                                                  byte[] content,
                                                  String dashboardKey,
                                                  String title,
                                                  boolean overwrite) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("dashboard import package file is required");
        }
        try {
            BiDashboardExportPackage packagePayload =
                    objectMapper.readValue(content, BiDashboardExportPackage.class);
            return importResource(tenantId, username,
                    new BiDashboardImportCommand(packagePayload, dashboardKey, title, overwrite));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid BI dashboard package file", e);
        }
    }

    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 archive 流程生成的业务结果。
     */
    public BiDashboardResource archive(Long tenantId, String dashboardKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        if (row == null) {
            throw new IllegalArgumentException("BI dashboard not found: " + dashboardKey);
        }
        dashboardMapper.archive(scopedTenantId, workspaceId, dashboardKey);
        BiDashboardDO archived = find(scopedTenantId, workspaceId, dashboardKey);
        if (archived == null) {
            row.setStatus(STATUS_ARCHIVED);
            return new BiDashboardResource(toPreset(row, widgets(scopedTenantId, row.getId())),
                    STATUS_ARCHIVED, value(row.getVersion(), 1), "PERSISTED");
        }
        return new BiDashboardResource(toPreset(archived, widgets(scopedTenantId, archived.getId())),
                STATUS_ARCHIVED, value(archived.getVersion(), 1), "PERSISTED");
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<BiDashboardVersionView> listVersions(Long tenantId, String dashboardKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiDashboardVersionDO>()
                        .eq(BiDashboardVersionDO::getTenantId, scopedTenantId)
                        .eq(BiDashboardVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiDashboardVersionDO::getDashboardId, row.getId())
                        .orderByDesc(BiDashboardVersionDO::getVersion)
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
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiDashboardResource restoreVersion(Long tenantId, String username, String dashboardKey, int version) {
        return restoreVersion(tenantId, username, null, dashboardKey, version);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiDashboardResource restoreVersion(Long tenantId, String username, String role, String dashboardKey, int version) {
        return restoreVersionInternal(tenantId, username, role, dashboardKey, version, null, false);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersion 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @return 返回 restoreVersion 流程生成的业务结果。
     */
    public BiDashboardResource restoreVersion(Long tenantId,
                                              String username,
                                              String role,
                                              String dashboardKey,
                                              int version,
                                              String lockToken) {
        return restoreVersionInternal(tenantId, username, role, dashboardKey, version, lockToken, true);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param username 操作人标识，用于审计和权限判断。
     * @param role 角色标识，用于权限校验和访问范围判断。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @param version version 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @param lockToken 令牌或锁标识，用于鉴权、幂等或并发控制。
     * @param enforceEditLock enforce edit lock 参数，用于 restoreVersionInternal 流程中的校验、计算或对象转换。
     * @return 返回 restoreVersionInternal 流程生成的业务结果。
     */
    private BiDashboardResource restoreVersionInternal(Long tenantId,
                                                       String username,
                                                       String role,
                                                       String dashboardKey,
                                                       int version,
                                                       String lockToken,
                                                       boolean enforceEditLock) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI dashboard not found: " + dashboardKey);
        }
        if (version <= 0) {
            throw new IllegalArgumentException("dashboard version must be positive");
        }
        if (versionMapper == null) {
            throw new IllegalStateException("BI dashboard version mapper is required");
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        BiDashboardVersionDO snapshot = versionMapper.selectOne(new LambdaQueryWrapper<BiDashboardVersionDO>()
                .eq(BiDashboardVersionDO::getTenantId, scopedTenantId)
                .eq(BiDashboardVersionDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardVersionDO::getDashboardId, row.getId())
                .eq(BiDashboardVersionDO::getVersion, version)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw new IllegalArgumentException("BI dashboard version not found: " + dashboardKey + " v" + version);
        }
        BiDashboardPreset preset = presetFromJson(snapshot.getPresetJson(), dashboardKey);
        if (enforceEditLock) {
            return saveDraft(scopedTenantId, username, role, preset, lockToken);
        }
        return saveDraft(scopedTenantId, username, role, preset);
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
                                        LocalDateTime resourceUpdatedAt,
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
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private BiDashboardDO find(Long tenantId, Long workspaceId, String dashboardKey) {
        return dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                .eq(BiDashboardDO::getTenantId, tenantId)
                .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardDO::getDashboardKey, required(dashboardKey, "dashboardKey"))
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
     * @param dashboardId 业务对象 ID，用于定位具体记录。
     * @return 返回 widgets 汇总后的集合、分页或映射视图。
     */
    private List<BiDashboardWidget> widgets(Long tenantId, Long dashboardId) {
        return safeList(widgetMapper.selectList(new LambdaQueryWrapper<BiDashboardWidgetDO>()
                        .eq(BiDashboardWidgetDO::getTenantId, tenantId)
                        .eq(BiDashboardWidgetDO::getDashboardId, dashboardId)))
                .stream()
                .sorted(Comparator.comparingInt(row -> grid(row).y() * 100 + grid(row).x()))
                .map(this::toWidget)
                .toList();
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param widgets widgets 参数，用于 toPreset 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDashboardPreset toPreset(BiDashboardDO row, List<BiDashboardWidget> widgets) {
        FilterState state = filterState(row.getFilterJson());
        ThemeState theme = themeState(row.getThemeJson());
        return new BiDashboardPreset(
                row.getDashboardKey(),
                row.getName(),
                row.getDescription(),
                theme.datasetKey(),
                widgets,
                state.filters(),
                state.interactions(),
                state.subscriptionChannels(),
                theme.embedScopes());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param dashboardId 业务对象 ID，用于定位具体记录。
     * @param widget widget 参数，用于 toWidget 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDashboardWidgetDO toWidget(Long tenantId, Long dashboardId, BiDashboardWidget widget) {
        BiDashboardWidgetDO row = new BiDashboardWidgetDO();
        row.setTenantId(tenantId);
        row.setDashboardId(dashboardId);
        row.setWidgetKey(required(widget.widgetKey(), "widgetKey"));
        row.setWidgetType(required(widget.chartType(), "chartType"));
        row.setTitle(required(widget.title(), "title"));
        row.setLayoutJson(json(new Grid(widget.gridX(), widget.gridY(), widget.gridW(), widget.gridH(),
                widget.stylePreset())));
        row.setQueryOverrideJson(json(Map.of("dimensions", widget.dimensions(), "metrics", widget.metrics())));
        row.setInteractionJson("{}");
        return row;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param dashboard dashboard 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param preset preset 参数，用于 insertVersionSnapshot 流程中的校验、计算或对象转换。
     * @param username 操作人标识，用于审计和权限判断。
     */
    private void insertVersionSnapshot(Long tenantId,
                                       Long workspaceId,
                                       BiDashboardDO dashboard,
                                       BiDashboardPreset preset,
                                       String username) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (versionMapper == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (dashboard.getId() == null) {
            throw new IllegalStateException("BI dashboard id is required for version snapshot: " + dashboard.getDashboardKey());
        }
        BiDashboardVersionDO row = new BiDashboardVersionDO();
        row.setTenantId(tenantId);
        row.setWorkspaceId(workspaceId);
        row.setDashboardId(dashboard.getId());
        row.setDashboardKey(dashboard.getDashboardKey());
        row.setVersion(value(dashboard.getVersion(), 1));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setStatus(STATUS_PUBLISHED);
        row.setPresetJson(json(preset));
        row.setPublishedBy(defaultUser(username));
        versionMapper.insert(row);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDashboardVersionView toVersionView(BiDashboardVersionDO row) {
        return new BiDashboardVersionView(
                row.getId(),
                row.getDashboardKey(),
                row.getVersion(),
                row.getStatus(),
                presetFromJson(row.getPresetJson(), row.getDashboardKey()),
                row.getPublishedBy(),
                row.getCreatedAt());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @param dashboardKey 业务键，用于在同一租户下定位资源。
     * @return 返回 presetFromJson 流程生成的业务结果。
     */
    private BiDashboardPreset presetFromJson(String json, String dashboardKey) {
        try {
            return objectMapper.readValue(json, BiDashboardPreset.class);
        } catch (Exception e) {
            return MarketingBiDashboardPresetRegistry.preset(dashboardKey);
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private BiDashboardWidget toWidget(BiDashboardWidgetDO row) {
        Grid grid = grid(row);
        QueryOverride query = query(row.getQueryOverrideJson());
        return new BiDashboardWidget(
                row.getWidgetKey(),
                row.getTitle(),
                row.getWidgetType(),
                query.dimensions(),
                query.metrics(),
                grid.x(),
                grid.y(),
                grid.w(),
                grid.h(),
                grid.stylePreset());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 grid 流程生成的业务结果。
     */
    private Grid grid(BiDashboardWidgetDO row) {
        try {
            Grid grid = objectMapper.readValue(row.getLayoutJson(), Grid.class);
            return new Grid(grid.x(), grid.y(), grid.w(), grid.h(), grid.stylePreset());
        } catch (Exception e) {
            return new Grid(0, 0, 6, 4, "default");
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回符合条件的数据列表或视图。
     */
    private QueryOverride query(String json) {
        try {
            QueryOverride query = objectMapper.readValue(json, QueryOverride.class);
            return new QueryOverride(safeList(query.dimensions()), safeList(query.metrics()));
        } catch (Exception e) {
            return new QueryOverride(List.of(), List.of());
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 filterState 流程生成的业务结果。
     */
    private FilterState filterState(String json) {
        try {
            FilterState state = objectMapper.readValue(json, FilterState.class);
            return new FilterState(safeList(state.filters()), safeList(state.interactions()),
                    safeList(state.subscriptionChannels()));
        } catch (Exception e) {
            return new FilterState(List.of(), List.of(), List.of());
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param json JSON 字符串，承载结构化配置或明细。
     * @return 返回 themeState 流程生成的业务结果。
     */
    private ThemeState themeState(String json) {
        try {
            ThemeState state = objectMapper.readValue(json, ThemeState.class);
            return new ThemeState(state.datasetKey() == null ? "canvas_daily_stats" : state.datasetKey(),
                    safeList(state.embedScopes()));
        } catch (Exception e) {
            return new ThemeState("canvas_daily_stats", List.of());
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
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI dashboard payload", e);
        }
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param preset preset 参数，用于 validatePreset 流程中的校验、计算或对象转换。
     */
    private void validatePreset(BiDashboardPreset preset) {
        if (preset == null) {
            throw new IllegalArgumentException("dashboard preset is required");
        }
        resourceKey(preset.dashboardKey(), "dashboardKey");
        required(preset.title(), "title");
        required(preset.datasetKey(), "datasetKey");
        if (preset.widgets().isEmpty()) {
            throw new IllegalArgumentException("dashboard widgets are required");
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 resource key 生成的文本或业务键。
     */
    private String resourceKey(String value, String field) {
        String key = required(value, field);
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException(field + " contains unsafe characters");
        }
        return key;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 textOr 流程中的校验、计算或对象转换。
     * @return 返回 text or 生成的文本或业务键。
     */
    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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
     * @param fallback fallback 参数，用于 value 流程中的校验、计算或对象转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param values values 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    /**
     * Grid 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record Grid(int x, int y, int w, int h, String stylePreset) {
        private Grid {
            stylePreset = stylePreset == null ? "default" : stylePreset;
        }
    }

    /**
     * QueryOverride 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record QueryOverride(List<String> dimensions, List<String> metrics) {
    }

    /**
     * FilterState 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record FilterState(
            List<BiDashboardFilter> filters,
            List<BiDashboardInteraction> interactions,
            List<String> subscriptionChannels) {
    }

    /**
     * ThemeState 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record ThemeState(String datasetKey, List<String> embedScopes) {
    }

    /**
     * DashboardPackageFile 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record DashboardPackageFile(
            String filename,
            String contentType,
            byte[] content) {
    }
}
