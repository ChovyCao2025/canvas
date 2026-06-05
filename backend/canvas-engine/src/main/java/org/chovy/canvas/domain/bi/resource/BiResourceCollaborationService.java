package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiResourceCommentDO;
import org.chovy.canvas.dal.dataobject.BiResourceLockDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceCommentMapper;
import org.chovy.canvas.dal.mapper.BiResourceLockMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class BiResourceCollaborationService {

    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final int DEFAULT_LOCK_TTL_SECONDS = 300;
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Pattern WIDGET_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Pattern LOCK_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiResourceCommentMapper commentMapper;
    private final BiResourceLockMapper lockMapper;
    private final Clock clock;

    public BiResourceCollaborationService(BiWorkspaceMapper workspaceMapper,
                                          BiDatasetMapper datasetMapper,
                                          BiDashboardMapper dashboardMapper,
                                          BiChartMapper chartMapper,
                                          BiPortalMapper portalMapper,
                                          BiResourceCommentMapper commentMapper,
                                          BiResourceLockMapper lockMapper) {
        this(workspaceMapper, datasetMapper, dashboardMapper, chartMapper, portalMapper,
                commentMapper, lockMapper, Clock.systemUTC());
    }

    BiResourceCollaborationService(BiWorkspaceMapper workspaceMapper,
                                   BiDatasetMapper datasetMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   BiPortalMapper portalMapper,
                                   BiResourceCommentMapper commentMapper,
                                   BiResourceLockMapper lockMapper,
                                   Clock clock) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.commentMapper = commentMapper;
        this.lockMapper = lockMapper;
        this.clock = clock;
    }

    public BiResourceCommentView addComment(Long tenantId, String username, BiResourceCommentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource comment command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        assertResourceExists(scopedTenantId, workspace.getId(), resourceType, resourceKey);

        BiResourceCommentDO row = new BiResourceCommentDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspace.getId());
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setWidgetKey(widgetKey(command.widgetKey()));
        row.setCommentText(commentText(command.commentText()));
        row.setCreatedBy(currentUser);
        row.setCreatedAt(now());
        commentMapper.insert(row);
        return toCommentView(row);
    }

    public List<BiResourceCommentView> listComments(Long tenantId, String resourceType, String resourceKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String type = normalizeResourceType(resourceType);
        String key = resourceKey(resourceKey);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        LambdaQueryWrapper<BiResourceCommentDO> query = new LambdaQueryWrapper<BiResourceCommentDO>()
                .eq(BiResourceCommentDO::getTenantId, scopedTenantId)
                .eq(BiResourceCommentDO::getWorkspaceId, workspace.getId())
                .eq(BiResourceCommentDO::getResourceType, type)
                .eq(BiResourceCommentDO::getResourceKey, key)
                .isNull(BiResourceCommentDO::getDeletedAt)
                .orderByAsc(BiResourceCommentDO::getCreatedAt)
                .orderByAsc(BiResourceCommentDO::getId);
        return safeList(commentMapper.selectList(query)).stream()
                .map(this::toCommentView)
                .toList();
    }

    public void deleteComment(Long tenantId, String username, Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("commentId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        commentMapper.softDelete(scopedTenantId, workspace.getId(), commentId, currentUser, now());
    }

    public BiResourceLockView acquireLock(Long tenantId, String username, BiResourceLockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource lock command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        assertResourceExists(scopedTenantId, workspace.getId(), resourceType, resourceKey);

        LocalDateTime lockedAt = now();
        BiResourceLockDO row = new BiResourceLockDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspace.getId());
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setLockToken(lockToken(command.lockToken(), false));
        row.setLockedBy(currentUser);
        row.setLockedAt(lockedAt);
        row.setExpiresAt(lockedAt.plusSeconds(lockTtlSeconds(command.ttlSeconds())));
        int changed = lockMapper.acquire(row);
        if (changed <= 0) {
            BiResourceLockDO current = lockMapper.selectCurrent(scopedTenantId, workspace.getId(), resourceType, resourceKey);
            if (current != null && current.getExpiresAt() != null && current.getExpiresAt().isAfter(lockedAt)) {
                throw new IllegalStateException("BI resource is locked by " + current.getLockedBy());
            }
            throw new IllegalStateException("BI resource lock could not be acquired");
        }
        return toLockView(row, true);
    }

    public BiResourceLockView currentLock(Long tenantId, String resourceType, String resourceKey) {
        Long scopedTenantId = normalizeTenant(tenantId);
        String type = normalizeResourceType(resourceType);
        String key = resourceKey(resourceKey);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        BiResourceLockDO current = lockMapper.selectCurrent(scopedTenantId, workspace.getId(), type, key);
        if (current == null) {
            return null;
        }
        return toLockView(current, current.getExpiresAt() != null && current.getExpiresAt().isAfter(now()));
    }

    public BiResourceLockView requireCurrentLock(Long tenantId,
                                                 Long workspaceId,
                                                 String resourceType,
                                                 String resourceKey,
                                                 String username,
                                                 String lockToken) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String type = normalizeResourceType(resourceType);
        String key = resourceKey(resourceKey);
        String currentUser = username(username);
        String token = lockToken(lockToken, true);
        BiResourceLockDO current = lockMapper.selectCurrent(scopedTenantId, workspaceId, type, key);
        if (current == null || current.getExpiresAt() == null || !current.getExpiresAt().isAfter(now())) {
            throw new BiResourceLockRequiredException(
                    "active BI resource lock is required for " + type + "/" + key);
        }
        if (!currentUser.equals(current.getLockedBy())) {
            throw new BiResourceLockRequiredException("BI resource is locked by " + current.getLockedBy());
        }
        if (!token.equals(current.getLockToken())) {
            throw new BiResourceLockRequiredException("lock token does not match for " + type + "/" + key);
        }
        return toLockView(current, true);
    }

    public void releaseLock(Long tenantId, String username, BiResourceLockCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI resource lock command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        String token = lockToken(command.lockToken(), true);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        lockMapper.release(scopedTenantId, workspace.getId(), resourceType, resourceKey, token, currentUser);
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

    private BiResourceCommentView toCommentView(BiResourceCommentDO row) {
        return new BiResourceCommentView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceKey(),
                row.getWidgetKey(),
                row.getCommentText(),
                row.getCreatedBy(),
                row.getCreatedAt(),
                row.getDeletedAt());
    }

    private BiResourceLockView toLockView(BiResourceLockDO row, boolean locked) {
        return new BiResourceLockView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceKey(),
                row.getLockToken(),
                row.getLockedBy(),
                row.getLockedAt(),
                row.getExpiresAt(),
                locked);
    }

    private String normalizeResourceType(String resourceType) {
        String value = required(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if ("DATASET".equals(value) || "DASHBOARD".equals(value)
                || "CHART".equals(value) || "PORTAL".equals(value)) {
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

    private String widgetKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = value.trim();
        if (!WIDGET_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("widgetKey contains unsafe characters");
        }
        return key;
    }

    private String commentText(String value) {
        String text = required(value, "commentText");
        if (text.length() > 4000) {
            throw new IllegalArgumentException("commentText is too long");
        }
        return text;
    }

    private String lockToken(String value, boolean required) {
        String token = value == null ? "" : value.trim();
        if (token.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("lockToken is required");
            }
            return UUID.randomUUID().toString();
        }
        if (!LOCK_TOKEN.matcher(token).matches()) {
            throw new IllegalArgumentException("lockToken contains unsafe characters");
        }
        return token;
    }

    private int lockTtlSeconds(Integer value) {
        int ttl = value == null ? DEFAULT_LOCK_TTL_SECONDS : value;
        if (ttl < 30 || ttl > 3600) {
            throw new IllegalArgumentException("ttlSeconds must be between 30 and 3600");
        }
        return ttl;
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

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    public static class BiResourceLockRequiredException extends IllegalStateException {
        public BiResourceLockRequiredException(String message) {
            super(message);
        }
    }
}
