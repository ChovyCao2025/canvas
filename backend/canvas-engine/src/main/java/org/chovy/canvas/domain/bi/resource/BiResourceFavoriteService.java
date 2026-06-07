package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiResourceFavoriteDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceFavoriteMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class BiResourceFavoriteService {

    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiBigScreenMapper bigScreenMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiResourceFavoriteMapper favoriteMapper;

    public BiResourceFavoriteService(BiWorkspaceMapper workspaceMapper,
                                     BiDatasetMapper datasetMapper,
                                     BiDashboardMapper dashboardMapper,
                                     BiChartMapper chartMapper,
                                     BiPortalMapper portalMapper,
                                     BiBigScreenMapper bigScreenMapper,
                                     BiSpreadsheetMapper spreadsheetMapper,
                                     BiResourceFavoriteMapper favoriteMapper) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.bigScreenMapper = bigScreenMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public BiResourceFavoriteView favorite(Long tenantId, String username, BiResourceFavoriteCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource favorite command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        if (!Boolean.FALSE.equals(command.favorite())) {
            assertResourceExists(scopedTenantId, workspace.getId(), resourceType, resourceKey);

            BiResourceFavoriteDO row = new BiResourceFavoriteDO();
            row.setTenantId(scopedTenantId);
            row.setWorkspaceId(workspace.getId());
            row.setResourceType(resourceType);
            row.setResourceKey(resourceKey);
            row.setUsername(currentUser);
            row.setCreatedAt(LocalDateTime.now());
            favoriteMapper.upsert(row);
            return toView(row, true);
        }
        favoriteMapper.deleteFavorite(scopedTenantId, workspace.getId(), resourceType, resourceKey, currentUser);
        BiResourceFavoriteDO row = new BiResourceFavoriteDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspace.getId());
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setUsername(currentUser);
        row.setCreatedAt(LocalDateTime.now());
        return toView(row, false);
    }

    public List<BiResourceFavoriteView> list(Long tenantId, String username, String resourceType) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        LambdaQueryWrapper<BiResourceFavoriteDO> query = new LambdaQueryWrapper<BiResourceFavoriteDO>()
                .eq(BiResourceFavoriteDO::getTenantId, scopedTenantId)
                .eq(BiResourceFavoriteDO::getWorkspaceId, workspace.getId())
                .eq(BiResourceFavoriteDO::getUsername, currentUser)
                .orderByAsc(BiResourceFavoriteDO::getResourceType)
                .orderByAsc(BiResourceFavoriteDO::getResourceKey);
        if (resourceType != null && !resourceType.isBlank()) {
            query.eq(BiResourceFavoriteDO::getResourceType, normalizeResourceType(resourceType));
        }
        return safeList(favoriteMapper.selectList(query)).stream()
                .map(row -> toView(row, true))
                .toList();
    }

    public void unfavorite(Long tenantId, String username, String resourceType, String resourceKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        String type = normalizeResourceType(resourceType);
        String key = resourceKey(resourceKey);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        favoriteMapper.deleteFavorite(scopedTenantId, workspace.getId(), type, key, currentUser);
    }

    private void assertResourceExists(Long tenantId, Long workspaceId, String resourceType, String resourceKey) {
        Object row = switch (resourceType) {
            case "DATASET" -> datasetMapper.selectOne(new LambdaQueryWrapper<BiDatasetDO>()
                    .eq(BiDatasetDO::getTenantId, tenantId)
                    .eq(BiDatasetDO::getWorkspaceId, workspaceId)
                    .eq(BiDatasetDO::getDatasetKey, resourceKey)
                    .last("LIMIT 1"));
            case "DASHBOARD" -> dashboardMapper.selectOne(new LambdaQueryWrapper<BiDashboardDO>()
                    .eq(BiDashboardDO::getTenantId, tenantId)
                    .eq(BiDashboardDO::getWorkspaceId, workspaceId)
                    .eq(BiDashboardDO::getDashboardKey, resourceKey)
                    .last("LIMIT 1"));
            case "CHART" -> chartMapper.selectOne(new LambdaQueryWrapper<BiChartDO>()
                    .eq(BiChartDO::getTenantId, tenantId)
                    .eq(BiChartDO::getWorkspaceId, workspaceId)
                    .eq(BiChartDO::getChartKey, resourceKey)
                    .last("LIMIT 1"));
            case "PORTAL" -> portalMapper.selectOne(new LambdaQueryWrapper<BiPortalDO>()
                    .eq(BiPortalDO::getTenantId, tenantId)
                    .eq(BiPortalDO::getWorkspaceId, workspaceId)
                    .eq(BiPortalDO::getPortalKey, resourceKey)
                    .last("LIMIT 1"));
            case "BIG_SCREEN" -> bigScreenMapper.selectOne(new LambdaQueryWrapper<BiBigScreenDO>()
                    .eq(BiBigScreenDO::getTenantId, tenantId)
                    .eq(BiBigScreenDO::getWorkspaceId, workspaceId)
                    .eq(BiBigScreenDO::getScreenKey, resourceKey)
                    .last("LIMIT 1"));
            case "SPREADSHEET" -> spreadsheetMapper.selectOne(new LambdaQueryWrapper<BiSpreadsheetDO>()
                    .eq(BiSpreadsheetDO::getTenantId, tenantId)
                    .eq(BiSpreadsheetDO::getWorkspaceId, workspaceId)
                    .eq(BiSpreadsheetDO::getSpreadsheetKey, resourceKey)
                    .last("LIMIT 1"));
            default -> throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
        };
        if (row == null) {
            throw new IllegalArgumentException("BI resource not found: " + resourceType + "/" + resourceKey);
        }
        if (STATUS_ARCHIVED.equalsIgnoreCase(resourceStatus(row))) {
            throw new IllegalArgumentException("BI resource is archived: " + resourceType + "/" + resourceKey);
        }
    }

    private String resourceStatus(Object row) {
        return switch (row) {
            case BiDatasetDO dataset -> dataset.getStatus();
            case BiDashboardDO dashboard -> dashboard.getStatus();
            case BiChartDO chart -> chart.getStatus();
            case BiPortalDO portal -> portal.getStatus();
            case BiBigScreenDO screen -> screen.getStatus();
            case BiSpreadsheetDO spreadsheet -> spreadsheet.getStatus();
            default -> null;
        };
    }

    private BiWorkspaceDO defaultWorkspace(Long tenantId) {
        LambdaQueryWrapper<BiWorkspaceDO> query = new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, DEFAULT_WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1");
        query.getParamNameValuePairs().put("workspaceKey", DEFAULT_WORKSPACE_KEY);
        BiWorkspaceDO workspace = workspaceMapper.selectOne(query);
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalArgumentException("BI workspace not found");
        }
        return workspace;
    }

    private BiResourceFavoriteView toView(BiResourceFavoriteDO row, boolean favorite) {
        return new BiResourceFavoriteView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceKey(),
                row.getUsername(),
                favorite,
                row.getCreatedAt());
    }

    private String normalizeResourceType(String resourceType) {
        String value = required(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if ("DATASET".equals(value) || "DASHBOARD".equals(value)
                || "CHART".equals(value) || "PORTAL".equals(value)
                || "BIG_SCREEN".equals(value) || "SPREADSHEET".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
    }

    private String resourceKey(String value) {
        String key = required(value, "resourceKey");
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("resourceKey contains unsafe characters");
        }
        return key;
    }

    private String username(String value) {
        String user = value == null || value.isBlank() ? "system" : value.trim();
        if (user.length() > 128) {
            throw new IllegalArgumentException("username is too long");
        }
        return user;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }
}
