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

/**
 * BiPermissionRequestService 编排 domain.bi.permission 场景的领域业务规则。
 */
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

    /**
     * 创建 BiPermissionRequestService 实例并注入 domain.bi.permission 场景依赖。
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param requestMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiPermissionRequestService(BiWorkspaceMapper workspaceMapper,
                                      BiPermissionRequestMapper requestMapper,
                                      BiPermissionAdminService permissionAdminService) {
        this(workspaceMapper, requestMapper, permissionAdminService, Clock.systemUTC());
    }

    /**
     * 执行 BiPermissionRequestService 流程，围绕 bi permission request service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param requestMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param permissionAdminService 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    BiPermissionRequestService(BiWorkspaceMapper workspaceMapper,
                               BiPermissionRequestMapper requestMapper,
                               BiPermissionAdminService permissionAdminService,
                               Clock clock) {
        this.workspaceMapper = workspaceMapper;
        this.requestMapper = requestMapper;
        this.permissionAdminService = permissionAdminService;
        this.clock = clock;
    }

    /**
     * 提交 BI 资源权限申请，记录申请范围和理由供管理员审核。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
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

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @param status 资源、审批、请求或队列状态过滤条件
     * @return 符合过滤条件的权限申请列表
     */
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
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (hasText(resourceType)) {
            query.eq(BiPermissionRequestDO::getResourceType, normalizeResourceType(resourceType));
        }
        if (hasText(resourceKey)) {
            query.eq(BiPermissionRequestDO::getResourceKey, resourceKey(resourceKey));
        }
        if (hasText(status)) {
            query.eq(BiPermissionRequestDO::getStatus, normalizeStatus(status));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return safeList(requestMapper.selectList(query)).stream()
                .map(this::toView)
                .toList();
    }

    /**
     * 审核 BI 权限申请，按审批结论写入资源权限或保留驳回原因。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiPermissionRequestView reviewPermissionRequest(Long tenantId,
                                                           String username,
                                                           BiPermissionRequestReviewCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null || command.requestId() == null || command.requestId() <= 0) {
            throw new IllegalArgumentException("requestId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toView(row);
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

    /**
     * 规范化输入值。
     *
     * @param resourceType 类型标识，用于选择对应处理分支。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeResourceType(String resourceType) {
        String value = required(resourceType, "resourceType").toUpperCase(Locale.ROOT);
        if (!RESOURCE_TYPES.contains(value)) {
            throw new IllegalArgumentException("unsupported BI resource type: " + resourceType);
        }
        return value;
    }

    /**
     * 规范化输入值。
     *
     * @param actionKey 业务键，用于在同一租户下定位资源。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeAction(String actionKey) {
        String value = required(actionKey, "requestedAction").toUpperCase(Locale.ROOT);
        if (!ACTION_KEYS.contains(value)) {
            throw new IllegalArgumentException("unsupported BI permission action: " + actionKey);
        }
        return value;
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
        throw new IllegalArgumentException("unsupported BI permission request status: " + status);
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
        String user = hasText(value) ? value.trim() : "system";
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
        if (!hasText(value)) {
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
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 判断业务条件是否成立。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
}
