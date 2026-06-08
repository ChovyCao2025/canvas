package org.chovy.canvas.domain.bi.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.BiBigScreenDO;
import org.chovy.canvas.dal.dataobject.BiChartDO;
import org.chovy.canvas.dal.dataobject.BiDashboardDO;
import org.chovy.canvas.dal.dataobject.BiDatasetDO;
import org.chovy.canvas.dal.dataobject.BiPortalDO;
import org.chovy.canvas.dal.dataobject.BiResourceCommentDO;
import org.chovy.canvas.dal.dataobject.BiResourceLockDO;
import org.chovy.canvas.dal.dataobject.BiSpreadsheetDO;
import org.chovy.canvas.dal.dataobject.BiWorkspaceDO;
import org.chovy.canvas.dal.mapper.BiBigScreenMapper;
import org.chovy.canvas.dal.mapper.BiChartMapper;
import org.chovy.canvas.dal.mapper.BiDashboardMapper;
import org.chovy.canvas.dal.mapper.BiDatasetMapper;
import org.chovy.canvas.dal.mapper.BiPortalMapper;
import org.chovy.canvas.dal.mapper.BiResourceCommentMapper;
import org.chovy.canvas.dal.mapper.BiResourceLockMapper;
import org.chovy.canvas.dal.mapper.BiSpreadsheetMapper;
import org.chovy.canvas.dal.mapper.BiWorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * BiResourceCollaborationService 编排 domain.bi.resource 场景的领域业务规则。
 */
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
    private final BiBigScreenMapper bigScreenMapper;
    private final BiSpreadsheetMapper spreadsheetMapper;
    private final BiResourceCommentMapper commentMapper;
    private final BiResourceLockMapper lockMapper;
    private final Clock clock;

    /**
     * 创建 BiResourceCollaborationService 实例并注入 domain.bi.resource 场景依赖。
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param commentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lockMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public BiResourceCollaborationService(BiWorkspaceMapper workspaceMapper,
                                          BiDatasetMapper datasetMapper,
                                          BiDashboardMapper dashboardMapper,
                                          BiChartMapper chartMapper,
                                          BiPortalMapper portalMapper,
                                          BiBigScreenMapper bigScreenMapper,
                                          BiSpreadsheetMapper spreadsheetMapper,
                                          BiResourceCommentMapper commentMapper,
                                          BiResourceLockMapper lockMapper) {
        this(workspaceMapper, datasetMapper, dashboardMapper, chartMapper, portalMapper,
                bigScreenMapper, spreadsheetMapper,
                commentMapper, lockMapper, Clock.systemUTC());
    }

    /**
     * 执行 BiResourceCollaborationService 流程，围绕 bi resource collaboration service 完成校验、计算或结果组装。
     *
     * @param workspaceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param datasetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param dashboardMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param chartMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param portalMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param bigScreenMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param spreadsheetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param commentMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param lockMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    BiResourceCollaborationService(BiWorkspaceMapper workspaceMapper,
                                   BiDatasetMapper datasetMapper,
                                   BiDashboardMapper dashboardMapper,
                                   BiChartMapper chartMapper,
                                   BiPortalMapper portalMapper,
                                   BiBigScreenMapper bigScreenMapper,
                                   BiSpreadsheetMapper spreadsheetMapper,
                                   BiResourceCommentMapper commentMapper,
                                   BiResourceLockMapper lockMapper,
                                   Clock clock) {
        this.workspaceMapper = workspaceMapper;
        this.datasetMapper = datasetMapper;
        this.dashboardMapper = dashboardMapper;
        this.chartMapper = chartMapper;
        this.portalMapper = portalMapper;
        this.bigScreenMapper = bigScreenMapper;
        this.spreadsheetMapper = spreadsheetMapper;
        this.commentMapper = commentMapper;
        this.lockMapper = lockMapper;
        this.clock = clock;
    }

    /**
     * 新增 BI 资源协作评论，记录操作者和资源定位信息。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiResourceCommentView addComment(Long tenantId, String username, BiResourceCommentCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        commentMapper.insert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toCommentView(row);
    }

    /**
     * 查询当前租户下符合条件的 BI 资源列表，过滤已归档或不可见数据并按业务更新时间返回。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @return 指定 BI 资源的协作评论列表
     */
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

    /**
     * 删除协作评论，并校验评论归属或操作者权限。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param commentId 评论主键，用于定位要删除的协作评论
     */
    public void deleteComment(Long tenantId, String username, Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("commentId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String currentUser = username(username);
        BiWorkspaceDO workspace = defaultWorkspace(scopedTenantId);
        commentMapper.softDelete(scopedTenantId, workspace.getId(), commentId, currentUser, now());
    }

    /**
     * 获取 BI 资源编辑锁，避免多人同时修改同一草稿造成覆盖。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiResourceLockView acquireLock(Long tenantId, String username, BiResourceLockCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        int changed = lockMapper.acquire(row);
        if (changed <= 0) {
            BiResourceLockDO current = lockMapper.selectCurrent(scopedTenantId, workspace.getId(), resourceType, resourceKey);
            if (current != null && current.getExpiresAt() != null && current.getExpiresAt().isAfter(lockedAt)) {
                throw new IllegalStateException("BI resource is locked by " + current.getLockedBy());
            }
            throw new IllegalStateException("BI resource lock could not be acquired");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toLockView(row, true);
    }

    /**
     * 读取 BI 资源当前编辑锁，用于前端提示锁持有人和过期时间。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @return 用于前端展示或管理端审计的业务视图
     */
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

    /**
     * 校验当前操作是否持有有效编辑锁，不满足时阻断保存或版本恢复。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param workspaceId 工作空间标识，用于限定资源生命周期和发布审批范围
     * @param resourceType BI 资源类型，例如 DASHBOARD、CHART、DATASET 或 PORTAL
     * @param resourceKey BI 资源业务键，用于定位权限、收藏、审批和协作记录
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param lockToken 编辑锁令牌，用于确认当前保存或恢复操作仍持有有效锁
     * @return 用于前端展示或管理端审计的业务视图
     */
    public BiResourceLockView requireCurrentLock(Long tenantId,
                                                 Long workspaceId,
                                                 String resourceType,
                                                 String resourceKey,
                                                 String username,
                                                 String lockToken) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String type = normalizeResourceType(resourceType);
        String key = resourceKey(resourceKey);
        String currentUser = username(username);
        String token = lockToken(lockToken, true);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toLockView(current, true);
    }

    /**
     * 释放 BI 资源编辑锁，使其他协作者可以进入编辑流程。
     *
     * @param tenantId 租户标识，用于限定 BI 资源、权限和审计数据的隔离范围
     * @param username 当前操作人账号，用于权限校验、锁持有人判断和审计记录
     * @param command 业务操作命令，包含本次请求需要写入或校验的字段
     */
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

    /**
     * 转换为接口返回或领域视图。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 将协作锁数据行转换为运行态视图。
     *
     * <p>locked 由调用方基于过期时间和令牌状态计算，避免把历史锁记录误展示为仍占用资源。</p>
     */
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
     * 执行 widgetKey 流程，围绕 widget key 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 widget key 生成的文本或业务键。
     */
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

    /**
     * 执行 commentText 流程，围绕 comment text 完成校验、计算或结果组装。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 comment text 生成的文本或业务键。
     */
    private String commentText(String value) {
        String text = required(value, "commentText");
        if (text.length() > 4000) {
            throw new IllegalArgumentException("commentText is too long");
        }
        return text;
    }

    /**
     * 校验协作锁令牌。
     *
     * <p>创建锁时允许自动生成 UUID；释放或续租锁时必须提供原令牌，并限制字符集以避免日志和审计字段污染。</p>
     */
    private String lockToken(String value, boolean required) {
        // 准备本次处理所需的上下文和中间变量。
        String token = value == null ? "" : value.trim();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (token.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("lockToken is required");
            }
            return UUID.randomUUID().toString();
        }
        if (!LOCK_TOKEN.matcher(token).matches()) {
            throw new IllegalArgumentException("lockToken contains unsafe characters");
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return token;
    }

    /**
     * 归一化协作锁 TTL。
     *
     * <p>TTL 限制在 30 秒到 1 小时之间，既避免短锁频繁抖动，也避免编辑锁长期占用资源。</p>
     */
    private int lockTtlSeconds(Integer value) {
        int ttl = value == null ? DEFAULT_LOCK_TTL_SECONDS : value;
        if (ttl < 30 || ttl > 3600) {
            throw new IllegalArgumentException("ttlSeconds must be between 30 and 3600");
        }
        return ttl;
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
     * BiResourceLockRequiredException 承载对应领域的业务规则、流程编排和结果转换。
     */
    public static class BiResourceLockRequiredException extends IllegalStateException {
        /**
         * 创建 BiResourceLockRequiredException 实例并注入 domain.bi.resource 场景依赖。
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         */
        public BiResourceLockRequiredException(String message) {
            super(message);
        }
    }
}
