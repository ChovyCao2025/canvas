package org.chovy.canvas.domain.bi.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiPermissionRequestDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiPermissionRequestMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BiPermissionRequestService {

    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");
    private static final Set<String> RESOURCE_TYPES = Set.of(
            "DATASET",
            "DASHBOARD",
            "CHART",
            "PORTAL",
            "BIG_SCREEN",
            "SPREADSHEET",
            "DATASOURCE");
    private static final Set<String> ACTION_KEYS = Set.of(
            "VIEW",
            "USE",
            "QUERY",
            "EDIT",
            "PUBLISH",
            "EXPORT",
            "EMBED",
            "SUBSCRIBE");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiPermissionRequestMapper requestMapper;
    private final BiPermissionAdminService permissionAdminService;
    private final Clock clock;

    @Autowired
    public BiPermissionRequestService(BiWorkspaceMapper workspaceMapper,
                                      BiPermissionRequestMapper requestMapper,
                                      BiPermissionAdminService permissionAdminService) {
        this(workspaceMapper, requestMapper, permissionAdminService, Clock.systemUTC());
    }

    BiPermissionRequestService(BiWorkspaceMapper workspaceMapper,
                               BiPermissionRequestMapper requestMapper,
                               BiPermissionAdminService permissionAdminService,
                               Clock clock) {
        this.workspaceMapper = workspaceMapper;
        this.requestMapper = requestMapper;
        this.permissionAdminService = permissionAdminService;
        this.clock = clock;
    }

    public BiPermissionRequestView requestPermission(Long tenantId,
                                                     String username,
                                                     BiPermissionRequestCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("permission request command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        BiPermissionRequestDO row = new BiPermissionRequestDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspace.getId());
        row.setResourceType(normalizeResourceType(command.resourceType()));
        row.setResourceKey(resourceKey(command.resourceKey()));
        row.setRequestedAction(normalizeAction(command.requestedAction()));
        row.setRequestedBy(username(username));
        row.setRequestedAt(now());
        row.setReason(optionalText(command.reason(), "reason"));
        row.setStatus(STATUS_PENDING);
        requestMapper.insert(row);
        return toView(row);
    }

    public List<BiPermissionRequestView> listPermissionRequests(Long tenantId,
                                                                String resourceType,
                                                                String resourceKey,
                                                                String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        LambdaQueryWrapper<BiPermissionRequestDO> query = new LambdaQueryWrapper<BiPermissionRequestDO>()
                .eq(BiPermissionRequestDO::getTenantId, scopedTenantId)
                .eq(BiPermissionRequestDO::getWorkspaceId, workspace.getId())
                .orderByDesc(BiPermissionRequestDO::getRequestedAt)
                .orderByDesc(BiPermissionRequestDO::getId);
        if (hasText(resourceType)) {
            query.eq(BiPermissionRequestDO::getResourceType, normalizeResourceType(resourceType));
        }
        if (hasText(resourceKey)) {
            query.eq(BiPermissionRequestDO::getResourceKey, resourceKey(resourceKey));
        }
        if (hasText(status)) {
            query.eq(BiPermissionRequestDO::getStatus, normalizeStatus(status));
        }
        return safeList(requestMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    public BiPermissionRequestView reviewPermissionRequest(Long tenantId,
                                                           String username,
                                                           BiPermissionRequestReviewCommand command) {
        if (command == null || command.requestId() == null || command.requestId() <= 0) {
            throw new IllegalArgumentException("requestId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        BiPermissionRequestDO row = requestMapper.selectOne(new LambdaQueryWrapper<BiPermissionRequestDO>()
                .eq(BiPermissionRequestDO::getTenantId, scopedTenantId)
                .eq(BiPermissionRequestDO::getWorkspaceId, workspace.getId())
                .eq(BiPermissionRequestDO::getId, command.requestId())
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("BI permission request not found: " + command.requestId());
        }
        if (!STATUS_PENDING.equalsIgnoreCase(row.getStatus())) {
            throw new IllegalStateException("BI permission request is not pending: " + command.requestId());
        }
        String reviewStatus = reviewStatus(command.status());
        row.setStatus(reviewStatus);
        row.setReviewedBy(username(username));
        row.setReviewedAt(now());
        row.setReviewComment(optionalText(command.reviewComment(), "reviewComment"));
        if (STATUS_APPROVED.equals(reviewStatus)) {
            BiResourcePermissionView grant = permissionAdminService.upsertResourcePermission(
                    scopedTenantId,
                    username(username),
                    new BiResourcePermissionCommand(
                            row.getResourceType(),
                            row.getResourceKey(),
                            null,
                            "USER",
                            row.getRequestedBy(),
                            row.getRequestedAction(),
                            "ALLOW"));
            row.setGrantedPermissionId(grant == null ? null : grant.id());
        }
        requestMapper.updateById(row);
        return toView(row);
    }

    private BiWorkspaceDO defaultWorkspace(Long tenantId) {
        BiWorkspaceDO workspace = workspaceMapper.selectOne(new LambdaQueryWrapper<BiWorkspaceDO>()
                .in(BiWorkspaceDO::getTenantId, List.of(tenantId, 0L))
                .eq(BiWorkspaceDO::getWorkspaceKey, DEFAULT_WORKSPACE_KEY)
                .orderByDesc(BiWorkspaceDO::getTenantId)
                .last("LIMIT 1"));
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalArgumentException("BI workspace not found");
        }
        return workspace;
    }

    private BiPermissionRequestView toView(BiPermissionRequestDO row) {
        return new BiPermissionRequestView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceKey(),
                row.getRequestedAction(),
                row.getRequestedBy(),
                row.getRequestedAt(),
                row.getReason(),
                row.getStatus(),
                row.getReviewedBy(),
                row.getReviewedAt(),
                row.getReviewComment(),
                row.getGrantedPermissionId());
    }

    private String normalizeResourceType(String resourceType) {
        String value = required(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if (!RESOURCE_TYPES.contains(value)) {
            throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
        }
        return value;
    }

    private String normalizeAction(String actionKey) {
        String value = required(actionKey, "requestedAction").toUpperCase(Locale.ROOT);
        if (!ACTION_KEYS.contains(value)) {
            throw new IllegalArgumentException("unsupported BI permission action: " + actionKey);
        }
        return value;
    }

    private String normalizeStatus(String status) {
        String value = required(status, "status").toUpperCase(Locale.ROOT);
        if (STATUS_PENDING.equals(value) || STATUS_APPROVED.equals(value) || STATUS_REJECTED.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI permission request status: " + status);
    }

    private String reviewStatus(String status) {
        String value = normalizeStatus(status);
        if (STATUS_PENDING.equals(value)) {
            throw new IllegalArgumentException("review status must be APPROVED or REJECTED");
        }
        return value;
    }

    private String resourceKey(String value) {
        String key = required(value, "resourceKey");
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("resourceKey contains unsafe characters");
        }
        return key;
    }

    private String username(String value) {
        String user = hasText(value) ? value.trim() : "system";
        if (user.length() > 128) {
            throw new IllegalArgumentException("username is too long");
        }
        return user;
    }

    private String optionalText(String value, String field) {
        if (!hasText(value)) {
            return null;
        }
        String text = value.trim();
        if (text.length() > 512) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return text;
    }

    private String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}
