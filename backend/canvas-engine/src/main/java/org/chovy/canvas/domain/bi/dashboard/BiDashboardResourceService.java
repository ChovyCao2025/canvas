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

    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      ObjectMapper objectMapper) {
        this(workspaceMapper, dashboardMapper, widgetMapper, null, objectMapper, null);
    }

    @Autowired
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

    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      BiDashboardVersionMapper versionMapper,
                                      ObjectMapper objectMapper) {
        this(workspaceMapper, dashboardMapper, widgetMapper, versionMapper, objectMapper, null);
    }

    public BiDashboardResourceService(BiWorkspaceMapper workspaceMapper,
                                      BiDashboardMapper dashboardMapper,
                                      BiDashboardWidgetMapper widgetMapper,
                                      BiDashboardVersionMapper versionMapper,
                                      ObjectMapper objectMapper,
                                      BiResourcePermissionGuard permissionGuard) {
        this(workspaceMapper, dashboardMapper, widgetMapper, versionMapper, objectMapper,
                permissionGuard, null, null);
    }

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

    public BiDashboardResource saveDraft(Long tenantId, String username, BiDashboardPreset preset) {
        return saveDraft(tenantId, username, null, preset);
    }

    public BiDashboardResource saveDraft(Long tenantId, String username, String role, BiDashboardPreset preset) {
        return saveDraftInternal(tenantId, username, role, preset, null, false);
    }

    public BiDashboardResource saveDraft(Long tenantId,
                                         String username,
                                         String role,
                                         BiDashboardPreset preset,
                                         String lockToken) {
        return saveDraftInternal(tenantId, username, role, preset, lockToken, true);
    }

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
        dashboardMapper.upsert(row);

        BiDashboardDO persisted = find(scopedTenantId, workspaceId, preset.dashboardKey());
        Long dashboardId = persisted == null ? row.getId() : persisted.getId();
        if (dashboardId == null) {
            throw new IllegalStateException("BI dashboard was not persisted: " + preset.dashboardKey());
        }
        widgetMapper.deleteByDashboard(scopedTenantId, dashboardId);
        for (BiDashboardWidget widget : preset.widgets()) {
            widgetMapper.insert(toWidget(scopedTenantId, dashboardId, widget));
        }
        return new BiDashboardResource(preset, STATUS_DRAFT, version, "PERSISTED");
    }

    public BiDashboardResource publish(Long tenantId, String dashboardKey) {
        return publish(tenantId, null, dashboardKey);
    }

    public BiDashboardResource publish(Long tenantId, String username, String dashboardKey) {
        return publish(tenantId, username, null, dashboardKey);
    }

    public BiDashboardResource publish(Long tenantId, String username, String role, String dashboardKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        if (row == null) {
            throw new IllegalArgumentException("BI dashboard not found: " + dashboardKey);
        }
        requirePermission(scopedTenantId, workspaceId, "DASHBOARD", row.getId(),
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

    public BiDashboardResource cloneResource(Long tenantId,
                                             String username,
                                             String sourceDashboardKey,
                                             BiDashboardCloneCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        String targetKey = resourceKey(command == null ? null : command.dashboardKey(), "dashboardKey");
        String sourceKey = required(sourceDashboardKey, "sourceDashboardKey");
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
        return saveDraft(scopedTenantId, username, null, cloned);
    }

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

    public BiDashboardResource importResource(Long tenantId, String username, BiDashboardImportCommand command) {
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
        return saveDraft(scopedTenantId, username, null, imported);
    }

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

    public List<BiDashboardVersionView> listVersions(Long tenantId, String dashboardKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long workspaceId = workspaceId(scopedTenantId);
        BiDashboardDO row = find(scopedTenantId, workspaceId, dashboardKey);
        if (row == null || row.getId() == null || versionMapper == null) {
            return List.of();
        }
        int capped = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return safeList(versionMapper.selectList(new LambdaQueryWrapper<BiDashboardVersionDO>()
                        .eq(BiDashboardVersionDO::getTenantId, scopedTenantId)
                        .eq(BiDashboardVersionDO::getWorkspaceId, workspaceId)
                        .eq(BiDashboardVersionDO::getDashboardId, row.getId())
                        .orderByDesc(BiDashboardVersionDO::getVersion)
                        .last("LIMIT " + capped)))
                .stream()
                .map(this::toVersionView)
                .toList();
    }

    public BiDashboardResource restoreVersion(Long tenantId, String username, String dashboardKey, int version) {
        return restoreVersion(tenantId, username, null, dashboardKey, version);
    }

    public BiDashboardResource restoreVersion(Long tenantId, String username, String role, String dashboardKey, int version) {
        return restoreVersionInternal(tenantId, username, role, dashboardKey, version, null, false);
    }

    public BiDashboardResource restoreVersion(Long tenantId,
                                              String username,
                                              String role,
                                              String dashboardKey,
                                              int version,
                                              String lockToken) {
        return restoreVersionInternal(tenantId, username, role, dashboardKey, version, lockToken, true);
    }

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
        if (row == null || row.getId() == null) {
            throw new IllegalArgumentException("BI dashboard not found: " + dashboardKey);
        }
        if (version <= 0) {
            throw new IllegalArgumentException("dashboard version must be positive");
        }
        if (versionMapper == null) {
            throw new IllegalStateException("BI dashboard version mapper is required");
        }
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

    private void requirePublishApproval(Long tenantId,
                                        Long workspaceId,
                                        String resourceType,
                                        String resourceKey,
                                        LocalDateTime resourceUpdatedAt,
                                        String role) {
        if (publishApprovalService != null && !canBypassPublishApproval(role)) {
            publishApprovalService.requireApprovedApproval(
                    tenantId, workspaceId, resourceType, resourceKey, resourceUpdatedAt);
        }
    }

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

    private boolean canBypassEditLock(String role) {
        return canBypassPublishApproval(role);
    }

    private boolean canBypassPublishApproval(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return RoleNames.ADMIN.equals(normalized)
                || RoleNames.SUPER_ADMIN.equals(normalized)
                || RoleNames.TENANT_ADMIN.equals(normalized);
    }

    private BiDashboardDO find(Long tenantId, Long workspaceId, String dashboardKey) {
        return dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                .eq(BiDashboardDO::getTenantId, tenantId)
                .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                .eq(BiDashboardDO::getDashboardKey, required(dashboardKey, "dashboardKey"))
                .last("LIMIT 1"));
    }

    private Long workspaceId(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1"));
        return workspace == null || workspace.getId() == null ? 0L : workspace.getId();
    }

    private List<BiDashboardWidget> widgets(Long tenantId, Long dashboardId) {
        return safeList(widgetMapper.selectList(new LambdaQueryWrapper<BiDashboardWidgetDO>()
                        .eq(BiDashboardWidgetDO::getTenantId, tenantId)
                        .eq(BiDashboardWidgetDO::getDashboardId, dashboardId)))
                .stream()
                .sorted(Comparator.comparingInt(row -> grid(row).y() * 100 + grid(row).x()))
                .map(this::toWidget)
                .toList();
    }

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

    private void insertVersionSnapshot(Long tenantId,
                                       Long workspaceId,
                                       BiDashboardDO dashboard,
                                       BiDashboardPreset preset,
                                       String username) {
        if (versionMapper == null) {
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
        row.setStatus(STATUS_PUBLISHED);
        row.setPresetJson(json(preset));
        row.setPublishedBy(defaultUser(username));
        versionMapper.insert(row);
    }

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

    private BiDashboardPreset presetFromJson(String json, String dashboardKey) {
        try {
            return objectMapper.readValue(json, BiDashboardPreset.class);
        } catch (Exception e) {
            return MarketingBiDashboardPresetRegistry.preset(dashboardKey);
        }
    }

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

    private Grid grid(BiDashboardWidgetDO row) {
        try {
            Grid grid = objectMapper.readValue(row.getLayoutJson(), Grid.class);
            return new Grid(grid.x(), grid.y(), grid.w(), grid.h(), grid.stylePreset());
        } catch (Exception e) {
            return new Grid(0, 0, 6, 4, "default");
        }
    }

    private QueryOverride query(String json) {
        try {
            QueryOverride query = objectMapper.readValue(json, QueryOverride.class);
            return new QueryOverride(safeList(query.dimensions()), safeList(query.metrics()));
        } catch (Exception e) {
            return new QueryOverride(List.of(), List.of());
        }
    }

    private FilterState filterState(String json) {
        try {
            FilterState state = objectMapper.readValue(json, FilterState.class);
            return new FilterState(safeList(state.filters()), safeList(state.interactions()),
                    safeList(state.subscriptionChannels()));
        } catch (Exception e) {
            return new FilterState(List.of(), List.of(), List.of());
        }
    }

    private ThemeState themeState(String json) {
        try {
            ThemeState state = objectMapper.readValue(json, ThemeState.class);
            return new ThemeState(state.datasetKey() == null ? "canvas_daily_stats" : state.datasetKey(),
                    safeList(state.embedScopes()));
        } catch (Exception e) {
            return new ThemeState("canvas_daily_stats", List.of());
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("invalid BI dashboard payload", e);
        }
    }

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

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String resourceKey(String value, String field) {
        String key = required(value, field);
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException(field + " contains unsafe characters");
        }
        return key;
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String defaultUser(String username) {
        return username == null || username.isBlank() ? "system" : username;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record Grid(int x, int y, int w, int h, String stylePreset) {
        private Grid {
            stylePreset = stylePreset == null ? "default" : stylePreset;
        }
    }

    private record QueryOverride(List<String> dimensions, List<String> metrics) {
    }

    private record FilterState(
            List<BiDashboardFilter> filters,
            List<BiDashboardInteraction> interactions,
            List<String> subscriptionChannels) {
    }

    private record ThemeState(String datasetKey, List<String> embedScopes) {
    }

    public record DashboardPackageFile(
            String filename,
            String contentType,
            byte[] content) {
    }
}
