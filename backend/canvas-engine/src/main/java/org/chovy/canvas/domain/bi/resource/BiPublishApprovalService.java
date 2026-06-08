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

/**
 * BiPublishApprovalService 编排 domain.bi.resource 场景的领域业务规则。
 */
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

    /**
     * 创建 BiPublishApprovalService 实例并注入 domain.bi.resource 场景依赖。
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param approvalMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
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

    /**
     * 执行 BiPublishApprovalService 流程，围绕 bi publish approval service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param approvalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 提交 BI 资源发布审批，锁定资源版本和更新时间以支持发布前管控。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiPublishApprovalView requestApproval(Long tenantId,
                                                 String username,
                                                 BiPublishApprovalRequestCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("BI publish approval request is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String resourceType = normalizeResourceType(command.resourceType());
        String resourceKey = resourceKey(command.resourceKey());
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        assertResourceExists(scopedTenantId, workspace.getId(), resourceType, resourceKey);

        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 审核 BI 资源发布申请，根据审批结论推进发布准入或驳回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiPublishApprovalView reviewApproval(Long tenantId,
                                                String username,
                                                BiPublishApprovalReviewCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null || command.approvalId() == null || command.approvalId() <= 0) {
            throw new IllegalArgumentException("approvalId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @param status 资源、审批、请求或队列状态过滤条件
     * @return 符合过滤条件的发布审批记录列表
     */
    public List<BiPublishApprovalView> listApprovals(Long tenantId,
                                                     String resourceType,
                                                     String resourceKey,
                                                     String status) {
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        LambdaQueryWrapper<BiPublishApprovalDO> query = new LambdaQueryWrapper<BiPublishApprovalDO>()
                .eq(BiPublishApprovalDO::getTenantId, scopedTenantId)
                .eq(BiPublishApprovalDO::getWorkspaceId, workspace.getId())
                .orderByDesc(BiPublishApprovalDO::getRequestedAt)
                .orderByDesc(BiPublishApprovalDO::getId);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (resourceType != null && !resourceType.isBlank()) {
            query.eq(BiPublishApprovalDO::getResourceType, normalizeResourceType(resourceType));
        }
        if (resourceKey != null && !resourceKey.isBlank()) {
            query.eq(BiPublishApprovalDO::getResourceKey, resourceKey(resourceKey));
        }
        if (status != null && !status.isBlank()) {
            query.eq(BiPublishApprovalDO::getStatus, normalizeStatus(status));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(approvalMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 校验资源当前版本是否已有有效发布审批，未通过时阻断发布。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param workspaceId 工作空间标识，用于限定资源生命周期和发布审批范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @param resourceUpdatedAt 资源最近更新时间，用于判断审批是否仍覆盖当前版本
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiPublishApprovalView requireApprovedApproval(Long tenantId,
                                                         Long workspaceId,
                                                         String resourceType,
                                                         String resourceKey,
                                                         LocalDateTime resourceUpdatedAt) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String normalizedResourceType = normalizeResourceType(resourceType);
        String normalizedResourceKey = resourceKey(resourceKey);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param workspaceId 业务对象 ID，用于定位具体记录。
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @param resourceKey 业务键，用于在同一租户下定位资源。
     */
    private void assertResourceExists(Long tenantId, Long workspaceId, String resourceType, String resourceKey) {
        // 准备本次处理所需的上下文和中间变量。
        Object row = switch (resourceType) {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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

    /**
     * 执行 resourceStatus 流程，围绕 resource status 完成校验、计算或结果组装。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 resource status 生成的文本或业务键。
     */
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

    /**
     * 按默认值规则处理输入值。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 defaultWorkspace 流程生成的业务结果。
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 规范化输入值。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeResourceType(String resourceType) {
        String value = required(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if ("DATASET".equals(value) || "DASHBOARD".equals(value)
                || "CHART".equals(value) || "PORTAL".equals(value)
                || "BIG_SCREEN".equals(value) || "SPREADSHEET".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
    }

    /**
     * 规范化输入值。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        String value = required(status, "status").toUpperCase(Locale.ROOT);
        if (STATUS_PENDING.equals(value) || STATUS_APPROVED.equals(value) || STATUS_REJECTED.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("unsupported BI publish approval status: " + status);
    }

    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 review status 生成的文本或业务键。
     */
    private String reviewStatus(String status) {
        String value = normalizeStatus(status);
        if (STATUS_PENDING.equals(value)) {
            throw new IllegalArgumentException("review status must be APPROVED or REJECTED");
        }
        return value;
    }

    /**
     * 执行 resourceKey 流程，围绕 resource key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 resource key 生成的文本或业务键。
     */
    private String resourceKey(String value) {
        String key = required(value, "resourceKey");
        if (!RESOURCE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("resourceKey contains unsafe characters");
        }
        return key;
    }

    /**
     * 解析操作人标识。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 username 生成的文本或业务键。
     */
    private String username(String value) {
        String user = value == null || value.isBlank() ? "system" : value.trim();
        if (user.length() > 128) {
            throw new IllegalArgumentException("username is too long");
        }
        return user;
    }

    /**
     * 执行 optionalText 流程，围绕 optional text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param field 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 optional text 生成的文本或业务键。
     */
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
     * 执行 now 流程，围绕 now 完成校验、计算或结果组装。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * BiPublishApprovalRequiredException 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class BiPublishApprovalRequiredException extends IllegalStateException {
        /**
         * 创建 BiPublishApprovalRequiredException 实例并注入 domain.bi.resource 场景依赖。
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         */
        public BiPublishApprovalRequiredException(String message) {
            super(message);
        }
    }
}
