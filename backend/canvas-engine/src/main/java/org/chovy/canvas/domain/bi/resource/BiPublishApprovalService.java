package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiPublishApprovalDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiPublishApprovalMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class BiPublishApprovalService {

    private static final String DEFAULT_WORKSPACE_KEY = "marketing_canvas";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final Pattern RESOURCE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");

    private final BiWorkspaceMapper workspaceMapper;
    private final BiDatasetMapper datasetMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiChartMapper chartMapper;
    private final BiPortalMapper portalMapper;
    private final BiBigScreenMapper bigScreenMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiPublishApprovalMapper approvalMapper;
    private final Clock clock;

    @Autowired
    public BiPublishApprovalService(BiWorkspaceMapper workspaceMapper,
                                    BiDatasetMapper datasetMapper,
                                    BiDashboardMapper dashboardMapper,
                                    BiChartMapper chartMapper,
                                    BiPortalMapper portalMapper,
                                    BiBigScreenMapper bigScreenMapper,
                                    BiSpreadsheetMapper spreadsheetMapper,
                                    BiPublishApprovalMapper approvalMapper) {
        this(workspaceMapper, datasetMapper, dashboardMapper, chartMapper, portalMapper,
                bigScreenMapper, spreadsheetMapper,
                approvalMapper, Clock.systemUTC());
    }

    BiPublishApprovalService(BiWorkspaceMapper workspaceMapper,
                             BiDatasetMapper datasetMapper,
                             BiDashboardMapper dashboardMapper,
                             BiChartMapper chartMapper,
                             BiPortalMapper portalMapper,
                             BiBigScreenMapper bigScreenMapper,
                             BiSpreadsheetMapper spreadsheetMapper,
                             BiPublishApprovalMapper approvalMapper,
                             Clock clock) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.bigScreenMapper = bigScreenMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.approvalMapper = approvalMapper;
        this.clock = clock;
    }

    public BiPublishApprovalView requestApproval(Long tenantId,
                                                 String username,
                                                 BiPublishApprovalRequestCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("BI publish approval request is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        assertResourceExists(scopedTenantId, workspace.getId(), resourceType, resourceKey);

        BiPublishApprovalDO row = new BiPublishApprovalDO();
        row.setTenantId(scopedTenantId);
        row.setWorkspaceId(workspace.getId());
        row.setResourceType(resourceType);
        row.setResourceKey(resourceKey);
        row.setStatus(STATUS_PENDING);
        row.setReason(optionalText(command.reason(), "reason"));
        row.setRequestedBy(username(username));
        row.setRequestedAt(now());
        approvalMapper.insert(row);
        return toView(row);
    }

    public BiPublishApprovalView reviewApproval(Long tenantId,
                                                String username,
                                                BiPublishApprovalReviewCommand command) {
        if (command == null || command.approvalId() == null || command.approvalId() <= 0) {
            throw new IllegalArgumentException("approvalId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        BiPublishApprovalDO row = approvalMapper.selectOne(new LambdaQueryWrapper<BiPublishApprovalDO>()
                .eq(BiPublishApprovalDO::getTenantId, scopedTenantId)
                .eq(BiPublishApprovalDO::getWorkspaceId, workspace.getId())
                .eq(BiPublishApprovalDO::getId, command.approvalId())
                .last("LIMIT 1"));
        if (row == null) {
            throw new IllegalArgumentException("BI publish approval not found: " + command.approvalId());
        }
        if (!STATUS_PENDING.equalsIgnoreCase(row.getStatus())) {
            throw new IllegalStateException("BI publish approval is not pending: " + command.approvalId());
        }
        row.setStatus(reviewStatus(command.status()));
        row.setReviewedBy(username(username));
        row.setReviewedAt(now());
        row.setReviewComment(optionalText(command.reviewComment(), "reviewComment"));
        approvalMapper.updateById(row);
        return toView(row);
    }

    public List<BiPublishApprovalView> listApprovals(Long tenantId,
                                                     String resourceType,
                                                     String resourceKey,
                                                     String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        LambdaQueryWrapper<BiPublishApprovalDO> query = new LambdaQueryWrapper<BiPublishApprovalDO>()
                .eq(BiPublishApprovalDO::getTenantId, scopedTenantId)
                .eq(BiPublishApprovalDO::getWorkspaceId, workspace.getId())
                .orderByDesc(BiPublishApprovalDO::getRequestedAt)
                .orderByDesc(BiPublishApprovalDO::getId);
        if (resourceType != null && !resourceType.isBlank()) {
            query.eq(BiPublishApprovalDO::getResourceType, normalizeResourceType(resourceType));
        }
        if (resourceKey != null && !resourceKey.isBlank()) {
            query.eq(BiPublishApprovalDO::getResourceKey, resourceKey(resourceKey));
        }
        if (status != null && !status.isBlank()) {
            query.eq(BiPublishApprovalDO::getStatus, normalizeStatus(status));
        }
        return safeList(approvalMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    public BiPublishApprovalView requireApprovedApproval(Long tenantId,
                                                         Long workspaceId,
                                                         String resourceType,
                                                         String resourceKey,
                                                         LocalDateTime resourceUpdatedAt) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedResourceType = normalizeResourceType(resourceType);
        String normalizedResourceKey = resourceKey(resourceKey);
        BiPublishApprovalDO row = approvalMapper.selectOne(new LambdaQueryWrapper<BiPublishApprovalDO>()
                .eq(BiPublishApprovalDO::getTenantId, scopedTenantId)
                .eq(BiPublishApprovalDO::getWorkspaceId, workspaceId)
                .eq(BiPublishApprovalDO::getResourceType, normalizedResourceType)
                .eq(BiPublishApprovalDO::getResourceKey, normalizedResourceKey)
                .eq(BiPublishApprovalDO::getStatus, STATUS_APPROVED)
                .orderByDesc(BiPublishApprovalDO::getReviewedAt)
                .orderByDesc(BiPublishApprovalDO::getRequestedAt)
                .orderByDesc(BiPublishApprovalDO::getId)
                .last("LIMIT 1"));
        if (row == null) {
            throw new BiPublishApprovalRequiredException(
                    "approved BI publish approval is required for "
                            + normalizedResourceType + "/" + normalizedResourceKey);
        }
        LocalDateTime approvedAt = row.getReviewedAt() == null ? row.getRequestedAt() : row.getReviewedAt();
        if (resourceUpdatedAt != null && (approvedAt == null || approvedAt.isBefore(resourceUpdatedAt))) {
            throw new BiPublishApprovalRequiredException(
                    "stale BI publish approval for " + normalizedResourceType + "/" + normalizedResourceKey);
        }
        return toView(row);
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

    private BiPublishApprovalView toView(BiPublishApprovalDO row) {
        return new BiPublishApprovalView(
                row.getId(),
                row.getTenantId(),
                row.getWorkspaceId(),
                row.getResourceType(),
                row.getResourceKey(),
                row.getStatus(),
                row.getReason(),
                row.getRequestedBy(),
                row.getRequestedAt(),
                row.getReviewedBy(),
                row.getReviewedAt(),
                row.getReviewComment());
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

    private String normalizeStatus(String status) {
        String value = required(status, "status").toUpperCase(Locale.ROOT);
        if (STATUS_PENDING.equals(value) || STATUS_APPROVED.equals(value) || STATUS_REJECTED.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI publish approval status: " + status);
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
        String user = value == null || value.isBlank() ? "system" : value.trim();
        if (user.length() > 128) {
            throw new IllegalArgumentException("username is too long");
        }
        return user;
    }

    private String optionalText(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() > 512) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return text;
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

    public static class BiPublishApprovalRequiredException extends IllegalStateException {
        public BiPublishApprovalRequiredException(String message) {
            super(message);
        }
    }
}
